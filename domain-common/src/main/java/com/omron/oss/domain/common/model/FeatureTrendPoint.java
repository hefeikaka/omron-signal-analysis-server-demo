package com.omron.oss.domain.common.model;

public final class FeatureTrendPoint {

    private final long collectedAtEpochMillis;
    private final double value;

    public FeatureTrendPoint(long collectedAtEpochMillis, double value) {
        this.collectedAtEpochMillis = collectedAtEpochMillis;
        this.value = value;
    }

    public long getCollectedAtEpochMillis() {
        return collectedAtEpochMillis;
    }

    public double getValue() {
        return value;
    }
}
