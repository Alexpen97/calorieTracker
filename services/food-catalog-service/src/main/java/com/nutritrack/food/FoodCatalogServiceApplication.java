package com.nutritrack.food;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FoodCatalogServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(FoodCatalogServiceApplication.class, args);
  }
}
