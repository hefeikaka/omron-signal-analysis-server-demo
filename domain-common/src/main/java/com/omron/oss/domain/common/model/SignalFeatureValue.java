package com.omron.oss.domain.common.model;

import java.util.Objects;

public final class SignalFeatureValue {

    private final SignalFeatureDescriptor descriptor;
    private final double value;

    public SignalFeatureValue(SignalFeatureDescriptor descriptor, double value) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.value = value;
    }

    public SignalFeatureDescriptor getDescriptor() {
        return descriptor;
    }

    public double getValue() {
        return value;
    }
}
