package com.nutritrack.user.service;

import com.nutritrack.user.domain.ActivityLevel;
import com.nutritrack.user.domain.AppUser;
import com.nutritrack.user.domain.AppUserRepository;
import com.nutritrack.user.domain.Objective;
import com.nutritrack.user.domain.Sex;
import com.nutritrack.user.domain.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

  private final AppUserRepository repository;

  public UserService(AppUserRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public AppUser upsertFromGoogle(String googleSub, String email, String displayName, String avatarUrl) {
    return repository
        .findByGoogleSub(googleSub)
        .map(
            existing -> {
              existing.setEmail(email);
              if (displayName != null && !displayName.isBlank()) {
                existing.setDisplayName(displayName);
              }
              if (avatarUrl != null && !avatarUrl.isBlank()) {
                existing.setAvatarUrl(avatarUrl);
              }
              return repository.save(existing);
            })
        .orElseGet(
            () -> {
              AppUser created = new AppUser();
              created.setId(UUID.randomUUID());
              created.setGoogleSub(googleSub);
              created.setEmail(email);
              created.setDisplayName(
                  displayName == null || displayName.isBlank() ? email : displayName);
              created.setAvatarUrl(blankToNull(avatarUrl));
              created.setRole(UserRole.USER);
              created.setObjective(Objective.MAINTAIN);
              created.setCreatedAt(Instant.now());
              return repository.save(created);
            });
  }

  @Transactional(readOnly = true)
  public AppUser requireById(UUID userId) {
    return repository
        .findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  }

  @Transactional
  public AppUser updateProfile(UUID userId, ProfileUpdate update) {
    AppUser user = requireById(userId);
    if (update.displayName() != null) {
      user.setDisplayName(update.displayName());
    }
    if (update.sex() != null) {
      user.setSex(update.sex());
    }
    if (update.birthDate() != null) {
      user.setBirthDate(update.birthDate());
    }
    if (update.heightCm() != null) {
      user.setHeightCm(update.heightCm());
    }
    if (update.activityLevel() != null) {
      user.setActivityLevel(update.activityLevel());
    }
    if (update.objective() != null) {
      user.setObjective(update.objective());
    }
    return repository.save(user);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  public record ProfileUpdate(
      String displayName,
      Sex sex,
      LocalDate birthDate,
      BigDecimal heightCm,
      ActivityLevel activityLevel,
      Objective objective) {}
}
