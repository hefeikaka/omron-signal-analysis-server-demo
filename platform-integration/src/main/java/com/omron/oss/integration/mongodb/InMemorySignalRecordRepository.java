package com.omron.oss.integration.mongodb;

import com.omron.oss.domain.common.model.SignalSample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InMemorySignalRecordRepository implements SignalRecordRepository {

    private final List<SignalSample> savedSamples = new ArrayList<SignalSample>();

    @Override
    public synchronized void saveRawSignal(SignalSample sample) {
        savedSamples.add(sample);
    }

    @Override
    public synchronized List<SignalSample> findRecentRawSignals(int limit) {
        if (limit <= 0 || savedSamples.isEmpty()) {
            return Collections.emptyList();
        }

        int startIndex = Math.max(0, savedSamples.size() - limit);
        List<SignalSample> recent = new ArrayList<SignalSample>();
        for (int index = savedSamples.size() - 1; index >= startIndex; index--) {
            recent.add(savedSamples.get(index));
        }
        return Collections.unmodifiableList(recent);
    }

    public synchronized List<SignalSample> getSavedSamples() {
        return Collections.unmodifiableList(new ArrayList<SignalSample>(savedSamples));
    }
}
