package org.reminstant.exception;

public class UnavailableReservationException extends RuntimeException {
  public UnavailableReservationException() {
  }

  public UnavailableReservationException(String message) {
    super(message);
  }
}
