package com.omron.oss.integration.jms;

public interface MessagePublisher {

    void publish(String topicName, String payload);
}
