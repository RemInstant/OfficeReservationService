package org.reminstant.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "rooms")
public class Room {

  @Getter
  @Setter
  public static class UnavailabilityMasks {
    private Integer monday;
    private Integer tuesday;
    private Integer wednesday;
    private Integer thursday;
    private Integer friday;
    private Integer saturday;
    private Integer sunday;
  }

  @Id
  private String id;

  @Indexed(unique = true)
  private String roomTitle;

  private UnavailabilityMasks unavailabilityMasks; // TODO: EnumMap<DayOfWeek, Integer>

  public Room(String roomTitle) {
    this.roomTitle = roomTitle;
    this.unavailabilityMasks = new UnavailabilityMasks();
  }
}
