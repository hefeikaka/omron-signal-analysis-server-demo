package com.omron.oss.integration.mongodb;

import com.omron.oss.domain.common.model.SignalSample;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JsonLineSignalRecordRepository implements SignalRecordRepository {

    private final Path outputFile;

    public JsonLineSignalRecordRepository(Path outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public synchronized void saveRawSignal(SignalSample sample) {
        try {
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String line = toJsonLine(sample) + System.lineSeparator();
            Files.write(
                outputFile,
                line.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to persist signal sample", ex);
        }
    }

    @Override
    public synchronized List<SignalSample> findRecentRawSignals(int limit) {
        if (limit <= 0 || !Files.exists(outputFile)) {
            return Collections.emptyList();
        }

        try {
            List<String> lines = Files.readAllLines(outputFile, StandardCharsets.UTF_8);
            List<SignalSample> recent = new ArrayList<SignalSample>();
            for (int index = lines.size() - 1; index >= 0 && recent.size() < limit; index--) {
                String line = lines.get(index).trim();
                if (!line.isEmpty()) {
                    recent.add(parseJsonLine(line));
                }
            }
            return Collections.unmodifiableList(recent);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read persisted signal samples", ex);
        }
    }

    private String toJsonLine(SignalSample sample) {
        return "{\"machineId\":\"" + escape(sample.getMachineId()) + "\","
            + "\"collectedAt\":" + sample.getCollectedAtEpochMillis() + ","
            + "\"samplingFrequency\":" + sample.getSamplingFrequency() + ","
            + "\"values\":\"" + escape(sample.getValues().toString()) + "\"}";
    }

    private SignalSample parseJsonLine(String line) {
        String machineId = extractQuotedValue(line, "\"machineId\":\"");
        long collectedAt = Long.parseLong(extractNumericValue(line, "\"collectedAt\":"));
        int samplingFrequency = Integer.parseInt(extractNumericValue(line, "\"samplingFrequency\":"));
        String valuesLiteral = unescape(extractQuotedValue(line, "\"values\":\""));
        return new SignalSample(machineId, collectedAt, samplingFrequency, parseValues(valuesLiteral));
    }

    private String extractQuotedValue(String line, String marker) {
        int start = line.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("Missing marker " + marker);
        }
        start += marker.length();
        StringBuilder value = new StringBuilder();
        boolean escaping = false;
        for (int index = start; index < line.length(); index++) {
            char character = line.charAt(index);
            if (escaping) {
                value.append(character);
                escaping = false;
            } else if (character == '\\') {
                escaping = true;
            } else if (character == '"') {
                return value.toString();
            } else {
                value.append(character);
            }
        }
        throw new IllegalStateException("Unterminated quoted value for marker " + marker);
    }

    private String extractNumericValue(String line, String marker) {
        int start = line.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("Missing marker " + marker);
        }
        start += marker.length();
        int end = start;
        while (end < line.length() && Character.isDigit(line.charAt(end))) {
            end++;
        }
        return line.substring(start, end);
    }

    private List<Double> parseValues(String literal) {
        String normalized = literal == null ? "" : literal.trim();
        if (normalized.isEmpty() || "[]".equals(normalized)) {
            return Collections.emptyList();
        }
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String[] parts = normalized.split(",");
        List<Double> values = new ArrayList<Double>(parts.length);
        for (String part : parts) {
            values.add(Double.valueOf(part.trim()));
        }
        return values;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
