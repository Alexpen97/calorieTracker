package com.nutritrack.nevo.translate;

import java.util.Optional;

public interface TranslationClient {

  /**
   * Translate {@code text} toward the configured target language. Returns empty when
   * translation is disabled, the input is blank, or the remote call fails.
   */
  Optional<String> translate(String text);
}
