package com.nutritrack.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class DayBoundsTest {

  @Test
  void defaultsBlankZoneToUtc() {
    assertThat(DayBounds.resolveZone(null)).isEqualTo(ZoneOffset.UTC);
    assertThat(DayBounds.resolveZone("")).isEqualTo(ZoneOffset.UTC);
  }

  @Test
  void usesClientZoneForDayBounds() {
    ZoneId zone = DayBounds.resolveZone("America/New_York");
    LocalDate date = LocalDate.of(2026, 7, 21);
    Instant start = DayBounds.startOfDay(date, zone);
    Instant end = DayBounds.startOfNextDay(date, zone);
    assertThat(start).isEqualTo(Instant.parse("2026-07-21T04:00:00Z"));
    assertThat(end).isEqualTo(Instant.parse("2026-07-22T04:00:00Z"));
    assertThat(DayBounds.localDate(Instant.parse("2026-07-22T03:30:00Z"), zone))
        .isEqualTo(date);
  }

  @Test
  void rejectsInvalidZone() {
    assertThatThrownBy(() -> DayBounds.resolveZone("Not/AZone"))
        .isInstanceOf(ResponseStatusException.class);
  }
}
