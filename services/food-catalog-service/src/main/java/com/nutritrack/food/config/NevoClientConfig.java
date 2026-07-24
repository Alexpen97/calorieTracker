package com.nutritrack.food.config;

import com.nutritrack.food.nevo.NevoClient;
import com.nutritrack.food.nevo.RestNevoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class NevoClientConfig {

  @Bean
  NevoClient nevoClient(RestClient.Builder restClientBuilder, FoodProperties properties) {
    RestClient client =
        restClientBuilder.clone().baseUrl(properties.nevo().serviceUrl()).build();
    return new RestNevoClient(client, properties.nevo().internalApiKey());
  }
}
