package com.shinchoku.shinchokuapp.dto;

import com.fasterxml.jackson.annotation.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Public API output; persistence entities and credential hashes are never serialized. */
public record InsightsResponse(
    UUID teamId,
    UUID cycleId,
    Long total,
    Long done,
    Long inProgress,
    Long blocked,
    Double completionPercent,
    List<InsightPointResponse> series,
    InsightSummaryResponse summary) {}
