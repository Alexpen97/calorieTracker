package com.nutritrack.nevo.imprt;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nutritrack.nevo.config.NevoProperties;
import com.nutritrack.nevo.domain.NevoFoodRepository;
import com.nutritrack.nevo.domain.NevoImportRun;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class NevoStartupImporterTest {

  @Mock private NevoFoodRepository foodRepository;
  @Mock private NevoCsvImporter importer;

  @Test
  void importsWhenDatabaseEmpty() {
    NevoProperties properties =
        new NevoProperties(
            "",
            "2025/9.0",
            "key",
            true,
            new NevoProperties.Match(0.72, 0.48, 25),
            new NevoProperties.Translate(
                false, "http://localhost:5000", "auto", "en", java.time.Duration.ofSeconds(3)));
    when(foodRepository.count()).thenReturn(0L);
    NevoImportRun run = new NevoImportRun();
    run.setId(UUID.randomUUID());
    run.setStatus("SUCCEEDED");
    run.setFoodCount(10);
    run.setNutrientCount(100);
    run.setCsvFilename("nevo.csv");
    run.setStartedAt(Instant.now());
    when(importer.importFromConfiguredPath()).thenReturn(run);

    new NevoStartupImporter(properties, foodRepository, importer)
        .run(new DefaultApplicationArguments(new String[0]));

    verify(importer).importFromConfiguredPath();
  }

  @Test
  void skipsWhenDatabaseAlreadyHasFoods() {
    NevoProperties properties =
        new NevoProperties(
            "",
            "2025/9.0",
            "key",
            true,
            new NevoProperties.Match(0.72, 0.48, 25),
            new NevoProperties.Translate(
                false, "http://localhost:5000", "auto", "en", java.time.Duration.ofSeconds(3)));
    when(foodRepository.count()).thenReturn(42L);

    new NevoStartupImporter(properties, foodRepository, importer)
        .run(new DefaultApplicationArguments(new String[0]));

    verify(importer, never()).importFromConfiguredPath();
  }

  @Test
  void skipsWhenAutoImportDisabled() {
    NevoProperties properties =
        new NevoProperties(
            "",
            "2025/9.0",
            "key",
            false,
            new NevoProperties.Match(0.72, 0.48, 25),
            new NevoProperties.Translate(
                false, "http://localhost:5000", "auto", "en", java.time.Duration.ofSeconds(3)));

    new NevoStartupImporter(properties, foodRepository, importer)
        .run(new DefaultApplicationArguments(new String[0]));

    verify(foodRepository, never()).count();
    verify(importer, never()).importFromConfiguredPath();
  }
}
