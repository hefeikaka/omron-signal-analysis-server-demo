package com.omron.oss.domain.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class SignalSample {

    private final String machineId;
    private final long collectedAtEpochMillis;
    private final int samplingFrequency;
    private final List<Double> values;

    public SignalSample(String machineId, long collectedAtEpochMillis, int samplingFrequency, List<Double> values) {
        this.machineId = Objects.requireNonNull(machineId, "machineId");
        this.collectedAtEpochMillis = collectedAtEpochMillis;
        this.samplingFrequency = samplingFrequency;
        this.values = Collections.unmodifiableList(new ArrayList<Double>(values));
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

    public List<Double> getValues() {
        return values;
    }
}
