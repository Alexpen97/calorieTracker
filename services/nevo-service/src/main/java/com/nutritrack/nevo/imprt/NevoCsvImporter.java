package com.nutritrack.nevo.imprt;

import com.nutritrack.nevo.config.NevoProperties;
import com.nutritrack.nevo.domain.NevoFood;
import com.nutritrack.nevo.domain.NevoFoodRepository;
import com.nutritrack.nevo.domain.NevoImportRun;
import com.nutritrack.nevo.domain.NevoImportRunRepository;
import com.nutritrack.nevo.domain.NevoNutrientValue;
import com.nutritrack.nevo.domain.NevoNutrientValueRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NevoCsvImporter {

  /** Default RIVM NEVO-online 2025/9.0 wide export shipped on the classpath. */
  public static final String DEFAULT_CLASSPATH_CSV = "nevo/NEVO2025_v9.0.csv";

  private static final String COL_VERSION = "NEVO-versie/NEVO-version";
  private static final String COL_GROUP_NL = "Voedingsmiddelgroep";
  private static final String COL_GROUP_EN = "Food group";
  private static final String COL_CODE = "NEVO-code";
  private static final String COL_NAME_NL = "Voedingsmiddelnaam/Dutch food name";
  private static final String COL_NAME_EN = "Engelse naam/Food name";
  private static final String COL_SYNONYM = "Synoniem";
  private static final String COL_QUANTITY = "Hoeveelheid/Quantity";
  private static final String COL_REMARK = "Opmerking";

  private static final Set<String> REQUIRED_HEADERS =
      Set.of(COL_CODE, COL_NAME_EN, COL_GROUP_EN);

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
    String configured = properties.csvPath();
    if (configured == null || configured.isBlank()) {
      return importResource(new ClassPathResource(DEFAULT_CLASSPATH_CSV), DEFAULT_CLASSPATH_CSV);
    }
    if (configured.startsWith("classpath:")) {
      String location = configured.substring("classpath:".length());
      return importResource(new ClassPathResource(location), location);
    }
    return importFromPath(Path.of(configured));
  }

  @Transactional
  public NevoImportRun importFromPath(Path path) {
    return importResource(new FileSystemResource(path), path.getFileName().toString());
  }

  @Transactional
  public NevoImportRun importResource(Resource resource, String displayName) {
    NevoImportRun run = new NevoImportRun();
    run.setId(UUID.randomUUID());
    run.setCsvFilename(displayName);
    run.setNevoVersion(properties.version());
    run.setStartedAt(Instant.now());
    run.setStatus("RUNNING");
    run.setFoodCount(0);
    run.setNutrientCount(0);
    importRunRepository.save(run);

    try {
      if (!resource.exists()) {
        throw new IllegalArgumentException("NEVO CSV not found: " + displayName);
      }
      ImportResult result;
      try (InputStream in = resource.getInputStream();
          Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
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
            .setDelimiter('|')
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .setQuote('"')
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

      // Preserve first-seen NEVO code preference (e.g. VITA_RAE before VITA_RE).
      Map<String, NevoNutrientColumnMapper.Mapping> nutrientColumns = new HashMap<>();
      List<String> nutrientHeaderOrder = new ArrayList<>();
      for (String header : headerMap.keySet()) {
        NevoNutrientColumnMapper.map(header)
            .ifPresent(
                mapping -> {
                  nutrientColumns.put(header, mapping);
                  nutrientHeaderOrder.add(header);
                });
      }
      if (nutrientColumns.isEmpty()) {
        throw new IllegalArgumentException("NEVO CSV has no recognizable nutrient columns");
      }

      List<NevoFood> foods = new ArrayList<>();
      List<NevoNutrientValue> nutrients = new ArrayList<>();
      Set<String> seenCodes = new HashSet<>();

      for (CSVRecord record : parser) {
        String code = value(record, COL_CODE);
        if (code == null || code.isBlank()) {
          continue;
        }
        if (!seenCodes.add(code)) {
          continue;
        }
        String foodName = value(record, COL_NAME_EN);
        if (foodName == null || foodName.isBlank()) {
          foodName = value(record, COL_NAME_NL);
        }
        if (foodName == null || foodName.isBlank()) {
          continue;
        }

        NevoFood food = new NevoFood();
        food.setNevoCode(code.trim());
        food.setFoodNameEn(foodName.trim());
        food.setFoodNameNl(blankToNull(value(record, COL_NAME_NL)));
        food.setFoodGroup(blankToNull(value(record, COL_GROUP_EN)));
        if (food.getFoodGroup() == null) {
          food.setFoodGroup(blankToNull(value(record, COL_GROUP_NL)));
        }
        food.setSynonym(blankToNull(value(record, COL_SYNONYM)));
        food.setQuantityLabel(blankToNull(value(record, COL_QUANTITY)));
        food.setRemark(blankToNull(value(record, COL_REMARK)));
        String versionFromRow = blankToNull(value(record, COL_VERSION));
        food.setNevoVersion(versionFromRow == null ? version : versionFromRow);
        food.setSearchDocument(buildSearchDocument(food));
        food.setEnergyKcal(amountByCode(record, nutrientHeaderOrder, nutrientColumns, "energy_kcal"));
        food.setProteinG(amountByCode(record, nutrientHeaderOrder, nutrientColumns, "protein"));
        food.setFatG(amountByCode(record, nutrientHeaderOrder, nutrientColumns, "fat"));
        food.setCarbohydrateG(
            amountByCode(record, nutrientHeaderOrder, nutrientColumns, "carbohydrates"));
        food.setSugarsG(amountByCode(record, nutrientHeaderOrder, nutrientColumns, "sugars"));
        food.setFiberG(amountByCode(record, nutrientHeaderOrder, nutrientColumns, "fiber"));
        food.setSodiumMg(amountByCode(record, nutrientHeaderOrder, nutrientColumns, "sodium"));
        foods.add(food);

        Set<String> mappedCodes = new LinkedHashSet<>();
        for (String header : nutrientHeaderOrder) {
          NevoNutrientColumnMapper.Mapping mapping = nutrientColumns.get(header);
          String raw = value(record, header);
          var amount = NevoNutrientColumnMapper.parseAmount(raw);
          if (amount.isEmpty()) {
            continue;
          }
          // Prefer first mapped column for a nutrient code (e.g. VITA_RAE before VITA_RE).
          if (!mappedCodes.add(mapping.nutrientCode())) {
            continue;
          }
          NevoNutrientValue nutrient = new NevoNutrientValue();
          nutrient.setId(UUID.randomUUID());
          nutrient.setNevoCode(food.getNevoCode());
          nutrient.setNutrientCode(mapping.nutrientCode());
          nutrient.setNevoColumn(header);
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

  private static BigDecimal amountByCode(
      CSVRecord record,
      List<String> headerOrder,
      Map<String, NevoNutrientColumnMapper.Mapping> nutrientColumns,
      String internalCode) {
    for (String header : headerOrder) {
      NevoNutrientColumnMapper.Mapping mapping = nutrientColumns.get(header);
      if (mapping != null && internalCode.equals(mapping.nutrientCode())) {
        return NevoNutrientColumnMapper.parseAmount(value(record, header)).orElse(null);
      }
    }
    return null;
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
    String raw = record.get(header);
    if (raw == null) {
      return null;
    }
    return raw.trim();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  record ImportResult(List<NevoFood> foods, List<NevoNutrientValue> nutrients) {}
}
