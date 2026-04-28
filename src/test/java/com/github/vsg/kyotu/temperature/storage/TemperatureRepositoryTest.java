package com.github.vsg.kyotu.temperature.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.vsg.kyotu.temperature.domain.YearlyAverage;
import com.github.vsg.kyotu.temperature.storage.exception.DataNotAvailableException;

class TemperatureRepositoryTest {

    TemperatureRepository repository = new TemperatureRepository();
    
    @Test
    void shouldThrowDataNotAvailableException_whenEmpty() {
        assertThatThrownBy(() -> {
            repository.findYearlyAveragesByCity("Warszawa");
        }).isInstanceOf(DataNotAvailableException.class);
    }
    
    @Test
    void shouldReturnYearlyAverages() {
        var data = Map.of("Warszawa", Map.of(
                2024, 11.1, 
                2025, 12.2));
        
        repository.setCityYearlyAverages(data);
        
        assertThat(repository.findYearlyAveragesByCity("Warszawa"))
                .isEqualTo(Optional.of(List.of(
                        new YearlyAverage(2024, 11.1),
                        new YearlyAverage(2025, 12.2))));
    }
    
    @Test
    void shouldReturnYearlyAveragesSorted() {
        var data = Map.of("Warszawa", Map.of(
                2025, 12.2, 
                2024, 11.1));
        
        repository.setCityYearlyAverages(data);
        
        assertThat(repository.findYearlyAveragesByCity("Warszawa"))
                .isEqualTo(Optional.of(List.of(
                        new YearlyAverage(2024, 11.1),
                        new YearlyAverage(2025, 12.2))));
    }
    
}
