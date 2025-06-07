package org.reminstant.dto.http.response;

import java.util.ArrayList;
import java.util.List;

public record RoomDayRangeAvailabilityDto(
    String roomTitle,
    List<Availability> availability) {

  public record Availability(
      String date,
      List<Integer> hours) {
  }

  public RoomDayRangeAvailabilityDto(String roomTitle) {
    this(roomTitle, new ArrayList<>());
  }
}