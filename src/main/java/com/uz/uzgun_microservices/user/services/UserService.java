package com.uz.uzgun_microservices.user.services;


import com.uz.uzgun_microservices.user.dto.CustomerContactsDto;
import com.uz.uzgun_microservices.user.dto.GenericResponsePermissionToCheckoutDto;
import com.uz.uzgun_microservices.user.dto.PermissionToCheckoutDto;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    public PermissionToCheckoutDto isCustomerPermittedToCheckout(CustomerContactsDto customerContactsDto){
        return new PermissionToCheckoutDto();
    }
}
