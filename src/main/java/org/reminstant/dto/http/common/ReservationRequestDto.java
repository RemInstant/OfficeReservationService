package org.reminstant.dto.http.common;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;

public record ReservationRequestDto(
    @NotBlank @Length(min = 1, max = 32)
    String roomTitle,
    @NotBlank @Length(min = 10, max = 10)
    String date,
    @NotNull @Min(0) @Max(23)
    Integer startHour,
    @NotNull @Min(0) @Max(23)
    Integer endHour) {

    @AssertTrue(message = "startHour mast be less or equal to endHour")
    public boolean getHourRangeValidity() {
        return startHour <= endHour;
    }
}
