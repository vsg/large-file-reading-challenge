package com.github.vsg.kyotu.temperature.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.github.vsg.kyotu.temperature.api.dto.YearlyAverageDto;
import com.github.vsg.kyotu.temperature.domain.YearlyAverage;
import com.github.vsg.kyotu.temperature.exception.CityNotFoundException;
import com.github.vsg.kyotu.temperature.service.TemperatureService;
import com.github.vsg.kyotu.temperature.storage.exception.DataNotAvailableException;
import com.github.vsg.kyotu.temperature.storage.exception.InvalidDataFormatException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient 
class TemperatureControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    TemperatureService temperatureService;
    
    @Test
    void shouldReturnTemperatures() throws Exception {
        var average1 = new YearlyAverage(2024, 11.1);
        var average2 = new YearlyAverage(2025, 12.2);
        
        when(temperatureService.getYearlyAverages("Warszawa")).thenReturn(List.of(average1, average2));
        
        webTestClient.get()
                .uri("/temperature/Warszawa")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(YearlyAverageDto.class)
                .isEqualTo(List.of(
                        new YearlyAverageDto("2024", 11.1),
                        new YearlyAverageDto("2025", 12.2)));
    }
    
    @Test
    void shouldReturnTemperaturesRounded() throws Exception {
        var average = new YearlyAverage(2025, 12.1234);
        
        when(temperatureService.getYearlyAverages("Warszawa")).thenReturn(List.of(average));
        
        webTestClient.get()
                .uri("/temperature/Warszawa")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(YearlyAverageDto.class)
                .isEqualTo(List.of(new YearlyAverageDto("2025", 12.1)));
    }
    
    @Test
    void shouldHandleCityNotFound() throws Exception {
        when(temperatureService.getYearlyAverages("Warszawa")).thenThrow(new CityNotFoundException("Warszawa"));
        
        webTestClient.get()
                .uri("/temperature/Warszawa")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ProblemDetail.class)
                .value(pd -> assertThat(pd.getDetail()).isEqualTo("City 'Warszawa' not found"));
    }
    
    @Test
    void shouldHandleDataNotAvailable() throws Exception {
        when(temperatureService.getYearlyAverages(any())).thenThrow(new DataNotAvailableException("Data not available"));
        
        webTestClient.get()
                .uri("/temperature/Warszawa")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    @Test
    void shouldHandleInvalidDataFormat() throws Exception {
        when(temperatureService.getYearlyAverages(any())).thenThrow(new InvalidDataFormatException("Invalid data format"));
        
        webTestClient.get()
                .uri("/temperature/Warszawa")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    @Test
    void shouldHandleUnexpectedError() throws Exception {
        when(temperatureService.getYearlyAverages(any())).thenThrow(new RuntimeException());
        
        webTestClient.get()
                .uri("/temperature/Warszawa")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
}
