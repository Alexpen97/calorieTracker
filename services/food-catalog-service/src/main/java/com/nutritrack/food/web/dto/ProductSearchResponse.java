package com.nutritrack.food.web.dto;

import java.util.List;

public record ProductSearchResponse(String query, int page, int pageSize, List<ProductResponse> items) {}
