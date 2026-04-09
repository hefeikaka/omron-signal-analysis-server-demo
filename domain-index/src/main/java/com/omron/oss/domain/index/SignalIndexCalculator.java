package com.omron.oss.domain.index;

import com.omron.oss.domain.common.model.SignalSample;

public final class SignalIndexCalculator {

    public double rms(SignalSample sample) {
        double sumSquares = 0.0d;
        for (Double value : sample.getValues()) {
            sumSquares += value * value;
        }
        return Math.sqrt(sumSquares / sample.getValues().size());
    }
}
