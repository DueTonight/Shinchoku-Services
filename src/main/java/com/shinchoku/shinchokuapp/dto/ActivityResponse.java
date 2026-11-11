package com.shinchoku.shinchokuapp.dto;

import com.fasterxml.jackson.annotation.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Public API output; persistence entities and credential hashes are never serialized. */
public record ActivityResponse(
    UUID id,
    UUID issueId,
    UUID actorId,
    ActivityKind kind,
    Map<String, Object> changes,
    Instant occurredAt) {}
