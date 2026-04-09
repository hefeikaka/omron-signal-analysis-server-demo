package com.omron.oss.domain.signal.pipeline;

import com.omron.oss.domain.common.model.SignalSample;
import com.omron.oss.integration.jms.MessagePublisher;
import com.omron.oss.integration.mongodb.InMemorySignalRecordRepository;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncSignalPublishingServiceTest {

    @Test
    void shouldWrapFailuresIntoAsyncResult() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            AsyncSignalPublishingService service = new AsyncSignalPublishingService(
                new SignalPublishingService(
                    new SignalMessageFormatter(),
                    new FailingPublisher(),
                    new InMemorySignalRecordRepository()
                ),
                executor
            );

            AsyncSignalProcessingResult result = service.processAndPublishAsyncSafely(
                new SignalSample("machine-01", 1712640000000L, 10000, Arrays.asList(1.0d, 2.0d))
            ).get(3, TimeUnit.SECONDS);

            assertFalse(result.isSuccess());
            assertTrue(result.getFailureReason().contains("publisher boom"));
        } finally {
            executor.shutdownNow();
        }
    }

    private static final class FailingPublisher implements MessagePublisher {
        @Override
        public void publish(String topicName, String payload) {
            throw new IllegalStateException("publisher boom");
        }
    }
}
