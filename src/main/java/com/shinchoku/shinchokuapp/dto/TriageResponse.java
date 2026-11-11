package com.shinchoku.shinchokuapp.dto;

import com.fasterxml.jackson.annotation.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Public API output; persistence entities and credential hashes are never serialized. */
public record TriageResponse(
    UUID id,
    UUID sourceTeamId,
    UUID targetTeamId,
    UUID requesterId,
    String title,
    String description,
    TriageState state,
    UUID decidedBy,
    Instant decidedAt,
    String decisionReason,
    UUID acceptedIssueId,
    Instant createdAt) {}
