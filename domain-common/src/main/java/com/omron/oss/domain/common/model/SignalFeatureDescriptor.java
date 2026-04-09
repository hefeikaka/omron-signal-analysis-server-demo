package com.omron.oss.domain.common.model;

import java.util.Objects;

public final class SignalFeatureDescriptor {

    private final String key;
    private final SignalFeatureDomain domain;
    private final String labelZh;
    private final String labelEn;

    public SignalFeatureDescriptor(String key, SignalFeatureDomain domain, String labelZh, String labelEn) {
        this.key = Objects.requireNonNull(key, "key");
        this.domain = Objects.requireNonNull(domain, "domain");
        this.labelZh = Objects.requireNonNull(labelZh, "labelZh");
        this.labelEn = Objects.requireNonNull(labelEn, "labelEn");
    }

    public String getKey() {
        return key;
    }

    public SignalFeatureDomain getDomain() {
        return domain;
    }

    public String getLabelZh() {
        return labelZh;
    }

    public String getLabelEn() {
        return labelEn;
    }
}
