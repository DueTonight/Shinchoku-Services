package com.shinchoku.shinchokuapp.dto;

import com.fasterxml.jackson.annotation.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Validated API input. */
public record SavedViewCreateRequest(
    @NotNull String name,
    @NotNull ViewVisibility visibility,
    @NotNull LabelMatch labelMatch,
    @NotNull List<UUID> labelIds,
    List<UUID> teamIds,
    List<IssueState> states,
    UUID assigneeId,
    String search) {}
