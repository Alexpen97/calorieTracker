package com.nutritrack.auth.web;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(WebClientRequestException.class)
  ResponseEntity<Map<String, String>> downstreamUnavailable(WebClientRequestException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(
            Map.of(
                "error",
                "A required backend service is unreachable. Verify USER_SERVICE_URL on auth-service"
                    + " points at user-profile-service and that the service is running."));
  }

  @ExceptionHandler(WebClientResponseException.class)
  ResponseEntity<Map<String, String>> downstreamError(WebClientResponseException ex) {
    HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
    if (status == null) {
      status = HttpStatus.BAD_GATEWAY;
    }
    return ResponseEntity.status(status)
        .body(Map.of("error", "Downstream request failed: " + ex.getStatusText()));
  }

  @ExceptionHandler(IllegalStateException.class)
  ResponseEntity<Map<String, String>> illegalState(IllegalStateException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.of("error", ex.getMessage()));
  }
}
