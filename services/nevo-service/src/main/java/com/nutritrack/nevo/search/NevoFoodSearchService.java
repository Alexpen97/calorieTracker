package com.nutritrack.nevo.search;

import com.nutritrack.nevo.domain.NevoFood;
import com.nutritrack.nevo.domain.NevoFoodRepository;
import com.nutritrack.nevo.domain.NevoNutrientValueRepository;
import com.nutritrack.nevo.match.ProductNameNormalizer;
import com.nutritrack.nevo.web.dto.NevoFoodSearchResponse;
import com.nutritrack.nevo.web.dto.NevoMatchResponse;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NevoFoodSearchService {

  private static final int CANDIDATE_LIMIT = 50;
  private static final Set<String> VEGETABLE_GROUPS = Set.of("vegetables", "groente");
  private static final Set<String> HERBS_AND_SPICES_GROUPS =
      Set.of("herbs and spices", "kruiden en specerijen");
  private static final Set<String> SAVOURY_SNACK_GROUPS =
      Set.of("savoury snacks", "hartige snacks en zoutjes");
  private static final Set<String> VEGETABLE_ALIASES =
      Set.of("paprika", "courgette", "courgettes", "zucchini", "aubergine", "eggplant");
  private static final Set<String> PREPARATION_WORDS =
      Set.of(
          "raw",
          "rauw",
          "boiled",
          "gekookt",
          "cooked",
          "fried",
          "baked",
          "grilled",
          "dried",
          "canned",
          "pickled");

  private final NevoFoodRepository foodRepository;
  private final NevoNutrientValueRepository nutrientRepository;
  private final ProductNameNormalizer normalizer;

  public NevoFoodSearchService(
      NevoFoodRepository foodRepository,
      NevoNutrientValueRepository nutrientRepository,
      ProductNameNormalizer normalizer) {
    this.foodRepository = foodRepository;
    this.nutrientRepository = nutrientRepository;
    this.normalizer = normalizer;
  }

  @Transactional(readOnly = true)
  public NevoFoodSearchResponse search(String rawQuery, int limit) {
    String query = rawQuery == null ? "" : rawQuery.trim();
    if (query.isBlank() || limit <= 0) {
      return new NevoFoodSearchResponse(query, List.of());
    }

    List<String> terms =
        normalizer.expandSearchTerms(query).stream().filter(term -> term.length() >= 2).toList();
    Map<String, NevoFood> candidates = new LinkedHashMap<>();
    for (String term : terms) {
      for (NevoFood food : foodRepository.searchByTerm(term, CANDIDATE_LIMIT)) {
        candidates.putIfAbsent(food.getNevoCode(), food);
        if (candidates.size() >= CANDIDATE_LIMIT) {
          break;
        }
      }
      if (candidates.size() >= CANDIDATE_LIMIT) {
        break;
      }
    }

    List<NevoFoodSearchResponse.Item> items =
        candidates.values().stream()
            .map(food -> new ScoredFood(food, score(food, query, terms)))
            .sorted(
                Comparator.comparingDouble(ScoredFood::score)
                    .reversed()
                    .thenComparing(scored -> safe(scored.food().getFoodNameEn())))
            .limit(limit)
            .map(scored -> toItem(scored.food()))
            .toList();
    return new NevoFoodSearchResponse(query, items);
  }

  private double score(NevoFood food, String query, List<String> terms) {
    double score = 0.0;
    String group = lower(food.getFoodGroup());
    List<String> fields =
        List.of(lower(food.getFoodNameEn()), lower(food.getFoodNameNl()), lower(food.getSynonym()));
    List<String> lowerTerms = terms.stream().map(NevoFoodSearchService::lower).toList();

    if (VEGETABLE_GROUPS.contains(group)) {
      score += 3.0;
    }
    if (matchesAny(fields, lowerTerms, String::startsWith)) {
      score += 2.0;
    }
    if (matchesAny(fields, lowerTerms, NevoFoodSearchService::containsWholeWord)) {
      score += 1.0;
    }
    if (containsRawName(food) && !hasPreparationWord(query)) {
      score += 0.5;
    }

    String lowerQuery = lower(query);
    if (isSingleTokenVegetableAlias(lowerQuery)) {
      if (HERBS_AND_SPICES_GROUPS.contains(group)) {
        score -= 2.5;
      }
      if (SAVOURY_SNACK_GROUPS.contains(group)) {
        score -= 2.0;
      }
    }
    return score;
  }

  private NevoFoodSearchResponse.Item toItem(NevoFood food) {
    List<NevoMatchResponse.NevoNutrientDto> nutrients =
        nutrientRepository.findByNevoCode(food.getNevoCode()).stream()
            .filter(n -> n.getNutrientCode() != null && n.getAmountPer100g() != null)
            .map(
                n ->
                    new NevoMatchResponse.NevoNutrientDto(
                        n.getNutrientCode(), n.getAmountPer100g(), n.getUnit()))
            .toList();
    return new NevoFoodSearchResponse.Item(
        food.getNevoCode(),
        food.getFoodNameEn(),
        food.getFoodNameNl(),
        food.getFoodGroup(),
        food.getSynonym(),
        nutrients);
  }

  private static boolean matchesAny(
      List<String> fields, List<String> terms, TermMatcher matcher) {
    for (String field : fields) {
      if (field.isBlank()) {
        continue;
      }
      for (String term : terms) {
        if (!term.isBlank() && matcher.matches(field, term)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean containsWholeWord(String field, String term) {
    return Pattern.compile("(?<![a-z0-9])" + Pattern.quote(term) + "(?![a-z0-9])")
        .matcher(field)
        .find();
  }

  private static boolean containsRawName(NevoFood food) {
    String names = lower(food.getFoodNameEn()) + " " + lower(food.getFoodNameNl());
    return containsWholeWord(names, "raw") || containsWholeWord(names, "rauw");
  }

  private static boolean hasPreparationWord(String query) {
    String lowerQuery = lower(query);
    return PREPARATION_WORDS.stream()
        .anyMatch(preparation -> containsWholeWord(lowerQuery, preparation));
  }

  private static boolean isSingleTokenVegetableAlias(String query) {
    return !query.contains(" ") && VEGETABLE_ALIASES.contains(query);
  }

  private static String lower(String value) {
    return safe(value).toLowerCase(Locale.ROOT);
  }

  private static String safe(String value) {
    return value == null ? "" : value;
  }

  private record ScoredFood(NevoFood food, double score) {}

  @FunctionalInterface
  private interface TermMatcher {
    boolean matches(String field, String term);
  }
}
