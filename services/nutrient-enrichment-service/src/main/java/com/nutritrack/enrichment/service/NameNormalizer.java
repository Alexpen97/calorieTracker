package com.nutritrack.enrichment.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class NameNormalizer {

  private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9\\s]+");
  private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
  private static final Pattern QUANTITY =
      Pattern.compile("\\b\\d+(?:\\.\\d+)?\\s*(?:g|kg|ml|l|oz|lb|mg|µg|ug)\\b");
  private static final Set<String> STOP_WORDS =
      Set.of(
          "with", "and", "the", "of", "original", "classic", "new", "a", "an", "in", "for", "by");

  private static final String[] FORTIFIED_MARKERS = {
    "cereal",
    "fortified",
    "enriched",
    "infant",
    "formula",
    "supplement",
    "protein powder",
    "energy drink",
    "meal replacement",
    "soy drink",
    "oat drink",
    "almond drink",
    "plant milk"
  };

  private NameNormalizer() {}

  public static String normalize(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    String lower = raw.toLowerCase(Locale.ROOT);
    lower = QUANTITY.matcher(lower).replaceAll(" ");
    lower = NON_ALNUM.matcher(lower).replaceAll(" ");
    lower = MULTI_SPACE.matcher(lower).replaceAll(" ").trim();
    return lower;
  }

  public static Set<String> tokens(String raw) {
    String normalized = normalize(raw);
    if (normalized.isEmpty()) {
      return Set.of();
    }
    return Arrays.stream(normalized.split(" "))
        .filter(t -> !t.isBlank())
        .filter(t -> !STOP_WORDS.contains(t))
        .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
  }

  public static double tokenJaccard(String a, String b) {
    Set<String> left = tokens(a);
    Set<String> right = tokens(b);
    if (left.isEmpty() && right.isEmpty()) {
      return 1.0;
    }
    if (left.isEmpty() || right.isEmpty()) {
      return 0.0;
    }
    Set<String> intersection = new HashSet<>(left);
    intersection.retainAll(right);
    Set<String> union = new HashSet<>(left);
    union.addAll(right);
    return (double) intersection.size() / (double) union.size();
  }

  /** Strip brand tokens from a product name for generic proxy search. */
  public static String genericQuery(String name, String brand) {
    Set<String> brandTokens = tokens(brand);
    return tokens(name).stream()
        .filter(t -> !brandTokens.contains(t))
        .collect(Collectors.joining(" "));
  }

  public static boolean looksFortified(String name) {
    if (name == null || name.isBlank()) {
      return false;
    }
    String lower = name.toLowerCase(Locale.ROOT);
    for (String marker : FORTIFIED_MARKERS) {
      if (lower.contains(marker)) {
        return true;
      }
    }
    return false;
  }

  public static String stripLeadingZeros(String barcode) {
    if (barcode == null) {
      return "";
    }
    String stripped = barcode.replaceFirst("^0+", "");
    return stripped.isEmpty() ? "0" : stripped;
  }
}
