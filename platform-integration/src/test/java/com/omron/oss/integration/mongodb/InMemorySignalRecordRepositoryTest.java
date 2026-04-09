package com.omron.oss.integration.mongodb;

import com.omron.oss.domain.common.model.SignalSample;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemorySignalRecordRepositoryTest {

    @Test
    void shouldReturnRecentSignalsInReverseChronologicalOrder() {
        InMemorySignalRecordRepository repository = new InMemorySignalRecordRepository();
        repository.saveRawSignal(new SignalSample("machine-01", 1L, 1000, Arrays.asList(1.0d)));
        repository.saveRawSignal(new SignalSample("machine-02", 2L, 1000, Arrays.asList(2.0d)));
        repository.saveRawSignal(new SignalSample("machine-03", 3L, 1000, Arrays.asList(3.0d)));

        List<SignalSample> recent = repository.findRecentRawSignals(2);

        assertEquals(2, recent.size());
        assertEquals("machine-03", recent.get(0).getMachineId());
        assertEquals("machine-02", recent.get(1).getMachineId());
    }
}
