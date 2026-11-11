package com.shinchoku.shinchokuapp.dto;

import com.fasterxml.jackson.annotation.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Public API output; persistence entities and credential hashes are never serialized. */
public record IssueResponse(
    UUID id,
    UUID teamId,
    String title,
    String description,
    IssueState state,
    IssuePriority priority,
    UUID assigneeId,
    UUID projectId,
    UUID cycleId,
    LocalDate dueOn,
    List<UUID> labelIds,
    String identifier,
    Long number,
    UUID workspaceId,
    UUID creatorId,
    List<LabelResponse> labels,
    GithubLinkResponse github,
    Integer version,
    Instant createdAt,
    Instant updatedAt,
    Instant completedAt,
    Instant archivedAt) {}
