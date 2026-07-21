package com.nutritrack.diary.web;

import com.nutritrack.diary.client.FoodCatalogUnavailableException;
import com.nutritrack.diary.client.ProductNotFoundException;
import com.nutritrack.diary.service.DiaryEntryNotFoundException;
import com.nutritrack.diary.service.WaterIntakeNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ProductNotFoundException.class)
  ResponseEntity<Map<String, String>> productNotFound(ProductNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(DiaryEntryNotFoundException.class)
  ResponseEntity<Map<String, String>> diaryEntryNotFound(DiaryEntryNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(WaterIntakeNotFoundException.class)
  ResponseEntity<Map<String, String>> waterIntakeNotFound(WaterIntakeNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(FoodCatalogUnavailableException.class)
  ResponseEntity<Map<String, String>> foodCatalogUnavailable(FoodCatalogUnavailableException ex) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", ex.getMessage()));
  }
}
