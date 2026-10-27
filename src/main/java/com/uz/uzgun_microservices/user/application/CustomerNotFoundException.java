package com.uz.uzgun_microservices.user.application;

import com.uz.uzgun_microservices.shared.error.ApiException;
import com.uz.uzgun_microservices.shared.error.ErrorCodes;
import org.springframework.http.HttpStatus;

public class CustomerNotFoundException extends ApiException {
  public CustomerNotFoundException(long userId) {
    super(HttpStatus.BAD_REQUEST, ErrorCodes.CUSTOMER_NOT_FOUND, "Customer not found", null, null);
  }
}
