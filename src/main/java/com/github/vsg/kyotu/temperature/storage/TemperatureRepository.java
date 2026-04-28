package com.github.vsg.kyotu.temperature.storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.github.vsg.kyotu.temperature.domain.YearlyAverage;
import com.github.vsg.kyotu.temperature.storage.exception.DataNotAvailableException;

@Repository
public class TemperatureRepository {

    private volatile Map<String, List<YearlyAverage>> cityYearlyAverages;
    
    Map<String, List<YearlyAverage>> getCityYearlyAverages() {
        return cityYearlyAverages;
    }

    void setCityYearlyAverages(Map<String, Map<Integer, Double>> cityYearlyAverages) {
        this.cityYearlyAverages = cityYearlyAverages.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, 
                        e -> e.getValue().entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(x -> new YearlyAverage(x.getKey(), x.getValue()))
                            .toList()));
    }

    public Optional<List<YearlyAverage>> findYearlyAveragesByCity(String city) {
        var data = this.cityYearlyAverages;
        if (data == null) {
            throw new DataNotAvailableException("Temperature data not available");
        }
        return Optional.ofNullable(data.get(city));
    }
    
}
