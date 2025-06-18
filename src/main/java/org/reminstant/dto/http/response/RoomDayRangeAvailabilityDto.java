package org.reminstant.dto.http.response;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(
    description = "Часы, доступные для бронирования помещения, в диапазоне дат",
    accessMode = Schema.AccessMode.READ_ONLY)
public record RoomDayRangeAvailabilityDto(
    @Schema(description = "Идентификатор помещения", example = "A-423")
    String roomTitle,
    List<DateAvailability> dateAvailability) {

  @Hidden
  public record DateAvailability(
      @Schema(description = "Дата (ISO 8601)", example = "2025-12-25")
      String date,
      @Schema(description = "Часы, доступные для бронирования помещения")
      List<Integer> hours) {
  }

  public RoomDayRangeAvailabilityDto(String roomTitle) {
    this(roomTitle, new ArrayList<>());
  }
}