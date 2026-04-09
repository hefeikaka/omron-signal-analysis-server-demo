package com.omron.oss.integration.acquisition;

import com.omron.oss.domain.common.model.SignalSample;

public interface SignalSampleListener {

    void onSample(SignalSample sample);
}
