package com.omron.oss.integration.mongodb;

import com.omron.oss.domain.common.model.SignalSample;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonLineSignalRecordRepositoryTest {

    @Test
    void shouldAppendSignalAsJsonLine() throws Exception {
        Path tempFile = Files.createTempFile("signal-records", ".jsonl");
        try {
            JsonLineSignalRecordRepository repository = new JsonLineSignalRecordRepository(tempFile);
            repository.saveRawSignal(new SignalSample("machine-01", 1712640000000L, 10000, Arrays.asList(1.0d, 2.0d)));

            String content = new String(Files.readAllBytes(tempFile), StandardCharsets.UTF_8);
            assertTrue(content.contains("\"machineId\":\"machine-01\""));
            assertTrue(content.contains("\"samplingFrequency\":10000"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void shouldReadRecentSignalsInReverseChronologicalOrder() throws Exception {
        Path tempFile = Files.createTempFile("signal-records", ".jsonl");
        try {
            JsonLineSignalRecordRepository repository = new JsonLineSignalRecordRepository(tempFile);
            repository.saveRawSignal(new SignalSample("machine-01", 1712640000000L, 10000, Arrays.asList(1.0d, 2.0d)));
            repository.saveRawSignal(new SignalSample("machine-02", 1712640001000L, 12000, Arrays.asList(3.0d, 4.0d)));

            List<SignalSample> recent = repository.findRecentRawSignals(2);

            assertEquals(2, recent.size());
            assertEquals("machine-02", recent.get(0).getMachineId());
            assertEquals("machine-01", recent.get(1).getMachineId());
            assertEquals(2, recent.get(0).getValues().size());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
