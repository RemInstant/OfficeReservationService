package org.reminstant.dto.http;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record RoomTitleDto(
    @NotBlank @Length(min = 1, max = 32)
    String roomTitle) {
}
