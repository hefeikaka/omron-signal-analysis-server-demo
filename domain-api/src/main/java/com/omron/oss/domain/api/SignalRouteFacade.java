package com.omron.oss.domain.api;

import com.omron.oss.domain.common.model.NormalizedSignalMessage;

public final class SignalRouteFacade {

    private final SignalRawPayloadParser rawPayloadParser;
    private final SignalIngestFacade ingestFacade;

    public SignalRouteFacade(SignalRawPayloadParser rawPayloadParser, SignalIngestFacade ingestFacade) {
        this.rawPayloadParser = rawPayloadParser;
        this.ingestFacade = ingestFacade;
    }

    public NormalizedSignalMessage ingestRawPayload(String rawJson) {
        return ingestFacade.ingest(rawPayloadParser.parse(rawJson));
    }
}
