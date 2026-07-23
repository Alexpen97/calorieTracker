package com.nutritrack.food.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nutritrack.food.web.dto.ProductNutrientResponse;
import com.nutritrack.food.web.dto.ProductResponse;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class RedisProductCacheTest {

  private static final String BARCODE = "3017620422003";

  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;

  private RedisProductCache cache;

  @BeforeEach
  void setUp() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    cache = new RedisProductCache(redisTemplate, JsonMapper.builder().build(), Duration.ofHours(1));
  }

  @Test
  void putAndGetRoundTripProductResponse() {
    ProductResponse product = sampleProduct();

    cache.putByBarcode(BARCODE, product);

    ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
    verify(valueOperations).set(eq("product:barcode:" + BARCODE), jsonCaptor.capture(), eq(Duration.ofHours(1)));

    when(valueOperations.get("product:barcode:" + BARCODE)).thenReturn(jsonCaptor.getValue());

    Optional<ProductResponse> cached = cache.getByBarcode(BARCODE);

    assertThat(cached).contains(product);
  }

  @Test
  void getReturnsEmptyForInvalidJsonAndEvictsKey() {
    when(valueOperations.get("product:barcode:" + BARCODE)).thenReturn("{not-json");

    Optional<ProductResponse> cached = cache.getByBarcode(BARCODE);

    assertThat(cached).isEmpty();
    verify(redisTemplate).delete("product:barcode:" + BARCODE);
  }

  private static ProductResponse sampleProduct() {
    return new ProductResponse(
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        null,
        BARCODE,
        "OFF",
        "Nutella",
        "Ferrero",
        "400 g",
        new BigDecimal("15"),
        "https://example.test/n.jpg",
        "E",
        "sugar, palm oil",
        List.of("en:milk"),
        Instant.parse("2026-01-01T00:00:00Z"),
        List.of(new ProductNutrientResponse("protein", new BigDecimal("6.3"), "g", false)));
  }
}
