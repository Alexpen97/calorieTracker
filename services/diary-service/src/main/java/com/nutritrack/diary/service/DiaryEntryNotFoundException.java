package com.nutritrack.diary.service;

import java.util.UUID;

public class DiaryEntryNotFoundException extends RuntimeException {
  public DiaryEntryNotFoundException(UUID id) {
    super("Diary entry not found: " + id);
  }
}
