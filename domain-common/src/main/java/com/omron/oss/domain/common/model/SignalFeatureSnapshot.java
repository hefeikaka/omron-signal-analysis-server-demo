package com.omron.oss.domain.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class SignalFeatureSnapshot {

    private final String machineId;
    private final long collectedAtEpochMillis;
    private final int samplingFrequency;
    private final int sampleCount;
    private final List<SignalFeatureValue> featureValues;

    public SignalFeatureSnapshot(
        String machineId,
        long collectedAtEpochMillis,
        int samplingFrequency,
        int sampleCount,
        List<SignalFeatureValue> featureValues
    ) {
        this.machineId = Objects.requireNonNull(machineId, "machineId");
        this.collectedAtEpochMillis = collectedAtEpochMillis;
        this.samplingFrequency = samplingFrequency;
        this.sampleCount = sampleCount;
        this.featureValues = Collections.unmodifiableList(new ArrayList<SignalFeatureValue>(featureValues));
    }

    public String getMachineId() {
        return machineId;
    }

    public long getCollectedAtEpochMillis() {
        return collectedAtEpochMillis;
    }

    public int getSamplingFrequency() {
        return samplingFrequency;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public List<SignalFeatureValue> getFeatureValues() {
        return featureValues;
    }
}
