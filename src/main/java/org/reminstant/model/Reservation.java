package org.reminstant.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Document(collection = "reservations")
public class Reservation {

  @Id
  private String id;

  @Indexed()
  private String roomId;

  @Field(targetType = FieldType.STRING)
  private OffsetDateTime date;

  private int reservationMask;

  public Reservation(String roomId, OffsetDateTime date, int reservationMask) {
    this.roomId = roomId;
    this.date = date;
    this.reservationMask = reservationMask;
  }
}
