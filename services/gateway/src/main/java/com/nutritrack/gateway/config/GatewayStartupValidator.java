package com.nutritrack.gateway.config;

import java.net.URI;
import java.util.Map;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class GatewayStartupValidator {

  private static final Map<String, String> ROUTE_ENV_VARS =
      Map.of(
          "auth-service", "AUTH_SERVICE_URL",
          "user-profile-service", "USER_SERVICE_URL",
          "food-catalog-service", "FOOD_SERVICE_URL",
          "diary-service", "DIARY_SERVICE_URL");

  private final RouteDefinitionLocator routeDefinitionLocator;
  private final Environment environment;

  public GatewayStartupValidator(
      RouteDefinitionLocator routeDefinitionLocator, Environment environment) {
    this.routeDefinitionLocator = routeDefinitionLocator;
    this.environment = environment;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void validateRouteTargets() {
    if (!isRailwayDeploy()) {
      return;
    }
    Flux<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions();
    routes
        .filter(route -> ROUTE_ENV_VARS.containsKey(route.getId()))
        .doOnNext(this::validateRoute)
        .blockLast();
  }

  private void validateRoute(RouteDefinition route) {
    URI uri = route.getUri();
    String host = uri.getHost();
    if (host != null && !host.isBlank()) {
      if (isRailwayDeploy() && isLocalHost(host)) {
        throw new IllegalStateException(
            routeMisconfigurationMessage(
                route.getId(),
                "points at "
                    + host
                    + " but gateway is running on Railway. Use the private service URL"
                    + " (e.g. http://"
                    + railwayHostHint(route.getId())
                    + ".railway.internal:8080)"));
      }
      return;
    }

    throw new IllegalStateException(
        routeMisconfigurationMessage(
            route.getId(),
            "has no host (value="
                + uri
                + "). Include the http:// scheme, e.g. http://"
                + railwayHostHint(route.getId())
                + ".railway.internal:8080"));
  }

  private String routeMisconfigurationMessage(String routeId, String problem) {
    String envVar = ROUTE_ENV_VARS.get(routeId);
    return "Gateway route "
        + routeId
        + " "
        + problem
        + ". Set "
        + envVar
        + " on gateway. An empty "
        + envVar
        + " variable overrides the localhost default and breaks /api/** proxying.";
  }

  private static String railwayHostHint(String routeId) {
    return switch (routeId) {
      case "auth-service" -> "auth-service";
      case "user-profile-service" -> "user-profile-service";
      case "food-catalog-service" -> "food-catalog-service";
      case "diary-service" -> "diary-service";
      default -> routeId;
    };
  }

  private boolean isRailwayDeploy() {
    String railwayEnv = environment.getProperty("RAILWAY_ENVIRONMENT");
    return railwayEnv != null && !railwayEnv.isBlank();
  }

  private static boolean isLocalHost(String host) {
    return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
  }
}
