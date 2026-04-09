package com.omron.oss.domain.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SignalRawPayloadParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SignalIngestRequest parse(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            SignalIngestRequest request = new SignalIngestRequest();
            request.setMachineId(readText(root, "machineId", "machineCode", "assetId"));
            request.setSamplingFrequency(readInt(root, 10000, "samplingFrequency", "frequency"));
            request.setValues(readValues(root));
            return request;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid raw signal payload", ex);
        }
    }

    private String readText(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode candidate = root.get(fieldName);
            if (candidate != null && !candidate.isNull() && !candidate.asText().trim().isEmpty()) {
                return candidate.asText().trim();
            }
        }
        return null;
    }

    private int readInt(JsonNode root, int defaultValue, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode candidate = root.get(fieldName);
            if (candidate != null && candidate.canConvertToInt()) {
                return candidate.asInt();
            }
        }
        return defaultValue;
    }

    private List<Double> readValues(JsonNode root) {
        JsonNode valuesNode = root.get("values");
        if (valuesNode == null || !valuesNode.isArray()) {
            throw new IllegalArgumentException("Raw signal payload must contain a values array");
        }
        List<Double> values = new ArrayList<Double>();
        for (JsonNode valueNode : valuesNode) {
            values.add(valueNode.asDouble());
        }
        return values;
    }
}
