package com.github.vsg.kyotu.temperature.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.github.vsg.kyotu.temperature.storage.exception.InvalidDataFormatException;

@Component
public class TemperatureDataWatchdog {

    private static final Logger log = LoggerFactory.getLogger(TemperatureDataWatchdog.class);
    
    private final TemperatureRepository repository;
    private final TemperatureDataLoader dataLoader;
    private final Path dataPath;
    
    private long dataTimestamp = -1;
    
    public TemperatureDataWatchdog(TemperatureRepository repository, TemperatureDataLoader dataLoader,
            @Value("${temperature.data.file}") Path dataPath) {
        this.repository = repository;
        this.dataLoader = dataLoader;
        this.dataPath = dataPath;
    }
    
    @Scheduled(fixedDelayString = "${temperature.data.reload-interval-ms}")
    public void checkAndLoad() {
        try {
            var lastModified = Files.getLastModifiedTime(dataPath);
            if (dataTimestamp == lastModified.toMillis()) {
                log.trace("No changes");
                return;
            }
            dataTimestamp = lastModified.toMillis();
            
            var data = loadCityYearAverages(dataPath);
            
            repository.setCityYearlyAverages(data);
        } catch (InvalidDataFormatException e) {
            log.error(e.getMessage());
        } catch (IOException e) {
            log.error("{}: {}", e.getClass(), e.getMessage());
        } catch (Exception e) {
            log.error("", e);
        }
    }

    private Map<String, Map<Integer, Double>> loadCityYearAverages(Path path) throws IOException {
        if (Files.notExists(path)) {
            log.info("Data file not found: {}", path);
            return null;
        }
        
        log.info("Reloading dataset from {}", path);
        
        var begin = System.currentTimeMillis();
        
        var data = dataLoader.loadCityYearAverages(path);
        
        var end = System.currentTimeMillis();
        
        log.info("Loaded in {} ms", end - begin);
        
        return data;
    }
    
}