package com.nutritrack.auth.token;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nutritrack.auth.config.AuthProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

  private final JwtEncoder jwtEncoder;
  private final AuthProperties properties;
  private final RefreshTokenStore refreshTokenStore;
  private final RsaKeyProvider rsaKeyProvider;

  public TokenService(
      AuthProperties properties, RefreshTokenStore refreshTokenStore, RsaKeyProvider rsaKeyProvider) {
    this.properties = properties;
    this.refreshTokenStore = refreshTokenStore;
    this.rsaKeyProvider = rsaKeyProvider;
    this.jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKeyProvider.rsaKey())));
  }

  public IssuedTokens issueTokens(UUID userId, String email, String role) {
    Instant now = Instant.now();
    Instant accessExpiry = now.plus(properties.accessTokenTtl());
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .issuedAt(now)
            .expiresAt(accessExpiry)
            .subject(userId.toString())
            .claim("email", email)
            .claim("roles", List.of(role))
            .build();
    JwsHeader header =
        JwsHeader.with(SignatureAlgorithm.RS256).keyId(rsaKeyProvider.rsaKey().getKeyID()).build();
    String accessToken =
        jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

    String refreshToken = UUID.randomUUID() + "." + UUID.randomUUID();
    Instant refreshExpiry = now.plus(properties.refreshTokenTtl());
    refreshTokenStore.store(refreshToken, userId, email, role, refreshExpiry);

    return new IssuedTokens(accessToken, refreshToken, accessExpiry, refreshExpiry);
  }

  public IssuedTokens refresh(String refreshToken) {
    RefreshTokenStore.StoredRefreshToken stored =
        refreshTokenStore
            .consume(refreshToken)
            .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
    return issueTokens(stored.userId(), stored.email(), stored.role());
  }

  public void revoke(String refreshToken) {
    refreshTokenStore.revoke(refreshToken);
  }

  public Map<String, Object> jwks() {
    return new JWKSet(rsaKeyProvider.rsaKey().toPublicJWK()).toJSONObject();
  }

  public record IssuedTokens(
      String accessToken, String refreshToken, Instant accessExpiresAt, Instant refreshExpiresAt) {}
}
