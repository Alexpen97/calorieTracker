package com.nutritrack.food.batch;

import com.nutritrack.food.config.FoodProperties;
import java.time.Instant;
import java.util.Map;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.stereotype.Service;

@Service
public class OffBulkImportLauncher {

  private final JobOperator jobOperator;
  private final Job offBulkImportJob;
  private final FoodProperties properties;

  public OffBulkImportLauncher(
      JobOperator jobOperator, Job offBulkImportJob, FoodProperties properties) {
    this.jobOperator = jobOperator;
    this.offBulkImportJob = offBulkImportJob;
    this.properties = properties;
  }

  public JobExecution launch(String inputResource) throws Exception {
    String resource = inputResource;
    if (resource == null || resource.isBlank()) {
      resource = properties.bulkImport().defaultUrl();
    }
    if (resource == null || resource.isBlank()) {
      throw new IllegalArgumentException("input resource (file path or URL) is required");
    }
    JobParameters params =
        new JobParametersBuilder()
            .addString(OffBulkImportJobConfig.PARAM_INPUT, resource)
            .addLong("launchedAt", Instant.now().toEpochMilli())
            .toJobParameters();
    return jobOperator.start(offBulkImportJob, params);
  }

  public Map<String, Object> toStatus(JobExecution execution) {
    return Map.of(
        "jobName", execution.getJobInstance().getJobName(),
        "executionId", execution.getId(),
        "status", execution.getStatus().toString(),
        "exitCode",
            execution.getExitStatus() == null ? "" : execution.getExitStatus().getExitCode(),
        "startTime", execution.getStartTime() == null ? "" : execution.getStartTime().toString(),
        "endTime", execution.getEndTime() == null ? "" : execution.getEndTime().toString());
  }
}
