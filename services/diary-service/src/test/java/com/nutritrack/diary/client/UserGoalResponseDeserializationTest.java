package com.nutritrack.diary.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:diary-goals-json;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.flyway.enabled=true",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused",
      "nutritrack.diary.food-service-url=http://localhost:8083",
      "nutritrack.diary.user-service-url=http://localhost:8082"
    })
class UserGoalResponseDeserializationTest {

  @Autowired private JsonMapper jsonMapper;

  @Test
  void bootJsonMapperReadsGoalsPayloadWithExtraOriginAndComputedAt() {
    String payload =
        """
        [
          {
            "nutrientCode": "energy_kcal",
            "dailyTarget": 2200.00,
            "unit": "kcal",
            "origin": "COMPUTED",
            "computedAt": "2026-07-21T00:00:00Z"
          },
          {
            "nutrientCode": "protein",
            "dailyTarget": 100.00,
            "unit": "g",
            "origin": "USER_OVERRIDE",
            "computedAt": null
          }
        ]
        """;

    UserGoalResponse[] goals = jsonMapper.readValue(payload, UserGoalResponse[].class);

    assertThat(goals).hasSize(2);
    assertThat(goals[0].nutrientCode()).isEqualTo("energy_kcal");
    assertThat(goals[0].dailyTarget()).isEqualByComparingTo("2200.00");
    assertThat(goals[0].unit()).isEqualTo("kcal");
    assertThat(goals[1].nutrientCode()).isEqualTo("protein");
  }
}
