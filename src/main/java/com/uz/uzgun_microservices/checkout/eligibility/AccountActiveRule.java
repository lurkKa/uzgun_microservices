package com.uz.uzgun_microservices.checkout.eligibility;

import com.uz.uzgun_microservices.user.domain.User;
import java.util.Optional;

public class AccountActiveRule implements CheckoutRule {
  @Override
  public Optional<DenialReason> check(User user) {
    return user.isActive() ? Optional.empty() : Optional.of(DenialReason.ACCOUNT_INACTIVE);
  }
}
