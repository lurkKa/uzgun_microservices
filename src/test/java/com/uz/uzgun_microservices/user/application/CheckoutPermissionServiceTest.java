package com.uz.uzgun_microservices.user.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.uz.uzgun_microservices.user.persistence.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CheckoutPermissionServiceTest {
  private final UserRepository repository = mock(UserRepository.class);
  private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
  private final CheckoutPermissionService service =
      new CheckoutPermissionService(repository, meters);

  @Test
  void noGrantIsFalseNotAnException() {
    when(repository.findCheckoutBypassPermissionById(1)).thenReturn(Optional.of(false));
    assertFalse(service.isPermitted(1));
    assertEquals(1, meters.counter("checkout.bypass.permission", "result", "not_granted").count());
    verify(repository).findCheckoutBypassPermissionById(1);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void explicitGrantIsTrue() {
    when(repository.findCheckoutBypassPermissionById(2)).thenReturn(Optional.of(true));
    assertTrue(service.isPermitted(2));
  }

  @Test
  void absentUserIsNotConfusedWithFalseGrant() {
    when(repository.findCheckoutBypassPermissionById(3)).thenReturn(Optional.empty());
    assertThrows(CustomerNotFoundException.class, () -> service.isPermitted(3));
  }
}
