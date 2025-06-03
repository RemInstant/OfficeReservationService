package org.reminstant.model;

import lombok.Getter;
import lombok.Setter;
import org.reminstant.dto.http.RoomConfigurationDto;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Document(collection = "rooms")
public class Room {

  @Getter
  @Setter
  public static class AvailabilityMask {
    private Integer monday;
    private Integer tuesday;
    private Integer wednesday;
    private Integer thursday;
    private Integer friday;
    private Integer sunday;
    private Integer saturday;
  }

  @Id
  private String id;

  @Indexed(unique = true)
  private String roomTitle;

  private AvailabilityMask availabilityMask;

  public Room(String roomTitle) {
    this.roomTitle = roomTitle;
    this.availabilityMask = new AvailabilityMask();
  }

  public static Room fromDto(RoomConfigurationDto dto) {
    Room room = new Room(dto.roomTitle().trim());
    room.getAvailabilityMask().setMonday(convertUnavailabilityListToMask(dto.mondayUnavailable()));
    room.getAvailabilityMask().setTuesday(convertUnavailabilityListToMask(dto.tuesdayUnavailable()));
    room.getAvailabilityMask().setThursday(convertUnavailabilityListToMask(dto.thursdayUnavailable()));
    room.getAvailabilityMask().setWednesday(convertUnavailabilityListToMask(dto.wednesdayUnavailable()));
    room.getAvailabilityMask().setFriday(convertUnavailabilityListToMask(dto.fridayUnavailable()));
    room.getAvailabilityMask().setSunday(convertUnavailabilityListToMask(dto.sundayUnavailable()));
    room.getAvailabilityMask().setSaturday(convertUnavailabilityListToMask(dto.saturdayUnavailable()));

    return room;
  }

  public RoomConfigurationDto toDto() {
    return new RoomConfigurationDto(
        roomTitle,
        convertMaskToUnavailabilityList(availabilityMask.monday),
        convertMaskToUnavailabilityList(availabilityMask.tuesday),
        convertMaskToUnavailabilityList(availabilityMask.wednesday),
        convertMaskToUnavailabilityList(availabilityMask.thursday),
        convertMaskToUnavailabilityList(availabilityMask.friday),
        convertMaskToUnavailabilityList(availabilityMask.sunday),
        convertMaskToUnavailabilityList(availabilityMask.saturday));
  }

  private static Integer convertUnavailabilityListToMask(List<Integer> unavailabilityArray) {
    if (unavailabilityArray == null) {
      return null;
    }
    int mask = 0;
    for (int hour : unavailabilityArray) {
      mask |= 1 << hour;
    }
    return ~mask;
  }

  private static List<Integer> convertMaskToUnavailabilityList(Integer mask) {
    if (mask == null) {
      return null; // NOSONAR
    }
    List<Integer> list = new ArrayList<>();
    for (int i = 0; i < 24; ++i) {
      if ((mask & (1 << i)) == 0) {
        list.add(i);
      }
    }
    return list;
  }
}
