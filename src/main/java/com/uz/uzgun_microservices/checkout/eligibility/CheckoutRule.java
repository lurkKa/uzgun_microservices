package com.uz.uzgun_microservices.checkout.eligibility;

import com.uz.uzgun_microservices.user.domain.User;
import java.util.Optional;

public interface CheckoutRule {
  Optional<DenialReason> check(User user);
}
