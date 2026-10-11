package com.uz.uzgun_microservices.user.web;


import com.uz.uzgun_microservices.user.dto.GenericResponsePermissionToCheckoutDto;
import com.uz.uzgun_microservices.user.services.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/user")
public class UserController {

    private UserService userService;

    @GetMapping("/permittedToCheckout")
    public GenericResponsePermissionToCheckoutDto isCustomerPermittedToCheckout(){

        return new GenericResponsePermissionToCheckoutDto();
    }
}
