package com.nutritrack.diary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DiaryServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(DiaryServiceApplication.class, args);
  }
}
