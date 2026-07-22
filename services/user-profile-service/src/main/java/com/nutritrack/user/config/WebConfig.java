package com.nutritrack.user.config;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Accepts both Instant ({@code 2026-07-16T00:00:00Z}) and LocalDate ({@code 2026-07-16}) query
 * params for {@code Instant} controller arguments. Date-only values map to start of that UTC day.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addFormatters(@NonNull FormatterRegistry registry) {
    registry.addConverter(new StringToInstantConverter());
  }

  static final class StringToInstantConverter implements Converter<String, Instant> {
    @Override
    public Instant convert(@NonNull String source) {
      String value = source.trim();
      if (value.isEmpty()) {
        return null;
      }
      try {
        return Instant.parse(value);
      } catch (DateTimeParseException ignored) {
        // Fall through to LocalDate.
      }
      try {
        return LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant();
      } catch (DateTimeParseException ex) {
        throw new IllegalArgumentException(
            "Invalid Instant value '" + source + "'; expected ISO-8601 Instant or LocalDate", ex);
      }
    }
  }
}
