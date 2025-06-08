package org.reminstant.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RoomsDayAvailability {

  @Getter
  @Setter
  @AllArgsConstructor
  public static class Availability {
    private String roomId;
    private String roomTitle;
    private int mask;
  }

  private OffsetDateTime date;
  private List<RoomsDayAvailability.Availability> availability;

  public RoomsDayAvailability(OffsetDateTime date) {
    this.date = date;
    this.availability = new ArrayList<>();
  }
}
