package com.omron.oss.domain.common.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SignalFeatureCatalog {

    private static final List<SignalFeatureDescriptor> DESCRIPTORS = Collections.unmodifiableList(Arrays.asList(
        new SignalFeatureDescriptor("time_mean", SignalFeatureDomain.TIME, "均值", "Mean"),
        new SignalFeatureDescriptor("time_rms", SignalFeatureDomain.TIME, "均方根", "RMS"),
        new SignalFeatureDescriptor("time_peak", SignalFeatureDomain.TIME, "峰值", "Peak"),
        new SignalFeatureDescriptor("time_peak_to_peak", SignalFeatureDomain.TIME, "峰峰值", "Peak-to-Peak"),
        new SignalFeatureDescriptor("time_stddev", SignalFeatureDomain.TIME, "标准差", "Std Dev"),
        new SignalFeatureDescriptor("time_skewness", SignalFeatureDomain.TIME, "偏度", "Skewness"),
        new SignalFeatureDescriptor("time_kurtosis", SignalFeatureDomain.TIME, "峭度", "Kurtosis"),
        new SignalFeatureDescriptor("time_crest_factor", SignalFeatureDomain.TIME, "峰值因子", "Crest Factor"),
        new SignalFeatureDescriptor("time_impulse_factor", SignalFeatureDomain.TIME, "脉冲因子", "Impulse Factor"),
        new SignalFeatureDescriptor("time_shape_factor", SignalFeatureDomain.TIME, "波形因子", "Shape Factor"),
        new SignalFeatureDescriptor("time_margin_factor", SignalFeatureDomain.TIME, "裕度因子", "Margin Factor"),
        new SignalFeatureDescriptor("freq_dominant_frequency", SignalFeatureDomain.FREQUENCY, "主频", "Dominant Frequency"),
        new SignalFeatureDescriptor("freq_dominant_amplitude", SignalFeatureDomain.FREQUENCY, "主频幅值", "Dominant Amplitude"),
        new SignalFeatureDescriptor("freq_spectral_centroid", SignalFeatureDomain.FREQUENCY, "频谱重心", "Spectral Centroid"),
        new SignalFeatureDescriptor("freq_spectral_rms", SignalFeatureDomain.FREQUENCY, "频谱均方根频率", "Spectral RMS"),
        new SignalFeatureDescriptor("freq_band_energy_low", SignalFeatureDomain.FREQUENCY, "低频能量", "Low Band Energy"),
        new SignalFeatureDescriptor("freq_band_energy_mid", SignalFeatureDomain.FREQUENCY, "中频能量", "Mid Band Energy"),
        new SignalFeatureDescriptor("freq_band_energy_high", SignalFeatureDomain.FREQUENCY, "高频能量", "High Band Energy")
    ));

    private SignalFeatureCatalog() {
    }

    public static List<SignalFeatureDescriptor> descriptors() {
        return DESCRIPTORS;
    }

    public static SignalFeatureDescriptor byKey(String key) {
        for (SignalFeatureDescriptor descriptor : DESCRIPTORS) {
            if (descriptor.getKey().equals(key)) {
                return descriptor;
            }
        }
        throw new IllegalArgumentException("Unsupported feature key: " + key);
    }
}
