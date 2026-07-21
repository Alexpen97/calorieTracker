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
      "nutritrack.diary.food-service-url=http://localhost:8083"
    })
class DiaryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private FoodCatalogClient foodCatalogClient;

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
}
