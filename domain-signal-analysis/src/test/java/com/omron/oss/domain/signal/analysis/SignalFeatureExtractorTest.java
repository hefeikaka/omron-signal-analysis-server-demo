package com.omron.oss.domain.signal.analysis;

import com.omron.oss.domain.common.model.SignalFeatureSnapshot;
import com.omron.oss.domain.common.model.SignalFeatureValue;
import com.omron.oss.domain.common.model.SignalSample;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalFeatureExtractorTest {

    @Test
    void shouldExtractTimeAndFrequencyFeatures() {
        List<Double> values = new ArrayList<Double>();
        for (int index = 0; index < 1024; index++) {
            values.add(Double.valueOf(Math.sin(2.0d * Math.PI * 50.0d * index / 1024.0d)));
        }

        SignalFeatureSnapshot snapshot = new SignalFeatureExtractor().extract(
            new SignalSample("M001-1", 1_700_000_000_000L, 1024, values)
        );

        assertEquals("M001-1", snapshot.getMachineId());
        assertEquals(18, snapshot.getFeatureValues().size());
        assertTrue(find(snapshot, "time_rms") > 0.7d);
        assertTrue(find(snapshot, "time_peak") > 0.9d);
        assertTrue(find(snapshot, "freq_dominant_frequency") > 45.0d);
        assertTrue(find(snapshot, "freq_dominant_frequency") < 55.0d);
    }

    private double find(SignalFeatureSnapshot snapshot, String key) {
        for (SignalFeatureValue featureValue : snapshot.getFeatureValues()) {
            if (featureValue.getDescriptor().getKey().equals(key)) {
                return featureValue.getValue();
            }
        }
        throw new IllegalArgumentException("Feature not found: " + key);
    }
}
