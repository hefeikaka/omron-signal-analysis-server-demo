package com.omron.oss.integration.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalSearchIndexCatalogTest {

    @Test
    void shouldExposeRc2IndexNames() {
        List<String> names = SignalSearchIndexCatalog.allIndexNames();

        assertTrue(names.contains(SignalSearchIndexCatalog.CHANNEL_PROCESS_INDEX));
        assertTrue(names.contains(SignalSearchIndexCatalog.INDEX_HISTORY_INDEX));
        assertTrue(names.contains(SignalSearchIndexCatalog.ALARM_HISTORY_INDEX));
    }
}
