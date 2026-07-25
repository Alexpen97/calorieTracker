package com.nutritrack.gateway.config;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Surfaces misconfigured upstream URIs as an explicit 502 instead of an opaque Netty
 * {@code Host is not specified} 500, and logs every diary proxy attempt (diary-service often
 * shows no logs because the request never leaves the gateway).
 */
@Component
public class UpstreamHostGuardFilter implements GlobalFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(UpstreamHostGuardFilter.class);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    URI requestUrl = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
    String path = exchange.getRequest().getURI().getRawPath();

    if (route != null
        && path != null
        && (path.startsWith("/api/diary") || path.startsWith("/api/integrations"))) {
      log.info(
          "Proxying {} {} via route {} -> requestUrl={} routeUri={}",
          exchange.getRequest().getMethod(),
          exchange.getRequest().getURI(),
          route.getId(),
          requestUrl,
          route.getUri());
    }

    if (requestUrl != null && (requestUrl.getHost() == null || requestUrl.getHost().isBlank())) {
      String routeId = route == null ? "unknown" : route.getId();
      String routeUri = route == null ? "unknown" : String.valueOf(route.getUri());
      log.error(
          "Upstream URI has no host for {} (routeId={}, routeUri={}, requestUrl={}). Check the"
              + " matching *_SERVICE_URL on gateway.",
          path,
          routeId,
          routeUri,
          requestUrl);
      return writeBadUpstream(exchange, routeId, routeUri, requestUrl);
    }

    return chain.filter(exchange);
  }

  private static Mono<Void> writeBadUpstream(
      ServerWebExchange exchange, String routeId, String routeUri, URI requestUrl) {
    String body =
        "{\"error\":\"Gateway upstream has no host\",\"routeId\":\""
            + routeId
            + "\",\"routeUri\":\""
            + routeUri
            + "\",\"requestUrl\":\""
            + requestUrl
            + "\",\"hint\":\"Set DIARY_SERVICE_URL=http://<diary-service-private-host>:8080 on"
            + " gateway (include http://). Railway private host is shown in diary-service"
            + " Networking / OpenAPI servers URL.\"}";
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
    return exchange.getResponse().writeWith(Mono.just(buffer));
  }

  @Override
  public int getOrder() {
    // After RouteToRequestUrlFilter has set GATEWAY_REQUEST_URL_ATTR.
    return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
  }
}
