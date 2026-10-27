package com.uz.uzgun_microservices.shared.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
  private final HttpStatus status;
  private final String code;
  private final transient Object payload;

  public ApiException(
      HttpStatus status, String code, String message, Object payload, Throwable cause) {
    super(message, cause);
    this.status = status;
    this.code = code;
    this.payload = payload;
  }
}
