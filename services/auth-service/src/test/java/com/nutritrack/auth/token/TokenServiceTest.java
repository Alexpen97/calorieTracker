package com.nutritrack.auth.token;

import static org.assertj.core.api.Assertions.assertThat;

import com.nutritrack.auth.config.AuthProperties;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class TokenServiceTest {

  @Test
  void issuedJwtIsValidAgainstPublicKey() throws Exception {
    AuthProperties properties =
        new AuthProperties(
            "dev",
            "",
            "",
            "https://oauth2.googleapis.com/token",
            "https://www.googleapis.com/oauth2/v3/certs",
            "http://test-issuer",
            "",
            "test-key",
            Duration.ofMinutes(15),
            Duration.ofDays(1),
            "http://localhost:8082",
            "key");
    RsaKeyProvider keyProvider = new RsaKeyProvider(properties);
    TokenService tokenService =
        new TokenService(properties, new RefreshTokenStore(), keyProvider);

    UUID userId = UUID.randomUUID();
    TokenService.IssuedTokens tokens = tokenService.issueTokens(userId, "a@b.c", "USER");

    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withPublicKey(keyProvider.rsaKey().toRSAPublicKey()).build();
    Jwt jwt = decoder.decode(tokens.accessToken());
    assertThat(jwt.getSubject()).isEqualTo(userId.toString());
    assertThat(jwt.getClaimAsString("email")).isEqualTo("a@b.c");
    assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
    assertThat(tokenService.jwks().get("keys")).isNotNull();
  }
}
