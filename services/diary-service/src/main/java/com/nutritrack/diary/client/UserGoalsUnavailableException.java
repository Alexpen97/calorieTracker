package com.nutritrack.diary.client;

public class UserGoalsUnavailableException extends RuntimeException {

  public UserGoalsUnavailableException(Throwable cause) {
    super("User goals service is unavailable", cause);
  }
}
