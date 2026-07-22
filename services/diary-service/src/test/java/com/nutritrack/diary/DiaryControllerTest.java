package com.nutritrack.diary;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nutritrack.diary.client.FoodCatalogClient;
import com.nutritrack.diary.client.ProductNotFoundException;
import com.nutritrack.diary.client.ProductResponse;
import com.nutritrack.diary.client.UserGoalsClient;
import com.nutritrack.diary.client.UserGoalResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:diary;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.flyway.enabled=true",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused",
      "nutritrack.diary.food-service-url=http://localhost:8083",
      "nutritrack.diary.user-service-url=http://localhost:8082"
    })
class DiaryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private FoodCatalogClient foodCatalogClient;

  @MockitoBean private UserGoalsClient userGoalsClient;

  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void createEntryPersistsProductSnapshotAndReturnsScaledNutrients() throws Exception {
    UUID productId = UUID.randomUUID();
    Jwt jwt = jwtForUser("00000000-0000-0000-0000-000000000101");
    when(jwtDecoder.decode("caller-token")).thenReturn(jwt);
    when(foodCatalogClient.getProduct(eq(productId), eq("Bearer caller-token")))
        .thenReturn(product(productId, "Original oats", "Acme", "200", "10"));

    MvcResult created =
        mockMvc
            .perform(
                post("/api/diary/entries")
                    .header("Authorization", "Bearer caller-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"productId":"%s","weightG":50,"mealType":"BREAKFAST","consumedAt":"2026-07-21T08:00:00Z"}
                        """
                            .formatted(productId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.productId").value(productId.toString()))
            .andExpect(jsonPath("$.productName").value("Original oats"))
            .andExpect(jsonPath("$.brand").value("Acme"))
            .andExpect(jsonPath("$.weightG").value(50))
            .andExpect(jsonPath("$.mealType").value("BREAKFAST"))
            .andExpect(jsonPath("$.consumedAt").value("2026-07-21T08:00:00Z"))
            .andExpect(jsonPath("$.nutrients[?(@.code == 'energy_kcal')].amount").value(100.00))
            .andExpect(jsonPath("$.nutrients[?(@.code == 'protein')].amount").value(5.00))
            .andReturn();

    when(foodCatalogClient.getProduct(eq(productId), eq("Bearer caller-token")))
        .thenReturn(product(productId, "Renamed oats", "Changed", "400", "20"));

    mockMvc
        .perform(
            get("/api/diary/entries")
                .queryParam("date", "2026-07-21")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(entryId(created)))
        .andExpect(jsonPath("$[0].productName").value("Original oats"))
        .andExpect(jsonPath("$[0].nutrients[?(@.code == 'energy_kcal')].amount").value(100.00));
  }

  @Test
  void listByDateReturnsOnlyJwtUsersEntriesNewestFirst() throws Exception {
    UUID productId = UUID.randomUUID();
    Jwt owner = jwtForUser("00000000-0000-0000-0000-000000000201");
    Jwt other = jwtForUser("00000000-0000-0000-0000-000000000202");
    when(foodCatalogClient.getProduct(eq(productId), eq("Bearer caller-token")))
        .thenReturn(product(productId, "Yogurt", "Dairy", "100", "8"));

    createEntry(owner, productId, "BREAKFAST", "2026-07-21T08:00:00Z", "100");
    createEntry(owner, productId, "SNACK", "2026-07-21T20:00:00Z", "125");
    createEntry(owner, productId, "DINNER", "2026-07-22T00:30:00Z", "150");
    createEntry(other, productId, "LUNCH", "2026-07-21T12:00:00Z", "200");

    mockMvc
        .perform(
            get("/api/diary/entries")
                .queryParam("date", "2026-07-21")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].mealType").value("SNACK"))
        .andExpect(jsonPath("$[0].consumedAt").value("2026-07-21T20:00:00Z"))
        .andExpect(jsonPath("$[1].mealType").value("BREAKFAST"))
        .andExpect(jsonPath("$[1].consumedAt").value("2026-07-21T08:00:00Z"));
  }

  @Test
  void updateWeightRecalculatesFromStoredSnapshotWithoutRefetchingProduct() throws Exception {
    UUID productId = UUID.randomUUID();
    Jwt jwt = jwtForUser("00000000-0000-0000-0000-000000000301");
    when(foodCatalogClient.getProduct(eq(productId), eq("Bearer caller-token")))
        .thenReturn(product(productId, "Pasta", "Mill", "200", "12"));
    MvcResult created = createEntry(jwt, productId, "LUNCH", "2026-07-21T12:00:00Z", "50");

    mockMvc
        .perform(
            put("/api/diary/entries/{id}", entryId(created))
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"weightG":125,"mealType":"DINNER","consumedAt":"2026-07-21T18:00:00Z"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weightG").value(125))
        .andExpect(jsonPath("$.mealType").value("DINNER"))
        .andExpect(jsonPath("$.consumedAt").value("2026-07-21T18:00:00Z"))
        .andExpect(jsonPath("$.nutrients[?(@.code == 'energy_kcal')].amount").value(250.00))
        .andExpect(jsonPath("$.nutrients[?(@.code == 'protein')].amount").value(15.00));

    verify(foodCatalogClient).getProduct(productId, "Bearer caller-token");
  }

  @Test
  void deleteRemovesOwnEntryAndReturns404ForOtherUsersEntry() throws Exception {
    UUID productId = UUID.randomUUID();
    Jwt owner = jwtForUser("00000000-0000-0000-0000-000000000401");
    Jwt other = jwtForUser("00000000-0000-0000-0000-000000000402");
    when(foodCatalogClient.getProduct(eq(productId), eq("Bearer caller-token")))
        .thenReturn(product(productId, "Bread", "Bake", "250", "9"));
    MvcResult created = createEntry(owner, productId, "SNACK", "2026-07-21T15:00:00Z", "40");
    String entryId = entryId(created);

    mockMvc
        .perform(
            delete("/api/diary/entries/{id}", entryId)
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(other)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            delete("/api/diary/entries/{id}", entryId)
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(owner)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/diary/entries")
                .queryParam("date", "2026-07-21")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void productNotFoundPropagatesAs404() throws Exception {
    UUID productId = UUID.randomUUID();
    Jwt jwt = jwtForUser("00000000-0000-0000-0000-000000000501");
    when(jwtDecoder.decode("caller-token")).thenReturn(jwt);
    when(foodCatalogClient.getProduct(eq(productId), eq("Bearer caller-token")))
        .thenThrow(new ProductNotFoundException(productId));

    mockMvc
        .perform(
            post("/api/diary/entries")
                .header("Authorization", "Bearer caller-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"productId":"%s","weightG":50,"mealType":"BREAKFAST","consumedAt":"2026-07-21T08:00:00Z"}
                    """
                        .formatted(productId)))
        .andExpect(status().isNotFound());

    verify(foodCatalogClient).getProduct(productId, "Bearer caller-token");
  }

  @Test
  void waterCrudReturnsOnlyJwtUsersLogsAndDeletesOwnLogs() throws Exception {
    Jwt owner = jwtForUser("00000000-0000-0000-0000-000000000601");
    Jwt other = jwtForUser("00000000-0000-0000-0000-000000000602");

    createWater(owner, "500", "2026-07-21T08:00:00Z");
    MvcResult newest = createWater(owner, "750", "2026-07-21T20:00:00Z");
    createWater(owner, "250", "2026-07-22T00:00:00Z");
    createWater(other, "1000", "2026-07-21T12:00:00Z");

    mockMvc
        .perform(
            get("/api/diary/water")
                .queryParam("date", "2026-07-21")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].amountMl").value(750.00))
        .andExpect(jsonPath("$[0].loggedAt").value("2026-07-21T20:00:00Z"))
        .andExpect(jsonPath("$[1].amountMl").value(500.00))
        .andExpect(jsonPath("$[1].loggedAt").value("2026-07-21T08:00:00Z"));

    mockMvc
        .perform(
            delete("/api/diary/water/{id}", entryId(newest))
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(other)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            delete("/api/diary/water/{id}", entryId(newest))
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(owner)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/diary/water")
                .queryParam("date", "2026-07-21")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].amountMl").value(500.00));
  }

  @Test
  void createWaterRejectsNonPositiveAmount() throws Exception {
    Jwt jwt = jwtForUser("00000000-0000-0000-0000-000000000701");

    mockMvc
        .perform(
            post("/api/diary/water")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"amountMl":0,"loggedAt":"2026-07-21T08:00:00Z"}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void dailySummaryAggregatesFoodWaterAndTargets() throws Exception {
    UUID productId = UUID.randomUUID();
    Jwt jwt = jwtForUser("00000000-0000-0000-0000-000000000801");
    when(foodCatalogClient.getProduct(eq(productId), eq("Bearer caller-token")))
        .thenReturn(product(productId, "Rice", "Mill", "200", "10"));
    when(userGoalsClient.getGoals(eq("Bearer caller-token")))
        .thenReturn(
            List.of(
                goal("energy_kcal", "2200.00", "kcal"),
                goal("protein", "100.00", "g"),
                goal("water_ml", "2600.00", "ml")));

    createEntry(jwt, productId, "BREAKFAST", "2026-07-21T08:00:00Z", "100");
    createEntry(jwt, productId, "DINNER", "2026-07-21T18:00:00Z", "50");
    createWater(jwt, "500", "2026-07-21T09:00:00Z");
    createWater(jwt, "750", "2026-07-21T19:00:00Z");
    when(jwtDecoder.decode("caller-token")).thenReturn(jwt);

    mockMvc
        .perform(
            get("/api/diary/summary")
                .header("Authorization", "Bearer caller-token")
                .queryParam("date", "2026-07-21"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.date").value("2026-07-21"))
        .andExpect(jsonPath("$.totals.length()").value(2))
        .andExpect(jsonPath("$.totals[?(@.code == 'energy_kcal')].amount").value(300.00))
        .andExpect(jsonPath("$.totals[?(@.code == 'energy_kcal')].unit").value("kcal"))
        .andExpect(jsonPath("$.totals[?(@.code == 'energy_kcal')].target").value(2200.00))
        .andExpect(jsonPath("$.totals[?(@.code == 'protein')].amount").value(15.00))
        .andExpect(jsonPath("$.totals[?(@.code == 'protein')].target").value(100.00))
        .andExpect(jsonPath("$.water.amountMl").value(1250.00))
        .andExpect(jsonPath("$.water.targetMl").value(2600.00));

    verify(userGoalsClient).getGoals("Bearer caller-token");
  }

  @Test
  void emptyDaySummaryStillIncludesGoalNutrientsAtZero() throws Exception {
    Jwt jwt = jwtForUser("00000000-0000-0000-0000-000000000851");
    when(userGoalsClient.getGoals(eq("Bearer caller-token")))
        .thenReturn(
            List.of(
                goal("energy_kcal", "2200.00", "kcal"),
                goal("protein", "100.00", "g"),
                goal("carbohydrates", "250.00", "g"),
                goal("fat", "70.00", "g"),
                goal("water_ml", "2600.00", "ml")));
    when(jwtDecoder.decode("caller-token")).thenReturn(jwt);

    mockMvc
        .perform(
            get("/api/diary/summary")
                .header("Authorization", "Bearer caller-token")
                .queryParam("date", "2026-07-21"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.date").value("2026-07-21"))
        .andExpect(jsonPath("$.totals.length()").value(4))
        .andExpect(jsonPath("$.totals[?(@.code == 'energy_kcal')].amount").value(0))
        .andExpect(jsonPath("$.totals[?(@.code == 'energy_kcal')].unit").value("kcal"))
        .andExpect(jsonPath("$.totals[?(@.code == 'energy_kcal')].target").value(2200.00))
        .andExpect(jsonPath("$.totals[?(@.code == 'protein')].amount").value(0))
        .andExpect(jsonPath("$.totals[?(@.code == 'protein')].target").value(100.00))
        .andExpect(jsonPath("$.totals[?(@.code == 'carbohydrates')].amount").value(0))
        .andExpect(jsonPath("$.totals[?(@.code == 'carbohydrates')].target").value(250.00))
        .andExpect(jsonPath("$.totals[?(@.code == 'fat')].amount").value(0))
        .andExpect(jsonPath("$.totals[?(@.code == 'fat')].target").value(70.00))
        .andExpect(jsonPath("$.water.amountMl").value(0))
        .andExpect(jsonPath("$.water.targetMl").value(2600.00));
  }

  @Test
  void summaryReturnsNullTargetsWhenGoalsServiceFails() throws Exception {
    UUID productId = UUID.randomUUID();
    Jwt jwt = jwtForUser("00000000-0000-0000-0000-000000000901");
    when(foodCatalogClient.getProduct(eq(productId), eq("Bearer caller-token")))
        .thenReturn(product(productId, "Beans", "Farm", "120", "8"));
    when(userGoalsClient.getGoals(eq("Bearer caller-token")))
        .thenThrow(new RuntimeException("goals unavailable"));

    createEntry(jwt, productId, "LUNCH", "2026-07-21T12:00:00Z", "100");
    createWater(jwt, "400", "2026-07-21T13:00:00Z");
    when(jwtDecoder.decode("caller-token")).thenReturn(jwt);

    mockMvc
        .perform(
            get("/api/diary/summary")
                .header("Authorization", "Bearer caller-token")
                .queryParam("date", "2026-07-21"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totals[?(@.code == 'energy_kcal')].amount").value(120.00))
        .andExpect(jsonPath("$.totals[?(@.code == 'energy_kcal')].target").value((Object) null))
        .andExpect(jsonPath("$.water.amountMl").value(400.00))
        .andExpect(jsonPath("$.water.targetMl").value((Object) null));
  }

  @Test
  void rangeSummaryReturnsEachDateInclusiveIncludingEmptyDays() throws Exception {
    UUID productId = UUID.randomUUID();
    Jwt jwt = jwtForUser("00000000-0000-0000-0000-000000001001");
    when(foodCatalogClient.getProduct(eq(productId), eq("Bearer caller-token")))
        .thenReturn(product(productId, "Soup", "Kitchen", "50", "3"));
    when(userGoalsClient.getGoals(eq("Bearer caller-token")))
        .thenReturn(List.of(goal("energy_kcal", "2200.00", "kcal"), goal("water_ml", "2600.00", "ml")));

    createEntry(jwt, productId, "DINNER", "2026-07-21T18:00:00Z", "200");
    createWater(jwt, "300", "2026-07-23T10:00:00Z");
    when(jwtDecoder.decode("caller-token")).thenReturn(jwt);

    mockMvc
        .perform(
            get("/api/diary/summary/range")
                .header("Authorization", "Bearer caller-token")
                .queryParam("from", "2026-07-21")
                .queryParam("to", "2026-07-23"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].date").value("2026-07-21"))
        .andExpect(jsonPath("$[0].totals[?(@.code == 'energy_kcal')].amount").value(100.00))
        .andExpect(jsonPath("$[0].water.amountMl").value(0))
        .andExpect(jsonPath("$[0].water.targetMl").value(2600.00))
        .andExpect(jsonPath("$[1].date").value("2026-07-22"))
        .andExpect(jsonPath("$[1].totals.length()").value(1))
        .andExpect(jsonPath("$[1].totals[?(@.code == 'energy_kcal')].amount").value(0))
        .andExpect(jsonPath("$[1].totals[?(@.code == 'energy_kcal')].target").value(2200.00))
        .andExpect(jsonPath("$[1].water.amountMl").value(0))
        .andExpect(jsonPath("$[1].water.targetMl").value(2600.00))
        .andExpect(jsonPath("$[2].date").value("2026-07-23"))
        .andExpect(jsonPath("$[2].totals.length()").value(1))
        .andExpect(jsonPath("$[2].totals[?(@.code == 'energy_kcal')].amount").value(0))
        .andExpect(jsonPath("$[2].totals[?(@.code == 'energy_kcal')].target").value(2200.00))
        .andExpect(jsonPath("$[2].water.amountMl").value(300.00))
        .andExpect(jsonPath("$[2].water.targetMl").value(2600.00));
  }

  private MvcResult createEntry(
      Jwt jwt, UUID productId, String mealType, String consumedAt, String weightG) throws Exception {
    when(jwtDecoder.decode("caller-token")).thenReturn(jwt);
    return mockMvc
        .perform(
            post("/api/diary/entries")
                .header("Authorization", "Bearer caller-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"productId":"%s","weightG":%s,"mealType":"%s","consumedAt":"%s"}
                    """
                        .formatted(productId, weightG, mealType, consumedAt)))
        .andExpect(status().isOk())
        .andReturn();
  }

  private MvcResult createWater(Jwt jwt, String amountMl, String loggedAt) throws Exception {
    return mockMvc
        .perform(
            post("/api/diary/water")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"amountMl":%s,"loggedAt":"%s"}
                    """
                        .formatted(amountMl, loggedAt)))
        .andExpect(status().isOk())
        .andReturn();
  }

  private String entryId(MvcResult result) throws Exception {
    return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }

  private Jwt jwtForUser(String subject) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject(subject)
        .claim("roles", List.of("USER"))
        .build();
  }

  private ProductResponse product(
      UUID productId, String name, String brand, String energyKcal, String proteinG) {
    return new ProductResponse(
        productId,
        null,
        "1234567890123",
        "OFF",
        name,
        brand,
        "100 g",
        new BigDecimal("100"),
        "https://example.test/image.jpg",
        "B",
        "ingredients",
        List.of("en:milk"),
        Instant.parse("2026-07-21T00:00:00Z"),
        List.of(
            new ProductResponse.NutrientResponse(
                "energy_kcal", new BigDecimal(energyKcal), "kcal"),
            new ProductResponse.NutrientResponse("protein", new BigDecimal(proteinG), "g")));
  }

  private UserGoalResponse goal(String nutrientCode, String dailyTarget, String unit) {
    return new UserGoalResponse(
        nutrientCode, new BigDecimal(dailyTarget), unit, "COMPUTED", Instant.parse("2026-07-21T00:00:00Z"));
  }
}
