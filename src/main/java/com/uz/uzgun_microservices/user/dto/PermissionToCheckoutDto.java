package com.uz.uzgun_microservices.user.dto;

public class PermissionToCheckoutDto {
    private Boolean permitted;

    public PermissionToCheckoutDto(Boolean permitted) {
        this.permitted = permitted;
    }

    public PermissionToCheckoutDto() {

    }

    public Boolean getPermitted() {
        return permitted;
    }

    public void setPermitted(Boolean permitted) {
        this.permitted = permitted;
    }
}
