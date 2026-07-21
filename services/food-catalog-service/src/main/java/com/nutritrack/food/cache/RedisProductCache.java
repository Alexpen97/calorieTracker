package com.nutritrack.food.cache;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import com.nutritrack.food.web.dto.ProductResponse;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisProductCache implements ProductCache {

  private static final String KEY_PREFIX = "product:barcode:";

  private final StringRedisTemplate redisTemplate;
  private final JsonMapper jsonMapper;
  private final Duration ttl;

  public RedisProductCache(
      StringRedisTemplate redisTemplate, JsonMapper jsonMapper, Duration ttl) {
    this.redisTemplate = redisTemplate;
    this.jsonMapper = jsonMapper;
    this.ttl = ttl;
  }

  @Override
  public Optional<ProductResponse> getByBarcode(String barcode) {
    String json = redisTemplate.opsForValue().get(KEY_PREFIX + barcode);
    if (json == null || json.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(jsonMapper.readValue(json, ProductResponse.class));
    } catch (JacksonException e) {
      redisTemplate.delete(KEY_PREFIX + barcode);
      return Optional.empty();
    }
  }

  @Override
  public void putByBarcode(String barcode, ProductResponse product) {
    try {
      String json = jsonMapper.writeValueAsString(product);
      redisTemplate.opsForValue().set(KEY_PREFIX + barcode, json, ttl);
    } catch (JacksonException e) {
      throw new IllegalStateException("Failed to serialize product for cache", e);
    }
  }

  @Override
  public void evictByBarcode(String barcode) {
    redisTemplate.delete(KEY_PREFIX + barcode);
  }
}
