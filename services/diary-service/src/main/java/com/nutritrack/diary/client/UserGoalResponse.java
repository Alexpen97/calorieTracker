package com.nutritrack.diary.client;

import java.math.BigDecimal;

/** Subset of user-profile GoalResponse used by diary summaries. */
public record UserGoalResponse(String nutrientCode, BigDecimal dailyTarget, String unit) {}
