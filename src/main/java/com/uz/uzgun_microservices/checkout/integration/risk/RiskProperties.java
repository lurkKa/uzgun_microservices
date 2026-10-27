package com.uz.uzgun_microservices.checkout.integration.risk;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("risk")
public record RiskProperties(URI baseUrl, Duration connectTimeout, Duration readTimeout) {
  public RiskProperties {
    if (baseUrl == null
        || baseUrl.getHost() == null
        || !("http".equals(baseUrl.getScheme()) || "https".equals(baseUrl.getScheme()))
        || baseUrl.getUserInfo() != null
        || baseUrl.getQuery() != null
        || baseUrl.getFragment() != null) {
      throw new IllegalArgumentException(
          "risk.base-url must be an absolute HTTP(S) URL without credentials, query or fragment");
    }
    if (connectTimeout == null
        || connectTimeout.isZero()
        || connectTimeout.isNegative()
        || readTimeout == null
        || readTimeout.isZero()
        || readTimeout.isNegative()) {
      throw new IllegalArgumentException("Risk client timeouts must be positive");
    }
  }
}
