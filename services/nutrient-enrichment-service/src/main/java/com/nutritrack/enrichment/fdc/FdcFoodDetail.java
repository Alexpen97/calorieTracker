package com.nutritrack.enrichment.fdc;

import java.util.List;

public record FdcFoodDetail(
    long fdcId, String description, String dataType, List<MappedNutrient> nutrients) {}
