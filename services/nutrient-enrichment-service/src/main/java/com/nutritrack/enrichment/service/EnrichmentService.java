package com.nutritrack.enrichment.service;

import com.nutritrack.enrichment.config.EnrichmentProperties;
import com.nutritrack.enrichment.domain.EnrichmentLookup;
import com.nutritrack.enrichment.domain.EnrichmentLookupRepository;
import com.nutritrack.enrichment.domain.MatchType;
import com.nutritrack.enrichment.fdc.FdcClient;
import com.nutritrack.enrichment.fdc.FdcFoodDetail;
import com.nutritrack.enrichment.fdc.FdcSearchHit;
import com.nutritrack.enrichment.fdc.MappedNutrient;
import com.nutritrack.enrichment.web.dto.EnrichRequest;
import com.nutritrack.enrichment.web.dto.EnrichResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
public class EnrichmentService {

  private static final List<String> BRANDED = List.of("Branded");
  private static final List<String> FOUNDATION = List.of("Foundation");
  private static final List<String> SR_LEGACY = List.of("SR Legacy");
  private static final double NAME_BRAND_THRESHOLD = 0.5;
  private static final double GENERIC_THRESHOLD = 0.35;

  private final EnrichmentLookupRepository lookupRepository;
  private final FdcClient fdcClient;
  private final EnrichmentProperties properties;
  private final JsonMapper jsonMapper;

  public EnrichmentService(
      EnrichmentLookupRepository lookupRepository,
      FdcClient fdcClient,
      EnrichmentProperties properties,
      JsonMapper jsonMapper) {
    this.lookupRepository = lookupRepository;
    this.fdcClient = fdcClient;
    this.properties = properties;
    this.jsonMapper = jsonMapper;
  }

  @Transactional
  public EnrichResponse enrich(EnrichRequest request) {
    String barcode = request.barcode().trim();
    Optional<EnrichmentLookup> cached = lookupRepository.findById(barcode);
    if (cached.isPresent() && !isExpired(cached.get())) {
      return toResponse(cached.get(), existingCodes(request));
    }

    MatchResult match = resolveMatch(barcode, request.name(), request.brand());
    EnrichmentLookup row = new EnrichmentLookup();
    row.setBarcode(barcode);
    row.setMatchType(match.matchType());
    row.setFdcId(match.fdcId());
    row.setMatchedDescription(match.description());
    row.setConfidence(match.confidence());
    row.setNutrientsJson(writeNutrients(match.nutrients()));
    row.setCreatedAt(Instant.now());
    lookupRepository.save(row);
    return toResponse(row, existingCodes(request));
  }

  private MatchResult resolveMatch(String barcode, String name, String brand) {
    Optional<MatchResult> gtin = matchGtin(barcode);
    if (gtin.isPresent()) {
      return gtin.get();
    }
    Optional<MatchResult> nameBrand = matchNameBrand(name, brand);
    if (nameBrand.isPresent()) {
      return nameBrand.get();
    }
    if (!NameNormalizer.looksFortified(name)) {
      Optional<MatchResult> proxy = matchGenericProxy(name, brand);
      if (proxy.isPresent()) {
        return proxy.get();
      }
    }
    return MatchResult.none();
  }

  private Optional<MatchResult> matchGtin(String barcode) {
    List<FdcSearchHit> hits = fdcClient.searchFoods(barcode, BRANDED, null, 10);
    String target = NameNormalizer.stripLeadingZeros(barcode);
    for (FdcSearchHit hit : hits) {
      if (hit.gtinUpc() == null) {
        continue;
      }
      if (NameNormalizer.stripLeadingZeros(hit.gtinUpc()).equals(target)) {
        return loadDetail(hit, MatchType.GTIN, BigDecimal.ONE);
      }
    }
    return Optional.empty();
  }

  private Optional<MatchResult> matchNameBrand(String name, String brand) {
    if (name == null || name.isBlank()) {
      return Optional.empty();
    }
    List<FdcSearchHit> hits = fdcClient.searchFoods(name, BRANDED, brand, 10);
    if (hits.isEmpty() && brand != null && !brand.isBlank()) {
      hits = fdcClient.searchFoods(name, BRANDED, null, 10);
    }
    return bestHit(hits, name, MatchType.NAME_BRAND, NAME_BRAND_THRESHOLD);
  }

  private Optional<MatchResult> matchGenericProxy(String name, String brand) {
    String query = NameNormalizer.genericQuery(name, brand);
    if (query.isBlank()) {
      query = NameNormalizer.normalize(name);
    }
    if (query.isBlank()) {
      return Optional.empty();
    }
    List<FdcSearchHit> foundation = fdcClient.searchFoods(query, FOUNDATION, null, 10);
    Optional<MatchResult> fromFoundation =
        bestHit(foundation, query, MatchType.GENERIC_PROXY, GENERIC_THRESHOLD);
    if (fromFoundation.isPresent()) {
      return fromFoundation;
    }
    List<FdcSearchHit> sr = fdcClient.searchFoods(query, SR_LEGACY, null, 10);
    return bestHit(sr, query, MatchType.GENERIC_PROXY, GENERIC_THRESHOLD);
  }

  private Optional<MatchResult> bestHit(
      List<FdcSearchHit> hits, String query, MatchType type, double threshold) {
    FdcSearchHit best = null;
    double bestScore = -1;
    for (FdcSearchHit hit : hits) {
      double score = NameNormalizer.tokenJaccard(query, hit.description());
      if (score > bestScore) {
        bestScore = score;
        best = hit;
      }
    }
    if (best == null || bestScore < threshold) {
      return Optional.empty();
    }
    return loadDetail(
        best, type, BigDecimal.valueOf(bestScore).setScale(3, RoundingMode.HALF_UP));
  }

  private Optional<MatchResult> loadDetail(FdcSearchHit hit, MatchType type, BigDecimal confidence) {
    Optional<FdcFoodDetail> detail = fdcClient.getFood(hit.fdcId());
    if (detail.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        new MatchResult(
            type,
            hit.fdcId(),
            detail.get().description() != null ? detail.get().description() : hit.description(),
            confidence,
            detail.get().nutrients()));
  }

  private boolean isExpired(EnrichmentLookup lookup) {
    Instant cutoff =
        Instant.now().minus(properties.cacheTtlDays(), ChronoUnit.DAYS);
    return lookup.getCreatedAt() == null || lookup.getCreatedAt().isBefore(cutoff);
  }

  private EnrichResponse toResponse(EnrichmentLookup lookup, Set<String> existing) {
    List<EnrichResponse.NutrientDto> nutrients =
        readNutrients(lookup.getNutrientsJson()).stream()
            .filter(n -> !existing.contains(n.code()))
            .map(n -> new EnrichResponse.NutrientDto(n.code(), n.amountPer100g(), n.unit()))
            .toList();
    return new EnrichResponse(
        lookup.getMatchType().name(),
        lookup.getFdcId(),
        lookup.getMatchedDescription(),
        lookup.getConfidence(),
        nutrients);
  }

  private static Set<String> existingCodes(EnrichRequest request) {
    if (request.existingNutrientCodes() == null) {
      return Set.of();
    }
    return new HashSet<>(request.existingNutrientCodes());
  }

  private String writeNutrients(List<MappedNutrient> nutrients) {
    try {
      return jsonMapper.writeValueAsString(nutrients == null ? List.of() : nutrients);
    } catch (JacksonException ex) {
      throw new IllegalStateException("Failed to serialize nutrients", ex);
    }
  }

  private List<MappedNutrient> readNutrients(String json) {
    try {
      List<MappedNutrient> list = jsonMapper.readValue(json, new TypeReference<>() {});
      return list == null ? List.of() : list;
    } catch (JacksonException ex) {
      throw new IllegalStateException("Corrupt nutrients_json", ex);
    }
  }

  private record MatchResult(
      MatchType matchType,
      Long fdcId,
      String description,
      BigDecimal confidence,
      List<MappedNutrient> nutrients) {

    static MatchResult none() {
      return new MatchResult(MatchType.NONE, null, null, null, new ArrayList<>());
    }
  }
}
