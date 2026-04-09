package com.omron.oss.domain.signal.analysis;

import com.omron.oss.domain.common.model.SignalFeatureCatalog;
import com.omron.oss.domain.common.model.SignalFeatureDescriptor;
import com.omron.oss.domain.common.model.SignalFeatureSnapshot;
import com.omron.oss.domain.common.model.SignalFeatureValue;
import com.omron.oss.domain.common.model.SignalSample;

import java.util.ArrayList;
import java.util.List;

public final class SignalFeatureExtractor {

    public SignalFeatureSnapshot extract(SignalSample sample) {
        List<Double> values = sample.getValues();
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Signal sample does not contain values.");
        }

        TimeFeatures timeFeatures = computeTimeFeatures(values);
        FrequencyFeatures frequencyFeatures = computeFrequencyFeatures(values, sample.getSamplingFrequency());

        List<SignalFeatureValue> features = new ArrayList<SignalFeatureValue>();
        add(features, "time_mean", timeFeatures.mean);
        add(features, "time_rms", timeFeatures.rms);
        add(features, "time_peak", timeFeatures.peak);
        add(features, "time_peak_to_peak", timeFeatures.peakToPeak);
        add(features, "time_stddev", timeFeatures.stddev);
        add(features, "time_skewness", timeFeatures.skewness);
        add(features, "time_kurtosis", timeFeatures.kurtosis);
        add(features, "time_crest_factor", timeFeatures.crestFactor);
        add(features, "time_impulse_factor", timeFeatures.impulseFactor);
        add(features, "time_shape_factor", timeFeatures.shapeFactor);
        add(features, "time_margin_factor", timeFeatures.marginFactor);
        add(features, "freq_dominant_frequency", frequencyFeatures.dominantFrequency);
        add(features, "freq_dominant_amplitude", frequencyFeatures.dominantAmplitude);
        add(features, "freq_spectral_centroid", frequencyFeatures.spectralCentroid);
        add(features, "freq_spectral_rms", frequencyFeatures.spectralRms);
        add(features, "freq_band_energy_low", frequencyFeatures.lowBandEnergy);
        add(features, "freq_band_energy_mid", frequencyFeatures.midBandEnergy);
        add(features, "freq_band_energy_high", frequencyFeatures.highBandEnergy);

        return new SignalFeatureSnapshot(
            sample.getMachineId(),
            sample.getCollectedAtEpochMillis(),
            sample.getSamplingFrequency(),
            sample.getValues().size(),
            features
        );
    }

    private void add(List<SignalFeatureValue> features, String key, double value) {
        SignalFeatureDescriptor descriptor = SignalFeatureCatalog.byKey(key);
        features.add(new SignalFeatureValue(descriptor, sanitize(value)));
    }

    private static double sanitize(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0d;
        }
        return value;
    }

    private TimeFeatures computeTimeFeatures(List<Double> values) {
        double min = values.get(0).doubleValue();
        double max = min;
        double sum = 0.0d;
        double sumSquares = 0.0d;
        double absSum = 0.0d;
        double sqrtAbsSum = 0.0d;
        double peakAbs = Math.abs(min);

        for (Double value : values) {
            double current = value.doubleValue();
            min = Math.min(min, current);
            max = Math.max(max, current);
            sum += current;
            sumSquares += current * current;
            absSum += Math.abs(current);
            sqrtAbsSum += Math.sqrt(Math.abs(current));
            peakAbs = Math.max(peakAbs, Math.abs(current));
        }

        int size = values.size();
        double mean = sum / size;
        double rms = Math.sqrt(sumSquares / size);
        double absMean = absSum / size;
        double sqrtAbsMean = sqrtAbsSum / size;

        double sumCenteredSquares = 0.0d;
        double sumCenteredCubes = 0.0d;
        double sumCenteredFourth = 0.0d;
        for (Double value : values) {
            double centered = value.doubleValue() - mean;
            double squared = centered * centered;
            sumCenteredSquares += squared;
            sumCenteredCubes += squared * centered;
            sumCenteredFourth += squared * squared;
        }

        double variance = sumCenteredSquares / size;
        double stddev = Math.sqrt(Math.max(variance, 0.0d));
        double skewness = stddev == 0.0d ? 0.0d : (sumCenteredCubes / size) / (stddev * stddev * stddev);
        double kurtosis = variance == 0.0d ? 0.0d : (sumCenteredFourth / size) / (variance * variance);

        return new TimeFeatures(
            mean,
            rms,
            peakAbs,
            max - min,
            stddev,
            skewness,
            kurtosis,
            divide(peakAbs, rms),
            divide(peakAbs, absMean),
            divide(rms, absMean),
            divide(peakAbs, sqrtAbsMean * sqrtAbsMean)
        );
    }

    private FrequencyFeatures computeFrequencyFeatures(List<Double> values, int samplingFrequency) {
        int fftSize = highestPowerOfTwo(Math.min(values.size(), 2048));
        if (fftSize < 8) {
            return new FrequencyFeatures();
        }

        double[] real = new double[fftSize];
        double[] imaginary = new double[fftSize];
        for (int index = 0; index < fftSize; index++) {
            real[index] = values.get(index).doubleValue();
            imaginary[index] = 0.0d;
        }

        FastFourierTransform.fft(real, imaginary);

        int half = fftSize / 2;
        double frequencyStep = samplingFrequency / (double) fftSize;
        double dominantAmplitude = 0.0d;
        double dominantFrequency = 0.0d;
        double weightedFrequency = 0.0d;
        double weightedFrequencySquares = 0.0d;
        double magnitudeSum = 0.0d;
        double lowBandEnergy = 0.0d;
        double midBandEnergy = 0.0d;
        double highBandEnergy = 0.0d;

        for (int index = 1; index <= half; index++) {
            double magnitude = Math.sqrt(real[index] * real[index] + imaginary[index] * imaginary[index]) / fftSize;
            double frequency = index * frequencyStep;
            magnitudeSum += magnitude;
            weightedFrequency += frequency * magnitude;
            weightedFrequencySquares += frequency * frequency * magnitude;

            if (magnitude > dominantAmplitude) {
                dominantAmplitude = magnitude;
                dominantFrequency = frequency;
            }

            if (frequency < samplingFrequency / 6.0d) {
                lowBandEnergy += magnitude * magnitude;
            } else if (frequency < samplingFrequency / 3.0d) {
                midBandEnergy += magnitude * magnitude;
            } else {
                highBandEnergy += magnitude * magnitude;
            }
        }

        double spectralCentroid = divide(weightedFrequency, magnitudeSum);
        double spectralRms = magnitudeSum == 0.0d ? 0.0d : Math.sqrt(weightedFrequencySquares / magnitudeSum);

        return new FrequencyFeatures(
            dominantFrequency,
            dominantAmplitude,
            spectralCentroid,
            spectralRms,
            lowBandEnergy,
            midBandEnergy,
            highBandEnergy
        );
    }

    private static int highestPowerOfTwo(int value) {
        int result = 1;
        while ((result << 1) <= value) {
            result <<= 1;
        }
        return result;
    }

    private static double divide(double numerator, double denominator) {
        if (denominator == 0.0d) {
            return 0.0d;
        }
        return numerator / denominator;
    }

    private static final class TimeFeatures {
        private final double mean;
        private final double rms;
        private final double peak;
        private final double peakToPeak;
        private final double stddev;
        private final double skewness;
        private final double kurtosis;
        private final double crestFactor;
        private final double impulseFactor;
        private final double shapeFactor;
        private final double marginFactor;

        private TimeFeatures(
            double mean,
            double rms,
            double peak,
            double peakToPeak,
            double stddev,
            double skewness,
            double kurtosis,
            double crestFactor,
            double impulseFactor,
            double shapeFactor,
            double marginFactor
        ) {
            this.mean = mean;
            this.rms = rms;
            this.peak = peak;
            this.peakToPeak = peakToPeak;
            this.stddev = stddev;
            this.skewness = skewness;
            this.kurtosis = kurtosis;
            this.crestFactor = crestFactor;
            this.impulseFactor = impulseFactor;
            this.shapeFactor = shapeFactor;
            this.marginFactor = marginFactor;
        }
    }

    private static final class FrequencyFeatures {
        private final double dominantFrequency;
        private final double dominantAmplitude;
        private final double spectralCentroid;
        private final double spectralRms;
        private final double lowBandEnergy;
        private final double midBandEnergy;
        private final double highBandEnergy;

        private FrequencyFeatures() {
            this(0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d);
        }

        private FrequencyFeatures(
            double dominantFrequency,
            double dominantAmplitude,
            double spectralCentroid,
            double spectralRms,
            double lowBandEnergy,
            double midBandEnergy,
            double highBandEnergy
        ) {
            this.dominantFrequency = dominantFrequency;
            this.dominantAmplitude = dominantAmplitude;
            this.spectralCentroid = spectralCentroid;
            this.spectralRms = spectralRms;
            this.lowBandEnergy = lowBandEnergy;
            this.midBandEnergy = midBandEnergy;
            this.highBandEnergy = highBandEnergy;
        }
    }
}
