package com.nutritrack.food.service;

import com.nutritrack.food.domain.Product;
import com.nutritrack.food.domain.ProductNutrient;
import com.nutritrack.food.domain.ProductRepository;
import com.nutritrack.food.domain.ProductSource;
import com.nutritrack.food.off.NormalizedOffProduct;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OffProductUpsertService {

  private final ProductRepository productRepository;

  public OffProductUpsertService(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  @Transactional
  public Product upsertFromOff(NormalizedOffProduct offProduct) {
    String barcode = offProduct.barcode();
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

    if (product.getSource() == ProductSource.USER_APPROVED) {
      return product;
    }

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
    product.refreshSearchDocument();

    List<ProductNutrient> nutrients =
        offProduct.nutrients().stream()
            .map(
                n -> {
                  ProductNutrient pn = new ProductNutrient();
                  pn.setProductId(product.getId());
                  pn.setNutrientCode(n.code());
                  pn.setAmountPer100g(n.amountPer100g());
                  pn.setUnit(n.unit());
                  pn.setSource("OFF");
                  pn.setSourceRef(barcode);
                  pn.setConfidence(null);
                  pn.setEstimated(false);
                  return pn;
                })
            .toList();
    product.replaceNutrients(nutrients);
    return productRepository.save(product);
  }

  private static String joinTags(List<String> tags) {
    if (tags == null || tags.isEmpty()) {
      return null;
    }
    return String.join(",", tags);
  }
}
