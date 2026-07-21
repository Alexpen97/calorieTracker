package com.nutritrack.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nutritrack.user.config.DatabaseUrls;
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
}
