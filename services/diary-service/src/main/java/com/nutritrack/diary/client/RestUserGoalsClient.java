package com.nutritrack.diary.client;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

public class RestUserGoalsClient implements UserGoalsClient {

  private static final Logger log = LoggerFactory.getLogger(RestUserGoalsClient.class);
  private static final ParameterizedTypeReference<List<UserGoalResponse>> GOAL_LIST =
      new ParameterizedTypeReference<>() {};

  private final RestClient restClient;

  public RestUserGoalsClient(RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public List<UserGoalResponse> getGoals(String bearerToken) {
    try {
      List<UserGoalResponse> goals =
          restClient
              .get()
              .uri("/api/users/me/goals")
              .headers(
                  headers -> {
                    if (bearerToken != null && !bearerToken.isBlank()) {
                      headers.set(HttpHeaders.AUTHORIZATION, bearerToken);
                    }
                  })
              .retrieve()
              .body(GOAL_LIST);
      if (goals == null) {
        log.warn("User goals response body was null");
        return List.of();
      }
      return List.copyOf(goals);
    } catch (RestClientResponseException ex) {
      log.warn(
          "User goals request failed status={} body={}",
          ex.getStatusCode().value(),
          ex.getResponseBodyAsString());
      throw new UserGoalsUnavailableException(ex);
    } catch (RuntimeException ex) {
      log.warn("User goals request failed: {}", ex.toString());
      throw new UserGoalsUnavailableException(ex);
    }
  }
}
