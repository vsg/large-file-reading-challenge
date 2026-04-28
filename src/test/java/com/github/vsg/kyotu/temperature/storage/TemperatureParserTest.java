package com.github.vsg.kyotu.temperature.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TemperatureParserTest {

    @Test
    void shouldParseTypicalTemperature() {
        for (int i = -9999; i <= 9999; i++) {
            double temperature = i / 100.0;
            assertThat(parseStringBytes(String.valueOf(temperature))).isEqualTo(temperature);
        }
    }
    
    @Test
    void shouldThrow_whenInvalidNumber() {
        assertThatThrownBy(() -> parseStringBytes("")).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> parseStringBytes(".")).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> parseStringBytes("..")).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> parseStringBytes("-.")).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> parseStringBytes("abc")).isInstanceOf(NumberFormatException.class);
    }

    @Test
    void shouldParseLongNumbers() {
        assertThat(parseStringBytes("0.00005")).isEqualTo(0.00005);
        assertThat(parseStringBytes("-0.00005")).isEqualTo(-0.00005);
        assertThat(parseStringBytes("10002.3")).isEqualTo(10002.3);
        assertThat(parseStringBytes("-10002.3")).isEqualTo(-10002.3);
    }
    
    @Test
    void shouldParseIntegerNumbers() {
        assertThat(parseStringBytes("0")).isEqualTo(0.0);
        assertThat(parseStringBytes("1")).isEqualTo(1.0);
        assertThat(parseStringBytes("-1")).isEqualTo(-1.0);
        assertThat(parseStringBytes("10001")).isEqualTo(10001.0);
        assertThat(parseStringBytes("-10001")).isEqualTo(-10001.0);
    }
    
    @Test
    void shouldParseZero() {
        assertThat(parseStringBytes("0")).isEqualTo(0.0);
        assertThat(parseStringBytes("+0")).isEqualTo(0.0);
        assertThat(parseStringBytes("-0")).isEqualTo(0.0);
        assertThat(parseStringBytes("0.0")).isEqualTo(0.0);
        assertThat(parseStringBytes("+0.0")).isEqualTo(0.0);
        assertThat(parseStringBytes("-0.0")).isEqualTo(0.0);
    }
    
    @Test
    void shouldParseStrangeNumbers() {
        assertThat(parseStringBytes("Infinity")).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(parseStringBytes("+Infinity")).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(parseStringBytes("-Infinity")).isEqualTo(Double.NEGATIVE_INFINITY);
        assertThat(parseStringBytes("NaN")).isNaN();
        assertThat(parseStringBytes("+NaN")).isNaN();
        assertThat(parseStringBytes("-NaN")).isNaN();
    }
    
    private static double parseStringBytes(String str) {
        return TemperatureParser.parse(str.getBytes(), 0, str.length());
    }
    
}
