package com.nutritrack.gateway.config;

import java.net.URI;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class GatewayStartupValidator {

  private static final Logger log = LoggerFactory.getLogger(GatewayStartupValidator.class);

  private static final Map<String, String> ROUTE_ENV_VARS =
      Map.of(
          "auth-service", "AUTH_SERVICE_URL",
          "user-profile-service", "USER_SERVICE_URL",
          "food-catalog-service", "FOOD_SERVICE_URL",
          "diary-service", "DIARY_SERVICE_URL",
          "nevo-service", "NEVO_SERVICE_URL");

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
    validateJwksUri();
    Flux<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions();
    routes
        .filter(route -> ROUTE_ENV_VARS.containsKey(route.getId()))
        .doOnNext(this::logResolvedRoute)
        .doOnNext(this::validateRoute)
        .blockLast();
  }

  private void validateJwksUri() {
    String jwksUri = environment.getProperty("JWKS_URI");
    String resolved =
        environment.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri");
    log.info("Gateway JWKS_URI={} (resolved jwk-set-uri={})", formatConfiguredValue(jwksUri), resolved);
    if (resolved == null || resolved.isBlank()) {
      throw new IllegalStateException(
          "JWKS_URI is missing on gateway. Set JWKS_URI to"
              + " http://<auth-service>.railway.internal:8080/.well-known/jwks.json");
    }
    URI uri = URI.create(resolved);
    if (uri.getHost() == null || uri.getHost().isBlank()) {
      throw new IllegalStateException(
          "JWKS_URI has no host (value="
              + formatConfiguredValue(jwksUri)
              + ", resolved="
              + resolved
              + "). Authenticated /api/** calls will fail with 'Host is not specified'."
              + " Use http://<auth-service-name>.railway.internal:8080/.well-known/jwks.json");
    }
    if (isLocalHost(uri.getHost())) {
      throw new IllegalStateException(
          "JWKS_URI points at "
              + uri.getHost()
              + " but gateway is on Railway. Set JWKS_URI to the auth-service private URL.");
    }
  }

  private void logResolvedRoute(RouteDefinition route) {
    String envVar = ROUTE_ENV_VARS.get(route.getId());
    String configured = environment.getProperty(envVar);
    log.info(
        "Gateway route {} -> {} ({}={})",
        route.getId(),
        route.getUri(),
        envVar,
        configured == null ? "<unset>" : configured);
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
            "has no host (routeUri="
                + uri
                + ", "
                + ROUTE_ENV_VARS.get(route.getId())
                + "="
                + formatConfiguredValue(environment.getProperty(ROUTE_ENV_VARS.get(route.getId())))
                + "). If using a Railway reference like http://${{service.RAILWAY_PRIVATE_DOMAIN}}:8080,"
                + " ensure that service exists and redeploy gateway after it is healthy"));
  }

  private static String formatConfiguredValue(String configured) {
    if (configured == null) {
      return "<unset>";
    }
    if (configured.isBlank()) {
      return "<blank>";
    }
    return configured;
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
      case "nevo-service" -> "nevo-service";
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
