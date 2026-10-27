package com.uz.uzgun_microservices.checkout.eligibility;

import com.uz.uzgun_microservices.user.domain.User;
import java.util.List;
import java.util.Optional;

public class CheckoutPolicy {
  private final List<CheckoutRule> rules;

  public CheckoutPolicy(List<CheckoutRule> rules) {
    if (rules.isEmpty()) throw new IllegalArgumentException("Checkout rules must not be empty");
    this.rules = List.copyOf(rules);
  }

  public CheckoutDecision evaluate(User user) {
    for (CheckoutRule rule : rules) {
      Optional<DenialReason> denial = rule.check(user);
      if (denial.isPresent()) {
        return CheckoutDecision.denied(denial.get());
      }
    }
    return CheckoutDecision.allowed();
  }
}
