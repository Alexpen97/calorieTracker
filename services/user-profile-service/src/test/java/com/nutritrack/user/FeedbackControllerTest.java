package com.nutritrack.user;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
      "spring.datasource.url=jdbc:h2:mem:feedback;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.flyway.enabled=true",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused",
      "nutritrack.user.internal-api-key=test-internal"
    })
class FeedbackControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void postFeedbackCreatesPendingEntryForJwtUser() throws Exception {
    Jwt jwt = jwtForNewUser("feedback-create");

    mockMvc
        .perform(
            post("/api/users/me/feedback")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"message":"Love the diary view, please add dark mode.","appVersion":"0.1.0"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.message").value("Love the diary view, please add dark mode."))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.appVersion").value("0.1.0"))
        .andExpect(jsonPath("$.createdAt").isNotEmpty())
        .andExpect(jsonPath("$.updatedAt").isNotEmpty());
  }

  @Test
  void listFeedbackReturnsOnlyOwnEntriesNewestFirst() throws Exception {
    Jwt userA = jwtForNewUser("feedback-a");
    Jwt userB = jwtForNewUser("feedback-b");

    postFeedback(userA, "First from A about settings");
    Thread.sleep(5);
    postFeedback(userA, "Second from A about barcode scan");
    postFeedback(userB, "From B should stay hidden");

    mockMvc
        .perform(get("/api/users/me/feedback").with(SecurityMockMvcRequestPostProcessors.jwt().jwt(userA)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].message").value("Second from A about barcode scan"))
        .andExpect(jsonPath("$[1].message").value("First from A about settings"))
        .andExpect(jsonPath("$[*].message", hasItem("Second from A about barcode scan")))
        .andExpect(jsonPath("$[*].message", org.hamcrest.Matchers.not(hasItem("From B should stay hidden"))));
  }

  @Test
  void postFeedbackRejectsTooShortMessage() throws Exception {
    Jwt jwt = jwtForNewUser("feedback-short");

    mockMvc
        .perform(
            post("/api/users/me/feedback")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"message":"too short"}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void moderatorCanUpdateFeedbackStatusAndUserSeesIt() throws Exception {
    Jwt user = jwtForNewUser("feedback-status-user");
    MvcResult created =
        mockMvc
            .perform(
                post("/api/users/me/feedback")
                    .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(user))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"message":"Please improve onboarding copy clarity."}
                        """))
            .andExpect(status().isOk())
            .andReturn();
    String feedbackId =
        com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

    Jwt moderator =
        Jwt.withTokenValue("mod")
            .header("alg", "none")
            .subject(java.util.UUID.randomUUID().toString())
            .claim("roles", java.util.List.of("MODERATOR"))
            .build();

    mockMvc
        .perform(
            patch("/api/users/feedback/" + feedbackId + "/status")
                .with(
                    SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(moderator)
                        .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"ACCEPTED"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACCEPTED"));

    mockMvc
        .perform(get("/api/users/me/feedback").with(SecurityMockMvcRequestPostProcessors.jwt().jwt(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(feedbackId))
        .andExpect(jsonPath("$[0].status").value("ACCEPTED"));
  }

  @Test
  void regularUserCannotUpdateFeedbackStatus() throws Exception {
    Jwt user = jwtForNewUser("feedback-forbid");
    MvcResult created =
        mockMvc
            .perform(
                post("/api/users/me/feedback")
                    .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(user))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"message":"Regular users must not triage feedback."}
                        """))
            .andExpect(status().isOk())
            .andReturn();
    String feedbackId =
        com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            patch("/api/users/feedback/" + feedbackId + "/status")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"COMPLETED"}
                    """))
        .andExpect(status().isForbidden());
  }

  private void postFeedback(Jwt jwt, String message) throws Exception {
    mockMvc
        .perform(
            post("/api/users/me/feedback")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"message":"%s"}
                    """
                        .formatted(message)))
        .andExpect(status().isOk());
  }

  private Jwt jwtForNewUser(String prefix) throws Exception {
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
        .claim("roles", java.util.List.of("USER"))
        .build();
  }
}
