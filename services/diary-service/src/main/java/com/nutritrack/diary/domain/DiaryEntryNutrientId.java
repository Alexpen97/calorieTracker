package com.nutritrack.diary.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class DiaryEntryNutrientId implements Serializable {

  private UUID entryId;
  private String nutrientCode;

  public DiaryEntryNutrientId() {}

  public DiaryEntryNutrientId(UUID entryId, String nutrientCode) {
    this.entryId = entryId;
    this.nutrientCode = nutrientCode;
  }

  public UUID getEntryId() {
    return entryId;
  }

  public void setEntryId(UUID entryId) {
    this.entryId = entryId;
  }

  public String getNutrientCode() {
    return nutrientCode;
  }

  public void setNutrientCode(String nutrientCode) {
    this.nutrientCode = nutrientCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DiaryEntryNutrientId that)) {
      return false;
    }
    return Objects.equals(entryId, that.entryId)
        && Objects.equals(nutrientCode, that.nutrientCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entryId, nutrientCode);
  }
}
