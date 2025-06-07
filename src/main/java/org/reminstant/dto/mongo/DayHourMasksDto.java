package org.reminstant.dto.mongo;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class DayHourMasksDto {
  private OffsetDateTime date;
  private List<Integer> masks;
}
