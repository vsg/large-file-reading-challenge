package com.github.vsg.kyotu.temperature.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.github.vsg.kyotu.temperature.api.dto.YearlyAverageDto;
import com.github.vsg.kyotu.temperature.api.dto.YearlyAverageDtoMapper;
import com.github.vsg.kyotu.temperature.service.TemperatureService;

@RestController
public class TemperatureController {

    private final TemperatureService temperatureService;
    
    public TemperatureController(TemperatureService temperatureService) {
        this.temperatureService = temperatureService;
    }

    @GetMapping("/temperature/{city}")
    public List<YearlyAverageDto> getTemperatureYearlyAverages(@PathVariable String city) {
        return temperatureService.getYearlyAverages(city).stream()
                .map(YearlyAverageDtoMapper::toDto)
                .toList();
    }
    
}
