package com.uz.uzgun_microservices.checkout.integration.risk;

import com.uz.uzgun_microservices.shared.error.DownstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.net.http.HttpClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/** Adapter for the project's proposed risk contract, not a confirmed atlas API. */
public class HttpRiskClient implements RiskClient {
  private final RestClient restClient;
  private final CircuitBreaker circuitBreaker;

  public HttpRiskClient(
      RestClient.Builder builder, RiskProperties properties, CircuitBreaker circuitBreaker) {
    var httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
    var factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(properties.readTimeout());
    this.restClient =
        builder.baseUrl(properties.baseUrl().toString()).requestFactory(factory).build();
    this.circuitBreaker = circuitBreaker;
  }

  @Override
  public boolean isBlocked(long userId) {
    try {
      return circuitBreaker.executeSupplier(
          () -> {
            JsonNode response =
                restClient.get().uri("/risk/users/{id}", userId).retrieve().body(JsonNode.class);
            JsonNode blocked = response == null ? null : response.get("blocked");
            if (blocked == null || !blocked.isBoolean()) {
              throw new IllegalStateException("Risk service returned no explicit boolean decision");
            }
            return blocked.booleanValue();
          });
    } catch (RuntimeException ex) {
      throw new DownstreamUnavailableException("risk-service", ex);
    }
  }
}
