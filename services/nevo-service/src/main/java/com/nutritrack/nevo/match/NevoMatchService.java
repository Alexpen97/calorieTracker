package com.nutritrack.nevo.match;

import com.nutritrack.nevo.config.NevoProperties;
import com.nutritrack.nevo.domain.NevoFood;
import com.nutritrack.nevo.domain.NevoFoodRepository;
import com.nutritrack.nevo.domain.NevoNutrientValue;
import com.nutritrack.nevo.domain.NevoNutrientValueRepository;
import com.nutritrack.nevo.translate.TranslationClient;
import com.nutritrack.nevo.web.dto.NevoMatchRequest;
import com.nutritrack.nevo.web.dto.NevoMatchResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NevoMatchService {

  private static final Logger log = LoggerFactory.getLogger(NevoMatchService.class);

  private static final Set<String> IMPORTANT_MODIFIERS =
      Set.of(
          "raw",
          "cooked",
          "boiled",
          "fried",
          "dried",
          "fresh",
          "canned",
          "sweetened",
          "unsweetened",
          "skimmed",
          "semi-skimmed",
          "whole",
          "plant-based",
          "soy",
          "fortified",
          "low-fat",
          "full-fat");

  private final NevoFoodRepository foodRepository;
  private final NevoNutrientValueRepository nutrientRepository;
  private final ProductNameNormalizer normalizer;
  private final NevoProperties properties;
  private final TranslationClient translationClient;
  private final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

  public NevoMatchService(
      NevoFoodRepository foodRepository,
      NevoNutrientValueRepository nutrientRepository,
      ProductNameNormalizer normalizer,
      NevoProperties properties,
      TranslationClient translationClient) {
    this.foodRepository = foodRepository;
    this.nutrientRepository = nutrientRepository;
    this.normalizer = normalizer;
    this.properties = properties;
    this.translationClient = translationClient;
  }

  @Transactional(readOnly = true)
  public NevoMatchResponse matchBest(NevoMatchRequest request) {
    if (request == null || (blank(request.name()) && blank(request.genericName()))) {
      return NevoMatchResponse.none();
    }

    String primaryName =
        !blank(request.genericName()) ? request.genericName() : request.name();
    String matchName =
        translationClient
            .translate(primaryName)
            .map(
                translated -> {
                  log.debug("Translated NEVO match name '{}' -> '{}'", primaryName, translated);
                  return translated;
                })
            .orElse(primaryName);

    ProductNameNormalizer.NormalizedQuery translatedQuery =
        normalizer.normalize(
            matchName, request.brand(), request.categories(), request.ingredientsText());

    // Also search with the original (often Dutch) tokens so NL names still hit Dutch columns.
    final ProductNameNormalizer.NormalizedQuery query;
    if (!matchName.equalsIgnoreCase(primaryName)) {
      ProductNameNormalizer.NormalizedQuery original =
          normalizer.normalize(
              primaryName, request.brand(), request.categories(), request.ingredientsText());
      query = mergeQueries(translatedQuery, original);
    } else {
      query = translatedQuery;
    }
    Map<String, NevoFood> candidates = new LinkedHashMap<>();
    int limit = properties.match().candidateLimit();
    for (String term : query.queryTerms()) {
      if (term.length() < 2) {
        continue;
      }
      for (NevoFood food : foodRepository.searchByTerm(term, limit)) {
        candidates.putIfAbsent(food.getNevoCode(), food);
      }
    }
    if (candidates.isEmpty() && !query.cleanedName().isBlank()) {
      for (String token : query.cleanedName().split(" ")) {
        if (token.length() < 3) {
          continue;
        }
        for (NevoFood food : foodRepository.searchByTerm(token, limit)) {
          candidates.putIfAbsent(food.getNevoCode(), food);
        }
      }
    }
    if (candidates.isEmpty()) {
      return NevoMatchResponse.none();
    }

    ScoredCandidate best =
        candidates.values().stream()
            .map(food -> score(food, query, request.knownMacros()))
            .max(Comparator.comparingDouble(ScoredCandidate::score))
            .orElse(null);
    if (best == null || best.score() <= 0.15) {
      return NevoMatchResponse.none();
    }

    MatchConfidence confidence = classify(best.score());
    List<NevoMatchResponse.NevoNutrientDto> nutrients =
        nutrientRepository.findByNevoCode(best.food().getNevoCode()).stream()
            .filter(n -> n.getNutrientCode() != null && n.getAmountPer100g() != null)
            .map(
                n ->
                    new NevoMatchResponse.NevoNutrientDto(
                        n.getNutrientCode(), n.getAmountPer100g(), n.getUnit()))
            .toList();

    return new NevoMatchResponse(
        true,
        best.food().getNevoCode(),
        best.food().getFoodNameEn(),
        best.food().getFoodGroup(),
        best.food().getNevoVersion(),
        confidence.name(),
        round(best.score()),
        best.reasons(),
        nutrients);
  }

  private static ProductNameNormalizer.NormalizedQuery mergeQueries(
      ProductNameNormalizer.NormalizedQuery primary, ProductNameNormalizer.NormalizedQuery extra) {
    LinkedHashMap<String, Boolean> terms = new LinkedHashMap<>();
    for (String term : primary.queryTerms()) {
      terms.put(term, Boolean.TRUE);
    }
    for (String term : extra.queryTerms()) {
      terms.putIfAbsent(term, Boolean.TRUE);
    }
    List<String> categories = new ArrayList<>(primary.categories());
    for (String category : extra.categories()) {
      if (!categories.contains(category)) {
        categories.add(category);
      }
    }
    String ingredients =
        blank(primary.ingredients()) ? extra.ingredients() : primary.ingredients();
    return new ProductNameNormalizer.NormalizedQuery(
        primary.cleanedName(), categories, ingredients, List.copyOf(terms.keySet()));
  }

  private ScoredCandidate score(
      NevoFood food,
      ProductNameNormalizer.NormalizedQuery query,
      List<NevoMatchRequest.KnownMacro> knownMacros) {
    List<String> reasons = new ArrayList<>();
    double score = 0.0;

    String foodName = safeLower(food.getFoodNameEn());
    String searchDoc = safeLower(food.getSearchDocument());
    double nameScore =
        Math.max(
            similarity.apply(query.cleanedName(), foodName),
            similarity.apply(query.cleanedName(), searchDoc));
    score += nameScore * 0.55;
    reasons.add("nameSimilarity=" + round(nameScore));

    double categoryScore = 0.0;
    if (food.getFoodGroup() != null && !query.categories().isEmpty()) {
      String group = safeLower(food.getFoodGroup());
      for (String category : query.categories()) {
        categoryScore = Math.max(categoryScore, similarity.apply(category, group));
        if (group.contains(category) || category.contains(group)) {
          categoryScore = Math.max(categoryScore, 0.85);
        }
      }
      score += categoryScore * 0.15;
      reasons.add("categorySimilarity=" + round(categoryScore));
    }

    double modifierPenalty = modifierPenalty(query.cleanedName(), foodName + " " + searchDoc);
    score -= modifierPenalty;
    if (modifierPenalty > 0) {
      reasons.add("modifierPenalty=" + round(modifierPenalty));
    }

    double macroScore = macroSimilarity(food, knownMacros);
    score += macroScore * 0.30;
    if (macroScore > 0) {
      reasons.add("macroSimilarity=" + round(macroScore));
    }

    if (query.cleanedName().contains("plant") && !foodName.contains("plant")
        && !searchDoc.contains("soy")
        && !searchDoc.contains("oat")
        && !searchDoc.contains("almond")) {
      score -= 0.20;
      reasons.add("plantBasedMismatch");
    }

    return new ScoredCandidate(food, Math.max(0.0, Math.min(1.0, score)), reasons);
  }

  private double modifierPenalty(String query, String candidate) {
    double penalty = 0.0;
    for (String modifier : IMPORTANT_MODIFIERS) {
      boolean inQuery = query.contains(modifier);
      boolean inCandidate = candidate.contains(modifier);
      if (inQuery != inCandidate) {
        penalty += 0.08;
      }
    }
    return Math.min(penalty, 0.32);
  }

  private double macroSimilarity(NevoFood food, List<NevoMatchRequest.KnownMacro> knownMacros) {
    if (knownMacros == null || knownMacros.isEmpty()) {
      return 0.0;
    }
    Map<String, BigDecimal> known = new LinkedHashMap<>();
    for (NevoMatchRequest.KnownMacro macro : knownMacros) {
      if (macro == null || macro.code() == null || macro.amountPer100g() == null) {
        continue;
      }
      known.put(macro.code(), macro.amountPer100g());
    }
    List<Double> similarities = new ArrayList<>();
    addMacroSimilarity(similarities, known.get("energy_kcal"), food.getEnergyKcal(), 50);
    addMacroSimilarity(similarities, known.get("protein"), food.getProteinG(), 5);
    addMacroSimilarity(similarities, known.get("fat"), food.getFatG(), 5);
    addMacroSimilarity(similarities, known.get("carbohydrates"), food.getCarbohydrateG(), 8);
    addMacroSimilarity(similarities, known.get("sugars"), food.getSugarsG(), 5);
    addMacroSimilarity(similarities, known.get("fiber"), food.getFiberG(), 3);
    addMacroSimilarity(similarities, known.get("sodium"), food.getSodiumMg(), 80);
    if (similarities.isEmpty()) {
      return 0.0;
    }
    return similarities.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
  }

  private void addMacroSimilarity(
      List<Double> out, BigDecimal known, BigDecimal candidate, double tolerance) {
    if (known == null || candidate == null) {
      return;
    }
    double diff = known.subtract(candidate).abs().doubleValue();
    double similarity = Math.max(0.0, 1.0 - (diff / tolerance));
    out.add(similarity);
  }

  private MatchConfidence classify(double score) {
    if (score >= properties.match().highThreshold()) {
      return MatchConfidence.HIGH;
    }
    if (score >= properties.match().mediumThreshold()) {
      return MatchConfidence.MEDIUM;
    }
    return MatchConfidence.LOW;
  }

  private static double round(double value) {
    return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static String safeLower(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }

  private record ScoredCandidate(NevoFood food, double score, List<String> reasons) {}
}
