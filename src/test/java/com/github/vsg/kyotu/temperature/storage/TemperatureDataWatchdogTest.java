package com.github.vsg.kyotu.temperature.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import com.github.vsg.kyotu.temperature.domain.YearlyAverage;

// Cannot properly delete mapped temporary files on Windows.
// https://bugs.java.com/bugdatabase/JDK-4715154
@DisabledOnOs(OS.WINDOWS)
class TemperatureDataWatchdogTest {

    @TempDir
    Path tempDir;
    
    Path testFile;
    
    TemperatureRepository repository = new TemperatureRepository();
    
    TemperatureDataLoader dataLoader = new TemperatureDataLoader();
    
    TemperatureDataWatchdog watchdog;

    @BeforeEach
    void init() throws Exception {
        testFile = Files.createTempFile(tempDir, "test", ".csv");
        
        Files.write(testFile, List.of(
                "Warszawa;2001-01-01 00:00:00.000;11.1", 
                "Warszawa;2002-02-02 00:00:00.000;22.2"));
        
        watchdog = new TemperatureDataWatchdog(repository, dataLoader, testFile);
    }

    @Test
    void shouldLoadIntoRepository() throws Exception {
        assertThat(repository.getCityYearlyAverages()).isNull();
        
        watchdog.checkAndLoad();
        
        assertThat(repository.getCityYearlyAverages()).isNotNull();
    }
    
    @Test
    void shouldReload_whenFileChanged() throws Exception {
        Path testFile = Files.createTempFile(tempDir, "test", ".csv");
        TemperatureDataWatchdog watchdog = new TemperatureDataWatchdog(repository, dataLoader, testFile);
        
        Files.writeString(testFile, "Warszawa;2001-01-01 00:00:00.000;11.1");
        var expectedResult1 = List.of(new YearlyAverage(2001, 11.1));
        watchdog.checkAndLoad();
        assertThat(repository.findYearlyAveragesByCity("Warszawa")).isEqualTo(Optional.of(expectedResult1));

        Files.writeString(testFile, "Warszawa;2002-02-02 00:00:00.000;22.2");
        var expectedResult2 = List.of(new YearlyAverage(2002, 22.2));
        watchdog.checkAndLoad();
        assertThat(repository.findYearlyAveragesByCity("Warszawa")).isEqualTo(Optional.of(expectedResult2));
    }
    
    @Test
    void shouldNotReload_whenFileNotChanged() throws Exception {
        watchdog.checkAndLoad();
        var averages1 = repository.getCityYearlyAverages();
        
        watchdog.checkAndLoad();
        var averages2 = repository.getCityYearlyAverages();
        
        assertThat(averages1).isSameAs(averages2);
    }
    
    @Test
    void shouldNotReload_whenFileRemoved() throws Exception {
        Path testFile = Files.createTempFile("test", ".csv");
        
        watchdog.checkAndLoad();
        assertThat(repository.getCityYearlyAverages()).isNotNull();

        Files.delete(testFile);
        
        watchdog.checkAndLoad();
        assertThat(repository.getCityYearlyAverages()).isNotNull();
    }
    
    @Test
    void shouldNotThrow_whenFileNotExists() throws Exception {
        TemperatureDataWatchdog watchdog = new TemperatureDataWatchdog(repository, dataLoader, Path.of("does-not-exist.csv"));
        watchdog.checkAndLoad();
        
        assertThat(repository.getCityYearlyAverages()).isNull();
    }
    
}
