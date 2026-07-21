package com.nutritrack.diary.config;

import com.nutritrack.diary.client.FoodCatalogClient;
import com.nutritrack.diary.client.RestFoodCatalogClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  @Bean
  RestClient.Builder restClientBuilder() {
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
    factory.setReadTimeout(Duration.ofSeconds(8));
    return RestClient.builder().requestFactory(factory);
  }

  @Bean
  FoodCatalogClient foodCatalogClient(
      RestClient.Builder restClientBuilder, DiaryProperties properties) {
    RestClient client = restClientBuilder.baseUrl(properties.foodServiceUrl()).build();
    return new RestFoodCatalogClient(client);
  }
}
