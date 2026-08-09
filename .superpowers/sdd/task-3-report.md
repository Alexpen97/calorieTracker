# Task 3 Report: Expose `densityGPerMl` on `ProductResponse`

## Summary

- Added nullable `BigDecimal densityGPerMl` to `ProductResponse` immediately after `quantityLabel`.
- Wired `ProductMapper.toResponse(Product)` to compute density with `ProductDensityResolver.resolve(quantityLabel, name, genericName)`.
- Kept `ProductMapper.toResponse(ProductSubmission)` density as `null` for pending submissions.
- Updated `RedisProductCacheTest` constructor usage for the new record field.
- Extended `ProductControllerTest` coverage:
  - Nutella-like OFF fixture with `"400 g"` asserts `densityGPerMl` is JSON `null`.
  - Coca-Cola-like OFF fixture with `"330 ml"` asserts `densityGPerMl` is `1.0`.

## TDD Evidence

### RED

Command:

```bash
cd services/food-catalog-service
mvn -q -Dtest=ProductControllerTest#barcodeLookupFetchesFromOffPersistsAndServesNutrients,ProductControllerTest#barcodeLookupReturnsDensityForVolumeProducts test
```

Result: failed as expected before implementation.

Evidence:

```text
Tests run: 2, Failures: 2, Errors: 0, Skipped: 0
ProductControllerTest.barcodeLookupFetchesFromOffPersistsAndServesNutrients:94 No value at JSON path "$.densityGPerMl"
ProductControllerTest.barcodeLookupReturnsDensityForVolumeProducts:135 No value at JSON path "$.densityGPerMl"
```

### GREEN - Targeted

Command:

```bash
cd services/food-catalog-service
mvn -q -Dtest=ProductControllerTest#barcodeLookupFetchesFromOffPersistsAndServesNutrients,ProductControllerTest#barcodeLookupReturnsDensityForVolumeProducts test
```

Result: passed with exit code 0 after adding the field and mapper wiring.

### Full Food Catalog Suite

Command:

```bash
cd services/food-catalog-service
mvn -q test
```

Result: passed with exit code 0.

Observed existing test-run warnings about Mockito dynamic agent loading, Spring Data Redis repository assignment, SpringDoc default endpoints, and Flyway's H2 version verification; no test failures.

## Acceptance Criteria Notes

- AC2: volume-labeled `"330 ml"` product returns non-null density (`1.00` serialized/asserted as `1.0`).
- AC9: gram-only `"400 g"` product returns `densityGPerMl: null`.
- Pending submissions continue to return `densityGPerMl: null`.

## Concerns

- None blocking.
