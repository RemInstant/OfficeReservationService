package org.reminstant.exception;

public class OccupiedUsernameException extends RuntimeException {
  public OccupiedUsernameException() {
  }

  public OccupiedUsernameException(String message) {
    super(message);
  }

  public OccupiedUsernameException(String message, Throwable cause) {
    super(message, cause);
  }

  public OccupiedUsernameException(Throwable cause) {
    super(cause);
  }
}
