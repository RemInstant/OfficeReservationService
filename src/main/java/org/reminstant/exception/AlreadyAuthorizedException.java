package org.reminstant.exception;

public class AlreadyAuthorizedException extends RuntimeException {
  public AlreadyAuthorizedException() {
  }

  public AlreadyAuthorizedException(String message) {
    super(message);
  }
}
