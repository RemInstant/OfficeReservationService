package org.reminstant.exception;

import lombok.Getter;

@Getter
public class RoomNotFoundException extends RuntimeException {

  private final String roomKey;
  private final String roomValue;

  public RoomNotFoundException(String roomKey, String roomValue) {
    this.roomKey = roomKey;
    this.roomValue = roomValue;
  }
}
