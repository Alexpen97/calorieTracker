package com.nutritrack.gateway;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.nutritrack.gateway.config.GatewayStartupValidator;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class GatewayStartupValidatorTest {

  @Mock private RouteDefinitionLocator routeDefinitionLocator;

  @Test
  void acceptsConfiguredPrivateServiceUrls() {
    when(routeDefinitionLocator.getRouteDefinitions())
        .thenReturn(
            Flux.just(
                route("diary-service", "http://diary-service.railway.internal:8080"),
                route("auth-service", "http://auth-service.railway.internal:8080")));

    Environment environment =
        new MockEnvironment()
            .withProperty("RAILWAY_ENVIRONMENT", "production")
            .withProperty(
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                "http://auth-service.railway.internal:8080/.well-known/jwks.json");
    GatewayStartupValidator validator = new GatewayStartupValidator(routeDefinitionLocator, environment);

    assertThatCode(validator::validateRouteTargets).doesNotThrowAnyException();
  }

  @Test
  void rejectsJwksUriWithoutHostOnRailway() {
    Environment environment =
        new MockEnvironment()
            .withProperty("RAILWAY_ENVIRONMENT", "production")
            .withProperty("JWKS_URI", "http://:8080/.well-known/jwks.json")
            .withProperty(
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                "http://:8080/.well-known/jwks.json");

    GatewayStartupValidator validator =
        new GatewayStartupValidator(routeDefinitionLocator, environment);

    assertThatThrownBy(validator::validateRouteTargets)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("JWKS_URI")
        .hasMessageContaining("Host is not specified");
  }

  @Test
  void rejectsDiaryRouteWithoutHost() {
    when(routeDefinitionLocator.getRouteDefinitions())
        .thenReturn(Flux.just(route("diary-service", "")));

    Environment environment =
        new MockEnvironment()
            .withProperty("RAILWAY_ENVIRONMENT", "production")
            .withProperty(
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                "http://auth-service.railway.internal:8080/.well-known/jwks.json");
    GatewayStartupValidator validator = new GatewayStartupValidator(routeDefinitionLocator, environment);

    assertThatThrownBy(validator::validateRouteTargets)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("DIARY_SERVICE_URL")
        .hasMessageContaining("has no host");
  }

  @Test
  void rejectsLocalhostTargetsOnRailway() {
    when(routeDefinitionLocator.getRouteDefinitions())
        .thenReturn(Flux.just(route("diary-service", "http://localhost:8084")));

    Environment environment =
        new MockEnvironment()
            .withProperty("RAILWAY_ENVIRONMENT", "production")
            .withProperty(
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                "http://auth-service.railway.internal:8080/.well-known/jwks.json");
    GatewayStartupValidator validator = new GatewayStartupValidator(routeDefinitionLocator, environment);

    assertThatThrownBy(validator::validateRouteTargets)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("DIARY_SERVICE_URL")
        .hasMessageContaining("running on Railway");
  }

  private static RouteDefinition route(String id, String uri) {
    RouteDefinition route = new RouteDefinition();
    route.setId(id);
    route.setUri(URI.create(uri));
    return route;
  }
}
