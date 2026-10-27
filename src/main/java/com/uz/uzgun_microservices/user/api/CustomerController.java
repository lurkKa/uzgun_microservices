package com.uz.uzgun_microservices.user.api;

import com.uz.uzgun_microservices.security.AuthContext;
import com.uz.uzgun_microservices.shared.api.GenericResponse;
import com.uz.uzgun_microservices.shared.api.TraceContext;
import com.uz.uzgun_microservices.user.api.dto.PermissionToCheckoutDto;
import com.uz.uzgun_microservices.user.application.CheckoutPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@StandardErrorResponses
public class CustomerController {
  private final CheckoutPermissionService permissionService;
  private final AuthContext authContext;

  @GetMapping(value = "/permittedToCheckout", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(
      operationId = "isCustomerPermittedToCheckout",
      summary =
          "Check that user can advance to the checkout page from cart bypassing all restrictions",
      description =
          "Reads an explicit grant for the authenticated customer. False is not a general purchase denial. "
              + "Order creation must independently validate its own invariants.")
  @ApiResponse(responseCode = "200", description = "Explicit bypass permission")
  public ResponseEntity<GenericResponse<PermissionToCheckoutDto>> isCustomerPermittedToCheckout() {
    var payload =
        new PermissionToCheckoutDto(permissionService.isPermitted(authContext.currentUserId()));
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(GenericResponse.ok(payload, TraceContext.traceId()));
  }
}
