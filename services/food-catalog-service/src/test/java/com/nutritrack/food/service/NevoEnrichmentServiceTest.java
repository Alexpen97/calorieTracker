package com.nutritrack.food.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nutritrack.food.cache.ProductCache;
import com.nutritrack.food.config.FoodProperties;
import com.nutritrack.food.domain.NutrientSource;
import com.nutritrack.food.domain.Product;
import com.nutritrack.food.domain.ProductNutrient;
import com.nutritrack.food.domain.ProductRepository;
import com.nutritrack.food.domain.ProductSource;
import com.nutritrack.food.nevo.NevoClient;
import com.nutritrack.food.nevo.NevoMatchRequest;
import com.nutritrack.food.nevo.NevoMatchResponse;
import com.nutritrack.food.nevo.NevoUnavailableException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NevoEnrichmentServiceTest {

  @Mock private NevoClient nevoClient;
  @Mock private ProductRepository productRepository;
  @Mock private ProductCache productCache;

  private NevoEnrichmentService service;

  @BeforeEach
  void setUp() {
    FoodProperties properties =
        new FoodProperties(
            new FoodProperties.Off(
                "https://world.openfoodfacts.org", "ua", "fields", 20),
            new FoodProperties.Cache(false, java.time.Duration.ofHours(1)),
            new FoodProperties.Resilience(
                12, 8, java.time.Duration.ofSeconds(30), 2),
            new FoodProperties.Search(20, 5, 8),
            new FoodProperties.BulkImport(false, "0 0 * * * *", ""),
            new FoodProperties.Enrichment(false, "http://localhost:8086", "key", java.time.Duration.ofSeconds(3)),
            new FoodProperties.Nevo(true, "http://localhost:8085", "key"));
    service = new NevoEnrichmentService(nevoClient, productRepository, productCache, properties);
  }

  @Test
  void fillsMissingMicrosFromHighConfidenceMatch() {
    Product product = productWithMacrosOnly();
    when(nevoClient.matchBest(any(NevoMatchRequest.class)))
        .thenReturn(
            new NevoMatchResponse(
                true,
                "1004",
                "Banana raw",
                "Fruits",
                "2025/9.0",
                "HIGH",
                0.9,
                List.of("nameSimilarity=0.9"),
                List.of(
                    new NevoMatchResponse.NevoNutrientDto(
                        "vitamin_c", new BigDecimal("8.7"), "mg"),
                    new NevoMatchResponse.NevoNutrientDto(
                        "potassium", new BigDecimal("358"), "mg"),
                    new NevoMatchResponse.NevoNutrientDto(
                        "protein", new BigDecimal("1.1"), "g"))));
    when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

    Product enriched = service.enrichMissingMicros(product);

    assertThat(enriched.getNutrients())
        .anyMatch(
            n ->
                "vitamin_c".equals(n.getNutrientCode())
                    && n.isEstimated()
                    && n.getSource() == NutrientSource.NEVO_ESTIMATE
                    && "1004".equals(n.getSourceRef()));
    assertThat(enriched.getNutrients())
        .filteredOn(n -> "protein".equals(n.getNutrientCode()))
        .hasSize(1)
        .first()
        .extracting(ProductNutrient::getAmountPer100g)
        .isEqualTo(new BigDecimal("1.0"));
  }

  @Test
  void skipsLowConfidenceMatches() {
    Product product = productWithMacrosOnly();
    when(nevoClient.matchBest(any(NevoMatchRequest.class)))
        .thenReturn(
            new NevoMatchResponse(
                true,
                "1004",
                "Banana raw",
                "Fruits",
                "2025/9.0",
                "LOW",
                0.2,
                List.of(),
                List.of(
                    new NevoMatchResponse.NevoNutrientDto(
                        "vitamin_c", new BigDecimal("8.7"), "mg"))));

    Product enriched = service.enrichMissingMicros(product);

    assertThat(enriched.getNutrients()).hasSize(1);
    verify(productRepository, never()).save(any());
  }

  @Test
  void unavailableNevoDoesNotBreakEnrichment() {
    Product product = productWithMacrosOnly();
    when(nevoClient.matchBest(any(NevoMatchRequest.class)))
        .thenThrow(new NevoUnavailableException(new RuntimeException("down")));

    Product enriched = service.enrichMissingMicros(product);

    assertThat(enriched.getNutrients()).hasSize(1);
    verify(productRepository, never()).save(any());
  }

  @Test
  void disabledFlagSkipsClient() {
    FoodProperties disabled =
        new FoodProperties(
            new FoodProperties.Off(
                "https://world.openfoodfacts.org", "ua", "fields", 20),
            new FoodProperties.Cache(false, java.time.Duration.ofHours(1)),
            new FoodProperties.Resilience(
                12, 8, java.time.Duration.ofSeconds(30), 2),
            new FoodProperties.Search(20, 5, 8),
            new FoodProperties.BulkImport(false, "0 0 * * * *", ""),
            new FoodProperties.Enrichment(false, "http://localhost:8086", "key", java.time.Duration.ofSeconds(3)),
            new FoodProperties.Nevo(false, "http://localhost:8085", "key"));
    service = new NevoEnrichmentService(nevoClient, productRepository, productCache, disabled);

    service.enrichMissingMicros(productWithMacrosOnly());

    verify(nevoClient, never()).matchBest(any());
  }

  @Test
  void replacesOffZeroMicrosWithNevoEstimates() {
    Product product = productWithMacrosOnly();
    ProductNutrient zeroCalcium = new ProductNutrient();
    zeroCalcium.setProductId(product.getId());
    zeroCalcium.setNutrientCode("calcium");
    zeroCalcium.setAmountPer100g(BigDecimal.ZERO);
    zeroCalcium.setUnit("mg");
    zeroCalcium.setSource(NutrientSource.OFF);
    zeroCalcium.setEstimated(false);
    product.replaceNutrients(new ArrayList<>(List.of(product.getNutrients().get(0), zeroCalcium)));

    when(nevoClient.matchBest(any(NevoMatchRequest.class)))
        .thenReturn(
            new NevoMatchResponse(
                true,
                "305",
                "Quark low fat",
                "Milk and milk products",
                "2025/9.0",
                "HIGH",
                0.9,
                List.of(),
                List.of(
                    new NevoMatchResponse.NevoNutrientDto(
                        "calcium", new BigDecimal("120"), "mg"))));
    when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

    Product enriched = service.enrichMissingMicros(product);

    assertThat(enriched.getNutrients())
        .filteredOn(n -> "calcium".equals(n.getNutrientCode()))
        .hasSize(1)
        .first()
        .satisfies(
            n -> {
              assertThat(n.getAmountPer100g()).isEqualByComparingTo("120");
              assertThat(n.getSource()).isEqualTo(NutrientSource.NEVO_ESTIMATE);
              assertThat(n.isEstimated()).isTrue();
            });
  }

  private static Product productWithMacrosOnly() {
    Product product = new Product();
    product.setId(UUID.randomUUID());
    product.setBarcode("1234567890123");
    product.setSource(ProductSource.OFF);
    product.setName("Banana");
    ProductNutrient protein = new ProductNutrient();
    protein.setProductId(product.getId());
    protein.setNutrientCode("protein");
    protein.setAmountPer100g(new BigDecimal("1.0"));
    protein.setUnit("g");
    protein.setSource(NutrientSource.OFF);
    protein.setEstimated(false);
    product.replaceNutrients(new ArrayList<>(List.of(protein)));
    return product;
  }
}
