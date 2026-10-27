package com.uz.uzgun_microservices.shared.error;

import com.uz.uzgun_microservices.shared.api.ApiErrorResponse;
import com.uz.uzgun_microservices.shared.api.FieldViolation;
import com.uz.uzgun_microservices.shared.api.GenericResponse;
import com.uz.uzgun_microservices.shared.api.TraceContext;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<GenericResponse<Object>> api(ApiException ex) {
    if (ex.getStatus().is5xxServerError()) log.error("API error {}", ex.getCode(), ex);
    else log.warn("API error {}: {}", ex.getCode(), ex.getMessage());
    return respond(
        ex.getStatus(), ApiErrorResponse.of(ex.getCode(), ex.getMessage(), ex.getPayload()));
  }

  private ResponseEntity<GenericResponse<Object>> respond(
      HttpStatus status, ApiErrorResponse error) {
    var response = ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON);
    if (status == HttpStatus.UNAUTHORIZED) {
      response.header(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    }
    return response.body(GenericResponse.failure(List.of(error), TraceContext.traceId()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<GenericResponse<Object>> validation(MethodArgumentNotValidException ex) {
    List<FieldViolation> fields =
        ex.getBindingResult().getFieldErrors().stream()
            .map(f -> new FieldViolation(f.getField(), f.getDefaultMessage()))
            .toList();
    return respond(
        HttpStatus.BAD_REQUEST,
        ApiErrorResponse.of(ErrorCodes.VALIDATION_FAILED, "Request validation failed", fields));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<GenericResponse<Object>> constraint(ConstraintViolationException ex) {
    List<FieldViolation> fields =
        ex.getConstraintViolations().stream()
            .map(v -> new FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
            .toList();
    return respond(
        HttpStatus.BAD_REQUEST,
        ApiErrorResponse.of(ErrorCodes.VALIDATION_FAILED, "Request validation failed", fields));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<GenericResponse<Object>> malformed(Exception ex) {
    log.warn("Malformed request: {}", ex.getMessage());
    return respond(
        HttpStatus.BAD_REQUEST, ApiErrorResponse.of(ErrorCodes.BAD_REQUEST, "Malformed request"));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<GenericResponse<Object>> unauthorized(AuthenticationException ex) {
    return respond(
        HttpStatus.UNAUTHORIZED,
        ApiErrorResponse.of(ErrorCodes.UNAUTHORIZED, "Authentication required"));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<GenericResponse<Object>> forbidden(AccessDeniedException ex) {
    return respond(
        HttpStatus.FORBIDDEN, ApiErrorResponse.of(ErrorCodes.FORBIDDEN, "Access denied"));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<GenericResponse<Object>> notFound(NoResourceFoundException ex) {
    return respond(
        HttpStatus.NOT_FOUND, ApiErrorResponse.of(ErrorCodes.NOT_FOUND, "Resource not found"));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<GenericResponse<Object>> methodNotAllowed(
      HttpRequestMethodNotSupportedException ex) {
    var response =
        respond(
            HttpStatus.METHOD_NOT_ALLOWED,
            ApiErrorResponse.of(ErrorCodes.METHOD_NOT_ALLOWED, "Method not allowed"));
    var headers = new HttpHeaders();
    headers.putAll(response.getHeaders());
    if (ex.getSupportedHttpMethods() != null) {
      headers.setAllow(ex.getSupportedHttpMethods());
    }
    return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
  }

  @ExceptionHandler({
    DataAccessResourceFailureException.class,
    QueryTimeoutException.class,
    CannotCreateTransactionException.class,
    TransactionTimedOutException.class
  })
  public ResponseEntity<GenericResponse<Object>> dbUnavailable(Exception ex) {
    log.error("Database unavailable", ex);
    return respond(
        HttpStatus.SERVICE_UNAVAILABLE,
        ApiErrorResponse.of(ErrorCodes.SERVICE_UNAVAILABLE, "Service unavailable"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<GenericResponse<Object>> unexpected(Exception ex) {
    log.error("Unexpected error", ex);
    return respond(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ApiErrorResponse.of(ErrorCodes.INTERNAL_ERROR, "Unexpected error"));
  }
}
