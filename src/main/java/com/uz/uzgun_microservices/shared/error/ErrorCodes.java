package com.uz.uzgun_microservices.shared.error;

public final class ErrorCodes {
  private ErrorCodes() {}

  public static final String BAD_REQUEST = "BAD_REQUEST";
  public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
  public static final String UNAUTHORIZED = "UNAUTHORIZED";
  public static final String FORBIDDEN = "FORBIDDEN";
  public static final String NOT_FOUND = "NOT_FOUND";
  public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
  public static final String CUSTOMER_NOT_FOUND = "CUSTOMER_NOT_FOUND";
  public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
  public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
}
