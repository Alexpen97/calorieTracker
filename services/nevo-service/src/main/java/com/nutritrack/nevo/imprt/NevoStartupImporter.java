package com.nutritrack.nevo.imprt;

import com.nutritrack.nevo.config.NevoProperties;
import com.nutritrack.nevo.domain.NevoFoodRepository;
import com.nutritrack.nevo.domain.NevoImportRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Loads the configured NEVO CSV once when {@code nevo_food} is empty. Restarts with an
 * already-populated database skip the import. Manual {@code POST /internal/nevo/import}
 * remains available for forced reloads.
 */
@Component
public class NevoStartupImporter implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(NevoStartupImporter.class);

  private final NevoProperties properties;
  private final NevoFoodRepository foodRepository;
  private final NevoCsvImporter importer;

  public NevoStartupImporter(
      NevoProperties properties, NevoFoodRepository foodRepository, NevoCsvImporter importer) {
    this.properties = properties;
    this.foodRepository = foodRepository;
    this.importer = importer;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!properties.autoImportOnStartup()) {
      log.info("NEVO auto-import on startup is disabled");
      return;
    }
    long existing = foodRepository.count();
    if (existing > 0) {
      log.info("NEVO database already has {} foods; skipping startup import", existing);
      return;
    }
    log.info("NEVO database is empty; importing configured CSV");
    NevoImportRun run = importer.importFromConfiguredPath();
    log.info(
        "NEVO startup import {} (foods={}, nutrients={}, file={})",
        run.getStatus(),
        run.getFoodCount(),
        run.getNutrientCount(),
        run.getCsvFilename());
  }
}
