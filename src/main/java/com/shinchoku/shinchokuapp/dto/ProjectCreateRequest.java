package com.shinchoku.shinchokuapp.dto;

import com.fasterxml.jackson.annotation.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Validated API input. */
public record ProjectCreateRequest(
    @NotNull UUID teamId,
    @NotNull String name,
    String description,
    ProjectState state,
    UUID leadId,
    LocalDate plannedStart,
    LocalDate targetDate,
    String planningNotes) {}
