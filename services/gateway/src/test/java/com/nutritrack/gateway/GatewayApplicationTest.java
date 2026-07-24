package com.nutritrack.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:9/jwks",
      "AUTH_SERVICE_URL=http://127.0.0.1:8081",
      "USER_SERVICE_URL=http://127.0.0.1:8082",
      "FOOD_SERVICE_URL=http://127.0.0.1:8083",
      "DIARY_SERVICE_URL=http://127.0.0.1:8084",
      "NEVO_SERVICE_URL=http://127.0.0.1:8085"
    })
class GatewayApplicationTest {

  @Autowired private RouteDefinitionLocator routeDefinitionLocator;

  @Test
  void loadsAuthAndUserRoutes() {
    var ids =
        routeDefinitionLocator
            .getRouteDefinitions()
            .map(def -> def.getId())
            .collectList()
            .block();
    assertThat(ids)
        .contains(
            "auth-service",
            "user-profile-service",
            "food-catalog-service",
            "diary-service",
            "diary-api-docs",
            "nevo-service",
            "nevo-api-docs");
  }
}
