package com.shinchoku.shinchokuapp.dto;

import com.shinchoku.shinchokuapp.api.ApiException;

public record CursorRequest(String cursor, int limit) {

  public CursorRequest {
    if (limit < 1 || limit > 100) {
      throw new ApiException(422, "VALIDATION_ERROR", "limit is outside the allowed range");
    }
  }
}
