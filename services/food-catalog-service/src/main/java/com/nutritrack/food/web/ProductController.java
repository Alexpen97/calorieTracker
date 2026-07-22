package com.nutritrack.food.web;

import com.nutritrack.food.service.ProductLookupService;
import com.nutritrack.food.service.ProductSearchService;
import com.nutritrack.food.web.dto.ProductResponse;
import com.nutritrack.food.web.dto.ProductSearchResponse;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductLookupService productLookupService;
  private final ProductSearchService productSearchService;

  public ProductController(
      ProductLookupService productLookupService, ProductSearchService productSearchService) {
    this.productLookupService = productLookupService;
    this.productSearchService = productSearchService;
  }

  @GetMapping("/barcode/{ean}")
  public ProductResponse byBarcode(
      @AuthenticationPrincipal Jwt jwt, @PathVariable("ean") String ean) {
    return productLookupService.lookupByBarcode(ean, userId(jwt));
  }

  @GetMapping("/search")
  public ProductSearchResponse search(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam("q") String q,
      @RequestParam(value = "page", defaultValue = "1") int page) {
    return productSearchService.search(q, page, userId(jwt));
  }

  @GetMapping("/{id}")
  public ProductResponse byId(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
    return productLookupService.getById(id, userId(jwt));
  }

  private static UUID userId(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }
}
