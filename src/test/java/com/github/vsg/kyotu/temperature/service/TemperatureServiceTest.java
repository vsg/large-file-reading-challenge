package com.github.vsg.kyotu.temperature.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.vsg.kyotu.temperature.domain.YearlyAverage;
import com.github.vsg.kyotu.temperature.exception.CityNotFoundException;
import com.github.vsg.kyotu.temperature.storage.TemperatureRepository;

@ExtendWith(MockitoExtension.class)
class TemperatureServiceTest {

    @InjectMocks
    TemperatureService temperatureService;
    
    @Mock
    TemperatureRepository temperatureRepository;
    
    @Test
    void shouldReturnAnnualAverages_whenCityExists() {
        var result = List.of(new YearlyAverage(2000, 12.3));
        
        when(temperatureRepository.findYearlyAveragesByCity("Warszawa")).thenReturn(Optional.of(result));
        
        assertThat(temperatureService.getYearlyAverages("Warszawa")).isEqualTo(result);
    }
    
    @Test
    void shouldThrowCityNotFoundException_whenCityNotFound() {
        assertThatThrownBy(() -> {
            temperatureService.getYearlyAverages("Unknown");
        }).isInstanceOf(CityNotFoundException.class);
    }
    
}
