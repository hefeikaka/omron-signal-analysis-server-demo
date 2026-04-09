package com.omron.oss.domain.api;

import com.omron.oss.domain.common.constants.TopicNames;
import com.omron.oss.domain.common.model.NormalizedSignalMessage;
import com.omron.oss.domain.signal.collection.SignalCollectionService;
import com.omron.oss.domain.signal.pipeline.SignalMessageFormatter;
import com.omron.oss.domain.signal.pipeline.SignalPublishingService;
import com.omron.oss.integration.jms.MessagePublisher;
import com.omron.oss.integration.mongodb.InMemorySignalRecordRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalRouteFacadeTest {

    @Test
    void shouldParseRawJsonAndPublishCompatibleSignalMessage() {
        CapturingMessagePublisher publisher = new CapturingMessagePublisher();
        InMemorySignalRecordRepository repository = new InMemorySignalRecordRepository();
        SignalIngestFacade ingestFacade = new SignalIngestFacade(
            new SignalCollectionService(),
            new SignalPublishingService(new SignalMessageFormatter(), publisher, repository)
        );
        SignalRouteFacade routeFacade = new SignalRouteFacade(new SignalRawPayloadParser(), ingestFacade);

        NormalizedSignalMessage message = routeFacade.ingestRawPayload(
            "{\"machineCode\":\"edge-01\",\"frequency\":12000,\"values\":[0.1,0.2,0.3]}"
        );

        assertEquals(1, repository.getSavedSamples().size());
        assertEquals("edge-01", repository.getSavedSamples().get(0).getMachineId());
        assertEquals(12000, repository.getSavedSamples().get(0).getSamplingFrequency());
        assertEquals(TopicNames.SOURCE_SIGNAL_CURVE, publisher.topicName);
        assertTrue(message.getPayloadJson().contains("\"machineId\":\"edge-01\""));
    }

    private static final class CapturingMessagePublisher implements MessagePublisher {
        private String topicName;

        @Override
        public void publish(String topicName, String payload) {
            this.topicName = topicName;
        }
    }
}
