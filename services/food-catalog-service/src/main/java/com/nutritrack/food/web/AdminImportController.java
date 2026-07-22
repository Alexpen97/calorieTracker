package com.nutritrack.food.web;

import com.nutritrack.food.batch.OffBulkImportLauncher;
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

  public AdminImportController(OffBulkImportLauncher launcher) {
    this.launcher = launcher;
  }

  @PostMapping("/off-import")
  @PreAuthorize("hasRole('ADMIN')")
  public Map<String, Object> triggerImport(
      @RequestParam(value = "input", required = false) String input) throws Exception {
    JobExecution execution = launcher.launch(input);
    return launcher.toStatus(execution);
  }
}
