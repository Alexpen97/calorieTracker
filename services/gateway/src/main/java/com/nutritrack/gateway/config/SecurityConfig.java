package com.nutritrack.gateway.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Flux;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Value("${nutritrack.cors.allowed-origins:http://localhost:5173,http://localhost}")
  private String allowedOrigins;

  @Value("${nutritrack.swagger-ui-enabled:true}")
  private boolean swaggerUiEnabled;

  @Bean
  SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeExchange(
            exchanges -> {
              exchanges
                  .pathMatchers(HttpMethod.OPTIONS)
                  .permitAll()
                  .pathMatchers(
                      "/api/auth/**",
                      "/actuator/health",
                      "/actuator/info",
                      "/.well-known/jwks.json")
                  .permitAll();
              if (swaggerUiEnabled) {
                exchanges
                    .pathMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/api-docs/**")
                    .permitAll();
              }
              exchanges.anyExchange().authenticated();
            })
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(reactiveJwtAuthConverter())));
    return http.build();
  }

  private ReactiveJwtAuthenticationConverter reactiveJwtAuthConverter() {
    ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          Object roles = jwt.getClaims().get("roles");
          if (!(roles instanceof Collection<?> collection)) {
            return Flux.empty();
          }
          return Flux.fromIterable(
              collection.stream()
                  .map(Object::toString)
                  .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                  .map(SimpleGrantedAuthority::new)
                  .collect(Collectors.toList()));
        });
    return converter;
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
