package com.shinchoku.shinchokuapp.api;

import com.shinchoku.shinchokuapp.dto.ErrorResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Converts exceptions into consistent JSON errors. Internal SQL details stay on the server so
 * another workspace's data or constraint names are not exposed.
 */
@RestControllerAdvice
public class Errors {

  public static ErrorResponse envelope(String code, String message, HttpServletRequest request) {
    Object requestId = request.getAttribute("requestId");
    return new ErrorResponse(
        code, message, requestId == null ? UUID.randomUUID().toString() : requestId.toString());
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<?> api(ApiException exception, HttpServletRequest request) {
    return ResponseEntity.status(exception.status)
        .body(envelope(exception.code, exception.getMessage(), request));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<?> integrity(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    // Never disclose SQL, constraint names or another tenant's identifiers.
    return ResponseEntity.status(409)
        .body(
            envelope("CONFLICT", "The change conflicts with existing data or references", request));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    IllegalArgumentException.class
  })
  public ResponseEntity<?> malformed(Exception exception, HttpServletRequest request) {
    return ResponseEntity.badRequest().body(envelope("BAD_REQUEST", "Malformed request", request));
  }

  @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
  public ResponseEntity<?> optimisticLock(Exception exception, HttpServletRequest request) {
    return ResponseEntity.status(409)
        .body(envelope("CONFLICT", "Issue was changed; reload and retry", request));
  }

  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<?> unavailable(Exception exception, HttpServletRequest request) {
    org.slf4j.LoggerFactory.getLogger(Errors.class)
        .error("Database failure for request {}", request.getAttribute("requestId"));
    return ResponseEntity.status(503)
        .body(envelope("SERVICE_UNAVAILABLE", "The database is temporarily unavailable", request));
  }

  @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
  public ResponseEntity<?> notFound(Exception exception, HttpServletRequest request) {
    return ResponseEntity.status(404).body(envelope("NOT_FOUND", "Resource not found", request));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<?> method(Exception exception, HttpServletRequest request) {
    return ResponseEntity.status(405)
        .body(envelope("METHOD_NOT_ALLOWED", "HTTP method not allowed", request));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<?> media(Exception exception, HttpServletRequest request) {
    return ResponseEntity.status(415)
        .body(envelope("UNSUPPORTED_MEDIA_TYPE", "Content-Type must be application/json", request));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> unexpected(Exception exception, HttpServletRequest request) {
    org.slf4j.LoggerFactory.getLogger(Errors.class)
        .error("Unexpected failure for request {}", request.getAttribute("requestId"), exception);
    return ResponseEntity.internalServerError()
        .body(envelope("INTERNAL_ERROR", "An unexpected error occurred", request));
  }
}
