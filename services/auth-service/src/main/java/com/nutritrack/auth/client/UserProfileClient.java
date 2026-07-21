package com.nutritrack.auth.client;

import com.nutritrack.auth.config.AuthProperties;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class UserProfileClient {

  private final WebClient webClient;
  private final AuthProperties properties;

  public UserProfileClient(WebClient.Builder builder, AuthProperties properties) {
    this.properties = properties;
    this.webClient = builder.baseUrl(properties.userServiceUrl()).build();
  }

  public UpsertedUser upsert(String googleSub, String email, String displayName, String avatarUrl) {
    Map<String, Object> body =
        Map.of(
            "googleSub", googleSub,
            "email", email,
            "displayName", displayName == null ? "" : displayName,
            "avatarUrl", avatarUrl == null ? "" : avatarUrl);

    Map<String, Object> response =
        webClient
            .post()
            .uri("/api/users/internal/upsert")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Internal-Api-Key", properties.internalApiKey())
            .bodyValue(body)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .block();

    if (response == null || response.get("id") == null) {
      throw new IllegalStateException("user-profile-service upsert returned empty response");
    }
    return new UpsertedUser(
        UUID.fromString(String.valueOf(response.get("id"))),
        String.valueOf(response.get("email")),
        String.valueOf(response.getOrDefault("role", "USER")));
  }

  public record UpsertedUser(UUID id, String email, String role) {}
}
