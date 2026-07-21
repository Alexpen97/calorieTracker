package com.nutritrack.food.cache;

import com.nutritrack.food.web.dto.ProductResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryProductCache implements ProductCache {

  private final Duration ttl;
  private final Map<String, CacheEntry> store = new ConcurrentHashMap<>();

  public InMemoryProductCache(Duration ttl) {
    this.ttl = ttl;
  }

  @Override
  public Optional<ProductResponse> getByBarcode(String barcode) {
    CacheEntry entry = store.get(barcode);
    if (entry == null) {
      return Optional.empty();
    }
    if (Instant.now().isAfter(entry.expiresAt())) {
      store.remove(barcode);
      return Optional.empty();
    }
    return Optional.of(entry.product());
  }

  @Override
  public void putByBarcode(String barcode, ProductResponse product) {
    store.put(barcode, new CacheEntry(product, Instant.now().plus(ttl)));
  }

  @Override
  public void evictByBarcode(String barcode) {
    store.remove(barcode);
  }

  private record CacheEntry(ProductResponse product, Instant expiresAt) {}
}
