package com.nutritrack.food.web;

import com.nutritrack.food.service.ProductLookupService;
import com.nutritrack.food.web.dto.ProductResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductLookupService productLookupService;

  public ProductController(ProductLookupService productLookupService) {
    this.productLookupService = productLookupService;
  }

  @GetMapping("/barcode/{ean}")
  public ProductResponse byBarcode(@PathVariable("ean") String ean) {
    return productLookupService.lookupByBarcode(ean);
  }

  @GetMapping("/{id}")
  public ProductResponse byId(@PathVariable("id") UUID id) {
    return productLookupService.getById(id);
  }
}
