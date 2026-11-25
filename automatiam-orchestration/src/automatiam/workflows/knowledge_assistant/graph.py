"""The ``iam.knowledge-assistant`` StateGraph.

Deterministic and read-only: it retrieves, generates from approved evidence, validates grounding
and answers. It holds no tool that can change anything in IAM.
"""

from __future__ import annotations

from langgraph.checkpoint.base import BaseCheckpointSaver
from langgraph.graph import END, START, StateGraph
from langgraph.graph.state import CompiledStateGraph

from automatiam.core.security import ExecutionContext
from automatiam.orchestration.state import KnowledgeAssistantState
from automatiam.workflows.knowledge_assistant.nodes import (
    NodeDependencies,
    assemble_response,
    controlled_failure,
    evaluate_evidence,
    knowledge_gap_response,
    make_generate_answer,
    make_retrieve_knowledge,
    request_clarification,
    resolve_question,
    route_on_assessment,
    validate_grounding,
)


def build_knowledge_assistant_graph(
    dependencies: NodeDependencies,
    checkpointer: BaseCheckpointSaver | None = None,
) -> CompiledStateGraph:
    """
    Build and compile the graph.

    ``context_schema`` carries the execution context as LangGraph runtime context rather than as
    state, which is what keeps identity out of the checkpoint.
    """
    graph: StateGraph = StateGraph(KnowledgeAssistantState, context_schema=ExecutionContext)

    graph.add_node("resolve_question", resolve_question)
    graph.add_node("retrieve_knowledge", make_retrieve_knowledge(dependencies))
    graph.add_node("evaluate_evidence", evaluate_evidence)
    graph.add_node("generate_answer", make_generate_answer(dependencies))
    graph.add_node("validate_grounding", validate_grounding)
    graph.add_node("request_clarification", request_clarification)
    graph.add_node("knowledge_gap_response", knowledge_gap_response)
    graph.add_node("controlled_failure", controlled_failure)
    graph.add_node("assemble_response", assemble_response)

    graph.add_edge(START, "resolve_question")
    graph.add_edge("resolve_question", "retrieve_knowledge")
    graph.add_edge("retrieve_knowledge", "evaluate_evidence")

    graph.add_conditional_edges(
        "evaluate_evidence",
        route_on_assessment,
        {
            "generate_answer": "generate_answer",
            "request_clarification": "request_clarification",
            "knowledge_gap_response": "knowledge_gap_response",
            "controlled_failure": "controlled_failure",
        },
    )

    graph.add_edge("generate_answer", "validate_grounding")
    # Grounding failure is a failure, not a lower-confidence answer: it routes to the controlled
    # failure path rather than releasing text that could not be checked.
    graph.add_conditional_edges(
        "validate_grounding",
        _route_after_grounding,
        {"assemble_response": "assemble_response", "controlled_failure": "controlled_failure"},
    )

    graph.add_edge("request_clarification", "assemble_response")
    graph.add_edge("knowledge_gap_response", "assemble_response")
    graph.add_edge("controlled_failure", "assemble_response")
    graph.add_edge("assemble_response", END)

    return graph.compile(checkpointer=checkpointer, name="iam.knowledge-assistant")


def _route_after_grounding(state: KnowledgeAssistantState) -> str:
    return "assemble_response" if state.get("grounding_verified") else "controlled_failure"
