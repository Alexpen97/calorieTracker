package com.nutritrack.food.service;

import com.nutritrack.food.domain.Product;
import com.nutritrack.food.domain.ProductNutrient;
import com.nutritrack.food.domain.ProductRepository;
import com.nutritrack.food.domain.ProductSource;
import com.nutritrack.food.domain.ProductSubmission;
import com.nutritrack.food.domain.ProductSubmissionRepository;
import com.nutritrack.food.domain.SubmissionStatus;
import com.nutritrack.food.web.ProductNotFoundException;
import com.nutritrack.food.web.SubmissionConflictException;
import com.nutritrack.food.web.dto.CreateSubmissionRequest;
import com.nutritrack.food.web.dto.ProductNutrientResponse;
import com.nutritrack.food.web.dto.SubmissionResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionService {

  private final ProductSubmissionRepository submissionRepository;
  private final ProductRepository productRepository;
  private final ProductMapper productMapper;

  public SubmissionService(
      ProductSubmissionRepository submissionRepository,
      ProductRepository productRepository,
      ProductMapper productMapper) {
    this.submissionRepository = submissionRepository;
    this.productRepository = productRepository;
    this.productMapper = productMapper;
  }

  @Transactional
  public SubmissionResponse submit(UUID userId, CreateSubmissionRequest request) {
    String barcode = normalizeOptionalBarcode(request.barcode());
    List<String> warnings = duplicateWarnings(request.name(), request.brand(), barcode);
    if (!warnings.isEmpty() && !request.force()) {
      throw new SubmissionConflictException(warnings);
    }

    ProductSubmission submission = new ProductSubmission();
    submission.setId(UUID.randomUUID());
    submission.setSubmitterUserId(userId);
    submission.setStatus(SubmissionStatus.PENDING);
    submission.setBarcode(barcode);
    submission.setName(request.name().trim());
    submission.setBrand(blankToNull(request.brand()));
    submission.setServingSizeG(request.servingSizeG());
    submission.setNutrients(
        productMapper.writeNutrients(
            request.nutrients().stream()
                .map(n -> new ProductNutrientResponse(n.code(), n.amountPer100g(), n.unit()))
                .toList()));
    submission.setSubmittedAt(Instant.now());
    ProductSubmission saved = submissionRepository.save(submission);
    return toResponse(saved, warnings);
  }

  @Transactional(readOnly = true)
  public List<SubmissionResponse> listMine(UUID userId) {
    return submissionRepository.findBySubmitterUserIdOrderBySubmittedAtDesc(userId).stream()
        .map(s -> toResponse(s, List.of()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<SubmissionResponse> listByStatus(SubmissionStatus status) {
    return submissionRepository.findByStatusOrderBySubmittedAtAsc(status).stream()
        .map(s -> toResponse(s, duplicateWarnings(s.getName(), s.getBrand(), s.getBarcode())))
        .toList();
  }

  @Transactional
  public SubmissionResponse approve(UUID submissionId, UUID reviewerId) {
    ProductSubmission submission = requirePending(submissionId);
    Product product = new Product();
    product.setId(UUID.randomUUID());
    product.setBarcode(submission.getBarcode());
    product.setSource(ProductSource.USER_APPROVED);
    product.setName(submission.getName());
    product.setBrand(submission.getBrand());
    product.setServingSizeG(submission.getServingSizeG());
    product.refreshSearchDocument();

    List<ProductNutrient> nutrients =
        productMapper.parseNutrients(submission.getNutrients()).stream()
            .map(
                n -> {
                  ProductNutrient pn = new ProductNutrient();
                  pn.setProductId(product.getId());
                  pn.setNutrientCode(n.code());
                  pn.setAmountPer100g(n.amountPer100g());
                  pn.setUnit(n.unit());
                  return pn;
                })
            .toList();
    product.replaceNutrients(nutrients);
    Product saved = productRepository.save(product);

    submission.setStatus(SubmissionStatus.APPROVED);
    submission.setReviewedBy(reviewerId);
    submission.setReviewedAt(Instant.now());
    submission.setPublishedProductId(saved.getId());
    return toResponse(submission, List.of());
  }

  @Transactional
  public SubmissionResponse reject(UUID submissionId, UUID reviewerId, String note) {
    ProductSubmission submission = requirePending(submissionId);
    submission.setStatus(SubmissionStatus.REJECTED);
    submission.setReviewedBy(reviewerId);
    submission.setReviewedAt(Instant.now());
    submission.setReviewNote(blankToNull(note));
    return toResponse(submission, List.of());
  }

  private ProductSubmission requirePending(UUID submissionId) {
    ProductSubmission submission =
        submissionRepository
            .findById(submissionId)
            .orElseThrow(() -> new ProductNotFoundException(submissionId.toString()));
    if (submission.getStatus() != SubmissionStatus.PENDING) {
      throw new IllegalArgumentException("Submission is not pending");
    }
    return submission;
  }

  private List<String> duplicateWarnings(String name, String brand, String barcode) {
    List<String> warnings = new ArrayList<>();
    if (barcode != null) {
      productRepository
          .findByBarcode(barcode)
          .ifPresent(
              p ->
                  warnings.add(
                      "Barcode already exists in catalog as \"" + p.getName() + "\" (" + p.getId() + ")"));
    }
    String needle = name == null ? "" : name.trim();
    if (needle.length() >= 2) {
      productRepository
          .findNameOrBrandMatches(needle, PageRequest.of(0, 5))
          .forEach(
              p ->
                  warnings.add(
                      "Similar catalog product: \""
                          + p.getName()
                          + "\""
                          + (p.getBrand() == null ? "" : " / " + p.getBrand())));
    }
    if (brand != null && !brand.isBlank() && needle.length() >= 2) {
      String combined = (needle + " " + brand).toLowerCase(Locale.ROOT);
      // brand already covered by name/brand match query above
      if (combined.isBlank()) {
        // no-op
      }
    }
    return warnings.stream().distinct().toList();
  }

  private SubmissionResponse toResponse(ProductSubmission submission, List<String> warnings) {
    return new SubmissionResponse(
        submission.getId(),
        submission.getSubmitterUserId(),
        submission.getStatus().name(),
        submission.getBarcode(),
        submission.getName(),
        submission.getBrand(),
        submission.getServingSizeG(),
        productMapper.parseNutrients(submission.getNutrients()),
        submission.getSubmittedAt(),
        submission.getReviewedBy(),
        submission.getReviewedAt(),
        submission.getReviewNote(),
        submission.getPublishedProductId(),
        warnings);
  }

  private static String normalizeOptionalBarcode(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    return ProductLookupService.sanitizeBarcode(raw);
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
