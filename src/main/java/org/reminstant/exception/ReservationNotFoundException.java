package org.reminstant.exception;

import lombok.Getter;

@Getter
public class ReservationNotFoundException extends RuntimeException {

  private final String reservationId;

  public ReservationNotFoundException(String reservationId) {
    this.reservationId = reservationId;
  }
}
