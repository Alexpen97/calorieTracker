package com.nutritrack.nevo.translate;

import com.nutritrack.nevo.config.NevoProperties;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class RestLibreTranslateClient implements TranslationClient {

  private static final Logger log = LoggerFactory.getLogger(RestLibreTranslateClient.class);

  private final RestClient restClient;
  private final NevoProperties.Translate settings;

  public RestLibreTranslateClient(RestClient restClient, NevoProperties.Translate settings) {
    this.restClient = restClient;
    this.settings = settings;
  }

  @Override
  public Optional<String> translate(String text) {
    if (!settings.enabled() || text == null || text.isBlank()) {
      return Optional.empty();
    }
    try {
      TranslateResponse body =
          restClient
              .post()
              .uri("/translate")
              .body(
                  Map.of(
                      "q", text,
                      "source", settings.source(),
                      "target", settings.target(),
                      "format", "text"))
              .retrieve()
              .body(TranslateResponse.class);
      if (body == null || body.translatedText() == null || body.translatedText().isBlank()) {
        return Optional.empty();
      }
      String translated = body.translatedText().trim();
      if (translated.equalsIgnoreCase(text.trim())) {
        return Optional.empty();
      }
      return Optional.of(translated);
    } catch (RestClientException ex) {
      log.warn("LibreTranslate failed for '{}': {}", truncate(text), ex.getMessage());
      return Optional.empty();
    }
  }

  private static String truncate(String text) {
    String trimmed = text.trim();
    return trimmed.length() <= 80 ? trimmed : trimmed.substring(0, 80) + "…";
  }

  public record TranslateResponse(String translatedText) {}
}
