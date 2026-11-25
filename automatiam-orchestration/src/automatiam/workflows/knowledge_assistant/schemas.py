"""Typed input and output for ``iam.knowledge-assistant``."""

from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field


class KnowledgeAssistantInput(BaseModel):
    """What a caller supplies. Identity is not here: it comes from the security context."""

    model_config = ConfigDict(extra="forbid")

    message: str = Field(min_length=1, max_length=2000)


class CitationOut(BaseModel):
    chunk_id: str = Field(serialization_alias="chunkId")
    article_number: str | None = Field(default=None, serialization_alias="articleNumber")
    title: str | None = None
    citation_url: str | None = Field(default=None, serialization_alias="citationUrl")


class KnowledgeAssistantOutput(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    answer: str
    citations: list[CitationOut] = Field(default_factory=list)
    assessment_outcome: str = Field(serialization_alias="assessmentOutcome")
    grounding_verified: bool = Field(serialization_alias="groundingVerified")
    grounding_method: str | None = Field(default=None, serialization_alias="groundingMethod")
    error_category: str | None = Field(default=None, serialization_alias="errorCategory")
