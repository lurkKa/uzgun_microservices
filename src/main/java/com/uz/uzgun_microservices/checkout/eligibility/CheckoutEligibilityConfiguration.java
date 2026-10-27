package com.uz.uzgun_microservices.checkout.eligibility;

import com.uz.uzgun_microservices.checkout.integration.risk.HttpRiskClient;
import com.uz.uzgun_microservices.checkout.integration.risk.RiskClient;
import com.uz.uzgun_microservices.checkout.integration.risk.RiskProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Optional preparation for a future order use case; not used by bypass permission lookup. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "checkout.eligibility", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RiskProperties.class)
public class CheckoutEligibilityConfiguration {
  @Bean
  RiskClient riskClient(
      RestClient.Builder builder, RiskProperties properties, MeterRegistry meters) {
    var config =
        CircuitBreakerConfig.custom()
            .slidingWindowSize(20)
            .minimumNumberOfCalls(10)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .permittedNumberOfCallsInHalfOpenState(3)
            .build();
    var registry = CircuitBreakerRegistry.of(config);
    var breaker = registry.circuitBreaker("risk");
    TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meters);
    return new HttpRiskClient(builder, properties, breaker);
  }

  @Bean
  CheckoutPolicy checkoutPolicy(RiskClient riskClient) {
    // Explicit order: reject locally before making a remote request.
    return new CheckoutPolicy(
        List.of(
            new CustomerRoleRule(),
            new AccountActiveRule(),
            new AccountCompletenessRule(),
            new RiskRule(riskClient)));
  }
}
