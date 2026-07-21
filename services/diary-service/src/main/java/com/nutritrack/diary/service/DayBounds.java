package com.nutritrack.diary.service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Inclusive calendar-day bounds in a client (or UTC) timezone. */
public final class DayBounds {

  private DayBounds() {}

  public static ZoneId resolveZone(String zoneId) {
    if (zoneId == null || zoneId.isBlank()) {
      return ZoneOffset.UTC;
    }
    try {
      return ZoneId.of(zoneId.trim());
    } catch (DateTimeException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid zone: " + zoneId);
    }
  }

  public static Instant startOfDay(LocalDate date, ZoneId zone) {
    return date.atStartOfDay(zone).toInstant();
  }

  public static Instant startOfNextDay(LocalDate date, ZoneId zone) {
    return date.plusDays(1).atStartOfDay(zone).toInstant();
  }

  public static LocalDate localDate(Instant instant, ZoneId zone) {
    return instant.atZone(zone).toLocalDate();
  }
}
