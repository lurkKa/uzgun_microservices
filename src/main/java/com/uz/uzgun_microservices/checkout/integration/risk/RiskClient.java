package com.uz.uzgun_microservices.checkout.integration.risk;

public interface RiskClient {
  boolean isBlocked(long userId);
}
