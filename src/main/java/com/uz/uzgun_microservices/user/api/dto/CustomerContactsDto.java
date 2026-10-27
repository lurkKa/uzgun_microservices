package com.uz.uzgun_microservices.user.api.dto;

import com.uz.uzgun_microservices.user.domain.Sex;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@EqualsAndHashCode
public class CustomerContactsDto {
  @NotBlank
  @Pattern(regexp = "^\\+998\\d{9}$", message = "phone must be in format +998XXXXXXXXX")
  @Schema(description = "Телефон, формат +998XXXXXXXXX", example = "+998901234567")
  private String phone;

  @Schema(description = "Email", nullable = true)
  @Email
  @Size(max = 254)
  private String email;

  @Schema(description = "Имя", nullable = true)
  @Size(max = 100)
  private String firstName;

  @Schema(description = "Фамилия", nullable = true)
  @Size(max = 100)
  private String lastName;

  @Schema(description = "Отчество", nullable = true)
  @Size(max = 100)
  private String patronymic;

  @Schema(description = "Пол", nullable = true)
  private Sex sex;

  @Schema(description = "Дата рождения", nullable = true)
  @Past
  private LocalDate birthDate;
}
