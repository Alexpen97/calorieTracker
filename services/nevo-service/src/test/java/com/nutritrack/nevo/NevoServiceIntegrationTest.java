package com.nutritrack.nevo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nutritrack.nevo.domain.NevoFoodRepository;
import com.nutritrack.nevo.imprt.NevoCsvImporter;
import com.nutritrack.nevo.web.dto.NevoMatchRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:nevo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.flyway.enabled=true",
      "spring.flyway.locations=classpath:db/migration",
      "spring.jpa.hibernate.ddl-auto=validate",
      "nutritrack.nevo.internal-api-key=test-internal",
      "nutritrack.nevo.version=2025/9.0"
    })
class NevoServiceIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private NevoCsvImporter importer;
  @Autowired private NevoFoodRepository foodRepository;
  @Autowired private JsonMapper jsonMapper;

  @BeforeEach
  void importSample() {
    importer.importResource(new ClassPathResource("nevo-sample.csv"), "nevo-sample.csv");
    assertThat(foodRepository.count()).isGreaterThan(0);
  }

  @Test
  void importAndMatchCornflakes() throws Exception {
    NevoMatchRequest request =
        new NevoMatchRequest(
            "Kellogg's Corn Flakes Original 500g",
            "Kellogg's",
            null,
            List.of("breakfast cereals"),
            "maize, sugar, salt",
            List.of(
                new NevoMatchRequest.KnownMacro("energy_kcal", new BigDecimal("370"), "kcal"),
                new NevoMatchRequest.KnownMacro("protein", new BigDecimal("7"), "g"),
                new NevoMatchRequest.KnownMacro("carbohydrates", new BigDecimal("84"), "g")));

    mockMvc
        .perform(
            post("/internal/nevo/matches/best")
                .header("X-Internal-Api-Key", "test-internal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.matched").value(true))
        .andExpect(jsonPath("$.nevoCode").value("1003"))
        .andExpect(jsonPath("$.confidence").value("HIGH"))
        .andExpect(jsonPath("$.nutrients[?(@.code=='iron')].amountPer100g").exists());
  }

  @Test
  void matchRejectsWithoutApiKey() throws Exception {
    mockMvc
        .perform(
            post("/internal/nevo/matches/best")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"banana\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void matchWholeMilkAndPlantDrinkStayDistinct() throws Exception {
    NevoMatchRequest milk =
        new NevoMatchRequest(
            "Whole milk",
            null,
            null,
            List.of("Milk and milk products"),
            null,
            List.of(new NevoMatchRequest.KnownMacro("fat", new BigDecimal("3.5"), "g")));
    mockMvc
        .perform(
            post("/internal/nevo/matches/best")
                .header("X-Internal-Api-Key", "test-internal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(milk)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nevoCode").value("1001"));

    NevoMatchRequest soy =
        new NevoMatchRequest(
            "Plant-based soy drink unsweetened",
            null,
            null,
            List.of("Meat substitutes and dairy substitutes"),
            "water, soybeans",
            List.of(new NevoMatchRequest.KnownMacro("protein", new BigDecimal("3.3"), "g")));
    mockMvc
        .perform(
            post("/internal/nevo/matches/best")
                .header("X-Internal-Api-Key", "test-internal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(soy)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nevoCode").value("1007"));
  }

  @Test
  void importsRealClasspathNevoMatrix() {
    // Smoke-test the actual RIVM export shipped under resources/nevo/.
    var run =
        importer.importResource(
            new ClassPathResource(NevoCsvImporter.DEFAULT_CLASSPATH_CSV),
            NevoCsvImporter.DEFAULT_CLASSPATH_CSV);
    assertThat(run.getStatus()).isEqualTo("SUCCEEDED");
    assertThat(run.getFoodCount()).isGreaterThan(2000);
    assertThat(foodRepository.findById("1")).isPresent();
    assertThat(foodRepository.findById("1").get().getFoodNameEn()).containsIgnoringCase("Potato");
  }
}
