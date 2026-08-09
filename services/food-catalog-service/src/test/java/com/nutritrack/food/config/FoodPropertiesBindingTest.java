package com.nutritrack.food.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;

class FoodPropertiesBindingTest {

  private static FoodProperties bind(Map<String, Object> properties) {
    var environment = new StandardEnvironment();
    environment
        .getPropertySources()
        .addFirst(new org.springframework.core.env.MapPropertySource("test", properties));
    return Binder.get(environment).bind("nutritrack.food", Bindable.of(FoodProperties.class)).get();
  }

  @Test
  void offHasDefaultsWhenOnlyCacheConfigured() {
    FoodProperties properties = bind(Map.of("nutritrack.food.cache.redis-enabled", "false"));

    assertThat(properties.off()).isNotNull();
    assertThat(properties.off().baseUrl()).isEqualTo("https://world.openfoodfacts.org");
    assertThat(properties.off().userAgent()).isEqualTo("NutriTrack - Server - Version 0.1");
    assertThat(properties.cache().redisEnabled()).isFalse();
  }

  @Test
  void offBaseUrlCanBeOverriddenViaRelaxedBinding() {
    FoodProperties properties =
        bind(Map.of("nutritrack.food.off.base-url", "https://example.test/off"));

    assertThat(properties.off().baseUrl()).isEqualTo("https://example.test/off");
  }

  @Test
  void searchHasFuzzyDefaults() {
    FoodProperties properties = bind(Map.of("nutritrack.food.cache.redis-enabled", "true"));

    assertThat(properties.search().fuzzyMinResults()).isEqualTo(3);
    assertThat(properties.search().similarityThreshold()).isEqualTo(0.35);
  }
}
