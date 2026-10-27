package com.uz.uzgun_microservices.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "Explicit permission to bypass checkout restrictions; not general purchase eligibility")
public record PermissionToCheckoutDto(
    @Schema(
            description =
                "True only for an explicit bypass grant. False means use ordinary checkout checks.")
        boolean permitted) {}
