package com.nutritrack.food.cache;

import com.nutritrack.food.web.dto.ProductResponse;
import java.util.Optional;

public interface ProductCache {
  Optional<ProductResponse> getByBarcode(String barcode);

  void putByBarcode(String barcode, ProductResponse product);

  void evictByBarcode(String barcode);
}
