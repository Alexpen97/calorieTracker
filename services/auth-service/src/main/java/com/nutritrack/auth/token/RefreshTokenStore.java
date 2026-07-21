package com.nutritrack.auth.token;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenStore {

  private final Map<String, StoredRefreshToken> tokens = new ConcurrentHashMap<>();

  public void store(String token, UUID userId, String email, String role, Instant expiresAt) {
    tokens.put(token, new StoredRefreshToken(userId, email, role, expiresAt));
  }

  public Optional<StoredRefreshToken> consume(String token) {
    StoredRefreshToken stored = tokens.remove(token);
    if (stored == null) {
      return Optional.empty();
    }
    if (stored.expiresAt().isBefore(Instant.now())) {
      return Optional.empty();
    }
    return Optional.of(stored);
  }

  public void revoke(String token) {
    tokens.remove(token);
  }

  public record StoredRefreshToken(UUID userId, String email, String role, Instant expiresAt) {}
}
