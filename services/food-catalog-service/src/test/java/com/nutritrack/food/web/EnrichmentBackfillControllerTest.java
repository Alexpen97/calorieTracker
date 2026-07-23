package com.nutritrack.food.web;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nutritrack.food.service.EnrichmentBackfillService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:food_admin;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
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
class EnrichmentBackfillControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private EnrichmentBackfillService enrichmentBackfillService;

  @Test
  void userForbidden() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/enrichment-backfill")
                .with(
                    SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(
                            jwt ->
                                jwt.subject(UUID.randomUUID().toString())
                                    .claim("roles", java.util.List.of("USER")))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminGetsCounts() throws Exception {
    when(enrichmentBackfillService.backfill(anyInt(), anyInt()))
        .thenReturn(Map.of("scanned", 3, "enriched", 2, "failed", 0, "page", 0, "size", 50));

    mockMvc
        .perform(
            post("/api/admin/enrichment-backfill")
                .with(
                    SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(
                            jwt ->
                                jwt.subject(UUID.randomUUID().toString())
                                    .claim("roles", java.util.List.of("ADMIN")))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scanned").value(3))
        .andExpect(jsonPath("$.enriched").value(2));
  }
}
