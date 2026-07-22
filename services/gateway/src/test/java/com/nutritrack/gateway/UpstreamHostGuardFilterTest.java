package com.nutritrack.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutritrack.gateway.config.UpstreamHostGuardFilter;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class UpstreamHostGuardFilterTest {

  @Test
  void returnsBadGatewayWhenUpstreamHostMissing() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("https://gw/api/diary/water").build());
    Route route =
        Route.async()
            .id("diary-service")
            .uri(URI.create("http://placeholder"))
            .predicate(ex -> true)
            .build();
    // Simulate RouteToRequestUrlFilter merging a hostless route URI.
    exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);
    exchange
        .getAttributes()
        .put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, URI.create("http:/api/diary/water"));

    UpstreamHostGuardFilter filter = new UpstreamHostGuardFilter();
    filter.filter(exchange, ex -> Mono.empty()).block();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(exchange.getResponse().getBodyAsString().block()).contains("no host");
    assertThat(filter.getOrder()).isEqualTo(RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1);
  }

  @Test
  void passesThroughWhenUpstreamHostPresent() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("https://gw/api/diary/water").build());
    Route route =
        Route.async()
            .id("diary-service")
            .uri(URI.create("http://diary.railway.internal:8080"))
            .predicate(ex -> true)
            .build();
    exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);
    exchange
        .getAttributes()
        .put(
            ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR,
            URI.create("http://diary.railway.internal:8080/api/diary/water"));

    boolean[] continued = {false};
    UpstreamHostGuardFilter filter = new UpstreamHostGuardFilter();
    filter
        .filter(
            exchange,
            ex -> {
              continued[0] = true;
              return Mono.empty();
            })
        .block();

    assertThat(continued[0]).isTrue();
    assertThat(exchange.getResponse().getStatusCode()).isNull();
  }
}
