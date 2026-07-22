package com.nutritrack.user.service;

import com.nutritrack.user.domain.AppUser;
import com.nutritrack.user.domain.AppUserRepository;
import com.nutritrack.user.domain.BodyWeightLog;
import com.nutritrack.user.domain.BodyWeightLogRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WeightService {

  private final AppUserRepository userRepository;
  private final BodyWeightLogRepository weightRepository;

  public WeightService(AppUserRepository userRepository, BodyWeightLogRepository weightRepository) {
    this.userRepository = userRepository;
    this.weightRepository = weightRepository;
  }

  @Transactional
  public BodyWeightLog create(UUID userId, BigDecimal weightKg, Instant measuredAt) {
    AppUser user = requireUser(userId);
    BodyWeightLog log = new BodyWeightLog();
    log.setId(UUID.randomUUID());
    log.setUser(user);
    log.setWeightKg(weightKg);
    log.setMeasuredAt(measuredAt == null ? Instant.now() : measuredAt);
    return weightRepository.save(log);
  }

  @Transactional(readOnly = true)
  public List<BodyWeightLog> list(UUID userId, Instant from, Instant to) {
    requireUser(userId);
    if (from != null && to != null) {
      return weightRepository.findByUser_IdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
          userId, from, to);
    }
    if (from != null) {
      return weightRepository.findByUser_IdAndMeasuredAtGreaterThanEqualOrderByMeasuredAtDesc(
          userId, from);
    }
    if (to != null) {
      return weightRepository.findByUser_IdAndMeasuredAtLessThanEqualOrderByMeasuredAtDesc(
          userId, to);
    }
    return weightRepository.findByUser_IdOrderByMeasuredAtDesc(userId);
  }

  @Transactional
  public void delete(UUID userId, UUID weightId) {
    requireUser(userId);
    BodyWeightLog log =
        weightRepository
            .findByIdAndUser_Id(weightId, userId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Weight log not found"));
    weightRepository.delete(log);
  }

  private AppUser requireUser(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  }
}
