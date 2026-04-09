package com.omron.oss.integration.acquisition;

import com.omron.oss.domain.common.model.SignalSample;
import com.omron.oss.integration.config.DirectAcquisitionProperties;
import com.omron.oss.integration.config.PropertiesLoader;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.ptr.IntByReference;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Deque;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class DirectAcquisitionService implements AutoCloseable {

    private final Path configPath;
    private final Path driverDirectory;
    private final PropertiesLoader propertiesLoader;
    private final ScheduledExecutorService executorService;
    private final Object lock;
    private final Deque<SignalSample> history;
    private final List<SignalSampleListener> listeners;

    private volatile DirectAcquisitionProperties properties;
    private volatile Vk70xNmcLibrary library;
    private volatile boolean tcpServerOpened;
    private volatile boolean initialized;
    private volatile boolean connected;
    private volatile boolean started;
    private volatile String lastError;
    private volatile long lastCollectedAtEpochMillis;
    private volatile SignalSample latestSample;
    private volatile long nextCollectionAtEpochMillis;

    public DirectAcquisitionService(Path configPath, Path driverDirectory) {
        this(configPath, driverDirectory, new PropertiesLoader());
    }

    DirectAcquisitionService(Path configPath, Path driverDirectory, PropertiesLoader propertiesLoader) {
        this.configPath = configPath;
        this.driverDirectory = driverDirectory;
        this.propertiesLoader = propertiesLoader;
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.lock = new Object();
        this.history = new ArrayDeque<SignalSample>();
        this.listeners = new CopyOnWriteArrayList<SignalSampleListener>();
        this.properties = DirectAcquisitionProperties.from(propertiesLoader.load(configPath));
        this.lastError = "Waiting to start direct acquisition.";
    }

    public void start() {
        if (started) {
            return;
        }
        started = true;
        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, 0L, 200L, TimeUnit.MILLISECONDS);
    }

    public DirectAcquisitionProperties getProperties() {
        return properties;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public String getLastError() {
        return lastError;
    }

    public long getLastCollectedAtEpochMillis() {
        return lastCollectedAtEpochMillis;
    }

    public SignalSample getLatestSample() {
        return latestSample;
    }

    public List<SignalSample> getRecentSamples() {
        synchronized (lock) {
            return Collections.unmodifiableList(new ArrayList<SignalSample>(history));
        }
    }

    public void addListener(SignalSampleListener listener) {
        listeners.add(listener);
    }

    public DirectAcquisitionProperties updateSettings(
        Boolean enabled,
        Integer samplingFrequency,
        Long acquisitionIntervalMillis,
        Long acquisitionDurationMillis
    ) {
        synchronized (lock) {
            DirectAcquisitionProperties updated = properties.withOverrides(
                enabled,
                samplingFrequency,
                acquisitionIntervalMillis,
                acquisitionDurationMillis
            );
            validate(updated);
            properties = updated;
            initialized = false;
            nextCollectionAtEpochMillis = 0L;
            persist(updated);
            return updated;
        }
    }

    private void tick() {
        try {
            DirectAcquisitionProperties current = properties;
            if (!current.isEnabled()) {
                lastError = "Direct acquisition is disabled.";
                connected = false;
                return;
            }

            if (!ensureReady(current)) {
                return;
            }

            long now = System.currentTimeMillis();
            if (now < nextCollectionAtEpochMillis) {
                return;
            }

            SignalSample sample = collectOnce(current, now);
            latestSample = sample;
            lastCollectedAtEpochMillis = sample.getCollectedAtEpochMillis();
            nextCollectionAtEpochMillis = now + current.getAcquisitionIntervalMillis();
            lastError = "";
            synchronized (lock) {
                history.addFirst(sample);
                while (history.size() > current.getHistorySize()) {
                    history.removeLast();
                }
            }
            notifyListeners(sample);
        } catch (Throwable throwable) {
            connected = false;
            initialized = false;
            lastError = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
        }
    }

    private boolean ensureReady(DirectAcquisitionProperties current) {
        if (library == null) {
            loadLibrary(current);
        }

        if (!tcpServerOpened) {
            library.Server_TCPOpen(current.getTcpPort());
            tcpServerOpened = true;
            lastError = "Opening TCP server on port " + current.getTcpPort() + ".";
        }

        IntByReference clients = new IntByReference();
        int clientResult = library.Server_Get_ConnectedClientNumbers(clients);
        if (clientResult < 0 || clients.getValue() <= 0) {
            connected = false;
            lastError = "Waiting for acquisition card connection on port " + current.getTcpPort() + ".";
            return false;
        }
        connected = true;

        if (!initialized) {
            int result = library.VK70xNMC_Set_SystemMode(current.getMci(), 0, 0, 0);
            ensureSuccess(result, "Failed to set system mode");

            result = library.VK70xNMC_InitializeAll(current.getMci(), current.buildInitializeParams(), 12);
            ensureSuccess(result, "Failed to initialize acquisition card");

            result = library.VK70xNMC_Set_BlockingMethodtoReadADCResult(
                current.getReadMode(),
                current.getReadModeTimeout()
            );
            ensureSuccess(result, "Failed to configure blocking read mode");

            initialized = true;
            lastError = "Direct acquisition is ready.";
        }
        return true;
    }

    private void loadLibrary(DirectAcquisitionProperties current) {
        NativeLibrary.addSearchPath(current.getDriverName(), driverDirectory.toAbsolutePath().toString());
        library = Native.load(current.getDriverName(), Vk70xNmcLibrary.class);
    }

    private SignalSample collectOnce(DirectAcquisitionProperties current, long now) {
        int sampleCount = current.computeSamplingNum();
        int result = library.VK70xNMC_StartSampling_NPoints(current.getMci(), sampleCount);
        ensureSuccess(result, "Failed to start sampling");

        Memory memory = new Memory(Math.max(320000L, sampleCount * 8L * 4L));
        int actualCount = library.VK70xNMC_GetFourChannel(current.getMci(), memory, sampleCount);
        if (actualCount <= 0) {
            throw new IllegalStateException("No data returned from acquisition card.");
        }

        library.VK70xNMC_StopSampling(current.getMci());

        List<Double> values = new ArrayList<Double>(actualCount);
        for (int index = 0; index < actualCount; index++) {
            values.add(Double.valueOf(memory.getDouble(index * 8L)));
        }
        return new SignalSample(current.getEquipmentNo(), now, current.getSamplingFrequency(), values);
    }

    private void ensureSuccess(int result, String message) {
        if (result < 0) {
            throw new IllegalStateException(message + " (code " + result + ")");
        }
    }

    private void validate(DirectAcquisitionProperties current) {
        if (current.getSamplingFrequency() <= 0) {
            throw new IllegalArgumentException("Sampling frequency must be greater than 0.");
        }
        if (current.getAcquisitionIntervalMillis() < 100L) {
            throw new IllegalArgumentException("Acquisition interval must be at least 100 ms.");
        }
        if (current.getAcquisitionDurationMillis() < 20L) {
            throw new IllegalArgumentException("Acquisition duration must be at least 20 ms.");
        }
        if (current.getAcquisitionDurationMillis() > current.getAcquisitionIntervalMillis()) {
            throw new IllegalArgumentException("Acquisition duration cannot be greater than the acquisition interval.");
        }
    }

    private void persist(DirectAcquisitionProperties current) {
        Properties persisted = current.toProperties();
        try (OutputStream outputStream = Files.newOutputStream(configPath)) {
            persisted.store(outputStream, "Open direct acquisition settings");
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to persist direct acquisition settings to " + configPath, ex);
        }
    }

    private void notifyListeners(SignalSample sample) {
        for (SignalSampleListener listener : listeners) {
            try {
                listener.onSample(sample);
            } catch (RuntimeException ignored) {
                // Listener failures must not stop direct acquisition.
            }
        }
    }

    @Override
    public void close() {
        executorService.shutdownNow();
    }
}
