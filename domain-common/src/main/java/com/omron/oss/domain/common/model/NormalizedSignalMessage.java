package com.omron.oss.domain.common.model;

public final class NormalizedSignalMessage {

    private final String topicName;
    private final SignalSample sample;
    private final String payloadJson;

    public NormalizedSignalMessage(String topicName, SignalSample sample, String payloadJson) {
        this.topicName = topicName;
        this.sample = sample;
        this.payloadJson = payloadJson;
    }

    public String getTopicName() {
        return topicName;
    }

    public SignalSample getSample() {
        return sample;
    }

    public String getPayloadJson() {
        return payloadJson;
    }
}
