package com.nutritrack.nevo.imprt;

import com.nutritrack.nevo.config.NevoProperties;
import com.nutritrack.nevo.domain.NevoFood;
import com.nutritrack.nevo.domain.NevoFoodRepository;
import com.nutritrack.nevo.domain.NevoImportRun;
import com.nutritrack.nevo.domain.NevoImportRunRepository;
import com.nutritrack.nevo.domain.NevoNutrientValue;
import com.nutritrack.nevo.domain.NevoNutrientValueRepository;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NevoCsvImporter {

  private static final Set<String> REQUIRED_HEADERS =
      Set.of("Nevocode", "Food name", "Food group");

  private final NevoProperties properties;
  private final NevoFoodRepository foodRepository;
  private final NevoNutrientValueRepository nutrientRepository;
  private final NevoImportRunRepository importRunRepository;

  public NevoCsvImporter(
      NevoProperties properties,
      NevoFoodRepository foodRepository,
      NevoNutrientValueRepository nutrientRepository,
      NevoImportRunRepository importRunRepository) {
    this.properties = properties;
    this.foodRepository = foodRepository;
    this.nutrientRepository = nutrientRepository;
    this.importRunRepository = importRunRepository;
  }

  @Transactional
  public NevoImportRun importFromConfiguredPath() {
    if (properties.csvPath() == null || properties.csvPath().isBlank()) {
      throw new IllegalArgumentException("NEVO_CSV_PATH is not configured");
    }
    return importFromPath(Path.of(properties.csvPath()));
  }

  @Transactional
  public NevoImportRun importFromPath(Path path) {
    NevoImportRun run = new NevoImportRun();
    run.setId(UUID.randomUUID());
    run.setCsvFilename(path.getFileName().toString());
    run.setNevoVersion(properties.version());
    run.setStartedAt(Instant.now());
    run.setStatus("RUNNING");
    run.setFoodCount(0);
    run.setNutrientCount(0);
    importRunRepository.save(run);

    try {
      if (!Files.exists(path)) {
        throw new IllegalArgumentException("NEVO CSV not found: " + path.toAbsolutePath());
      }
      ImportResult result;
      try (Reader reader =
          new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
        result = parse(reader, properties.version());
      }
      replaceAll(result);
      run.setFoodCount(result.foods().size());
      run.setNutrientCount(result.nutrients().size());
      run.setStatus("SUCCEEDED");
      run.setFinishedAt(Instant.now());
      return importRunRepository.save(run);
    } catch (RuntimeException | IOException ex) {
      run.setStatus("FAILED");
      run.setErrorMessage(ex.getMessage());
      run.setFinishedAt(Instant.now());
      importRunRepository.save(run);
      if (ex instanceof RuntimeException runtime) {
        throw runtime;
      }
      throw new IllegalStateException("Failed to import NEVO CSV", ex);
    }
  }

  ImportResult parse(Reader reader, String version) throws IOException {
    CSVFormat format =
        CSVFormat.DEFAULT
            .builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build();

    try (CSVParser parser = format.parse(reader)) {
      Map<String, Integer> headerMap = parser.getHeaderMap();
      List<String> missing = new ArrayList<>();
      for (String required : REQUIRED_HEADERS) {
        if (!headerMap.containsKey(required)) {
          missing.add(required);
        }
      }
      if (!missing.isEmpty()) {
        throw new IllegalArgumentException("NEVO CSV missing required columns: " + missing);
      }

      Map<String, NevoNutrientColumnMapper.Mapping> nutrientColumns = new HashMap<>();
      for (String header : headerMap.keySet()) {
        NevoNutrientColumnMapper.map(header)
            .ifPresent(mapping -> nutrientColumns.put(header, mapping));
      }
      if (nutrientColumns.isEmpty()) {
        throw new IllegalArgumentException("NEVO CSV has no recognizable nutrient columns");
      }

      List<NevoFood> foods = new ArrayList<>();
      List<NevoNutrientValue> nutrients = new ArrayList<>();
      Set<String> seenCodes = new HashSet<>();

      for (CSVRecord record : parser) {
        String code = value(record, "Nevocode");
        if (code == null || code.isBlank()) {
          continue;
        }
        if (!seenCodes.add(code)) {
          continue;
        }
        String foodName = value(record, "Food name");
        if (foodName == null || foodName.isBlank()) {
          continue;
        }

        NevoFood food = new NevoFood();
        food.setNevoCode(code.trim());
        food.setFoodNameEn(foodName.trim());
        food.setFoodNameNl(blankToNull(value(record, "Voedingsmiddelnaam")));
        food.setFoodGroup(blankToNull(value(record, "Food group")));
        food.setSynonym(blankToNull(value(record, "Synonym")));
        food.setQuantityLabel(blankToNull(value(record, "Quantity")));
        food.setRemark(blankToNull(value(record, "Remark")));
        String versionFromRow = blankToNull(value(record, "NEVO-version"));
        food.setNevoVersion(versionFromRow == null ? version : versionFromRow);
        food.setSearchDocument(buildSearchDocument(food));
        food.setEnergyKcal(amount(record, "kcal (kcal)"));
        food.setProteinG(firstAmount(record, "Protein (g)"));
        food.setFatG(firstAmount(record, "Fat (g)"));
        food.setCarbohydrateG(firstAmount(record, "Carbohydrate (g)"));
        food.setSugarsG(amount(record, "Sugars (g)"));
        food.setFiberG(firstAmount(record, "Fibre (g)"));
        food.setSodiumMg(amount(record, "Sodium (mg)"));
        foods.add(food);

        Set<String> mappedCodes = new LinkedHashSet<>();
        for (Map.Entry<String, NevoNutrientColumnMapper.Mapping> entry : nutrientColumns.entrySet()) {
          String raw = value(record, entry.getKey());
          var amount = NevoNutrientColumnMapper.parseAmount(raw);
          if (amount.isEmpty()) {
            continue;
          }
          NevoNutrientColumnMapper.Mapping mapping = entry.getValue();
          // Prefer first mapped column for a nutrient code (e.g. RAE before RE).
          if (!mappedCodes.add(mapping.nutrientCode())) {
            continue;
          }
          NevoNutrientValue nutrient = new NevoNutrientValue();
          nutrient.setId(UUID.randomUUID());
          nutrient.setNevoCode(food.getNevoCode());
          nutrient.setNutrientCode(mapping.nutrientCode());
          nutrient.setNevoColumn(entry.getKey());
          nutrient.setAmountPer100g(amount.get());
          nutrient.setUnit(mapping.unit());
          nutrient.setRawValue(raw);
          nutrients.add(nutrient);
        }
      }

      if (foods.isEmpty()) {
        throw new IllegalArgumentException("NEVO CSV contained no food rows");
      }
      return new ImportResult(foods, nutrients);
    }
  }

  private void replaceAll(ImportResult result) {
    nutrientRepository.deleteAllInBatch();
    foodRepository.deleteAllInBatch();
    foodRepository.flush();
    foodRepository.saveAll(result.foods());
    nutrientRepository.saveAll(result.nutrients());
  }

  private static String buildSearchDocument(NevoFood food) {
    StringBuilder sb = new StringBuilder();
    append(sb, food.getFoodNameEn());
    append(sb, food.getFoodNameNl());
    append(sb, food.getSynonym());
    append(sb, food.getFoodGroup());
    append(sb, food.getRemark());
    return sb.toString().toLowerCase(Locale.ROOT).trim();
  }

  private static void append(StringBuilder sb, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    if (!sb.isEmpty()) {
      sb.append(' ');
    }
    sb.append(value.trim());
  }

  private static String value(CSVRecord record, String header) {
    if (!record.isMapped(header)) {
      return null;
    }
    return record.get(header);
  }

  private static BigDecimal amount(CSVRecord record, String header) {
    return NevoNutrientColumnMapper.parseAmount(value(record, header)).orElse(null);
  }

  private static BigDecimal firstAmount(CSVRecord record, String header) {
    // NEVO exports duplicate some macro columns; first non-empty wins via parse.
    return amount(record, header);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  record ImportResult(List<NevoFood> foods, List<NevoNutrientValue> nutrients) {}
}
