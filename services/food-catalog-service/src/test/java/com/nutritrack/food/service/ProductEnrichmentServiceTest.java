package com.nutritrack.food.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nutritrack.food.cache.InMemoryProductCache;
import com.nutritrack.food.domain.NutrientSource;
import com.nutritrack.food.domain.Product;
import com.nutritrack.food.domain.ProductNutrient;
import com.nutritrack.food.domain.ProductRepository;
import com.nutritrack.food.domain.ProductSource;
import com.nutritrack.food.enrichment.EnrichmentClient;
import com.nutritrack.food.enrichment.EnrichmentResult;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductEnrichmentServiceTest {

  @Mock private EnrichmentClient enrichmentClient;
  @Mock private ProductRepository productRepository;

  private ProductEnrichmentService service;
  private InMemoryProductCache cache;

  @BeforeEach
  void setUp() {
    cache = new InMemoryProductCache(Duration.ofHours(1));
    service = new ProductEnrichmentService(enrichmentClient, productRepository, cache);
  }

  @Test
  void fillsMissingMicrosFromUsdaWithoutOverwriting() {
    Product product = sparseOffProduct();
    product
        .getNutrients()
        .add(nutrient(product.getId(), "calcium", "80", "mg", NutrientSource.OFF));

    when(enrichmentClient.enrich(
            eq("3017620422003"), eq("Nutella"), eq("Ferrero"), anyList()))
        .thenReturn(
            Optional.of(
                new EnrichmentResult(
                    "GENERIC_PROXY",
                    1L,
                    "Hazelnut spread",
                    BigDecimal.ONE,
                    List.of(
                        new EnrichmentResult.Nutrient(
                            "calcium", new BigDecimal("999"), "mg"),
                        new EnrichmentResult.Nutrient(
                            "vitamin_b3", new BigDecimal("1.2"), "mg")))));
    when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

    Product result = service.enrichIfSparse(product);

    assertThat(result.getNutrients())
        .anySatisfy(
            n -> {
              assertThat(n.getNutrientCode()).isEqualTo("calcium");
              assertThat(n.getAmountPer100g()).isEqualByComparingTo("80");
              assertThat(n.getSource()).isEqualTo(NutrientSource.OFF);
            })
        .anySatisfy(
            n -> {
              assertThat(n.getNutrientCode()).isEqualTo("vitamin_b3");
              assertThat(n.getSource()).isEqualTo(NutrientSource.USDA_PROXY);
            });
  }

  @Test
  void returnsUnchangedWhenEnrichmentFails() {
    Product product = sparseOffProduct();
    when(enrichmentClient.enrich(anyString(), anyString(), any(), anyList()))
        .thenReturn(Optional.empty());

    Product result = service.enrichIfSparse(product);

    assertThat(result.getNutrients()).hasSize(1);
    verify(productRepository, never()).save(any());
  }

  @Test
  void skipsWhenAlreadyHasEnoughMicros() {
    Product product = sparseOffProduct();
    for (String code :
        List.of(
            "vitamin_a",
            "vitamin_b1",
            "vitamin_b2",
            "vitamin_b3",
            "vitamin_c",
            "calcium",
            "iron")) {
      product.getNutrients().add(nutrient(product.getId(), code, "1", "mg", NutrientSource.OFF));
    }

    Product result = service.enrichIfSparse(product);

    verify(enrichmentClient, never()).enrich(anyString(), anyString(), any(), anyList());
    assertThat(result.getNutrients()).hasSizeGreaterThanOrEqualTo(7);
  }

  private static Product sparseOffProduct() {
    Product product = new Product();
    product.setId(UUID.randomUUID());
    product.setBarcode("3017620422003");
    product.setSource(ProductSource.OFF);
    product.setName("Nutella");
    product.setBrand("Ferrero");
    product.setNutrients(new ArrayList<>());
    product
        .getNutrients()
        .add(nutrient(product.getId(), "energy_kcal", "539", "kcal", NutrientSource.OFF));
    return product;
  }

  private static ProductNutrient nutrient(
      UUID productId, String code, String amount, String unit, NutrientSource source) {
    ProductNutrient pn = new ProductNutrient();
    pn.setProductId(productId);
    pn.setNutrientCode(code);
    pn.setAmountPer100g(new BigDecimal(amount));
    pn.setUnit(unit);
    pn.setSource(source);
    return pn;
  }
}
