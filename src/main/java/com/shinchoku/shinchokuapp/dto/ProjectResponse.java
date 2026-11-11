package com.shinchoku.shinchokuapp.dto;

import com.fasterxml.jackson.annotation.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Public API output; persistence entities and credential hashes are never serialized. */
public record ProjectResponse(
    UUID id,
    UUID teamId,
    String name,
    String description,
    ProjectState state,
    UUID leadId,
    LocalDate plannedStart,
    LocalDate targetDate,
    String planningNotes,
    Instant archivedAt) {}
