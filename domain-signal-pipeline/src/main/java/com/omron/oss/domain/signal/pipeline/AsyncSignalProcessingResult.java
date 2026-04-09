package com.omron.oss.domain.signal.pipeline;

import com.omron.oss.domain.common.model.NormalizedSignalMessage;

public final class AsyncSignalProcessingResult {

    private final boolean success;
    private final NormalizedSignalMessage message;
    private final String failureReason;

    private AsyncSignalProcessingResult(boolean success, NormalizedSignalMessage message, String failureReason) {
        this.success = success;
        this.message = message;
        this.failureReason = failureReason;
    }

    public static AsyncSignalProcessingResult success(NormalizedSignalMessage message) {
        return new AsyncSignalProcessingResult(true, message, null);
    }

    public static AsyncSignalProcessingResult failure(String failureReason) {
        return new AsyncSignalProcessingResult(false, null, failureReason);
    }

    public boolean isSuccess() {
        return success;
    }

    public NormalizedSignalMessage getMessage() {
        return message;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
