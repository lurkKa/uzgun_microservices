package com.uz.uzgun_microservices.shared.api;

import org.slf4j.MDC;

public final class TraceContext {
  public static final String MDC_KEY = "traceId";

  private TraceContext() {}

  public static String traceId() {
    String id = MDC.get(MDC_KEY);
    return id != null ? id : "n/a";
  }
}
