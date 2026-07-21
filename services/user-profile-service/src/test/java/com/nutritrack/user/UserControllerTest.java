package com.nutritrack.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nutritrack.user.config.DatabaseUrls;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:users;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.flyway.enabled=true",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused",
      "nutritrack.user.internal-api-key=test-internal"
    })
class UserControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void databaseUrlConversion() {
    org.assertj.core.api.Assertions.assertThat(
            DatabaseUrls.toJdbcUrl("postgres://user:pass@host:5432/users"))
        .isEqualTo("jdbc:postgresql://host:5432/users");
  }

  @Test
  void upsertRequiresInternalKeyAndMeUsesJwtSubject() throws Exception {
    mockMvc
        .perform(
            post("/api/users/internal/upsert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"googleSub":"g-1","email":"a@b.c","displayName":"Ada","avatarUrl":""}
                    """))
        .andExpect(status().isUnauthorized());

    MvcResult upsert =
        mockMvc
            .perform(
                post("/api/users/internal/upsert")
                    .header("X-Internal-Api-Key", "test-internal")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"googleSub":"g-1","email":"a@b.c","displayName":"Ada","avatarUrl":""}
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("a@b.c"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andReturn();

    String userId =
        com.jayway.jsonpath.JsonPath.read(upsert.getResponse().getContentAsString(), "$.id");

    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(userId)
            .claim("email", "a@b.c")
            .claim("roles", java.util.List.of("USER"))
            .build();

    mockMvc
        .perform(get("/api/users/me").with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId))
        .andExpect(jsonPath("$.displayName").value("Ada"));

    mockMvc
        .perform(
            put("/api/users/me")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sex":"FEMALE","heightCm":168,"activityLevel":"MODERATE","objective":"MAINTAIN"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sex").value("FEMALE"))
        .andExpect(jsonPath("$.heightCm").value(168));
  }

  @Test
  void postWeightCreatesEntryForJwtUserAndDefaultsMeasuredAt() throws Exception {
    Jwt jwt = jwtForNewUser("post-weight");

    mockMvc
        .perform(
            post("/api/users/me/weight")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"weightKg":70.5,"measuredAt":"2026-07-21T10:00:00Z"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.weightKg").value(70.5))
        .andExpect(jsonPath("$.measuredAt").value("2026-07-21T10:00:00Z"));

    Instant before = Instant.now();
    MvcResult defaultMeasuredAt =
        mockMvc
            .perform(
                post("/api/users/me/weight")
                    .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"weightKg":71.0}
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.weightKg").value(71.0))
            .andReturn();
    Instant after = Instant.now();

    String measuredAt =
        com.jayway.jsonpath.JsonPath.read(
            defaultMeasuredAt.getResponse().getContentAsString(), "$.measuredAt");
    org.assertj.core.api.Assertions.assertThat(Instant.parse(measuredAt))
        .isBetween(before.minusSeconds(1), after.plusSeconds(1));
  }

  @Test
  void getWeightReturnsOnlyJwtUsersEntriesNewestFirst() throws Exception {
    Jwt jwt = jwtForNewUser("get-weight-owner");
    Jwt otherJwt = jwtForNewUser("get-weight-other");

    postWeight(jwt, "70.0", "2026-07-21T08:00:00Z");
    postWeight(jwt, "71.5", "2026-07-21T10:00:00Z");
    postWeight(otherJwt, "99.0", "2026-07-21T12:00:00Z");

    mockMvc
        .perform(
            get("/api/users/me/weight").with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].weightKg").value(71.5))
        .andExpect(jsonPath("$[0].measuredAt").value("2026-07-21T10:00:00Z"))
        .andExpect(jsonPath("$[1].weightKg").value(70.0))
        .andExpect(jsonPath("$[1].measuredAt").value("2026-07-21T08:00:00Z"));
  }

  @Test
  void getWeightFiltersByMeasuredAtRange() throws Exception {
    Jwt jwt = jwtForNewUser("range-weight");

    postWeight(jwt, "69.0", "2026-07-20T10:00:00Z");
    postWeight(jwt, "70.0", "2026-07-21T10:00:00Z");
    postWeight(jwt, "71.0", "2026-07-22T10:00:00Z");

    mockMvc
        .perform(
            get("/api/users/me/weight")
                .queryParam("from", "2026-07-21T00:00:00Z")
                .queryParam("to", "2026-07-21T23:59:59Z")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].weightKg").value(70.0))
        .andExpect(jsonPath("$[0].measuredAt").value("2026-07-21T10:00:00Z"));
  }

  @Test
  void postWeightRejectsNonPositiveWeight() throws Exception {
    Jwt jwt = jwtForNewUser("invalid-weight");

    mockMvc
        .perform(
            post("/api/users/me/weight")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"weightKg":0,"measuredAt":"2026-07-21T10:00:00Z"}
                    """))
        .andExpect(status().isBadRequest());
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

  private void postWeight(Jwt jwt, String weightKg, String measuredAt) throws Exception {
    mockMvc
        .perform(
            post("/api/users/me/weight")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"weightKg":%s,"measuredAt":"%s"}
                    """
                        .formatted(weightKg, measuredAt)))
        .andExpect(status().isOk());
  }
}
