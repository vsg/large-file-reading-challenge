package com.github.vsg.kyotu.temperature.api.dto;

import com.github.vsg.kyotu.temperature.domain.YearlyAverage;

public class YearlyAverageDtoMapper {
    
    public static YearlyAverageDto toDto(YearlyAverage value) {
        return new YearlyAverageDto(String.valueOf(value.year()), round(value.averageTemperature()));
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
    
}
