package com.uz.uzgun_microservices.shared.error;

import org.springframework.http.HttpStatus;

public class DownstreamUnavailableException extends ApiException {
  private final String dependency;

  public String getDependency() {
    return dependency;
  }

  public DownstreamUnavailableException(String dependency, Throwable cause) {
    super(
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorCodes.SERVICE_UNAVAILABLE,
        "Service temporarily unavailable",
        null,
        cause);
    this.dependency = dependency;
  }
}
