package com.shinchoku.shinchokuapp.dto;

import java.util.List;

/** A cursor page; its size is not a total count. */
public record PageResponse<T>(List<T> items, String nextCursor) {}
