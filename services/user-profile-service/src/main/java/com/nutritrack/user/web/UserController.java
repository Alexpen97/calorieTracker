package com.nutritrack.user.web;

import com.nutritrack.user.config.UserProperties;
import com.nutritrack.user.domain.ActivityLevel;
import com.nutritrack.user.domain.AppUser;
import com.nutritrack.user.domain.Objective;
import com.nutritrack.user.domain.Sex;
import com.nutritrack.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UserController {

  private final UserService userService;
  private final UserProperties properties;

  public UserController(UserService userService, UserProperties properties) {
    this.userService = userService;
    this.properties = properties;
  }

  @PostMapping("/api/users/internal/upsert")
  public UserResponse upsert(
      @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
      @Valid @RequestBody UpsertRequest request) {
    if (apiKey == null || !apiKey.equals(properties.internalApiKey())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal API key");
    }
    AppUser user =
        userService.upsertFromGoogle(
            request.googleSub(), request.email(), request.displayName(), request.avatarUrl());
    return UserResponse.from(user);
  }

  @GetMapping("/api/users/me")
  public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
    return UserResponse.from(userService.requireById(UUID.fromString(jwt.getSubject())));
  }

  @PutMapping("/api/users/me")
  public UserResponse updateMe(
      @AuthenticationPrincipal Jwt jwt, @RequestBody UpdateProfileRequest request) {
    AppUser updated =
        userService.updateProfile(
            UUID.fromString(jwt.getSubject()),
            new UserService.ProfileUpdate(
                request.displayName(),
                request.sex(),
                request.birthDate(),
                request.heightCm(),
                request.activityLevel(),
                request.objective()));
    return UserResponse.from(updated);
  }

  public record UpsertRequest(
      @NotBlank String googleSub,
      @NotBlank String email,
      String displayName,
      String avatarUrl) {}

  public record UpdateProfileRequest(
      String displayName,
      Sex sex,
      LocalDate birthDate,
      BigDecimal heightCm,
      ActivityLevel activityLevel,
      Objective objective) {}

  public record UserResponse(
      UUID id,
      String googleSub,
      String email,
      String displayName,
      String avatarUrl,
      String role,
      Sex sex,
      LocalDate birthDate,
      BigDecimal heightCm,
      ActivityLevel activityLevel,
      Objective objective,
      Instant createdAt) {
    static UserResponse from(AppUser user) {
      return new UserResponse(
          user.getId(),
          user.getGoogleSub(),
          user.getEmail(),
          user.getDisplayName(),
          user.getAvatarUrl(),
          user.getRole().name(),
          user.getSex(),
          user.getBirthDate(),
          user.getHeightCm(),
          user.getActivityLevel(),
          user.getObjective(),
          user.getCreatedAt());
    }
  }
}
