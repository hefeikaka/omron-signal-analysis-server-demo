package com.omron.oss.integration.config;

import java.util.Objects;
import java.util.Properties;

public final class DirectAcquisitionProperties {

    private final boolean enabled;
    private final String driverName;
    private final int tcpPort;
    private final String equipmentNo;
    private final int mci;
    private final int samplingFrequency;
    private final long acquisitionIntervalMillis;
    private final long acquisitionDurationMillis;
    private final int readMode;
    private final int readModeTimeout;
    private final int historySize;

    public DirectAcquisitionProperties(
        boolean enabled,
        String driverName,
        int tcpPort,
        String equipmentNo,
        int mci,
        int samplingFrequency,
        long acquisitionIntervalMillis,
        long acquisitionDurationMillis,
        int readMode,
        int readModeTimeout,
        int historySize
    ) {
        this.enabled = enabled;
        this.driverName = Objects.requireNonNull(driverName, "driverName");
        this.tcpPort = tcpPort;
        this.equipmentNo = Objects.requireNonNull(equipmentNo, "equipmentNo");
        this.mci = mci;
        this.samplingFrequency = samplingFrequency;
        this.acquisitionIntervalMillis = acquisitionIntervalMillis;
        this.acquisitionDurationMillis = acquisitionDurationMillis;
        this.readMode = readMode;
        this.readModeTimeout = readModeTimeout;
        this.historySize = historySize;
    }

    public static DirectAcquisitionProperties from(Properties properties) {
        return new DirectAcquisitionProperties(
            Boolean.parseBoolean(properties.getProperty("enabled", "true")),
            properties.getProperty("driverName", "VK70xNMC_DAQ2.dll"),
            Integer.parseInt(properties.getProperty("tcpPort", "8234")),
            properties.getProperty("equipmentNo", "M001-1"),
            Integer.parseInt(properties.getProperty("mci", "0")),
            Integer.parseInt(properties.getProperty("samplingFrequency", "5000")),
            Long.parseLong(properties.getProperty("acquisitionIntervalMillis", "10000")),
            Long.parseLong(properties.getProperty("acquisitionDurationMillis", "1000")),
            Integer.parseInt(properties.getProperty("readMode", "1")),
            Integer.parseInt(properties.getProperty("readModeTimeout", "100")),
            Integer.parseInt(properties.getProperty("historySize", "30"))
        );
    }

    public Properties toProperties() {
        Properties properties = new Properties();
        properties.setProperty("enabled", String.valueOf(enabled));
        properties.setProperty("driverName", driverName);
        properties.setProperty("tcpPort", String.valueOf(tcpPort));
        properties.setProperty("equipmentNo", equipmentNo);
        properties.setProperty("mci", String.valueOf(mci));
        properties.setProperty("samplingFrequency", String.valueOf(samplingFrequency));
        properties.setProperty("acquisitionIntervalMillis", String.valueOf(acquisitionIntervalMillis));
        properties.setProperty("acquisitionDurationMillis", String.valueOf(acquisitionDurationMillis));
        properties.setProperty("readMode", String.valueOf(readMode));
        properties.setProperty("readModeTimeout", String.valueOf(readModeTimeout));
        properties.setProperty("historySize", String.valueOf(historySize));
        return properties;
    }

    public DirectAcquisitionProperties withOverrides(
        Boolean enabledOverride,
        Integer samplingFrequencyOverride,
        Long acquisitionIntervalMillisOverride,
        Long acquisitionDurationMillisOverride
    ) {
        return new DirectAcquisitionProperties(
            enabledOverride != null ? enabledOverride.booleanValue() : enabled,
            driverName,
            tcpPort,
            equipmentNo,
            mci,
            samplingFrequencyOverride != null ? samplingFrequencyOverride.intValue() : samplingFrequency,
            acquisitionIntervalMillisOverride != null
                ? acquisitionIntervalMillisOverride.longValue()
                : acquisitionIntervalMillis,
            acquisitionDurationMillisOverride != null
                ? acquisitionDurationMillisOverride.longValue()
                : acquisitionDurationMillis,
            readMode,
            readModeTimeout,
            historySize
        );
    }

    public int computeSamplingNum() {
        long points = Math.round(samplingFrequency * (acquisitionDurationMillis / 1000.0d));
        return (int) Math.max(1L, points);
    }

    public int[] buildInitializeParams() {
        return new int[] {
            samplingFrequency,
            4,
            16,
            computeSamplingNum(),
            0,
            0,
            0,
            0,
            1,
            1,
            1,
            1
        };
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getDriverName() {
        return driverName;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public String getEquipmentNo() {
        return equipmentNo;
    }

    public int getMci() {
        return mci;
    }

    public int getSamplingFrequency() {
        return samplingFrequency;
    }

    public long getAcquisitionIntervalMillis() {
        return acquisitionIntervalMillis;
    }

    public long getAcquisitionDurationMillis() {
        return acquisitionDurationMillis;
    }

    public int getReadMode() {
        return readMode;
    }

    public int getReadModeTimeout() {
        return readModeTimeout;
    }

    public int getHistorySize() {
        return historySize;
    }
}
