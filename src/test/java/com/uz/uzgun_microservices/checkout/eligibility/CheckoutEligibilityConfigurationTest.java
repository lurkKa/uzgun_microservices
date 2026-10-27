package com.uz.uzgun_microservices.checkout.eligibility;

import static org.assertj.core.api.Assertions.assertThat;

import com.uz.uzgun_microservices.checkout.integration.risk.RiskClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class CheckoutEligibilityConfigurationTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(CheckoutEligibilityConfiguration.class)
          .withBean(RestClient.Builder.class, RestClient::builder)
          .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

  @Test
  void disabledModuleDoesNotRequireRiskService() {
    runner.run(
        context -> {
          assertThat(context)
              .hasNotFailed()
              .doesNotHaveBean(CheckoutPolicy.class)
              .doesNotHaveBean(RiskClient.class);
        });
  }

  @Test
  void enabledModuleRequiresExplicitRiskConfiguration() {
    runner
        .withPropertyValues("checkout.eligibility.enabled=true")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void enabledModuleCreatesTheWholePolicy() {
    runner
        .withPropertyValues(
            "checkout.eligibility.enabled=true",
            "risk.base-url=http://127.0.0.1:12345",
            "risk.connect-timeout=500ms",
            "risk.read-timeout=2s")
        .run(
            context -> {
              assertThat(context)
                  .hasNotFailed()
                  .hasSingleBean(CheckoutPolicy.class)
                  .hasSingleBean(RiskClient.class);
            });
  }
}
