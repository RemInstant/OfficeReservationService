package org.reminstant.dto.http.request;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(
    description = "Пара авторизационных токенов в формате JWT",
    accessMode = Schema.AccessMode.READ_WRITE)
public record JwtTokenDto(
    @NotBlank
    @Parameter(description = "Токен доступа")
    String accessToken,
    @NotBlank
    @Parameter(description = "Токен обновления")
    String refreshToken) {
}
