package com.nutritrack.nevo.web;

import com.nutritrack.nevo.config.NevoProperties;
import com.nutritrack.nevo.domain.NevoImportRun;
import com.nutritrack.nevo.imprt.NevoCsvImporter;
import com.nutritrack.nevo.match.NevoMatchService;
import com.nutritrack.nevo.web.dto.NevoImportRequest;
import com.nutritrack.nevo.web.dto.NevoImportResponse;
import com.nutritrack.nevo.web.dto.NevoMatchRequest;
import com.nutritrack.nevo.web.dto.NevoMatchResponse;
import java.nio.file.Path;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/nevo")
public class NevoInternalController {

  private final NevoProperties properties;
  private final NevoCsvImporter importer;
  private final NevoMatchService matchService;

  public NevoInternalController(
      NevoProperties properties, NevoCsvImporter importer, NevoMatchService matchService) {
    this.properties = properties;
    this.importer = importer;
    this.matchService = matchService;
  }

  @PostMapping("/import")
  public NevoImportResponse importCsv(
      @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
      @RequestBody(required = false) NevoImportRequest request) {
    requireInternalKey(apiKey);
    NevoImportRun run;
    if (request != null && request.csvPath() != null && !request.csvPath().isBlank()) {
      run = importer.importFromPath(Path.of(request.csvPath()));
    } else {
      run = importer.importFromConfiguredPath();
    }
    return toResponse(run);
  }

  @PostMapping("/matches/best")
  public NevoMatchResponse matchBest(
      @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
      @RequestBody NevoMatchRequest request) {
    requireInternalKey(apiKey);
    return matchService.matchBest(request);
  }

  private void requireInternalKey(String apiKey) {
    if (apiKey == null || !apiKey.equals(properties.internalApiKey())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal API key");
    }
  }

  private static NevoImportResponse toResponse(NevoImportRun run) {
    return new NevoImportResponse(
        run.getId(),
        run.getCsvFilename(),
        run.getNevoVersion(),
        run.getFoodCount(),
        run.getNutrientCount(),
        run.getStatus(),
        run.getStartedAt(),
        run.getFinishedAt(),
        run.getErrorMessage());
  }
}
