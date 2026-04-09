package com.omron.oss.integration.mongodb;

import java.util.Arrays;
import java.util.List;

public final class SignalStorageCollections {

    public static final String RAW_SIGNAL_CURVE = "rawdata_signal_curve";
    public static final String RAW_SIGNAL_ANORMALY_SOURCE = "rawdata_signal_anormaly_source";
    public static final String RAW_INDEX_SOURCE = "rawdata_index_source";
    public static final String RAW_PRODUCTION_INFO_SOURCE = "rawdata_production_info_source";
    public static final String SIGNAL_MACHINE_INDEX_HISTORY = "signal_machine_index_history";
    public static final String ID_SEQUENCE_COLLECTION = "id-sequence-coll";

    private SignalStorageCollections() {
    }

    public static List<String> allCollections() {
        return Arrays.asList(
            ID_SEQUENCE_COLLECTION,
            RAW_SIGNAL_CURVE,
            RAW_SIGNAL_ANORMALY_SOURCE,
            RAW_INDEX_SOURCE,
            RAW_PRODUCTION_INFO_SOURCE,
            SIGNAL_MACHINE_INDEX_HISTORY
        );
    }
}
