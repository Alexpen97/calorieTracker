package com.nutritrack.enrichment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NutrientEnrichmentServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(NutrientEnrichmentServiceApplication.class, args);
  }
}
