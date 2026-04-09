package com.omron.oss.integration.runtime;

import com.omron.oss.domain.common.model.SignalFeatureSnapshot;
import com.omron.oss.domain.common.model.SignalSample;
import com.omron.oss.domain.signal.analysis.SignalFeatureExtractor;
import com.omron.oss.integration.acquisition.SignalSampleListener;
import com.omron.oss.integration.mysql.FeatureStoreRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public final class FeatureAnalysisService implements SignalSampleListener, AutoCloseable {

    private final SignalFeatureExtractor extractor;
    private final FeatureStoreRepository repository;
    private final ExecutorService executorService;
    private final AtomicLong submittedCount;
    private final AtomicLong successCount;
    private volatile long latestProcessedAt;
    private volatile long latestSubmittedAt;
    private volatile SignalFeatureSnapshot latestSnapshot;
    private volatile String lastError;

    public FeatureAnalysisService(SignalFeatureExtractor extractor, FeatureStoreRepository repository) {
        this.extractor = extractor;
        this.repository = repository;
        this.executorService = Executors.newSingleThreadExecutor();
        this.submittedCount = new AtomicLong();
        this.successCount = new AtomicLong();
        this.lastError = "";
        this.repository.initialize();
    }

    @Override
    public void onSample(final SignalSample sample) {
        if (sample.getValues().size() < 64) {
            return;
        }
        if (sample.getCollectedAtEpochMillis() <= latestProcessedAt) {
            return;
        }
        latestSubmittedAt = sample.getCollectedAtEpochMillis();
        submittedCount.incrementAndGet();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (sample.getCollectedAtEpochMillis() <= latestProcessedAt) {
                    return;
                }
                try {
                    SignalFeatureSnapshot snapshot = extractor.extract(sample);
                    repository.save(snapshot);
                    latestSnapshot = snapshot;
                    latestProcessedAt = sample.getCollectedAtEpochMillis();
                    successCount.incrementAndGet();
                    lastError = "";
                } catch (RuntimeException ex) {
                    lastError = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                    System.err.println("Feature analysis failed: " + lastError);
                    ex.printStackTrace(System.err);
                }
            }
        });
    }

    public SignalFeatureSnapshot getLatestSnapshot() {
        return latestSnapshot;
    }

    public long getLatestProcessedAt() {
        return latestProcessedAt;
    }

    public long getLatestSubmittedAt() {
        return latestSubmittedAt;
    }

    public long getSubmittedCount() {
        return submittedCount.get();
    }

    public long getSuccessCount() {
        return successCount.get();
    }

    public String getLastError() {
        return lastError;
    }

    @Override
    public void close() {
        executorService.shutdownNow();
    }
}
