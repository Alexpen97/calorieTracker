package com.nutritrack.food.nevo;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class RestNevoClient implements NevoClient {

  private final RestClient restClient;
  private final String internalApiKey;

  public RestNevoClient(RestClient restClient, String internalApiKey) {
    this.restClient = restClient;
    this.internalApiKey = internalApiKey;
  }

  @Override
  public NevoMatchResponse matchBest(NevoMatchRequest request) {
    try {
      return restClient
          .post()
          .uri("/internal/nevo/matches/best")
          .header("X-Internal-Api-Key", internalApiKey)
          .contentType(MediaType.APPLICATION_JSON)
          .body(request)
          .retrieve()
          .body(NevoMatchResponse.class);
    } catch (RestClientException ex) {
      throw new NevoUnavailableException(ex);
    }
  }

  @Override
  public List<NevoFoodSearchResponse.Item> searchFoods(String q, int limit) {
    try {
      NevoFoodSearchResponse response =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/api/nevo/foods/search")
                          .queryParam("q", q)
                          .queryParam("limit", limit)
                          .build())
              .retrieve()
              .body(NevoFoodSearchResponse.class);
      return response == null ? List.of() : response.items();
    } catch (RestClientException ex) {
      throw new NevoUnavailableException(ex);
    }
  }
}
