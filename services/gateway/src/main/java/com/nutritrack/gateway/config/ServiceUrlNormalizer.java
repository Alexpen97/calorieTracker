package com.nutritrack.gateway.config;

import java.net.URI;

/** Normalizes downstream service base URLs from environment variables. */
public final class ServiceUrlNormalizer {

  private ServiceUrlNormalizer() {}

  public static String normalize(String raw) {
    if (raw == null) {
      return null;
    }
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) {
      return trimmed;
    }
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
      return requireHost(trimmed, raw);
    }
    return requireHost("http://" + trimmed, raw);
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
