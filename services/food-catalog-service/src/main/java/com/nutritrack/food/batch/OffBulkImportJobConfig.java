package com.nutritrack.food.batch;

import com.nutritrack.food.off.NormalizedOffProduct;
import com.nutritrack.food.off.OffNutrientNormalizer;
import com.nutritrack.food.service.OffProductUpsertService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.mapping.PassThroughLineMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class OffBulkImportJobConfig {

  public static final String JOB_NAME = "offBulkImportJob";
  public static final String PARAM_INPUT = "input.resource";

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @Bean
  Job offBulkImportJob(JobRepository jobRepository, Step offBulkImportStep) {
    return new JobBuilder(JOB_NAME, jobRepository).start(offBulkImportStep).build();
  }

  @Bean
  Step offBulkImportStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      FlatFileItemReader<String> offJsonlReader,
      ItemProcessor<String, NormalizedOffProduct> offJsonlProcessor,
      ItemWriter<NormalizedOffProduct> offProductWriter) {
    return new StepBuilder("offBulkImportStep", jobRepository)
        .<String, NormalizedOffProduct>chunk(50, transactionManager)
        .reader(offJsonlReader)
        .processor(offJsonlProcessor)
        .writer(offProductWriter)
        .build();
  }

  @Bean
  @StepScope
  FlatFileItemReader<String> offJsonlReader(
      @Value("#{jobParameters['input.resource']}") String inputResource) throws Exception {
    Resource resource = resolveResource(inputResource);
    return new FlatFileItemReaderBuilder<String>()
        .name("offJsonlReader")
        .resource(resource)
        .lineMapper(new PassThroughLineMapper())
        .strict(true)
        .build();
  }

  @Bean
  ItemProcessor<String, NormalizedOffProduct> offJsonlProcessor() {
    return line -> {
      if (line == null || line.isBlank()) {
        return null;
      }
      JsonNode node = jsonMapper.readTree(line);
      String code = text(node, "code");
      if (code == null || code.isBlank()) {
        return null;
      }
      return OffNutrientNormalizer.normalize(code.trim(), node);
    };
  }

  @Bean
  ItemWriter<NormalizedOffProduct> offProductWriter(OffProductUpsertService upsertService) {
    return items -> {
      for (NormalizedOffProduct item : items) {
        upsertService.upsertFromOff(item);
      }
    };
  }

  static Resource resolveResource(String input) throws Exception {
    if (input == null || input.isBlank()) {
      throw new IllegalArgumentException("input.resource job parameter is required");
    }
    if (input.startsWith("http://") || input.startsWith("https://")) {
      return new UrlResource(input);
    }
    return new FileSystemResource(input);
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    String text = value.asString();
    return text == null || text.isBlank() ? null : text;
  }
}
