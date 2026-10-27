package com.uz.uzgun_microservices.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ApiErrorResponse", description = "Описание одной ошибки API")
public record ApiErrorResponse(
    @Schema(
            description = "Error code",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "CHECKOUT_NOT_PERMITTED")
        String code,
    @Schema(
            description = "Error message",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Customer is not permitted to checkout")
        String message,
    @Deprecated
        @Schema(
            deprecated = true,
            description =
                "Устарело, используйте message и payload. Будет удалено в следующей версии API")
        String detailMessage,
    @Schema(description = "Дополнительные данные об ошибке, структура зависит от code")
        Object payload) {
  public ApiErrorResponse {
    Objects.requireNonNull(code, "code must not be null");
    Objects.requireNonNull(message, "message must not be null");
  }

  public static ApiErrorResponse of(String code, String message) {
    return new ApiErrorResponse(code, message, null, null);
  }

  public static ApiErrorResponse of(String code, String message, Object payload) {
    return new ApiErrorResponse(code, message, null, payload);
  }
}
