package com.nutritrack.food.config;

import tools.jackson.databind.json.JsonMapper;
import com.nutritrack.food.cache.InMemoryProductCache;
import com.nutritrack.food.cache.ProductCache;
import com.nutritrack.food.cache.RedisProductCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class CacheConfig {

  @Bean
  @ConditionalOnProperty(
      prefix = "nutritrack.food.cache",
      name = "redis-enabled",
      havingValue = "true",
      matchIfMissing = true)
  ProductCache redisProductCache(
      StringRedisTemplate redisTemplate, JsonMapper jsonMapper, FoodProperties properties) {
    return new RedisProductCache(redisTemplate, jsonMapper, properties.cache().ttl());
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "nutritrack.food.cache",
      name = "redis-enabled",
      havingValue = "false")
  ProductCache inMemoryProductCache(FoodProperties properties) {
    return new InMemoryProductCache(properties.cache().ttl());
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "nutritrack.food.cache",
      name = "redis-enabled",
      havingValue = "true",
      matchIfMissing = true)
  StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
    return new StringRedisTemplate(connectionFactory);
  }
}
