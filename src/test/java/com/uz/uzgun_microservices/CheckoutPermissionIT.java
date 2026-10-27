package com.uz.uzgun_microservices;

import static org.junit.jupiter.api.Assertions.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import com.uz.uzgun_microservices.checkout.eligibility.CheckoutPolicy;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

/**
 * Real PostgreSQL + Flyway upgrade + signed JWT + HTTP. Requires Docker, never uses the app
 * database.
 */
@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"springdoc.api-docs.enabled=true", "checkout.eligibility.enabled=false"})
class CheckoutPermissionIT {
  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  private static final RSAKey KEY;
  private static final HttpServer JWKS;
  private static final String ISSUER;

  static {
    try {
      KEY = new RSAKeyGenerator(2048).keyID("integration-key").generate();
      JWKS = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      ISSUER = "http://127.0.0.1:" + JWKS.getAddress().getPort();
      JWKS.createContext(
          "/jwks",
          exchange -> {
            byte[] body = new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
          });
      JWKS.start();
    } catch (Exception ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) throws Exception {
    // Simulate an existing V1 database before the new application runs V2.
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .target("1")
        .load()
        .migrate();
    try (var connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var statement = connection.createStatement()) {
      statement.executeUpdate(
          "INSERT INTO users(id, role, active) VALUES (700, 'CUSTOMER', false)");
    }
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> ISSUER);
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> ISSUER + "/jwks");
    registry.add("spring.security.oauth2.resourceserver.jwt.audiences", () -> "uzgun-user-service");
  }

  @AfterAll
  static void closeJwks() {
    JWKS.stop(0);
  }

  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired ApplicationContext context;
  private final HttpClient http = HttpClient.newHttpClient();

  private String token(long subject, String role, String audience, String issuer, Instant expiry)
      throws Exception {
    var claims =
        new JWTClaimsSet.Builder()
            .subject(Long.toString(subject))
            .issuer(issuer)
            .audience(audience)
            .issueTime(new Date())
            .expirationTime(Date.from(expiry))
            .claim("roles", List.of(role))
            .build();
    var token =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY.getKeyID()).build(), claims);
    token.sign(new RSASSASigner(KEY));
    return token.serialize();
  }

  private String customer(long id) throws Exception {
    return token(id, "CUSTOMER", "uzgun-user-service", ISSUER, Instant.now().plusSeconds(120));
  }

  private HttpResponse<String> request(String path, String token) throws Exception {
    var builder =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .header("X-Trace-Id", "integration-trace-123")
            .GET();
    if (token != null) builder.header("Authorization", "Bearer " + token);
    return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void existingUserGetsNoGrantAfterUpgrade() throws Exception {
    assertEquals(
        2,
        jdbc.queryForObject(
            "SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class));
    var response = request("/user/permittedToCheckout", customer(700));
    assertEquals(200, response.statusCode());
    var body = json.readTree(response.body());
    assertFalse(body.at("/payload/permitted").booleanValue());
    assertEquals("integration-trace-123", body.path("trace").asText());
    Instant.parse(body.path("timestamp").asText());
    assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
  }

  @Test
  void grantIsIndependentOfOrdinaryEligibilityAndRevocationIsImmediate() throws Exception {
    jdbc.update(
        "INSERT INTO users(id, role, active, checkout_bypass_permitted) VALUES (701, 'CUSTOMER', false, true)");
    var granted = request("/user/permittedToCheckout", customer(701));
    assertEquals(200, granted.statusCode());
    assertTrue(json.readTree(granted.body()).at("/payload/permitted").booleanValue());
    jdbc.update(
        "UPDATE users SET checkout_bypass_permitted = false, version = version + 1 WHERE id = 701");
    var revoked = request("/user/permittedToCheckout", customer(701));
    assertEquals(200, revoked.statusCode());
    assertFalse(json.readTree(revoked.body()).at("/payload/permitted").booleanValue());
    assertTrue(context.getBeansOfType(CheckoutPolicy.class).isEmpty());
  }

  @Test
  void authenticationAndAuthorizationUseRealJwtValidation() throws Exception {
    assertEquals(401, request("/user/permittedToCheckout", null).statusCode());
    assertEquals(401, request("/user/permittedToCheckout", "not-a-jwt").statusCode());
    assertEquals(
        401,
        request(
                "/user/permittedToCheckout",
                token(700, "CUSTOMER", "wrong-audience", ISSUER, Instant.now().plusSeconds(120)))
            .statusCode());
    assertEquals(
        401,
        request(
                "/user/permittedToCheckout",
                token(
                    700,
                    "CUSTOMER",
                    "uzgun-user-service",
                    "https://wrong-issuer.test",
                    Instant.now().plusSeconds(120)))
            .statusCode());
    assertEquals(
        401,
        request(
                "/user/permittedToCheckout",
                token(
                    700, "CUSTOMER", "uzgun-user-service", ISSUER, Instant.now().minusSeconds(120)))
            .statusCode());
    assertEquals(
        403,
        request(
                "/user/permittedToCheckout",
                token(700, "SELLER", "uzgun-user-service", ISSUER, Instant.now().plusSeconds(120)))
            .statusCode());
    assertEquals(400, request("/user/permittedToCheckout", customer(999999)).statusCode());
  }

  @Test
  void healthAndOpenApiAreAvailable() throws Exception {
    assertEquals(200, request("/actuator/health/liveness", null).statusCode());
    assertEquals(200, request("/actuator/health/readiness", null).statusCode());
    var response = request("/v3/api-docs", null);
    assertEquals(200, response.statusCode());
    var schema = json.readTree(response.body());
    assertEquals(
        "isCustomerPermittedToCheckout",
        schema.at("/paths/~1user~1permittedToCheckout/get/operationId").asText());
    assertTrue(schema.at("/paths/~1user~1permittedToCheckout/get/responses/503").isObject());
    assertEquals(
        "boolean",
        schema
            .at("/components/schemas/PermissionToCheckoutDto/properties/permitted/type")
            .asText());
    assertEquals(403, request("/actuator/prometheus", customer(700)).statusCode());
  }
}
