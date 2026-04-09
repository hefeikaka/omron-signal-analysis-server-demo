package com.omron.oss.integration.mongodb;

import com.omron.oss.domain.common.model.SignalSample;

import java.util.List;

public interface SignalRecordRepository {

    void saveRawSignal(SignalSample sample);

    List<SignalSample> findRecentRawSignals(int limit);
}
