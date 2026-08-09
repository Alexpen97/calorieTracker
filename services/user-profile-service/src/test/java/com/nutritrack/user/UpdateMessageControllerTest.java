package com.nutritrack.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nutritrack.user.domain.UpdateMessageAckRepository;
import com.nutritrack.user.domain.UpdateMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:update_messages;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.flyway.enabled=true",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused",
      "nutritrack.user.internal-api-key=test-internal"
    })
class UpdateMessageControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UpdateMessageAckRepository ackRepository;
  @Autowired private UpdateMessageRepository messageRepository;

  @BeforeEach
  void clearMessages() {
    ackRepository.deleteAll();
    messageRepository.deleteAll();
  }

  @Test
  void pushRequiresInternalKeyAndPendingIsPerUserOnce() throws Exception {
    mockMvc
        .perform(
            post("/api/users/internal/update-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"What's new","body":"We added water goals."}
                    """))
        .andExpect(status().isUnauthorized());

    MvcResult pushed =
        mockMvc
            .perform(
                post("/api/users/internal/update-messages")
                    .header("X-Internal-Api-Key", "test-internal")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "title":"What's new",
                          "body":"We added water goals.",
                          "imageUrl":"https://cdn.example/update.png",
                          "actionLabel":"See today",
                          "actionUrl":"/today"
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.title").value("What's new"))
            .andExpect(jsonPath("$.body").value("We added water goals."))
            .andExpect(jsonPath("$.imageUrl").value("https://cdn.example/update.png"))
            .andExpect(jsonPath("$.actionLabel").value("See today"))
            .andExpect(jsonPath("$.actionUrl").value("/today"))
            .andExpect(jsonPath("$.pushedAt").exists())
            .andReturn();

    String messageId =
        com.jayway.jsonpath.JsonPath.read(pushed.getResponse().getContentAsString(), "$.id");

    Jwt userA = jwtForNewUser("upd-a");
    Jwt userB = jwtForNewUser("upd-b");

    mockMvc
        .perform(
            get("/api/users/me/update-messages/pending")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(userA)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(messageId))
        .andExpect(jsonPath("$.title").value("What's new"));

    mockMvc
        .perform(
            post("/api/users/me/update-messages/" + messageId + "/acknowledge")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(userA)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/users/me/update-messages/pending")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(userA)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/users/me/update-messages/pending")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(userB)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(messageId));

    // Idempotent acknowledge
    mockMvc
        .perform(
            post("/api/users/me/update-messages/" + messageId + "/acknowledge")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(userA)))
        .andExpect(status().isNoContent());
  }

  @Test
  void newPushOpensAgainEvenAfterPreviousAck() throws Exception {
    Jwt user = jwtForNewUser("upd-reopen");

    String firstId = pushMessage("First", "First body");
    mockMvc
        .perform(
            post("/api/users/me/update-messages/" + firstId + "/acknowledge")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(user)))
        .andExpect(status().isNoContent());

    String secondId = pushMessage("Second", "Second body");

    mockMvc
        .perform(
            get("/api/users/me/update-messages/pending")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(secondId))
        .andExpect(jsonPath("$.title").value("Second"));
  }

  @Test
  void pendingReturnsOldestUnackedWhenMultiplePushed() throws Exception {
    Jwt user = jwtForNewUser("upd-fifo");

    String olderId = pushMessage("Older", "Older body");
    Thread.sleep(5);
    String newerId = pushMessage("Newer", "Newer body");

    mockMvc
        .perform(
            get("/api/users/me/update-messages/pending")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(olderId));

    mockMvc
        .perform(
            post("/api/users/me/update-messages/" + olderId + "/acknowledge")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(user)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/users/me/update-messages/pending")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(newerId));
  }

  @Test
  void adminCanPushWithAdminJwt() throws Exception {
    Jwt admin = jwtForNewUser("upd-admin", "ADMIN");

    mockMvc
        .perform(
            post("/api/users/admin/update-messages")
                .with(
                    SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(admin)
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Admin push","body":"From admin token"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Admin push"));

    Jwt user = jwtForNewUser("upd-admin-viewer");
    mockMvc
        .perform(
            post("/api/users/admin/update-messages")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Nope","body":"User cannot push"}
                    """))
        .andExpect(status().isForbidden());
  }

  @Test
  void acknowledgeUnknownMessageReturnsNotFound() throws Exception {
    Jwt user = jwtForNewUser("upd-missing");
    mockMvc
        .perform(
            post("/api/users/me/update-messages/00000000-0000-0000-0000-000000000099/acknowledge")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(user)))
        .andExpect(status().isNotFound());
  }

  private String pushMessage(String title, String body) throws Exception {
    MvcResult pushed =
        mockMvc
            .perform(
                post("/api/users/internal/update-messages")
                    .header("X-Internal-Api-Key", "test-internal")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"title":"%s","body":"%s"}
                        """
                            .formatted(title, body)))
            .andExpect(status().isOk())
            .andReturn();
    return com.jayway.jsonpath.JsonPath.read(pushed.getResponse().getContentAsString(), "$.id");
  }

  private Jwt jwtForNewUser(String prefix) throws Exception {
    return jwtForNewUser(prefix, "USER");
  }

  private Jwt jwtForNewUser(String prefix, String role) throws Exception {
    String unique = prefix + "-" + java.util.UUID.randomUUID();
    MvcResult upsert =
        mockMvc
            .perform(
                post("/api/users/internal/upsert")
                    .header("X-Internal-Api-Key", "test-internal")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"googleSub":"%s","email":"%s@example.test","displayName":"Test User","avatarUrl":""}
                        """
                            .formatted(unique, unique)))
            .andExpect(status().isOk())
            .andReturn();

    String userId =
        com.jayway.jsonpath.JsonPath.read(upsert.getResponse().getContentAsString(), "$.id");
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject(userId)
        .claim("email", unique + "@example.test")
        .claim("roles", java.util.List.of(role))
        .build();
  }
}
