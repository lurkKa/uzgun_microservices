package com.uz.uzgun_microservices.checkout.eligibility;

public enum DenialReason {
  NOT_A_CUSTOMER,
  ACCOUNT_INACTIVE,
  PHONE_MISSING,
  NAME_MISSING,
  RISK_REJECTED
}
