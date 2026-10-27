package com.uz.uzgun_microservices.checkout.eligibility;

import com.uz.uzgun_microservices.user.domain.Role;
import com.uz.uzgun_microservices.user.domain.User;
import java.util.Optional;

public class CustomerRoleRule implements CheckoutRule {
  @Override
  public Optional<DenialReason> check(User user) {
    return user.getRole() == Role.CUSTOMER
        ? Optional.empty()
        : Optional.of(DenialReason.NOT_A_CUSTOMER);
  }
}
