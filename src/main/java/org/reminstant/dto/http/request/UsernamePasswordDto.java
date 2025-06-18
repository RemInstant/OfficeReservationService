package org.reminstant.dto.http.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Данные для бронирования", accessMode = Schema.AccessMode.WRITE_ONLY)
public record UsernamePasswordDto(
    @Size(min = 4, max = 16)
    @Pattern(regexp = "\\w+")
    @Schema(description = "Логин пользователя", example = "aboba")
    String username,
    @Size(min = 6, max = 64)
    @Schema(description = "Пароль пользователя", example = "tY24RlA6F6s4416o")
    String password) {
}
