package com.shinchoku.shinchokuapp.dto;

import java.time.Instant;

public record InsightSummaryResponse(String method, String text, Instant generatedAt) {}
