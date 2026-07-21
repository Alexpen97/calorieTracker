package com.nutritrack.auth;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nutritrack.auth.client.UserProfileClient;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
    properties = {
      "nutritrack.auth.mode=dev",
      "nutritrack.auth.user-service-url=http://localhost:9",
      "nutritrack.auth.internal-api-key=test-key",
      "nutritrack.auth.access-token-ttl=15m",
      "nutritrack.auth.refresh-token-ttl=1d",
      "nutritrack.auth.issuer=http://test"
    })
@AutoConfigureMockMvc
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserProfileClient userProfileClient;

  @Test
  void googleCallbackInDevModeIssuesTokens() throws Exception {
    UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    when(userProfileClient.upsert(anyString(), anyString(), nullable(String.class), nullable(String.class)))
        .thenReturn(new UserProfileClient.UpsertedUser(userId, "dev-user@example.com", "USER"));

    mockMvc
        .perform(
            post("/api/auth/google/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code":"dev","codeVerifier":"x","redirectUri":"http://localhost/callback"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.tokenType").value("Bearer"));
  }

  @Test
  void refreshRotatesTokens() throws Exception {
    UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    when(userProfileClient.upsert(anyString(), anyString(), nullable(String.class), nullable(String.class)))
        .thenReturn(new UserProfileClient.UpsertedUser(userId, "dev-user@example.com", "USER"));

    MvcResult login =
        mockMvc
            .perform(
                post("/api/auth/google/callback")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"code":"dev","redirectUri":"http://localhost/callback"}
                        """))
            .andExpect(status().isOk())
            .andReturn();

    String refreshToken =
        com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.refreshToken");

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty());
  }

  @Test
  void jwksExposesPublicKey() throws Exception {
    mockMvc
        .perform(get("/.well-known/jwks.json"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.keys").isArray())
        .andExpect(jsonPath("$.keys[0].kty").value("RSA"));
  }
}
