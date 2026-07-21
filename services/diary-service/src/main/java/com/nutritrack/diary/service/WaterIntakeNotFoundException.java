package com.nutritrack.diary.service;

import java.util.UUID;

public class WaterIntakeNotFoundException extends RuntimeException {

  public WaterIntakeNotFoundException(UUID id) {
    super("Water intake log not found: " + id);
  }
}
