package com.shinchoku.shinchokuapp.api;

/**
 * An expected application error with an HTTP status and a stable public error code. Errors
 * translates it into the shared JSON error envelope.
 */
public class ApiException extends RuntimeException {

  public final int status;

  public final String code;

  public ApiException(int status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public static ApiException missing() {
    return new ApiException(404, "NOT_FOUND", "Resource not found");
  }

  public static ApiException forbidden() {
    return new ApiException(403, "FORBIDDEN", "This action is not permitted");
  }

  public static ApiException invalid(String message) {
    return new ApiException(422, "VALIDATION_ERROR", message);
  }

  public static ApiException conflict(String message) {
    return new ApiException(409, "CONFLICT", message);
  }
}
