package com.github.vsg.kyotu.temperature;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.github.vsg.kyotu.temperature.exception.CityNotFoundException;
import com.github.vsg.kyotu.temperature.exception.InvalidDataFormatException;

@RestController
@SpringBootApplication
public class TemperatureApplication {

    private static final Logger log = LoggerFactory.getLogger(TemperatureApplication.class);
    
    public record YearlyAverageDto(String year, double averageTemperature) {

        public YearlyAverageDto {
            averageTemperature = Math.round(averageTemperature * 10.0) / 10.0;
        }
        
    }

    @Value("${temperature.data.file}") 
    private Path dataPath;
    
    @Autowired
    private TemperatureDataLoader loader;
    
    @GetMapping("/temperature/{city}")
    public List<YearlyAverageDto> getTemperatureYearlyAverages(@PathVariable String city) throws IOException {
        long begin = System.currentTimeMillis();
        Map<String, Map<String, Double>> data = loader.loadCityYearAverages(dataPath);
        long end = System.currentTimeMillis();
        log.info("Loaded in: {} ms", end - begin);
        
        return Optional.ofNullable(data.get(city))
                .orElseThrow(() -> new CityNotFoundException("City not found: " + city))
                .entrySet().stream()
                .map(e -> new YearlyAverageDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(YearlyAverageDto::year))
                .toList();
    }

    @ExceptionHandler({CityNotFoundException.class, NoResourceFoundException.class})
    public ProblemDetail handleNotFound(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({InvalidDataFormatException.class, IOException.class})
    public ProblemDetail handleUnavailable(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Temporarily unavailable");
    }
    
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");
    }

    public static void main(String[] args) {
		SpringApplication.run(TemperatureApplication.class, args);
	}
	
}
