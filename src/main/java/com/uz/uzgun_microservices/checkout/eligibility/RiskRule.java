package com.uz.uzgun_microservices.checkout.eligibility;

import com.uz.uzgun_microservices.checkout.integration.risk.RiskClient;
import com.uz.uzgun_microservices.user.domain.User;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RiskRule implements CheckoutRule {
  private final RiskClient riskClient;

  @Override
  public Optional<DenialReason> check(User user) {
    return riskClient.isBlocked(user.getUserId())
        ? Optional.of(DenialReason.RISK_REJECTED)
        : Optional.empty();
  }
}
