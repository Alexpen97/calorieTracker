package com.nutritrack.food.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutritrack.food.web.dto.ProductResponse;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisProductCache implements ProductCache {

  private static final String KEY_PREFIX = "product:barcode:";

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final Duration ttl;

  public RedisProductCache(
      StringRedisTemplate redisTemplate, ObjectMapper objectMapper, Duration ttl) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
    this.ttl = ttl;
  }

  @Override
  public Optional<ProductResponse> getByBarcode(String barcode) {
    String json = redisTemplate.opsForValue().get(KEY_PREFIX + barcode);
    if (json == null || json.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(objectMapper.readValue(json, ProductResponse.class));
    } catch (JsonProcessingException e) {
      redisTemplate.delete(KEY_PREFIX + barcode);
      return Optional.empty();
    }
  }

  @Override
  public void putByBarcode(String barcode, ProductResponse product) {
    try {
      String json = objectMapper.writeValueAsString(product);
      redisTemplate.opsForValue().set(KEY_PREFIX + barcode, json, ttl);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize product for cache", e);
    }
  }

  @Override
  public void evictByBarcode(String barcode) {
    redisTemplate.delete(KEY_PREFIX + barcode);
  }
}
