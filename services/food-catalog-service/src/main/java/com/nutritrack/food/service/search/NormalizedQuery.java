package com.nutritrack.food.service.search;

import java.util.List;
import java.util.Set;

public record NormalizedQuery(
    String raw, String normalized, List<String> tokens, Set<String> expandedTokens) {}
