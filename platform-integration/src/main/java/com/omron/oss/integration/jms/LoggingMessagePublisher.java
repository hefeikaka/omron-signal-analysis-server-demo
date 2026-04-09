package com.omron.oss.integration.jms;

import java.util.logging.Logger;

public final class LoggingMessagePublisher implements MessagePublisher {

    private static final Logger LOGGER = Logger.getLogger(LoggingMessagePublisher.class.getName());

    @Override
    public void publish(String topicName, String payload) {
        LOGGER.info("Publishing to topic '" + topicName + "' payload=" + payload);
    }
}
