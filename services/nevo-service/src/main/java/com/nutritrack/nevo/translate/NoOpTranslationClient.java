package com.nutritrack.nevo.translate;

/** No-op client used when LibreTranslate is disabled. */
public class NoOpTranslationClient implements TranslationClient {

  @Override
  public java.util.Optional<String> translate(String text) {
    return java.util.Optional.empty();
  }
}
