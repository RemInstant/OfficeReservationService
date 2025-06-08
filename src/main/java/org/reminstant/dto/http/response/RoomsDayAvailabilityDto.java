package org.reminstant.dto.http.response;

import java.util.ArrayList;
import java.util.List;

public record RoomsDayAvailabilityDto(
  String date,
  List<RoomsDayAvailabilityDto.Availability> availability) {

    public record Availability(
        String roomTitle,
        List<Integer> hours) {
    }

  public RoomsDayAvailabilityDto(String roomTitle) {
      this(roomTitle, new ArrayList<>());
    }
}
