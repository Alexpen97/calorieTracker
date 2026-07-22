package com.nutritrack.food.batch;

import com.nutritrack.food.config.FoodProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "nutritrack.food.bulk-import", name = "enabled", havingValue = "true")
public class OffBulkImportScheduler {

  private static final Logger log = LoggerFactory.getLogger(OffBulkImportScheduler.class);

  private final OffBulkImportLauncher launcher;
  private final FoodProperties properties;

  public OffBulkImportScheduler(OffBulkImportLauncher launcher, FoodProperties properties) {
    this.launcher = launcher;
    this.properties = properties;
  }

  @Scheduled(cron = "${nutritrack.food.bulk-import.cron:0 30 3 * * *}")
  public void runScheduledImport() {
    String input = properties.bulkImport().defaultUrl();
    if (input == null || input.isBlank()) {
      log.warn("Scheduled OFF bulk import skipped: nutritrack.food.bulk-import.default-url is empty");
      return;
    }
    try {
      launcher.launch(input);
    } catch (Exception ex) {
      log.error("Scheduled OFF bulk import failed", ex);
    }
  }
}
