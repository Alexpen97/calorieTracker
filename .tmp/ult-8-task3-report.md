# ULT-8 Task 3 Report: Merge NEVO hits into product search

## Scope

- Implemented Task 3 only in `services/food-catalog-service`.
- Added catalog-side NEVO search DTO mirror and `NevoClient.searchFoods(String q, int limit)`.
- Prepended mapped NEVO search hits to `GET /api/products/search` before submissions, local mirror results, and OFF fallback.
- Added nullable `nevoCode` and `foodGroup` fields to `ProductResponse`.
- Left OFF upsert/enrichment paths unchanged.

## RED evidence

Command:

```bash
cd /workspace/services/food-catalog-service
mvn -q test -Dtest=ProductControllerTest
```

Expected failure after adding the MockMvc test first:

```text
[ERROR] COMPILATION ERROR :
[ERROR] /workspace/services/food-catalog-service/src/test/java/com/nutritrack/food/ProductControllerTest.java:[20,32] cannot find symbol
  symbol:   class NevoFoodSearchResponse
  location: package com.nutritrack.food.nevo
[ERROR] /workspace/services/food-catalog-service/src/test/java/com/nutritrack/food/ProductControllerTest.java:[69,20] cannot find symbol
  symbol:   method searchFoods(java.lang.String,int)
  location: variable nevoClient of type com.nutritrack.food.nevo.NevoClient
```

This showed the test was exercising the missing NEVO search client contract before implementation.

## GREEN evidence

Focused test:

```bash
cd /workspace/services/food-catalog-service
mvn -q test -Dtest=ProductControllerTest
```

Result: exit code 0.

Full catalog service test suite:

```bash
cd /workspace/services/food-catalog-service
mvn -q test
```

Result: exit code 0.

## Behavior covered

- `searchPaprikaPrependsNevoVegetables` stubs NEVO `searchFoods("paprika", anyInt())` and verifies the first product search item has:
  - `source = "NEVO"`
  - `nevoCode = "31"`
  - `foodGroup = "Vegetables"`
  - `name = "Sweet pepper green raw"`
  - mapped nutrient data
- Existing local/OFF regression remains covered by `searchReturnsLocalProducts`, with default NEVO search stubbed to `List.of()`.
