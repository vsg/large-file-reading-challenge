package com.github.vsg.kyotu.temperature;

import java.util.Arrays;

public class TemperatureParser {

    // Integer key encodes a string of up to 6 characters from "*+,-./0123456789".
    // Each character is mapped to 0..15 and packed 2 characters per byte.
    private static final double[] TEMPERATURE_LOOKUP = new double[1 << 24];
    static {
        Arrays.fill(TEMPERATURE_LOOKUP, Double.NaN);
        for (int t = 0; t <= 9999; t++) {
            double temperature = t / 100.0;
            String temperatureStr = String.valueOf(temperature);
            addTemperatureLookup(temperatureStr, temperature);
            addTemperatureLookup('+' + temperatureStr, temperature);
            addTemperatureLookup('-' + temperatureStr, -temperature);
        }
    }

    private static void addTemperatureLookup(String temperatureStr, double temperature) {
        int key = 0;
        for (byte ch : temperatureStr.getBytes()) {
            int b = ch - '*';
            if ((b & ~0xf) != 0) {
                throw new IllegalArgumentException(temperatureStr);
            }
            key = (key << 4) | b;
        }
        TEMPERATURE_LOOKUP[key] = temperature;
    }
    
    public static double parse(byte[] array, int begin, int end) {
        if (end - begin <= 6) {
            int key = 0;
            for (int i = begin; i < end; i++) {
                int b = array[i] - '*';
                if ((b & ~0xf) != 0) {
                    key = 0;
                    break;
                }
                key = (key << 4) | b;
            }
            double result = TEMPERATURE_LOOKUP[key];
            if (!Double.isNaN(result)) {
                return result;
            }
        }
        return Double.parseDouble(new String(array, begin, end-begin)); // fallback
    }

}
