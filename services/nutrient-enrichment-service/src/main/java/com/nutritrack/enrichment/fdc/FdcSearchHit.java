package com.nutritrack.enrichment.fdc;

public record FdcSearchHit(
    long fdcId, String description, String brandOwner, String gtinUpc, String dataType) {}
