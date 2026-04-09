package com.omron.oss.domain.signal.collection;

import com.omron.oss.domain.common.model.SignalSample;

public final class SignalCollectionService {

    public SignalSample collect(CollectedSignalRequest request) {
        if (request.getMachineId() == null || request.getMachineId().trim().isEmpty()) {
            throw new IllegalArgumentException("machineId must not be blank");
        }
        if (request.getValues() == null || request.getValues().isEmpty()) {
            throw new IllegalArgumentException("values must not be empty");
        }
        return new SignalSample(
            request.getMachineId().trim(),
            System.currentTimeMillis(),
            request.getSamplingFrequency(),
            request.getValues()
        );
    }
}
