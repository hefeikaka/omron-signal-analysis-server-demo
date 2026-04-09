package com.omron.oss.domain.signal.analysis;

final class FastFourierTransform {

    private FastFourierTransform() {
    }

    static void fft(double[] real, double[] imaginary) {
        int n = real.length;
        int j = 0;
        for (int i = 1; i < n; i++) {
            int bit = n >> 1;
            while ((j & bit) != 0) {
                j ^= bit;
                bit >>= 1;
            }
            j ^= bit;
            if (i < j) {
                double tempReal = real[i];
                real[i] = real[j];
                real[j] = tempReal;

                double tempImaginary = imaginary[i];
                imaginary[i] = imaginary[j];
                imaginary[j] = tempImaginary;
            }
        }

        for (int length = 2; length <= n; length <<= 1) {
            double angle = -2.0d * Math.PI / length;
            double wlenReal = Math.cos(angle);
            double wlenImaginary = Math.sin(angle);
            for (int start = 0; start < n; start += length) {
                double wReal = 1.0d;
                double wImaginary = 0.0d;
                for (int offset = 0; offset < length / 2; offset++) {
                    int evenIndex = start + offset;
                    int oddIndex = evenIndex + (length / 2);

                    double oddReal = real[oddIndex] * wReal - imaginary[oddIndex] * wImaginary;
                    double oddImaginary = real[oddIndex] * wImaginary + imaginary[oddIndex] * wReal;

                    real[oddIndex] = real[evenIndex] - oddReal;
                    imaginary[oddIndex] = imaginary[evenIndex] - oddImaginary;
                    real[evenIndex] += oddReal;
                    imaginary[evenIndex] += oddImaginary;

                    double nextWReal = wReal * wlenReal - wImaginary * wlenImaginary;
                    double nextWImaginary = wReal * wlenImaginary + wImaginary * wlenReal;
                    wReal = nextWReal;
                    wImaginary = nextWImaginary;
                }
            }
        }
    }
}
