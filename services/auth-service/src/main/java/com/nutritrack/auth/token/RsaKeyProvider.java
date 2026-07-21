package com.nutritrack.auth.token;

import com.nimbusds.jose.jwk.RSAKey;
import com.nutritrack.auth.config.AuthProperties;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class RsaKeyProvider {

  private final RSAKey rsaKey;

  public RsaKeyProvider(AuthProperties properties) {
    this.rsaKey = buildKey(properties);
  }

  public RSAKey rsaKey() {
    return rsaKey;
  }

  private static RSAKey buildKey(AuthProperties properties) {
    String keyId = properties.jwtKeyId() == null || properties.jwtKeyId().isBlank()
        ? "nutritrack-1"
        : properties.jwtKeyId();
    try {
      if (properties.jwtPrivateKeyPem() != null && !properties.jwtPrivateKeyPem().isBlank()) {
        RSAPrivateKey privateKey = parsePrivateKey(properties.jwtPrivateKeyPem());
        RSAPublicKey publicKey = derivePublicKey(privateKey);
        return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(keyId).build();
      }
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      KeyPair keyPair = generator.generateKeyPair();
      return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
          .privateKey((RSAPrivateKey) keyPair.getPrivate())
          .keyID(keyId)
          .build();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to initialize JWT signing key", ex);
    }
  }

  private static RSAPrivateKey parsePrivateKey(String pem) throws Exception {
    String sanitized =
        pem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
    byte[] decoded = Base64.getDecoder().decode(sanitized);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
    return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
  }

  private static RSAPublicKey derivePublicKey(RSAPrivateKey privateKey) throws Exception {
    // Rebuild a key pair from the private key modulus/exponent via KeyFactory is not direct;
    // generate public from CRT or use KeyPair. For PKCS#8 RSA private keys we extract modulus.
    var keySpec =
        KeyFactory.getInstance("RSA")
            .getKeySpec(privateKey, java.security.spec.RSAPrivateCrtKeySpec.class);
    var publicSpec =
        new java.security.spec.RSAPublicKeySpec(keySpec.getModulus(), keySpec.getPublicExponent());
    return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(publicSpec);
  }
}
