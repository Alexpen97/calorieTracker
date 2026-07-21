package com.nutritrack.auth.web;

import com.nutritrack.auth.client.UserProfileClient;
import com.nutritrack.auth.google.GoogleTokenClient;
import com.nutritrack.auth.token.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthController {

  private final GoogleTokenClient googleTokenClient;
  private final UserProfileClient userProfileClient;
  private final TokenService tokenService;

  public AuthController(
      GoogleTokenClient googleTokenClient,
      UserProfileClient userProfileClient,
      TokenService tokenService) {
    this.googleTokenClient = googleTokenClient;
    this.userProfileClient = userProfileClient;
    this.tokenService = tokenService;
  }

  @PostMapping("/api/auth/google/callback")
  public ResponseEntity<TokenResponse> googleCallback(@Valid @RequestBody GoogleCallbackRequest request) {
    try {
      GoogleTokenClient.GoogleIdentity identity =
          googleTokenClient.exchangeAuthorizationCode(
              request.code(), request.codeVerifier(), request.redirectUri());
      UserProfileClient.UpsertedUser user =
          userProfileClient.upsert(
              identity.sub(), identity.email(), identity.displayName(), identity.avatarUrl());
      TokenService.IssuedTokens tokens =
          tokenService.issueTokens(user.id(), user.email(), user.role());
      return withRefreshCookie(tokens);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
  }

  @PostMapping("/api/auth/refresh")
  public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    try {
      TokenService.IssuedTokens tokens = tokenService.refresh(request.refreshToken());
      return withRefreshCookie(tokens);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }
  }

  @PostMapping("/api/auth/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
    tokenService.revoke(request.refreshToken());
    ResponseCookie clear =
        ResponseCookie.from("refresh_token", "")
            .httpOnly(true)
            .secure(false)
            .path("/api/auth")
            .maxAge(0)
            .sameSite("Lax")
            .build();
    return ResponseEntity.noContent().header("Set-Cookie", clear.toString()).build();
  }

  @GetMapping("/.well-known/jwks.json")
  public Map<String, Object> jwks() {
    return tokenService.jwks();
  }

  private ResponseEntity<TokenResponse> withRefreshCookie(TokenService.IssuedTokens tokens) {
    long maxAgeSeconds =
        Math.max(0, Duration.between(Instant.now(), tokens.refreshExpiresAt()).getSeconds());
    ResponseCookie cookie =
        ResponseCookie.from("refresh_token", tokens.refreshToken())
            .httpOnly(true)
            .secure(false)
            .path("/api/auth")
            .maxAge(maxAgeSeconds)
            .sameSite("Lax")
            .build();
    TokenResponse body =
        new TokenResponse(
            tokens.accessToken(),
            tokens.refreshToken(),
            "Bearer",
            Math.max(
                0, Duration.between(Instant.now(), tokens.accessExpiresAt()).getSeconds()));
    return ResponseEntity.ok().header("Set-Cookie", cookie.toString()).body(body);
  }

  public record GoogleCallbackRequest(
      @NotBlank String code, String codeVerifier, @NotBlank String redirectUri) {}

  public record RefreshRequest(@NotBlank String refreshToken) {}

  public record TokenResponse(
      String accessToken, String refreshToken, String tokenType, long expiresIn) {}
}
