package org.reminstant.dto.http.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Список броней", accessMode = Schema.AccessMode.READ_ONLY)
public record ReservationsListDto(
    List<String> reservations) {
}
