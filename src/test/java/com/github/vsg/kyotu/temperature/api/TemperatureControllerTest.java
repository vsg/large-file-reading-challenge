package com.github.vsg.kyotu.temperature.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.github.vsg.kyotu.temperature.TemperatureApplication.YearlyAverageDto;
import com.github.vsg.kyotu.temperature.TemperatureDataLoader;
import com.github.vsg.kyotu.temperature.exception.InvalidDataFormatException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient 
class TemperatureControllerTest {

    @Autowired
    RestTestClient restTestClient;
    
    @MockitoBean
    TemperatureDataLoader dataLoader;

    @Test
    void shouldReturnTemperatures() throws Exception {
        Map<String, Map<String, Double>> data = Map.of("Warszawa", Map.of("2024", 11.1, "2025", 12.2));
        
        when(dataLoader.loadCityYearAverages(any())).thenReturn(data);
        
        restTestClient.get()
                .uri("/temperature/Warszawa")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<YearlyAverageDto>>() {})
                .isEqualTo(List.of(
                        new YearlyAverageDto("2024", 11.1),
                        new YearlyAverageDto("2025", 12.2)));
    }
    
    @Test
    void shouldReturnTemperaturesRounded() throws Exception {
        Map<String, Map<String, Double>> data = Map.of("Warszawa", Map.of("2025", 12.1234));
        
        when(dataLoader.loadCityYearAverages(any())).thenReturn(data);
        
        restTestClient.get()
                .uri("/temperature/Warszawa")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<YearlyAverageDto>>() {})
                .isEqualTo(List.of(
                        new YearlyAverageDto("2025", 12.1)));
    }
    
    @Test
    void shouldReturnTemperaturesSorted() throws Exception {
        Map<String, Map<String, Double>> data = Map.of("Warszawa", treeMapOf("2025", 12.2, "2024", 11.1));
        
        when(dataLoader.loadCityYearAverages(any())).thenReturn(data);
        
        restTestClient.get()
                .uri("/temperature/Warszawa")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<YearlyAverageDto>>() {})
                .isEqualTo(List.of(
                        new YearlyAverageDto("2024", 11.1),
                        new YearlyAverageDto("2025", 12.2)));
    }
    
    @Test
    void shouldHandleCityNotFound() throws Exception {
        Map<String, Map<String, Double>> data = Map.of();
        
        when(dataLoader.loadCityYearAverages(any())).thenReturn(data);
        
        restTestClient.get()
                .uri("/temperature/Warszawa")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ProblemDetail.class)
                .value(pd -> assertThat(pd.getDetail()).isEqualTo("City not found: Warszawa"));
    }
    
    @Test
    void shouldHandleInvalidDataFormat() throws Exception {
        when(dataLoader.loadCityYearAverages(any())).thenThrow(new InvalidDataFormatException("Invalid data format", null));
        
        restTestClient.get()
                .uri("/temperature/Warszawa")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody(ProblemDetail.class)
                .value(pd -> assertThat(pd.getDetail()).isNotBlank());
    }
    
    @Test
    void shouldHandleHttpNotFound() throws Exception {
        restTestClient.get()
                .uri("/noexistent/path")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ProblemDetail.class)
                .value(pd -> assertThat(pd.getDetail()).isNotBlank());
    }
    
    @Test
    void shouldHandleUnexpectedError() throws Exception {
        when(dataLoader.loadCityYearAverages(any())).thenThrow(new RuntimeException());
        
        restTestClient.get()
                .uri("/temperature/Warszawa")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ProblemDetail.class)
                .value(pd -> assertThat(pd.getDetail()).isNotBlank());
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static <K, V> TreeMap<K, V> treeMapOf(Object... args) {
        TreeMap result = new TreeMap();
        for (int i = 0; i < args.length; i += 2) {
            result.put(args[i], args[i+1]);
        }
        return result;
    }
}
