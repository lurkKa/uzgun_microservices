package com.uz.uzgun_microservices.user.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uz.uzgun_microservices.security.*;
import com.uz.uzgun_microservices.shared.api.TraceIdFilter;
import com.uz.uzgun_microservices.shared.error.*;
import com.uz.uzgun_microservices.user.application.CheckoutPermissionService;
import com.uz.uzgun_microservices.user.application.CustomerNotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = CustomerController.class,
    properties = {
      "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.example.test",
      "springdoc.api-docs.enabled=true",
      "springdoc.swagger-ui.enabled=true"
    })
@Import({
  SecurityConfig.class,
  ApiAuthenticationEntryPoint.class,
  ApiAccessDeniedHandler.class,
  SecurityErrorWriter.class,
  ApiExceptionHandler.class,
  AuthContext.class,
  TraceIdFilter.class
})
class CustomerControllerTest {
  private static final String URL = "/user/permittedToCheckout";
  @Autowired MockMvc mvc;
  @MockitoBean CheckoutPermissionService service;
  @MockitoBean JwtDecoder decoder;

  private void token(String subject, String role) {
    when(decoder.decode("test-token"))
        .thenReturn(
            Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(subject)
                .claim("roles", List.of(role))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build());
  }

  @Test
  void unauthenticatedIsJson401WithoutRedirect() throws Exception {
    mvc.perform(get(URL).header("X-Trace-Id", "test-trace-123"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer"))
        .andExpect(header().doesNotExist("Location"))
        .andExpect(header().string("X-Trace-Id", "test-trace-123"))
        .andExpect(jsonPath("$.errors[0].code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.trace").value("test-trace-123"));
    verifyNoInteractions(service);
  }

  @Test
  void sellerIsForbidden() throws Exception {
    token("1", "SELLER");
    mvc.perform(get(URL).header("Authorization", "Bearer test-token"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errors[0].code").value("FORBIDDEN"));
    verifyNoInteractions(service);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void returnsExplicitGrant(boolean permitted) throws Exception {
    token("7", "CUSTOMER");
    when(service.isPermitted(7)).thenReturn(permitted);
    mvc.perform(get(URL).header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.payload.permitted").value(permitted))
        .andExpect(jsonPath("$.payload.checkoutAllowed").doesNotExist())
        .andExpect(jsonPath("$.timestamp").isString())
        .andExpect(jsonPath("$.trace").isString())
        .andExpect(jsonPath("$.errors").doesNotExist());
    verify(service).isPermitted(7);
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "0", "-1", "999999999999999999999"})
  void invalidSubjectIs401(String subject) throws Exception {
    token(subject, "CUSTOMER");
    mvc.perform(get(URL).header("Authorization", "Bearer test-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer"))
        .andExpect(jsonPath("$.errors[0].code").value("UNAUTHORIZED"));
    verifyNoInteractions(service);
  }

  @Test
  void unknownCustomerIs400() throws Exception {
    token("9", "CUSTOMER");
    when(service.isPermitted(9)).thenThrow(new CustomerNotFoundException(9));
    mvc.perform(get(URL).header("Authorization", "Bearer test-token"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].code").value("CUSTOMER_NOT_FOUND"));
  }

  @Test
  void unavailableDatabaseIs503() throws Exception {
    token("1", "CUSTOMER");
    when(service.isPermitted(1))
        .thenThrow(
            new org.springframework.transaction.CannotCreateTransactionException("db unavailable"));
    mvc.perform(get(URL).header("Authorization", "Bearer test-token"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.errors[0].code").value("SERVICE_UNAVAILABLE"))
        .andExpect(jsonPath("$.payload").doesNotExist());
  }

  @Test
  void queryTimeoutIs503() throws Exception {
    token("1", "CUSTOMER");
    when(service.isPermitted(1))
        .thenThrow(new org.springframework.dao.QueryTimeoutException("query timeout"));
    mvc.perform(get(URL).header("Authorization", "Bearer test-token"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.errors[0].code").value("SERVICE_UNAVAILABLE"));
  }

  @Test
  void wrongMethodReturns405AndAllowHeader() throws Exception {
    token("1", "CUSTOMER");
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(URL)
                .header("Authorization", "Bearer test-token"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("GET")))
        .andExpect(jsonPath("$.errors[0].code").value("METHOD_NOT_ALLOWED"));
    verifyNoInteractions(service);
  }

  @Test
  void invalidTraceIsReplacedAndDoesNotLeakToNextRequest() throws Exception {
    String first =
        mvc.perform(get(URL).header("X-Trace-Id", "bad trace"))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse()
            .getHeader("X-Trace-Id");
    java.util.UUID.fromString(first);
    String second = mvc.perform(get(URL)).andReturn().getResponse().getHeader("X-Trace-Id");
    org.junit.jupiter.api.Assertions.assertNotEquals(first, second);
    org.junit.jupiter.api.Assertions.assertNull(org.slf4j.MDC.get("traceId"));
  }
}
