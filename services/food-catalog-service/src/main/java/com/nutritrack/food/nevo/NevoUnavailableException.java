package com.nutritrack.food.nevo;

public class NevoUnavailableException extends RuntimeException {
  public NevoUnavailableException(Throwable cause) {
    super("NEVO service unavailable", cause);
  }
}
