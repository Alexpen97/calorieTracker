package com.nutritrack.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import com.nutritrack.auth.client.UserProfileClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
class AuthControllerHttpIntegrationTest {

  @LocalServerPort private int port;

  @MockitoBean private UserProfileClient userProfileClient;

  @Test
  void googleCallbackIsReachableOverHttp() throws Exception {
    UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    when(userProfileClient.upsert(anyString(), anyString(), nullable(String.class), nullable(String.class)))
        .thenReturn(new UserProfileClient.UpsertedUser(userId, "dev-user@example.com", "USER"));

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
        .as("auth callback should be public over real HTTP, body=%s", response.body())
        .isEqualTo(200);
  }
}
