package com.uz.uzgun_microservices.checkout.eligibility;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.uz.uzgun_microservices.checkout.integration.risk.RiskClient;
import com.uz.uzgun_microservices.user.domain.Role;
import com.uz.uzgun_microservices.user.domain.User;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckoutPolicyTest {
  private final RiskClient risk = mock(RiskClient.class);
  private final CheckoutPolicy policy = new CheckoutEligibilityConfiguration().checkoutPolicy(risk);

  private User customer() {
    User user = new User();
    user.setUserId(1L);
    user.setRole(Role.CUSTOMER);
    user.setPhone("+998901234567");
    user.setFirstName("Name");
    return user;
  }

  @Test
  void noPatronymicIsAllowed() {
    assertTrue(policy.evaluate(customer()).permitted());
    verify(risk).isBlocked(1);
  }

  @Test
  void noPhoneIsDeniedBeforeRemoteCall() {
    User user = customer();
    user.setPhone(null);
    user.setPatronymic("Patronymic");
    assertEquals(DenialReason.PHONE_MISSING, policy.evaluate(user).reason());
    verifyNoInteractions(risk);
  }

  @Test
  void inactiveComesBeforeContactsAndRisk() {
    User user = customer();
    user.setActive(false);
    user.setPhone(null);
    assertEquals(DenialReason.ACCOUNT_INACTIVE, policy.evaluate(user).reason());
    verifyNoInteractions(risk);
  }

  @Test
  void roleComesFirst() {
    User user = customer();
    user.setRole(Role.SELLER);
    user.setActive(false);
    assertEquals(DenialReason.NOT_A_CUSTOMER, policy.evaluate(user).reason());
    verifyNoInteractions(risk);
  }

  @Test
  void blankNameIsDenied() {
    User user = customer();
    user.setFirstName(" ");
    assertEquals(DenialReason.NAME_MISSING, policy.evaluate(user).reason());
    verifyNoInteractions(risk);
  }

  @Test
  void blockedRiskIsDenied() {
    when(risk.isBlocked(1)).thenReturn(true);
    assertEquals(DenialReason.RISK_REJECTED, policy.evaluate(customer()).reason());
  }

  @Test
  void emptyPolicyCannotAllowEveryone() {
    assertThrows(IllegalArgumentException.class, () -> new CheckoutPolicy(List.of()));
  }
}
