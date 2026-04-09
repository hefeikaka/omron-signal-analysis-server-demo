package com.omron.oss.domain.common;

import com.omron.oss.domain.common.constants.TopicNames;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TopicNamesTest {

    @Test
    void shouldKeepCompatibleSourceSignalTopicName() {
        assertEquals("sourceSiganlCurve", TopicNames.SOURCE_SIGNAL_CURVE);
    }
}
