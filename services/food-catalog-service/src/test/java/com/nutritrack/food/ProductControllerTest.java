package com.nutritrack.food;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nutritrack.food.off.NormalizedOffProduct;
import com.nutritrack.food.off.OffClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:food;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.flyway.enabled=true",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused",
      "nutritrack.food.cache.redis-enabled=false",
      "spring.autoconfigure.exclude=org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.RedisRepositoriesAutoConfiguration"
    })
class ProductControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private OffClient offClient;

  @Test
  void barcodeLookupFetchesFromOffPersistsAndServesNutrients() throws Exception {
    when(offClient.fetchByBarcode(eq("3017620422003")))
        .thenReturn(
            Optional.of(
                new NormalizedOffProduct(
                    "3017620422003",
                    "Nutella",
                    "Ferrero",
                    "400 g",
                    new BigDecimal("15"),
                    "https://example.test/n.jpg",
                    "E",
                    "sugar, palm oil",
                    List.of("en:milk"),
                    List.of(
                        new NormalizedOffProduct.NormalizedNutrient(
                            "energy_kcal", new BigDecimal("539"), "kcal"),
                        new NormalizedOffProduct.NormalizedNutrient(
                            "protein", new BigDecimal("6.3"), "g")))));

    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("user-1")
            .claim("roles", List.of("USER"))
            .build();

    mockMvc
        .perform(
            get("/api/products/barcode/3017620422003")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Nutella"))
        .andExpect(jsonPath("$.brand").value("Ferrero"))
        .andExpect(jsonPath("$.source").value("OFF"))
        .andExpect(jsonPath("$.nutrients[0].code").exists());

    // Second call should hit cache/DB and not call OFF again.
    mockMvc
        .perform(
            get("/api/products/barcode/3017620422003")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Nutella"));

    verify(offClient, times(1)).fetchByBarcode("3017620422003");
  }

  @Test
  void barcodeMissReturns404() throws Exception {
    when(offClient.fetchByBarcode(eq("0000000000000"))).thenReturn(Optional.empty());

    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("user-1")
            .claim("roles", List.of("USER"))
            .build();

    mockMvc
        .perform(
            get("/api/products/barcode/0000000000000")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt)))
        .andExpect(status().isNotFound());
  }

  @Test
  void nutrientsEndpointReturnsSeededEducationContent() throws Exception {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("user-1")
            .claim("roles", List.of("USER"))
            .build();

    mockMvc
        .perform(get("/api/nutrients").with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.code=='protein')].displayName").value("Protein"))
        .andExpect(jsonPath("$[?(@.code=='protein')].bodyEffects").exists())
        .andExpect(jsonPath("$[?(@.code=='protein')].contentSource").exists());

    mockMvc
        .perform(
            get("/api/nutrients/vitamin_c")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Vitamin C"))
        .andExpect(jsonPath("$.category").value("VITAMIN"));
  }
}
