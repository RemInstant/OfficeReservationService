package org.reminstant.dto.http.request;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Данные для бронирования", accessMode = Schema.AccessMode.WRITE_ONLY)
public record ReservationRequestDto(
    @NotBlank @Size(min = 1, max = 32)
    @Schema(description = "Идентификатор помещения", example = "A-423")
    String roomTitle,
    @NotBlank @Size(min = 10, max = 10)
    @Schema(description = "Дата бронирования (ISO 8601)", example = "2025-12-25")
    String date,
    @NotNull @Min(0) @Max(23)
    @Schema(description = "Первый час брони (12:00-xx:xx)", example = "12")
    Integer startHour,
    @NotNull @Min(0) @Max(23)
    @Schema(description = "Последний час брони (xx:xx-14:59)", example = "14")
    Integer endHour) {

    @Hidden
    @AssertTrue(message = "startHour mast be less or equal to endHour")
    public boolean getHourRangeValidity() {
        return startHour <= endHour;
    }
}
