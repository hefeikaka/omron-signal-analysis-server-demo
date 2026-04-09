package com.omron.oss.domain.api;

import java.util.List;

public final class SignalIngestRequest {

    private String machineId;
    private int samplingFrequency;
    private List<Double> values;

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public int getSamplingFrequency() {
        return samplingFrequency;
    }

    public void setSamplingFrequency(int samplingFrequency) {
        this.samplingFrequency = samplingFrequency;
    }

    public List<Double> getValues() {
        return values;
    }

    public void setValues(List<Double> values) {
        this.values = values;
    }
}
