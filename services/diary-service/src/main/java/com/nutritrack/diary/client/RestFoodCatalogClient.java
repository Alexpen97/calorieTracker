package com.nutritrack.diary.client;

import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

public class RestFoodCatalogClient implements FoodCatalogClient {

  private final RestClient restClient;

  public RestFoodCatalogClient(RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public ProductResponse getProduct(UUID id, String bearerToken) {
    try {
      return restClient
          .get()
          .uri("/api/products/{id}", id)
          .headers(
              headers -> {
                if (bearerToken != null && !bearerToken.isBlank()) {
                  headers.set(HttpHeaders.AUTHORIZATION, bearerToken);
                }
              })
          .retrieve()
          .body(ProductResponse.class);
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() == 404) {
        throw new ProductNotFoundException(id);
      }
      throw new FoodCatalogUnavailableException(ex);
    } catch (RuntimeException ex) {
      throw new FoodCatalogUnavailableException(ex);
    }
  }
}
