package com.nutritrack.user.web;

import com.nutritrack.user.domain.BodyWeightLog;
import com.nutritrack.user.service.WeightService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeightController {

  private final WeightService weightService;

  public WeightController(WeightService weightService) {
    this.weightService = weightService;
  }

  @PostMapping("/api/users/me/weight")
  public WeightResponse createWeight(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateWeightRequest request) {
    BodyWeightLog log =
        weightService.create(
            UUID.fromString(jwt.getSubject()), request.weightKg(), request.measuredAt());
    return WeightResponse.from(log);
  }

  @GetMapping("/api/users/me/weight")
  public List<WeightResponse> listWeights(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to) {
    return weightService.list(UUID.fromString(jwt.getSubject()), from, to).stream()
        .map(WeightResponse::from)
        .toList();
  }

  @DeleteMapping("/api/users/me/weight/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteWeight(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
    weightService.delete(UUID.fromString(jwt.getSubject()), id);
  }

  public record CreateWeightRequest(@NotNull @Positive BigDecimal weightKg, Instant measuredAt) {}

  public record WeightResponse(UUID id, BigDecimal weightKg, Instant measuredAt) {
    static WeightResponse from(BodyWeightLog log) {
      return new WeightResponse(log.getId(), log.getWeightKg(), log.getMeasuredAt());
    }
  }
}
