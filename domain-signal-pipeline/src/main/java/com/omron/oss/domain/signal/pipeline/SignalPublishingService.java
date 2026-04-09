package com.omron.oss.domain.signal.pipeline;

import com.omron.oss.domain.common.model.NormalizedSignalMessage;
import com.omron.oss.domain.common.model.SignalSample;
import com.omron.oss.integration.jms.MessagePublisher;
import com.omron.oss.integration.mongodb.SignalRecordRepository;

public final class SignalPublishingService {

    private final SignalMessageFormatter formatter;
    private final MessagePublisher publisher;
    private final SignalRecordRepository repository;

    public SignalPublishingService(SignalMessageFormatter formatter, MessagePublisher publisher, SignalRecordRepository repository) {
        this.formatter = formatter;
        this.publisher = publisher;
        this.repository = repository;
    }

    public NormalizedSignalMessage processAndPublish(SignalSample sample) {
        repository.saveRawSignal(sample);
        NormalizedSignalMessage message = formatter.toMessage(sample);
        publisher.publish(message.getTopicName(), message.getPayloadJson());
        return message;
    }
}
