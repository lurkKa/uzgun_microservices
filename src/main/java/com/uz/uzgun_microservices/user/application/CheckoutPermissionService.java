package com.uz.uzgun_microservices.user.application;

import com.uz.uzgun_microservices.user.persistence.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads an explicit bypass grant; does not evaluate ordinary checkout eligibility. */
@Service
@RequiredArgsConstructor
public class CheckoutPermissionService {
  private final UserRepository userRepository;
  private final MeterRegistry meterRegistry;

  @Transactional(readOnly = true, timeout = 3)
  public boolean isPermitted(long userId) {
    boolean permitted =
        userRepository
            .findCheckoutBypassPermissionById(userId)
            .orElseThrow(() -> new CustomerNotFoundException(userId));
    meterRegistry
        .counter("checkout.bypass.permission", "result", permitted ? "granted" : "not_granted")
        .increment();
    return permitted;
  }
}
