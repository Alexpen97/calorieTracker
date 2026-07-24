package com.nutritrack.food;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nutritrack.food.domain.Product;
import com.nutritrack.food.domain.ProductRepository;
import com.nutritrack.food.domain.ProductSource;
import com.nutritrack.food.off.NormalizedOffProduct;
import com.nutritrack.food.off.OffClient;
import com.nutritrack.food.nevo.NevoClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
      "spring.flyway.locations=classpath:db/migration",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused",
      "nutritrack.food.cache.redis-enabled=false",
      "spring.batch.jdbc.initialize-schema=always",
      "spring.batch.job.enabled=false",
      "spring.autoconfigure.exclude=org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.RedisRepositoriesAutoConfiguration"
    })
class ProductControllerTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID MOD_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  @Autowired private MockMvc mockMvc;
  @Autowired private ProductRepository productRepository;

  @MockitoBean private OffClient offClient;
  @MockitoBean private NevoClient nevoClient;

  @Test
  void barcodeLookupFetchesFromOffPersistsAndServesNutrients() throws Exception {
    when(offClient.fetchByBarcode(eq("3017620422003")))
        .thenReturn(
            Optional.of(
                new NormalizedOffProduct(
                    "3017620422003",
                    "Nutella",
                    "Ferrero",
                    null,
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

    mockMvc
        .perform(
            get("/api/products/barcode/3017620422003")
                .with(asUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Nutella"))
        .andExpect(jsonPath("$.brand").value("Ferrero"))
        .andExpect(jsonPath("$.source").value("OFF"))
        .andExpect(jsonPath("$.nutrients[0].code").exists());

    mockMvc
        .perform(
            get("/api/products/barcode/3017620422003")
                .with(asUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Nutella"));

    verify(offClient, times(1)).fetchByBarcode("3017620422003");
  }

  @Test
  void barcodeMissReturns404() throws Exception {
    when(offClient.fetchByBarcode(eq("0000000000000"))).thenReturn(Optional.empty());

    mockMvc
        .perform(
            get("/api/products/barcode/0000000000000")
                .with(asUser()))
        .andExpect(status().isNotFound());
  }

  @Test
  void nutrientsEndpointReturnsSeededEducationContent() throws Exception {
    mockMvc
        .perform(
            get("/api/nutrients").with(asUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").exists())
        .andExpect(jsonPath("$[0].bodyEffects").exists());
  }

  @Test
  void searchReturnsLocalProducts() throws Exception {
    Product product = new Product();
    product.setId(UUID.randomUUID());
    product.setBarcode("4006381333931");
    product.setSource(ProductSource.OFF);
    product.setName("Oat Milk Barista");
    product.setBrand("Oatly");
    product.refreshSearchDocument();
    productRepository.save(product);

    when(offClient.searchByName(anyString(), eq(1))).thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/products/search")
                .param("q", "oat milk")
                .with(asUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].name").value("Oat Milk Barista"));
  }

  @Test
  void submitAndApproveModerationFlow() throws Exception {
    String body =
        """
        {
          "name": "Homemade Granola",
          "brand": "Kitchen",
          "barcode": "1234567890123",
          "servingSizeG": 40,
          "nutrients": [
            {"code": "energy_kcal", "amountPer100g": 450, "unit": "kcal"},
            {"code": "protein", "amountPer100g": 10, "unit": "g"}
          ],
          "force": true
        }
        """;

    String submissionJson =
        mockMvc
            .perform(
                post("/api/products/submissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .with(asUser()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.name").value("Homemade Granola"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String id =
        submissionJson.replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

    mockMvc
        .perform(
            get("/api/products/submissions")
                .param("status", "PENDING")
                .with(asUser()))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/products/submissions/" + id + "/approve")
                .with(asModerator()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"))
        .andExpect(jsonPath("$.publishedProductId").exists());
  }

  @Test
  void userCannotApprove() throws Exception {
    mockMvc
        .perform(
            post("/api/products/submissions/" + UUID.randomUUID() + "/approve")
                .with(asUser()))
        .andExpect(status().isForbidden());
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor asUser() {
    return SecurityMockMvcRequestPostProcessors.jwt()
        .jwt(userJwt())
        .authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor asModerator() {
    return SecurityMockMvcRequestPostProcessors.jwt()
        .jwt(modJwt())
        .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"));
  }

  private static Jwt userJwt() {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject(USER_ID.toString())
        .claim("roles", List.of("USER"))
        .build();
  }

  private static Jwt modJwt() {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject(MOD_ID.toString())
        .claim("roles", List.of("MODERATOR"))
        .build();
  }
}
