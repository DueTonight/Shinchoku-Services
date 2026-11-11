package com.shinchoku.shinchokuapp.dto;

import com.fasterxml.jackson.annotation.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Validated API input. */
public record IssueCreateRequest(
    @NotNull UUID teamId,
    @NotNull String title,
    String description,
    IssueState state,
    IssuePriority priority,
    UUID assigneeId,
    UUID projectId,
    UUID cycleId,
    LocalDate dueOn,
    List<UUID> labelIds) {}
