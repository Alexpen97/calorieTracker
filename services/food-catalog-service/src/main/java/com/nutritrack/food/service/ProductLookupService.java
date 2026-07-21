package com.nutritrack.food.service;

import com.nutritrack.food.cache.ProductCache;
import com.nutritrack.food.domain.Product;
import com.nutritrack.food.domain.ProductNutrient;
import com.nutritrack.food.domain.ProductRepository;
import com.nutritrack.food.domain.ProductSource;
import com.nutritrack.food.off.NormalizedOffProduct;
import com.nutritrack.food.off.OffClient;
import com.nutritrack.food.web.ProductNotFoundException;
import com.nutritrack.food.web.dto.ProductNutrientResponse;
import com.nutritrack.food.web.dto.ProductResponse;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductLookupService {

  private final ProductRepository productRepository;
  private final ProductCache productCache;
  private final OffClient offClient;

  public ProductLookupService(
      ProductRepository productRepository, ProductCache productCache, OffClient offClient) {
    this.productRepository = productRepository;
    this.productCache = productCache;
    this.offClient = offClient;
  }

  @Transactional
  public ProductResponse lookupByBarcode(String rawBarcode) {
    String barcode = sanitizeBarcode(rawBarcode);
    return productCache
        .getByBarcode(barcode)
        .orElseGet(
            () ->
                productRepository
                    .findByBarcode(barcode)
                    .map(
                        product -> {
                          ProductResponse response = toResponse(product);
                          productCache.putByBarcode(barcode, response);
                          return response;
                        })
                    .orElseGet(() -> fetchPersistAndCache(barcode)));
  }

  @Transactional(readOnly = true)
  public ProductResponse getById(UUID id) {
    Product product =
        productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id.toString()));
    return toResponse(product);
  }

  private ProductResponse fetchPersistAndCache(String barcode) {
    NormalizedOffProduct offProduct =
        offClient
            .fetchByBarcode(barcode)
            .orElseThrow(() -> new ProductNotFoundException(barcode));

    Product product =
        productRepository
            .findByBarcode(barcode)
            .orElseGet(
                () -> {
                  Product created = new Product();
                  created.setId(UUID.randomUUID());
                  created.setBarcode(barcode);
                  return created;
                });

    product.setSource(ProductSource.OFF);
    product.setName(offProduct.name());
    product.setBrand(offProduct.brand());
    product.setQuantityLabel(offProduct.quantityLabel());
    product.setServingSizeG(offProduct.servingSizeG());
    product.setImageUrl(offProduct.imageUrl());
    product.setNutriScore(offProduct.nutriScore());
    product.setIngredientsText(offProduct.ingredientsText());
    product.setAllergenTags(joinTags(offProduct.allergenTags()));
    product.setOffLastSyncedAt(Instant.now());

    List<ProductNutrient> nutrients =
        offProduct.nutrients().stream()
            .map(
                n -> {
                  ProductNutrient pn = new ProductNutrient();
                  pn.setProductId(product.getId());
                  pn.setNutrientCode(n.code());
                  pn.setAmountPer100g(n.amountPer100g());
                  pn.setUnit(n.unit());
                  return pn;
                })
            .toList();
    product.replaceNutrients(nutrients);

    Product saved = productRepository.save(product);
    ProductResponse response = toResponse(saved);
    productCache.putByBarcode(barcode, response);
    return response;
  }

  public static String sanitizeBarcode(String raw) {
    if (raw == null) {
      throw new IllegalArgumentException("Barcode is required");
    }
    String digits = raw.trim().replaceAll("\\s+", "");
    if (!digits.matches("\\d{8,14}")) {
      throw new IllegalArgumentException("Barcode must be 8–14 digits");
    }
    return digits;
  }

  static ProductResponse toResponse(Product product) {
    List<String> allergens =
        product.getAllergenTags() == null || product.getAllergenTags().isBlank()
            ? List.of()
            : Arrays.stream(product.getAllergenTags().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    List<ProductNutrientResponse> nutrients =
        product.getNutrients().stream()
            .map(
                n ->
                    new ProductNutrientResponse(
                        n.getNutrientCode(), n.getAmountPer100g(), n.getUnit()))
            .toList();
    return new ProductResponse(
        product.getId(),
        product.getBarcode(),
        product.getSource().name(),
        product.getName(),
        product.getBrand(),
        product.getQuantityLabel(),
        product.getServingSizeG(),
        product.getImageUrl(),
        product.getNutriScore(),
        product.getIngredientsText(),
        allergens,
        product.getOffLastSyncedAt(),
        nutrients);
  }

  private static String joinTags(List<String> tags) {
    if (tags == null || tags.isEmpty()) {
      return null;
    }
    return String.join(",", tags);
  }
}
