package org.reminstant.dto.http.response;

public record ReservationParamsDto(
    String roomTitle,
    String date,
    Integer startHour,
    Integer endHour) {
}
