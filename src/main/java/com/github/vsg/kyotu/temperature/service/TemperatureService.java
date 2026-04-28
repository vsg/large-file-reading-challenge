package com.github.vsg.kyotu.temperature.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.github.vsg.kyotu.temperature.domain.YearlyAverage;
import com.github.vsg.kyotu.temperature.exception.CityNotFoundException;
import com.github.vsg.kyotu.temperature.storage.TemperatureRepository;

@Service
public class TemperatureService {

    private final TemperatureRepository temperatureRepository;
    
    public TemperatureService(TemperatureRepository temperatureRepository) {
        this.temperatureRepository = temperatureRepository;
    }

    public List<YearlyAverage> getYearlyAverages(String city) {
        return temperatureRepository.findYearlyAveragesByCity(city)
                .orElseThrow(() -> new CityNotFoundException(city));
    }

}
