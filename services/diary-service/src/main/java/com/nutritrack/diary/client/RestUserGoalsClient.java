package com.nutritrack.diary.client;

import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

public class RestUserGoalsClient implements UserGoalsClient {

  private final RestClient restClient;

  public RestUserGoalsClient(RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public List<UserGoalResponse> getGoals(String bearerToken) {
    try {
      UserGoalResponse[] goals =
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
              .body(UserGoalResponse[].class);
      return goals == null ? List.of() : Arrays.asList(goals);
    } catch (RestClientResponseException ex) {
      throw new UserGoalsUnavailableException(ex);
    } catch (RuntimeException ex) {
      throw new UserGoalsUnavailableException(ex);
    }
  }
}
