package com.uz.uzgun_microservices.checkout.eligibility;

import com.uz.uzgun_microservices.user.domain.User;
import java.util.Optional;
import org.springframework.util.StringUtils;

public class AccountCompletenessRule implements CheckoutRule {
  @Override
  public Optional<DenialReason> check(User user) {
    if (!StringUtils.hasText(user.getPhone())) return Optional.of(DenialReason.PHONE_MISSING);
    if (!StringUtils.hasText(user.getFirstName())) return Optional.of(DenialReason.NAME_MISSING);
    return Optional.empty();
  }
}
