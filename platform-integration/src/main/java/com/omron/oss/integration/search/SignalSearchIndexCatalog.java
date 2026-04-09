package com.omron.oss.integration.search;

import java.util.Arrays;
import java.util.List;

public final class SignalSearchIndexCatalog {

    public static final String CHANNEL_PROCESS_INDEX = "signal-analysischannel-process-v1.0";
    public static final String INDEX_HISTORY_INDEX = "signal-analysisindex-history-v1.0";
    public static final String ALARM_SUPPRESS_HISTORY_INDEX = "signal-analysisalarm-suppress-history-v1.0";
    public static final String STATUS_HISTORY_INDEX = "signal-analysisstatus-history-v1.0";
    public static final String STATUS_STAT_HISTORY_INDEX = "signal-analysisstatus-stat-history-v1.0";
    public static final String INDEX_STAT_HISTORY_INDEX = "signal-analysisindex-stat-history-v1.0";
    public static final String ALARM_HISTORY_INDEX = "signal-analysisalarm-history-v1.0";
    public static final String STATUS_SUPPRESS_HISTORY_INDEX = "signal-analysisstatus-suppress-history-v1.0";
    public static final String ALARM_STAT_HISTORY_INDEX = "signal-analysisalarm-stat-history-v1.0";

    private SignalSearchIndexCatalog() {
    }

    public static List<String> allIndexNames() {
        return Arrays.asList(
            CHANNEL_PROCESS_INDEX,
            INDEX_HISTORY_INDEX,
            ALARM_SUPPRESS_HISTORY_INDEX,
            STATUS_HISTORY_INDEX,
            STATUS_STAT_HISTORY_INDEX,
            INDEX_STAT_HISTORY_INDEX,
            ALARM_HISTORY_INDEX,
            STATUS_SUPPRESS_HISTORY_INDEX,
            ALARM_STAT_HISTORY_INDEX
        );
    }
}
