package com.omron.oss.integration.mongodb;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalStorageCollectionsTest {

    @Test
    void shouldExposeOpenCollections() {
        List<String> collections = SignalStorageCollections.allCollections();

        assertTrue(collections.contains(SignalStorageCollections.RAW_SIGNAL_CURVE));
        assertTrue(collections.contains(SignalStorageCollections.ID_SEQUENCE_COLLECTION));
    }
}
