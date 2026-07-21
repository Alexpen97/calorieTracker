package com.nutritrack.diary.service;

import com.nutritrack.diary.domain.WaterIntake;
import com.nutritrack.diary.domain.WaterIntakeRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WaterIntakeService {

  private final WaterIntakeRepository waterRepository;

  public WaterIntakeService(WaterIntakeRepository waterRepository) {
    this.waterRepository = waterRepository;
  }

  @Transactional
  public WaterIntake create(UUID userId, BigDecimal amountMl, Instant loggedAt) {
    WaterIntake water = new WaterIntake();
    water.setId(UUID.randomUUID());
    water.setUserId(userId);
    water.setAmountMl(amountMl);
    water.setLoggedAt(loggedAt == null ? Instant.now() : loggedAt);
    return waterRepository.save(water);
  }

  @Transactional(readOnly = true)
  public List<WaterIntake> listByDate(UUID userId, LocalDate date, ZoneId zone) {
    Instant from = DayBounds.startOfDay(date, zone);
    Instant to = DayBounds.startOfNextDay(date, zone);
    return waterRepository
        .findByUserIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThanOrderByLoggedAtDesc(
            userId, from, to);
  }

  @Transactional
  public void delete(UUID userId, UUID id) {
    WaterIntake water =
        waterRepository
            .findByIdAndUserId(id, userId)
            .orElseThrow(() -> new WaterIntakeNotFoundException(id));
    waterRepository.delete(water);
  }
}
