package com.nutritrack.auth.google;

import com.nutritrack.auth.config.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class GoogleTokenClient {

  private final WebClient webClient;
  private final AuthProperties properties;

  public GoogleTokenClient(WebClient.Builder builder, AuthProperties properties) {
    this.webClient = builder.build();
    this.properties = properties;
  }

  public GoogleIdentity exchangeAuthorizationCode(String code, String codeVerifier, String redirectUri) {
    if (properties.isDevMode() && ("dev".equals(code) || code.startsWith("dev:"))) {
      return parseDevIdentity(code);
    }

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "authorization_code");
    form.add("code", code);
    form.add("client_id", properties.googleClientId());
    form.add("client_secret", properties.googleClientSecret());
    // Web PKCE requires redirect_uri; Android server-auth-code exchange omits it.
    if (redirectUri != null && !redirectUri.isBlank()) {
      form.add("redirect_uri", redirectUri);
    }
    if (codeVerifier != null && !codeVerifier.isBlank()) {
      form.add("code_verifier", codeVerifier);
    }

    Map<String, Object> tokenResponse =
        webClient
            .post()
            .uri(properties.googleTokenUri())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(form))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .block();

    if (tokenResponse == null || tokenResponse.get("id_token") == null) {
      throw new IllegalArgumentException("Google token exchange failed");
    }
    return parseIdToken((String) tokenResponse.get("id_token"));
  }

  private GoogleIdentity parseDevIdentity(String code) {
    String suffix = code.startsWith("dev:") ? code.substring(4) : "dev-user";
    return new GoogleIdentity(
        "dev-sub-" + suffix,
        suffix.contains("@") ? suffix : suffix + "@example.com",
        "Dev User",
        null);
  }

  @SuppressWarnings("unchecked")
  private GoogleIdentity parseIdToken(String idToken) {
    String[] parts = idToken.split("\\.");
    if (parts.length < 2) {
      throw new IllegalArgumentException("Malformed Google ID token");
    }
    String json =
        new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
    // Minimal JSON parse without pulling another dependency — use Jackson via Spring.
    try {
      Map<String, Object> claims =
          new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
      String aud = String.valueOf(claims.get("aud"));
      if (properties.googleClientId() != null
          && !properties.googleClientId().isBlank()
          && !properties.googleClientId().equals(aud)) {
        throw new IllegalArgumentException("Invalid Google ID token audience");
      }
      Object exp = claims.get("exp");
      if (exp instanceof Number number && Instant.ofEpochSecond(number.longValue()).isBefore(Instant.now())) {
        throw new IllegalArgumentException("Google ID token expired");
      }
      return new GoogleIdentity(
          String.valueOf(claims.get("sub")),
          String.valueOf(claims.getOrDefault("email", "")),
          String.valueOf(claims.getOrDefault("name", "")),
          claims.get("picture") == null ? null : String.valueOf(claims.get("picture")));
    } catch (IllegalArgumentException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalArgumentException("Unable to parse Google ID token", ex);
    }
  }

  public record GoogleIdentity(String sub, String email, String displayName, String avatarUrl) {}
}
