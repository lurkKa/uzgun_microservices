package com.uz.uzgun_microservices.checkout.eligibility;

public record CheckoutDecision(boolean permitted, DenialReason reason) {
  public static CheckoutDecision allowed() {
    return new CheckoutDecision(true, null);
  }

  public static CheckoutDecision denied(DenialReason denialReason) {
    return new CheckoutDecision(false, denialReason);
  }
}
