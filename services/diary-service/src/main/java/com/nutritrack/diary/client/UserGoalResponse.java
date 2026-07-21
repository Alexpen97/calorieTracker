package com.nutritrack.diary.client;

import java.math.BigDecimal;
import java.time.Instant;

public record UserGoalResponse(
    String nutrientCode, BigDecimal dailyTarget, String unit, String origin, Instant computedAt) {}
