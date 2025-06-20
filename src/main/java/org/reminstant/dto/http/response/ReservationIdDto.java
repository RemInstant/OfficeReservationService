package org.reminstant.dto.http.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Идентификатор брони", accessMode = Schema.AccessMode.READ_WRITE)
public record ReservationIdDto(
    String reservationId) {
}
