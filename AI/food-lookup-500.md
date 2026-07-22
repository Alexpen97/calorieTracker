# Food lookup production failure (traced 2026-07-22)

## Symptom

- UI / API barcode lookup fails with 500
- Name search returns no products

## Root cause (confirmed)

`OffClient` called `RestClient.body(com.fasterxml.jackson.databind.JsonNode.class)` on Spring Boot **4.1**, which uses Jackson **3** (`tools.jackson`) message converters.

```
HttpMessageConversionException: Type definition error: [simple type, class com.fasterxml.jackson.databind.JsonNode]
Caused by: tools.jackson.databind.exc.InvalidDefinitionException:
  Cannot construct instance of `com.fasterxml.jackson.databind.JsonNode`
```

- Barcode path: exception → opaque **500**
- Search path: same exception caught → empty `items`
- Soft-miss barcodes also **500** (decode fails before `status: 0` handling)
- Repeated failures open Resilience4j circuit breaker (`CallNotPermittedException`)

Controller tests mocked `OffClient`, so CI never exercised RestClient JSON decoding.

## Fix

Migrated OFF JSON handling to Jackson 3:

- `OffClient`, `OffNutrientNormalizer`, `OffBulkImportJobConfig`
- `asText` → `asString`, `isTextual` → `isString`, `ObjectMapper` → `JsonMapper`
- Added `OffClientIntegrationTest` (JDK HttpServer stub + real `OffClient` bean)

## Verification

`mvn -B test` in `services/food-catalog-service`: **16 tests, BUILD SUCCESS**

## Deploy

Redeploy `food-catalog-service` on Railway, then:

```http
GET {{gateway}}/api/products/barcode/3017620422003
Authorization: Bearer {{access_token}}
```

Expect 200 Nutella. Unknown barcode → 404. Search `q=nutella` → non-empty items.
