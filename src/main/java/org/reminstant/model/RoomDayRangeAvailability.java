package org.reminstant.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RoomDayRangeAvailability {

  @Getter
  @Setter
  @AllArgsConstructor
  public static class Availability {
      private OffsetDateTime date;
      private int mask;
  }

  private String roomId;
  private String roomTitle;
  private List<Availability> availability;

  public RoomDayRangeAvailability(String roomId, String roomTitle) {
    this.roomId = roomId;
    this.roomTitle = roomTitle;
    this.availability = new ArrayList<>();
  }
}
