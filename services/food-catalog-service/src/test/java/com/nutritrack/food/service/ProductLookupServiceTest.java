package com.nutritrack.food.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProductLookupServiceTest {

  @Test
  void sanitizeBarcodeAcceptsDigitsAndRejectsJunk() {
    assertThat(ProductLookupService.sanitizeBarcode(" 3017620422003 ")).isEqualTo("3017620422003");
    assertThatThrownBy(() -> ProductLookupService.sanitizeBarcode("abc"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ProductLookupService.sanitizeBarcode("123"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
