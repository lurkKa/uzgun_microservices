package com.uz.uzgun_microservices.checkout.integration.risk;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import com.uz.uzgun_microservices.shared.error.DownstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

class HttpRiskClientTest {
  private HttpServer server;
  private HttpRiskClient client;
  private CircuitBreaker breaker;
  private final AtomicInteger calls = new AtomicInteger();
  private String body = "{\"blocked\":false}";
  private int status = 200;
  private long delay;

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/risk/users/1",
        exchange -> {
          calls.incrementAndGet();
          try {
            if (delay > 0) Thread.sleep(delay);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
            if (status != 204) exchange.getResponseBody().write(bytes);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    server.start();
    breaker = CircuitBreaker.ofDefaults("risk-test");
    var props =
        new RiskProperties(
            URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
            Duration.ofMillis(100),
            Duration.ofMillis(150));
    client = new HttpRiskClient(RestClient.builder(), props, breaker);
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void explicitBooleanIsRequired(boolean blocked) {
    body = "{\"blocked\":" + blocked + "}";
    assertEquals(blocked, client.isBlocked(1));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "{\"blocked\":null}",
        "{\"blocked\":\"false\"}",
        "{\"blocked\":0}",
        "broken json",
        "null"
      })
  void malformedResponseNeverAllows(String response) {
    body = response;
    assertThrows(DownstreamUnavailableException.class, () -> client.isBlocked(1));
    assertEquals(1, breaker.getMetrics().getNumberOfFailedCalls());
  }

  @Test
  void emptyBodyIsUnavailable() {
    status = 204;
    assertThrows(DownstreamUnavailableException.class, () -> client.isBlocked(1));
  }

  @Test
  void serverErrorIsUnavailable() {
    status = 503;
    assertThrows(DownstreamUnavailableException.class, () -> client.isBlocked(1));
  }

  @Test
  void timeoutIsUnavailable() {
    delay = 400;
    assertThrows(DownstreamUnavailableException.class, () -> client.isBlocked(1));
  }

  @Test
  void openCircuitDoesNotSendRequest() {
    breaker.transitionToOpenState();
    assertThrows(DownstreamUnavailableException.class, () -> client.isBlocked(1));
    assertEquals(0, calls.get());
  }
}
