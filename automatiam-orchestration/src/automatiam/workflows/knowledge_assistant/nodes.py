"""Graph nodes for ``iam.knowledge-assistant``.

Every node is async, takes typed state and the runtime context, and returns only the fields it
owns. Identity lives in the runtime context and never enters state, because state is
checkpointed.
"""

from __future__ import annotations

import logging
import re
import time
from collections.abc import Awaitable, Callable
from dataclasses import dataclass

from langgraph.runtime import Runtime

from automatiam.api.errors import ErrorCategory
from automatiam.core.logging import log_fields
from automatiam.core.security import ExecutionContext
from automatiam.integrations.ehap.client import EhapClient
from automatiam.integrations.ehap.models import GenerationRequest
from automatiam.integrations.knowledge.client import KnowledgeClient
from automatiam.integrations.knowledge.models import (
    AssessmentOutcome,
    KnowledgeScope,
    KnowledgeSearchRequest,
    RetrievalOptions,
)
from automatiam.orchestration.state import Citation, EvidenceRef, KnowledgeAssistantState
from automatiam.workflows.knowledge_assistant.prompts import (
    CONTROLLED_FAILURE_RESPONSE,
    FAILURE_RESPONSES,
    SYSTEM_INSTRUCTION,
)

logger = logging.getLogger(__name__)

# Bounded so a long article cannot grow the checkpoint without limit.
MAX_EXCERPT_CHARACTERS = 1200
MAX_EVIDENCE_PASSAGES = 8
# A resolved question longer than this is a sign the resolver concatenated history rather than
# resolving a reference.
MAX_RESOLVED_QUESTION_CHARACTERS = 2000


@dataclass(slots=True)
class NodeDependencies:
    """Injected rather than imported, so nodes are testable without patching module globals."""

    knowledge_client: KnowledgeClient
    ehap_client: EhapClient
    max_conversation_messages: int = 10


def _excerpt(text: str) -> str:
    return text[:MAX_EXCERPT_CHARACTERS]


async def resolve_question(
    state: KnowledgeAssistantState, runtime: Runtime[ExecutionContext]
) -> dict:
    """
    Resolve references against bounded conversation context, preserving intent.

    Resolution here is deliberately mechanical: it appends the last user turn when the question
    is short and reference-shaped ("what about it", "and for that one"). It does not invent an
    application, identity or procedure, and it does not call a model - a model asked to rewrite a
    question will happily supply a plausible missing noun, which is the failure this node exists
    to avoid.
    """
    question = (state.get("question") or "").strip()
    if not question:
        return {
            "resolved_question": "",
            "clarification_needed": True,
            "status": "running",
        }

    messages = state.get("messages") or []
    resolved = question
    if _looks_like_a_reference(question) and messages:
        previous = next(
            (message for message in reversed(messages) if message["role"] == "user"), None
        )
        if previous is not None:
            resolved = f"{previous['content'].strip()} {question}"

    return {
        "resolved_question": resolved[:MAX_RESOLVED_QUESTION_CHARACTERS],
        "clarification_needed": False,
    }


def _looks_like_a_reference(question: str) -> bool:
    """Short questions leaning on a pronoun or bare continuation need the prior turn."""
    if len(question.split()) > 8:
        return False
    return bool(re.search(r"\b(it|that|those|them|this|the same)\b", question, re.IGNORECASE))


# A node as LangGraph sees it: state and runtime in, the fields the node owns out.
GraphNode = Callable[[KnowledgeAssistantState, "Runtime[ExecutionContext]"], Awaitable[dict]]


def make_retrieve_knowledge(dependencies: NodeDependencies) -> GraphNode:
    async def retrieve_knowledge(
        state: KnowledgeAssistantState, runtime: Runtime[ExecutionContext]
    ) -> dict:
        """Call the Knowledge API. No OpenSearch, scoring or embedding happens here."""
        context = runtime.context
        started = time.monotonic()
        request = KnowledgeSearchRequest(
            query=state.get("resolved_question") or state.get("question", ""),
            scope=KnowledgeScope(),
            options=RetrievalOptions(evaluate_evidence=True),
            max_results=MAX_EVIDENCE_PASSAGES,
        )
        try:
            response = await dependencies.knowledge_client.search(request, context.correlation_id)
        except Exception as error:  # noqa: BLE001 - translated into a routable outcome
            logger.error(
                "Knowledge retrieval failed",
                extra=log_fields(
                    errorType=type(error).__name__,
                    errorCategory=ErrorCategory.KNOWLEDGE_UNAVAILABLE,
                    knowledgeLatencyMs=int((time.monotonic() - started) * 1000),
                ),
            )
            return {
                "assessment_outcome": AssessmentOutcome.RETRIEVAL_ERROR,
                "assessment_reason": "knowledge_api_unavailable",
                "evidence": [],
                "approved_evidence_ids": [],
            }

        # Only approved evidence is carried forward. Everything downstream, including generation,
        # sees the approved subset and nothing else.
        approved = response.approved_evidence()
        evidence: list[EvidenceRef] = [
            EvidenceRef(
                chunk_id=item.chunk_id,
                document_id=item.document_id,
                article_number=item.article_number,
                title=item.title,
                section_path=item.section_path,
                excerpt=_excerpt(item.text),
                citation_url=item.citation_url,
            )
            for item in approved
        ]
        return {
            "evidence": evidence,
            "approved_evidence_ids": list(response.assessment.approved_evidence_ids),
            "assessment_outcome": response.assessment.outcome,
            "assessment_reason": response.assessment.reason_code,
            "evidence_policy_version": response.assessment.policy_version,
        }

    return retrieve_knowledge


async def evaluate_evidence(
    state: KnowledgeAssistantState, runtime: Runtime[ExecutionContext]
) -> dict:
    """
    Consume the Knowledge API's assessment.

    No scoring happens here. Evidence quality is that service's responsibility, and recomputing
    it would create a second opinion that could disagree with the one the policy produced.
    """
    outcome = state.get("assessment_outcome") or AssessmentOutcome.RETRIEVAL_ERROR
    logger.info(
        "Evidence assessment consumed",
        extra=log_fields(
            assessmentOutcome=outcome,
            evidenceCount=len(state.get("evidence") or []),
            evidencePolicyVersion=state.get("evidence_policy_version"),
        ),
    )
    return {"assessment_outcome": outcome}


def route_on_assessment(state: KnowledgeAssistantState) -> str:
    """Branch on the assessment. Unknown outcomes take the failure path rather than a happy one."""
    outcome = state.get("assessment_outcome")
    if outcome == AssessmentOutcome.SUFFICIENT:
        return "generate_answer"
    if outcome == AssessmentOutcome.AMBIGUOUS:
        return "request_clarification"
    if outcome == AssessmentOutcome.INSUFFICIENT:
        return "knowledge_gap_response"
    return "controlled_failure"


def make_generate_answer(dependencies: NodeDependencies) -> GraphNode:
    async def generate_answer(
        state: KnowledgeAssistantState, runtime: Runtime[ExecutionContext]
    ) -> dict:
        """
        Generate from approved evidence through EHAP.

        Evidence is passed in its own field rather than concatenated into the instruction, so the
        adapter can present it as data. Retrieved articles are untrusted input.
        """
        evidence = state.get("evidence") or []
        if not evidence:
            return {
                "assessment_outcome": AssessmentOutcome.INSUFFICIENT,
                "draft_answer": "",
            }

        blocks = [
            f"[{item['chunk_id']}] {item['title'] or ''}\n{item['excerpt']}" for item in evidence
        ]
        request = GenerationRequest(
            instruction=SYSTEM_INSTRUCTION,
            question=state.get("resolved_question") or state.get("question", ""),
            evidence_blocks=blocks,
        )
        try:
            result = await dependencies.ehap_client.generate(
                request, runtime.context.correlation_id
            )
        except Exception as error:  # noqa: BLE001 - translated into a routable outcome
            logger.error(
                "Answer generation failed",
                extra=log_fields(
                    errorType=type(error).__name__, errorCategory=ErrorCategory.MODEL_UNAVAILABLE
                ),
            )
            return {
                "assessment_outcome": "EVALUATION_ERROR",
                "error_category": ErrorCategory.MODEL_UNAVAILABLE,
                "draft_answer": "",
            }

        logger.info(
            "Answer generated",
            extra=log_fields(
                ehapLatencyMs=result.latency_ms,
                model=result.model,
                outputTokens=result.usage.output_tokens,
            ),
        )
        return {"draft_answer": result.text}

    return generate_answer


async def validate_grounding(
    state: KnowledgeAssistantState, runtime: Runtime[ExecutionContext]
) -> dict:
    """
    Check the draft answer against approved evidence before any of it is released.

    What this does: confirms there is approved evidence, rejects any citation marker naming a
    chunk that was not approved, and requires measurable lexical overlap with the evidence.

    What this does not do: establish semantic grounding. Overlap is not entailment - a fluent
    sentence can reuse the evidence's vocabulary and still assert something the evidence does not
    support. The fallback is deliberately conservative: anything that fails goes to
    ``controlled_failure`` rather than being released with a warning, and a passing result means
    "not obviously ungrounded", not "verified true". A real entailment check through EHAP is the
    intended successor and is listed as an unresolved integration.
    """
    draft = (state.get("draft_answer") or "").strip()
    approved_ids = set(state.get("approved_evidence_ids") or [])
    evidence = state.get("evidence") or []

    if not draft or not approved_ids:
        # An empty draft is usually generation having failed upstream, not a grounding problem.
        return _grounding_failure(
            "no_draft_or_no_approved_evidence", state.get("error_category")
        )

    cited = set(re.findall(r"\[([A-Za-z0-9._:-]+)\]", draft))
    unsupported = cited - approved_ids
    if unsupported:
        logger.warning(
            "Draft answer cited evidence that was not approved",
            extra=log_fields(
                errorCategory=ErrorCategory.GROUNDING_FAILED,
                unsupportedCitationCount=len(unsupported),
            ),
        )
        return _grounding_failure("unsupported_citation")

    if not _has_lexical_support(draft, evidence):
        return _grounding_failure("insufficient_lexical_overlap")

    citations: list[Citation] = [
        Citation(
            chunk_id=item["chunk_id"],
            article_number=item["article_number"],
            title=item["title"],
            citation_url=item["citation_url"],
        )
        for item in evidence
    ]
    # Citation markers are an internal convention and are stripped before the text is shown.
    answer = re.sub(r"\s*\[[A-Za-z0-9._:-]+\]", "", draft).strip()
    return {
        "answer": answer,
        "citations": citations,
        "grounding_verified": True,
        "grounding_method": "citation_id_and_lexical_overlap",
        "status": "succeeded",
    }


def _grounding_failure(reason: str, existing_category: str | None = None) -> dict:
    """
    Report a grounding failure without erasing a more specific upstream cause.

    ``error_category`` uses the ``keep_last`` reducer, so the last writer wins. When generation
    failed, ``generate_answer`` has already set MODEL_UNAVAILABLE and the draft is empty - which
    then fails grounding for the obvious reason. Overwriting the category here reported a gateway
    outage as GROUNDING_FAILED: the wrong cause, and the wrong message to the user.
    """
    return {
        "answer": "",
        "citations": [],
        "grounding_verified": False,
        "grounding_method": "citation_id_and_lexical_overlap",
        "error_category": existing_category or ErrorCategory.GROUNDING_FAILED,
        "assessment_reason": reason,
    }


def _has_lexical_support(draft: str, evidence: list[EvidenceRef]) -> bool:
    """At least a third of the answer's substantive words must appear in the evidence."""
    corpus = " ".join(item["excerpt"] for item in evidence).lower()
    words = {word for word in re.findall(r"[a-z]{4,}", draft.lower())}
    if not words:
        return False
    supported = sum(1 for word in words if word in corpus)
    return supported / len(words) >= 0.33


async def request_clarification(
    state: KnowledgeAssistantState, runtime: Runtime[ExecutionContext]
) -> dict:
    """Ask for what is missing rather than guessing at it."""
    evidence = state.get("evidence") or []
    citations: list[Citation] = [
        Citation(
            chunk_id=item["chunk_id"],
            article_number=item["article_number"],
            title=item["title"],
            citation_url=item["citation_url"],
        )
        for item in evidence
    ]
    return {
        "answer": (
            "I need a little more detail to answer accurately. Could you say which application or "
            "entitlement you mean, and whether this is standard or privileged access?"
        ),
        "citations": citations,
        "grounding_verified": False,
        "grounding_method": "clarification",
        "status": "succeeded",
    }


async def knowledge_gap_response(
    state: KnowledgeAssistantState, runtime: Runtime[ExecutionContext]
) -> dict:
    """Say that nothing was found. Never fill the gap with an invented procedure."""
    return {
        "answer": (
            "I could not find a knowledge article that answers that. Rather than guess at an IAM "
            "procedure, please raise a ticket with the Identity Governance team."
        ),
        "citations": [],
        "grounding_verified": False,
        "grounding_method": "knowledge_gap",
        "status": "succeeded",
    }


async def controlled_failure(
    state: KnowledgeAssistantState, runtime: Runtime[ExecutionContext]
) -> dict:
    """A safe, structured answer when something upstream failed."""
    category = state.get("error_category") or _category_for(state.get("assessment_outcome"))
    logger.error(
        "Workflow took the controlled failure path",
        extra=log_fields(errorCategory=category, assessmentOutcome=state.get("assessment_outcome")),
    )
    return {
        "answer": FAILURE_RESPONSES.get(category, CONTROLLED_FAILURE_RESPONSE),
        "citations": [],
        "grounding_verified": False,
        "status": "failed",
        "error_category": category,
    }


def _category_for(outcome: str | None) -> str:
    if outcome == AssessmentOutcome.RETRIEVAL_ERROR:
        return ErrorCategory.KNOWLEDGE_UNAVAILABLE
    if outcome == AssessmentOutcome.EVALUATION_ERROR:
        return ErrorCategory.INTERNAL
    return ErrorCategory.INTERNAL


async def assemble_response(
    state: KnowledgeAssistantState, runtime: Runtime[ExecutionContext]
) -> dict:
    """Settle the terminal status. Every branch converges here so there is one exit shape."""
    if state.get("status") in ("succeeded", "failed", "cancelled"):
        return {}
    if state.get("grounding_verified"):
        return {"status": "succeeded"}
    return {
        "status": "failed",
        "error_category": state.get("error_category") or ErrorCategory.GROUNDING_FAILED,
    }
