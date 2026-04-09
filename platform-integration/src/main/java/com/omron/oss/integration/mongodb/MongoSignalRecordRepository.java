package com.omron.oss.integration.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.omron.oss.domain.common.model.SignalSample;
import org.bson.BsonDateTime;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MongoSignalRecordRepository implements SignalRecordRepository {

    private final MongoCollection<Document> collection;

    public MongoSignalRecordRepository(MongoClient mongoClient, String databaseName, String collectionName) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        this.collection = database.getCollection(collectionName);
    }

    @Override
    public void saveRawSignal(SignalSample sample) {
        Document document = new Document("machineId", sample.getMachineId())
            .append("collectedAt", sample.getCollectedAtEpochMillis())
            .append("samplingFrequency", sample.getSamplingFrequency())
            .append("values", sample.getValues());
        collection.insertOne(document);
    }

    @Override
    public List<SignalSample> findRecentRawSignals(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        List<SignalSample> recent = new ArrayList<SignalSample>();
        for (Document document : collection.find()
            .sort(Sorts.orderBy(Sorts.descending("time"), Sorts.descending("collectedAt"), Sorts.descending("_id")))
            .limit(limit)) {
            recent.add(toSample(document));
        }
        return Collections.unmodifiableList(recent);
    }

    private SignalSample toSample(Document document) {
        List<Double> values = readSignalValues(document);
        long collectedAt = readCollectedAt(document);
        int samplingFrequency = readSamplingFrequency(document, values);
        return new SignalSample(
            readMachineId(document),
            collectedAt,
            samplingFrequency,
            values
        );
    }

    private String readMachineId(Document document) {
        String machineId = document.getString("machineId");
        if (machineId != null && !machineId.trim().isEmpty()) {
            return machineId;
        }
        String subjectCode = document.getString("subjectCode");
        if (subjectCode != null && !subjectCode.trim().isEmpty()) {
            return subjectCode;
        }
        return "unknown-machine";
    }

    private long readCollectedAt(Document document) {
        Number collectedAt = document.get("collectedAt", Number.class);
        if (collectedAt != null) {
            return collectedAt.longValue();
        }

        Object time = document.get("time");
        if (time instanceof Date) {
            return ((Date) time).getTime();
        }
        if (time instanceof BsonDateTime) {
            return ((BsonDateTime) time).getValue();
        }
        if (time instanceof Number) {
            return ((Number) time).longValue();
        }
        return 0L;
    }

    private int readSamplingFrequency(Document document, List<Double> values) {
        Number samplingFrequency = document.get("samplingFrequency", Number.class);
        if (samplingFrequency != null) {
            return samplingFrequency.intValue();
        }
        if (!values.isEmpty()) {
            return values.size();
        }
        return 0;
    }

    private List<Double> readSignalValues(Document document) {
        List<Double> values = readNumericList(document.get("values"));
        if (!values.isEmpty()) {
            return values;
        }
        values = readNumericList(document.get("y"));
        if (!values.isEmpty()) {
            return values;
        }
        return Collections.emptyList();
    }

    private List<Double> readNumericList(Object source) {
        if (source == null) {
            return Collections.emptyList();
        }

        if (source instanceof List<?>) {
            List<Double> values = new ArrayList<Double>();
            for (Object item : (List<?>) source) {
                if (item instanceof Number) {
                    values.add(((Number) item).doubleValue());
                }
            }
            return values;
        }

        if (source instanceof String) {
            String text = ((String) source).trim();
            if (text.isEmpty() || "[]".equals(text)) {
                return Collections.emptyList();
            }
            if (text.startsWith("[")) {
                text = text.substring(1);
            }
            if (text.endsWith("]")) {
                text = text.substring(0, text.length() - 1);
            }
            if (text.trim().isEmpty()) {
                return Collections.emptyList();
            }

            String[] parts = text.split(",");
            List<Double> values = new ArrayList<Double>(parts.length);
            for (String part : parts) {
                String candidate = part.trim();
                if (!candidate.isEmpty()) {
                    values.add(Double.parseDouble(candidate));
                }
            }
            return values;
        }

        if (source instanceof Number) {
            return Collections.singletonList(((Number) source).doubleValue());
        }

        throw new IllegalStateException(
            "Unsupported signal value type: " + source.getClass().getName().toLowerCase(Locale.ROOT)
        );
    }
}
