package com.nutritrack.enrichment.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nutritrack.enrichment.fdc.FdcClient;
import com.nutritrack.enrichment.fdc.FdcFoodDetail;
import com.nutritrack.enrichment.fdc.FdcSearchHit;
import com.nutritrack.enrichment.fdc.MappedNutrient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:enrich_api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.flyway.enabled=true",
      "spring.flyway.locations=classpath:db/migration",
      "spring.jpa.hibernate.ddl-auto=validate",
      "nutritrack.enrichment.internal-api-key=test-internal",
      "nutritrack.enrichment.fdc.base-url=http://127.0.0.1:9",
      "nutritrack.enrichment.fdc.api-key=DEMO_KEY"
    })
class EnrichmentControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private FdcClient fdcClient;

  @BeforeEach
  void resetFdcClient() {
    reset(fdcClient);
  }

  @Test
  void unauthorizedWithoutKey() throws Exception {
    mockMvc
        .perform(
            post("/internal/enrich")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"barcode":"3017620422003","name":"Nutella","brand":"Ferrero",
                     "existingNutrientCodes":[]}
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void gtinMatchReturnsNutrients() throws Exception {
    when(fdcClient.searchFoods(eq("3017620422003"), eq(List.of("Branded")), isNull(), anyInt()))
        .thenReturn(
            List.of(
                new FdcSearchHit(
                    2262074L, "NUTELLA", "Ferrero", "3017620422003", "Branded")));
    when(fdcClient.getFood(2262074L))
        .thenReturn(
            Optional.of(
                new FdcFoodDetail(
                    2262074L,
                    "NUTELLA",
                    "Branded",
                    List.of(new MappedNutrient("calcium", new BigDecimal("80"), "mg")))));

    mockMvc
        .perform(
            post("/internal/enrich")
                .header("X-Internal-Api-Key", "test-internal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"barcode":"3017620422003","name":"Nutella","brand":"Ferrero",
                     "existingNutrientCodes":[]}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.matchType").value("GTIN"))
        .andExpect(jsonPath("$.fdcId").value(2262074))
        .andExpect(jsonPath("$.nutrients[0].code").value("calcium"));
  }

  @Test
  void filtersExistingCodesAndCachesSecondCall() throws Exception {
    when(fdcClient.searchFoods(eq("0001112223334"), eq(List.of("Branded")), isNull(), anyInt()))
        .thenReturn(
            List.of(
                new FdcSearchHit(
                    99L, "WHOLE MILK", "Dairy Co", "0001112223334", "Branded")));
    when(fdcClient.getFood(99L))
        .thenReturn(
            Optional.of(
                new FdcFoodDetail(
                    99L,
                    "WHOLE MILK",
                    "Branded",
                    List.of(
                        new MappedNutrient("calcium", new BigDecimal("113"), "mg"),
                        new MappedNutrient("vitamin_c", new BigDecimal("5"), "mg")))));

    String body =
        """
        {"barcode":"0001112223334","name":"Whole Milk","brand":"Dairy Co",
         "existingNutrientCodes":["calcium"]}
        """;

    mockMvc
        .perform(
            post("/internal/enrich")
                .header("X-Internal-Api-Key", "test-internal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nutrients.length()").value(1))
        .andExpect(jsonPath("$.nutrients[0].code").value("vitamin_c"));

    mockMvc
        .perform(
            post("/internal/enrich")
                .header("X-Internal-Api-Key", "test-internal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.matchType").value("GTIN"));

    verify(fdcClient, times(1)).searchFoods(anyString(), anyList(), any(), anyInt());
    verify(fdcClient, times(1)).getFood(anyLong());
  }

  @Test
  void fortifiedNameSkipsGenericProxyAndReturnsNone() throws Exception {
    when(fdcClient.searchFoods(anyString(), anyList(), any(), anyInt())).thenReturn(List.of());

    mockMvc
        .perform(
            post("/internal/enrich")
                .header("X-Internal-Api-Key", "test-internal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"barcode":"9998887776665","name":"Fortified breakfast cereal",
                     "brand":"BrandX","existingNutrientCodes":[]}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.matchType").value("NONE"))
        .andExpect(jsonPath("$.nutrients.length()").value(0));

    // Branded searches only (GTIN + name+brand ± brand retry) — no Foundation/SR
    verify(fdcClient, never()).searchFoods(anyString(), eq(List.of("Foundation")), any(), anyInt());
    verify(fdcClient, never()).searchFoods(anyString(), eq(List.of("SR Legacy")), any(), anyInt());
  }

  @Test
  void genericProxyWhenNoBrandedHit() throws Exception {
    when(fdcClient.searchFoods(eq("5554443332221"), eq(List.of("Branded")), isNull(), anyInt()))
        .thenReturn(List.of());
    when(fdcClient.searchFoods(eq("Whole milk"), eq(List.of("Branded")), eq("Farm"), anyInt()))
        .thenReturn(List.of());
    when(fdcClient.searchFoods(eq("Whole milk"), eq(List.of("Branded")), isNull(), anyInt()))
        .thenReturn(List.of());
    when(fdcClient.searchFoods(eq("whole milk"), eq(List.of("Foundation")), isNull(), anyInt()))
        .thenReturn(List.of(new FdcSearchHit(171265L, "Milk, whole", null, null, "Foundation")));
    when(fdcClient.getFood(171265L))
        .thenReturn(
            Optional.of(
                new FdcFoodDetail(
                    171265L,
                    "Milk, whole",
                    "Foundation",
                    List.of(new MappedNutrient("calcium", new BigDecimal("113"), "mg")))));

    mockMvc
        .perform(
            post("/internal/enrich")
                .header("X-Internal-Api-Key", "test-internal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"barcode":"5554443332221","name":"Whole milk","brand":"Farm",
                     "existingNutrientCodes":[]}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.matchType").value("GENERIC_PROXY"))
        .andExpect(jsonPath("$.nutrients[0].code").value("calcium"));
  }
}
