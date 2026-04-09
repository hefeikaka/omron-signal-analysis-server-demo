package com.omron.oss.domain.signal.pipeline;

import com.omron.oss.domain.common.constants.TopicNames;
import com.omron.oss.domain.common.model.NormalizedSignalMessage;
import com.omron.oss.domain.common.model.SignalSample;
import com.omron.oss.integration.jms.MessagePublisher;
import com.omron.oss.integration.mongodb.InMemorySignalRecordRepository;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalPublishingServiceTest {

    @Test
    void shouldSaveAndPublishCompatibleSourceSignalMessage() {
        InMemorySignalRecordRepository repository = new InMemorySignalRecordRepository();
        CapturingMessagePublisher publisher = new CapturingMessagePublisher();
        SignalPublishingService service = new SignalPublishingService(new SignalMessageFormatter(), publisher, repository);

        SignalSample sample = new SignalSample("machine-01", 1712640000000L, 10000, Arrays.asList(1.0d, 2.0d, 3.0d));
        NormalizedSignalMessage message = service.processAndPublish(sample);

        assertEquals(1, repository.getSavedSamples().size());
        assertEquals(TopicNames.SOURCE_SIGNAL_CURVE, publisher.topicName);
        assertTrue(publisher.payload.contains("\"formattedAt\""));
        assertEquals(TopicNames.SOURCE_SIGNAL_CURVE, message.getTopicName());
    }

    private static final class CapturingMessagePublisher implements MessagePublisher {
        private String topicName;
        private String payload;

        @Override
        public void publish(String topicName, String payload) {
            this.topicName = topicName;
            this.payload = payload;
        }
    }
}
