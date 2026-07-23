package com.nutritrack.food.web;

import com.nutritrack.food.batch.OffBulkImportLauncher;
import com.nutritrack.food.service.EnrichmentBackfillService;
import java.util.Map;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminImportController {

  private final OffBulkImportLauncher launcher;
  private final EnrichmentBackfillService enrichmentBackfillService;

  public AdminImportController(
      OffBulkImportLauncher launcher, EnrichmentBackfillService enrichmentBackfillService) {
    this.launcher = launcher;
    this.enrichmentBackfillService = enrichmentBackfillService;
  }

  @PostMapping("/off-import")
  @PreAuthorize("hasRole('ADMIN')")
  public Map<String, Object> triggerImport(
      @RequestParam(value = "input", required = false) String input) throws Exception {
    JobExecution execution = launcher.launch(input);
    return launcher.toStatus(execution);
  }

  @PostMapping("/enrichment-backfill")
  @PreAuthorize("hasRole('ADMIN')")
  public Map<String, Object> enrichmentBackfill(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "50") int size) {
    return enrichmentBackfillService.backfill(page, Math.min(Math.max(size, 1), 200));
  }
}
