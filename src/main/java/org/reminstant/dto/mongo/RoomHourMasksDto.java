package org.reminstant.dto.mongo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RoomHourMasksDto {
  private String roomId;
  private List<Integer> masks;
}
