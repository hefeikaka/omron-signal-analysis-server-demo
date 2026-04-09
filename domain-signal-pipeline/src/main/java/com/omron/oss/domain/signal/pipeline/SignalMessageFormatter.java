package com.omron.oss.domain.signal.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omron.oss.domain.common.constants.TopicNames;
import com.omron.oss.domain.common.model.NormalizedSignalMessage;
import com.omron.oss.domain.common.model.SignalSample;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SignalMessageFormatter {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final ObjectMapper objectMapper = new ObjectMapper();

    public NormalizedSignalMessage toMessage(SignalSample sample) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("machineId", sample.getMachineId());
        payload.put("collectedAt", sample.getCollectedAtEpochMillis());
        payload.put("formattedAt", TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(sample.getCollectedAtEpochMillis())));
        payload.put("samplingFrequency", sample.getSamplingFrequency());
        payload.put("values", sample.getValues());
        try {
            return new NormalizedSignalMessage(
                TopicNames.SOURCE_SIGNAL_CURVE,
                sample,
                objectMapper.writeValueAsString(payload)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize signal message", ex);
        }
    }
}
