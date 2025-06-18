package org.reminstant.dto.http.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Данные уже совершённого бронирования", accessMode = Schema.AccessMode.READ_ONLY)
public record ReservationParamsDto(
    @Schema(description = "Идентификатор помещения", example = "A-423")
    String roomTitle,
    @Schema(description = "Дата бронирования (ISO 8601)", example = "2025-12-25")
    String date,
    @Schema(description = "Первый час брони (12:00-xx:xx)", example = "12")
    Integer startHour,
    @Schema(description = "Последний час брони (xx:xx-14:59)", example = "14")
    Integer endHour) {
}
