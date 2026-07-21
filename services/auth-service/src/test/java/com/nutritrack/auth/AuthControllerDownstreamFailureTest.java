package com.nutritrack.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = {
      "nutritrack.auth.mode=dev",
      "nutritrack.auth.user-service-url=http://127.0.0.1:9",
      "nutritrack.auth.internal-api-key=test-key",
      "nutritrack.auth.access-token-ttl=15m",
      "nutritrack.auth.refresh-token-ttl=1d",
      "nutritrack.auth.issuer=http://test"
    })
class AuthControllerDownstreamFailureTest {

  @LocalServerPort private int port;

  @Test
  void googleCallbackReturns503WhenUserServiceIsUnreachable() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/api/auth/google/callback"))
            .header("Content-Type", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    """
                    {"code":"dev","redirectUri":"http://localhost/auth/callback"}
                    """))
            .build();

    HttpResponse<String> response =
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode())
        .as("misleading 403 hid downstream failure; body=%s", response.body())
        .isEqualTo(503);
    assertThat(response.body()).contains("USER_SERVICE_URL");
  }
}
