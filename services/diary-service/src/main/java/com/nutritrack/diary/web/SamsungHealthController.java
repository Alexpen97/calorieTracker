package com.nutritrack.diary.web;

import com.nutritrack.diary.service.SamsungHealthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SamsungHealthController {

  private final SamsungHealthService samsungHealthService;

  public SamsungHealthController(SamsungHealthService samsungHealthService) {
    this.samsungHealthService = samsungHealthService;
  }

  @GetMapping("/api/integrations/samsung-health/status")
  public SamsungHealthService.StatusResponse status(@AuthenticationPrincipal Jwt jwt) {
    return samsungHealthService.status(UUID.fromString(jwt.getSubject()));
  }

  @PostMapping("/api/integrations/samsung-health/sync")
  public SamsungHealthService.SyncResponse sync(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SyncBody body) {
    return samsungHealthService.sync(
        UUID.fromString(jwt.getSubject()),
        new SamsungHealthService.SyncRequest(
            body.zone(),
            body.permissionState(),
            body.days().stream()
                .map(
                    day ->
                        new SamsungHealthService.DayBurn(
                            day.localDate(),
                            day.activeEnergyKcal(),
                            day.totalEnergyKcal(),
                            day.selectedBurnKcal(),
                            day.sourceRecordCount() == null ? 0 : day.sourceRecordCount()))
                .toList()));
  }

  @DeleteMapping("/api/integrations/samsung-health")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void disconnect(@AuthenticationPrincipal Jwt jwt) {
    samsungHealthService.disconnect(UUID.fromString(jwt.getSubject()));
  }

  public record SyncBody(
      @NotBlank String zone, String permissionState, @NotEmpty List<@Valid DayBody> days) {}

  public record DayBody(
      @NotNull LocalDate localDate,
      BigDecimal activeEnergyKcal,
      BigDecimal totalEnergyKcal,
      BigDecimal selectedBurnKcal,
      Integer sourceRecordCount) {}
}
