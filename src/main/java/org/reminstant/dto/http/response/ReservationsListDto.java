package org.reminstant.dto.http.response;

import java.util.List;

public record ReservationsListDto(
    List<String> reservations) {
}
