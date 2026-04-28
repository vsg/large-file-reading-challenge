package com.github.vsg.kyotu.temperature.storage;

import static com.github.vsg.kyotu.temperature.util.TestUtils.resourceFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.vsg.kyotu.temperature.storage.exception.InvalidDataFormatException;

class TemperatureDataLoaderTest {

    TemperatureDataLoader dataLoader = new TemperatureDataLoader();
    
    @Test
    void shouldLoadExampleData() throws Exception {
        Map<String, Map<Integer, Double>> data = dataLoader.loadCityYearAverages(resourceFile("example_file.csv"));
        
        assertThat(data.get("Warszawa").get(2018)).isEqualTo(13.524711538461535);
        assertThat(data.get("Kraków").get(2018)).isEqualTo(14.077788461538459);
    }
    
    @Test
    void shouldCalculateAverageByYear() throws Exception {
        Map<String, Map<Integer, Double>> data = dataLoader.loadCityYearAverages(resourceFile("simple.csv"));
        
        assertThat(data).isEqualTo(Map.of(
                "Warszawa", Map.of(2018, 9.97, 2019, 1.45),
                "Kraków", Map.of(2018, 33.435, 2019, 30.67)));
    }
    
    @Test
    void shouldThrowInvalidDataFormatException_whenInvalidData() throws Exception {
        assertThatThrownBy(() -> {
            dataLoader.loadCityYearAverages(resourceFile("invalid.csv"));
        }).isInstanceOf(InvalidDataFormatException.class);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"windows-eol.csv", "mac-eol.csv", "unix-eol.csv"})
    void shouldLoadDifferentEol(String dataPath) throws Exception {
        Map<String, Map<Integer, Double>> data = dataLoader.loadCityYearAverages(resourceFile(dataPath));
        
        assertThat(data.get("Warszawa").get(2018)).isEqualTo(9.97);
        assertThat(data.get("Kraków").get(2018)).isEqualTo(37.21);
    }
    
}
