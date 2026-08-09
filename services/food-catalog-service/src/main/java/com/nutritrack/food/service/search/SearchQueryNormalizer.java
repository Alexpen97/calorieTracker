package com.nutritrack.food.service.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SearchQueryNormalizer {

  private static final Map<String, Set<String>> SYNONYMS =
      Map.of(
          "milk", Set.of("drink"),
          "drink", Set.of("milk"),
          "yogurt", Set.of("yoghurt"),
          "yoghurt", Set.of("yogurt"));

  public NormalizedQuery normalize(String raw) {
    String input = raw == null ? "" : raw.trim();
    String normalized = normalizeText(input);
    List<String> tokens = tokenize(normalized);
    Set<String> expandedTokens = expandTokens(tokens);
    return new NormalizedQuery(raw, normalized, tokens, expandedTokens);
  }

  private static String normalizeText(String input) {
    String lower = input.toLowerCase(Locale.ROOT);
    String stripped = lower.replaceAll("[\\p{Punct}\\-]+", " ");
    return stripped.replaceAll("\\s+", " ").trim();
  }

  private static List<String> tokenize(String normalized) {
    if (normalized.isEmpty()) {
      return List.of();
    }
    List<String> tokens = new ArrayList<>();
    for (String token : normalized.split(" ")) {
      if (!token.isBlank() && token.length() >= 1) {
        tokens.add(token);
      }
    }
    return List.copyOf(tokens);
  }

  private static Set<String> expandTokens(List<String> tokens) {
    Set<String> expanded = new HashSet<>(tokens);
    for (String token : tokens) {
      Set<String> synonyms = SYNONYMS.get(token);
      if (synonyms != null) {
        expanded.addAll(synonyms);
      }
    }
    return Set.copyOf(expanded);
  }
}
