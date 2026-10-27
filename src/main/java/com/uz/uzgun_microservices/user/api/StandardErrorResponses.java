package com.uz.uzgun_microservices.user.api;

import com.uz.uzgun_microservices.shared.api.GenericResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content = @Content(schema = @Schema(implementation = GenericResponse.class))),
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized",
      content = @Content(schema = @Schema(implementation = GenericResponse.class))),
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden",
      content = @Content(schema = @Schema(implementation = GenericResponse.class))),
  @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error",
      content = @Content(schema = @Schema(implementation = GenericResponse.class))),
  @ApiResponse(
      responseCode = "503",
      description = "Service Unavailable",
      content = @Content(schema = @Schema(implementation = GenericResponse.class)))
})
public @interface StandardErrorResponses {}
