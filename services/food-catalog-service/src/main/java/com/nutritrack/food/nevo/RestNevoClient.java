package com.nutritrack.food.nevo;

import org.springframework.http.HttpHeaders;
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
}
