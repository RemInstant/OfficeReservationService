package org.reminstant.dto.http.response;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(
    description = "Часы, доступные для бронирования помещений, в выбранную дату",
    accessMode = Schema.AccessMode.READ_ONLY)
public record RoomsDayAvailabilityDto(
    @Schema(description = "Дата (ISO 8601)", example = "2025-12-25")
    String date,
    List<RoomAvailability> roomAvailability) {

  @Hidden
  public record RoomAvailability(
      @Schema(description = "Идентификатор помещения", example = "A-423")
      String roomTitle,
      @Schema(description = "Часы, доступные для бронирования помещения")
      List<Integer> hours) {
  }

  public RoomsDayAvailabilityDto(String roomTitle) {
      this(roomTitle, new ArrayList<>());
    }
}
