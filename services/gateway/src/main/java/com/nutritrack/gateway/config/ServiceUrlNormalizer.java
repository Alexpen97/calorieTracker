package com.nutritrack.gateway.config;

import java.net.URI;

/** Normalizes downstream service base URLs from environment variables. */
public final class ServiceUrlNormalizer {

  private ServiceUrlNormalizer() {}

  public static String normalize(String raw) {
    if (raw == null) {
      return null;
    }
    String trimmed = stripTrailingJunk(raw.trim());
    if (trimmed.isEmpty()) {
      return trimmed;
    }
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
      return requireHost(trimmed, raw);
    }
    return requireHost("http://" + trimmed, raw);
  }

  /**
   * Railway / dotenv paste mistakes often leave a trailing {@code =} (from {@code KEY=value} lines)
   * or quotes around the URL.
   */
  static String stripTrailingJunk(String value) {
    String cleaned = value;
    while (cleaned.length() >= 2
        && ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
            || (cleaned.startsWith("'") && cleaned.endsWith("'")))) {
      cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
    }
    while (cleaned.endsWith("=")) {
      cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
    }
    return cleaned;
  }

  private static String requireHost(String candidate, String raw) {
    try {
      URI uri = URI.create(candidate);
      if (uri.getHost() == null || uri.getHost().isBlank()) {
        throw new IllegalArgumentException("Service URL has no host: " + raw);
      }
      return candidate;
    } catch (IllegalArgumentException ex) {
      if (ex.getMessage() != null && ex.getMessage().contains("no host")) {
        throw ex;
      }
      throw new IllegalArgumentException("Service URL has no host: " + raw, ex);
    }
  }
}
