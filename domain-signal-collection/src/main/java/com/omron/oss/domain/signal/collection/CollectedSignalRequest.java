package com.omron.oss.domain.signal.collection;

import java.util.List;

public final class CollectedSignalRequest {

    private final String machineId;
    private final int samplingFrequency;
    private final List<Double> values;

    public CollectedSignalRequest(String machineId, int samplingFrequency, List<Double> values) {
        this.machineId = machineId;
        this.samplingFrequency = samplingFrequency;
        this.values = values;
    }

    public String getMachineId() {
        return machineId;
    }

    public int getSamplingFrequency() {
        return samplingFrequency;
    }

    public List<Double> getValues() {
        return values;
    }
}
