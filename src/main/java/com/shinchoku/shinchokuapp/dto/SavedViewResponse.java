package com.shinchoku.shinchokuapp.dto;

import com.fasterxml.jackson.annotation.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Public API output; persistence entities and credential hashes are never serialized. */
public record SavedViewResponse(
    UUID id,
    String name,
    ViewVisibility visibility,
    LabelMatch labelMatch,
    List<UUID> labelIds,
    List<UUID> teamIds,
    List<IssueState> states,
    UUID assigneeId,
    String search,
    UUID ownerId,
    Long issueCount) {}
