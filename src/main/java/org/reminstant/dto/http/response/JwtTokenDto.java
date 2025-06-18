package org.reminstant.dto.http.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Авторизационный токен в формате JWT", accessMode = Schema.AccessMode.READ_ONLY)
public record JwtTokenDto(
    String token) {
}
