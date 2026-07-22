package com.nutritrack.food.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutritrack.food.domain.ProductRepository;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ResourceUtils;

@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:food_batch;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.flyway.enabled=true",
      "spring.flyway.locations=classpath:db/migration",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused",
      "nutritrack.food.cache.redis-enabled=false",
      "spring.batch.jdbc.initialize-schema=always",
      "spring.batch.job.enabled=false",
      "spring.autoconfigure.exclude=org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.RedisRepositoriesAutoConfiguration"
    })
class OffBulkImportJobTest {

  @Autowired private OffBulkImportLauncher launcher;
  @Autowired private ProductRepository productRepository;

  @Test
  void importsJsonlFixtureIntoMirror() throws Exception {
    Path fixture = ResourceUtils.getFile("classpath:off-sample.jsonl").toPath();
    JobExecution execution = launcher.launch(fixture.toAbsolutePath().toString());
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(productRepository.findByBarcode("3017620422003")).isPresent();
    assertThat(productRepository.findByBarcode("5449000000996")).isPresent();
    assertThat(productRepository.findByBarcode("3017620422003").orElseThrow().getSearchDocument())
        .contains("nutella");
  }
}
