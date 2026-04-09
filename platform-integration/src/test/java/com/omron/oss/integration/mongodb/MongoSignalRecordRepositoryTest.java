package com.omron.oss.integration.mongodb;

import com.omron.oss.domain.common.model.SignalSample;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MongoSignalRecordRepositoryTest {

    @Test
    void shouldReadOpenSchemaDocument() throws Exception {
        SignalSample sample = invokeToSample(new Document("machineId", "open-machine")
            .append("collectedAt", 1712640000000L)
            .append("samplingFrequency", 10000)
            .append("values", Arrays.asList(1.0d, 2.5d, 3.75d)));

        assertEquals("open-machine", sample.getMachineId());
        assertEquals(1712640000000L, sample.getCollectedAtEpochMillis());
        assertEquals(10000, sample.getSamplingFrequency());
        assertEquals(3, sample.getValues().size());
        assertEquals(3.75d, sample.getValues().get(2), 0.0001d);
    }

    @Test
    void shouldReadRc2SchemaDocument() throws Exception {
        SignalSample sample = invokeToSample(new Document("subjectCode", "M001-1")
            .append("time", new Date(1712640000000L))
            .append("y", "[0.11,0.22,0.33,0.44]"));

        assertEquals("M001-1", sample.getMachineId());
        assertEquals(1712640000000L, sample.getCollectedAtEpochMillis());
        assertEquals(4, sample.getSamplingFrequency());
        assertEquals(Arrays.asList(0.11d, 0.22d, 0.33d, 0.44d), sample.getValues());
    }

    @SuppressWarnings("unchecked")
    private SignalSample invokeToSample(Document document) throws Exception {
        Method method = MongoSignalRecordRepository.class.getDeclaredMethod("toSample", Document.class);
        method.setAccessible(true);
        return (SignalSample) method.invoke(allocateRepository(), document);
    }

    private MongoSignalRecordRepository allocateRepository() throws Exception {
        java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);
        return (MongoSignalRecordRepository) unsafe.allocateInstance(MongoSignalRecordRepository.class);
    }
}
