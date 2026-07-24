package com.nutritrack.nevo.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "nutritrack.nevo")
public record NevoProperties(
    @DefaultValue("") String csvPath,
    @DefaultValue("2025/9.0") String version,
    @DefaultValue("dev-internal-key") String internalApiKey,
    @DefaultValue("true") boolean autoImportOnStartup,
    @DefaultValue Match match,
    @DefaultValue Translate translate) {

  public record Match(
      @DefaultValue("0.72") double highThreshold,
      @DefaultValue("0.48") double mediumThreshold,
      @DefaultValue("25") int candidateLimit) {}

  public record Translate(
      @DefaultValue("false") boolean enabled,
      @DefaultValue("http://localhost:5000") String baseUrl,
      @DefaultValue("auto") String source,
      @DefaultValue("en") String target,
      @DefaultValue("3s") Duration timeout) {}
}
