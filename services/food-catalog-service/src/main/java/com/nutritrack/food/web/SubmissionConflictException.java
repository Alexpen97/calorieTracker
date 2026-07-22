package com.nutritrack.food.web;

import java.util.List;

public class SubmissionConflictException extends RuntimeException {

  private final List<String> warnings;

  public SubmissionConflictException(List<String> warnings) {
    super("Possible duplicate product");
    this.warnings = List.copyOf(warnings);
  }

  public List<String> getWarnings() {
    return warnings;
  }
}
