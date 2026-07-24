package com.nutritrack.nevo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "nutritrack.nevo")
public record NevoProperties(
    @DefaultValue("") String csvPath,
    @DefaultValue("2025/9.0") String version,
    @DefaultValue("dev-internal-key") String internalApiKey,
    @DefaultValue Match match) {

  public record Match(
      @DefaultValue("0.72") double highThreshold,
      @DefaultValue("0.48") double mediumThreshold,
      @DefaultValue("25") int candidateLimit) {}
}
