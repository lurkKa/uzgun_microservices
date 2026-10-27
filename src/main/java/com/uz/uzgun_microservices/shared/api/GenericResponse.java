package com.uz.uzgun_microservices.shared.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(value = "error", allowGetters = true)
public record GenericResponse<T>(
    @Schema(description = "Данные ответа; отсутсвуют, если есть errors") T payload,
    @Schema(description = "Список ошибок; отсутствует при успешном ответе")
        List<ApiErrorResponse> errors,
    @Schema(
            description = "Время формирования ответа(UTC)",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "2026-10-17T10:15:30Z")
        Instant timestamp,
    @Schema(description = "ID трассировки для поиска запроса в логах") String trace) {

  public GenericResponse {
    errors = errors == null ? null : List.copyOf(errors);
  }

  @JsonProperty("error")
  @Schema(
      accessMode = Schema.AccessMode.READ_ONLY,
      description = "Текст первой ошибки из errors. Только для чтения")
  public String error() {
    return ((errors == null || errors.isEmpty()) ? null : errors.get(0).message());
  }

  public static <T> GenericResponse<T> ok(T payload, String trace) {
    return new GenericResponse<>(payload, null, Instant.now(), trace);
  }

  public static GenericResponse<Object> failure(List<ApiErrorResponse> errors, String trace) {
    return new GenericResponse<>(null, errors, Instant.now(), trace);
  }
}
