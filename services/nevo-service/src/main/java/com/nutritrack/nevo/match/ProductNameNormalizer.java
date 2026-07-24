package com.nutritrack.nevo.match;

import com.nutritrack.nevo.domain.NevoAlias;
import com.nutritrack.nevo.domain.NevoAliasRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProductNameNormalizer {

  private static final Pattern PACK_SIZE =
      Pattern.compile(
          "\\b\\d+([.,]\\d+)?\\s*(g|kg|ml|l|cl|oz|lb)\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9\\s-]+");
  private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

  private static final Set<String> DROP_WORDS =
      Set.of(
          "bio",
          "organic",
          "biologisch",
          "original",
          "classic",
          "premium",
          "new",
          "limited",
          "edition",
          "ah",
          "jumbo",
          "albert",
          "heijn",
          "plus",
          "lidl",
          "aldi",
          "brand",
          "product");

  private final NevoAliasRepository aliasRepository;

  public ProductNameNormalizer(NevoAliasRepository aliasRepository) {
    this.aliasRepository = aliasRepository;
  }

  public NormalizedQuery normalize(
      String name, String brand, List<String> categories, String ingredientsText) {
    Map<String, String> aliases =
        aliasRepository.findAll().stream()
            .collect(
                Collectors.toMap(
                    a -> a.getAliasTerm().toLowerCase(Locale.ROOT),
                    NevoAlias::getCanonicalTerm,
                    (a, b) -> a));

    String cleanedName = clean(name, brand, aliases);
    List<String> cleanedCategories = new ArrayList<>();
    if (categories != null) {
      for (String category : categories) {
        String cleaned = clean(category, null, aliases);
        if (!cleaned.isBlank()) {
          cleanedCategories.add(cleaned);
        }
      }
    }
    String cleanedIngredients = clean(ingredientsText, null, aliases);

    LinkedHashSet<String> queries = new LinkedHashSet<>();
    if (!cleanedName.isBlank()) {
      queries.add(cleanedName);
      String[] tokens = cleanedName.split(" ");
      if (tokens.length > 2) {
        queries.add(tokens[0] + " " + tokens[1]);
      }
      if (tokens.length > 1) {
        queries.add(tokens[tokens.length - 2] + " " + tokens[tokens.length - 1]);
        queries.add(tokens[tokens.length - 1]);
      }
    }
    cleanedCategories.stream().limit(3).forEach(queries::add);
    if (!cleanedIngredients.isBlank()) {
      String[] ingredientTokens = cleanedIngredients.split(" ");
      if (ingredientTokens.length > 0) {
        queries.add(ingredientTokens[0]);
      }
    }

    return new NormalizedQuery(
        cleanedName, cleanedCategories, cleanedIngredients, List.copyOf(queries));
  }

  private String clean(String raw, String brand, Map<String, String> aliases) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    String value = raw.toLowerCase(Locale.ROOT);
    if (brand != null && !brand.isBlank()) {
      for (String brandToken : brand.toLowerCase(Locale.ROOT).split("[,/&]")) {
        String token = brandToken.trim();
        if (!token.isEmpty()) {
          value = value.replace(token, " ");
        }
      }
    }
    value = PACK_SIZE.matcher(value).replaceAll(" ");
    value = NON_ALNUM.matcher(value).replaceAll(" ");
    value = MULTI_SPACE.matcher(value).replaceAll(" ").trim();

    List<String> kept = new ArrayList<>();
    for (String token : value.split(" ")) {
      if (token.isBlank() || DROP_WORDS.contains(token)) {
        continue;
      }
      kept.add(aliases.getOrDefault(token, token));
    }
    return String.join(" ", kept).trim();
  }

  public record NormalizedQuery(
      String cleanedName,
      List<String> categories,
      String ingredients,
      List<String> queryTerms) {}
}
