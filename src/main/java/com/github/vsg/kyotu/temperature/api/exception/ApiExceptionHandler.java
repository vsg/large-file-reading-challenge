package com.github.vsg.kyotu.temperature.api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.github.vsg.kyotu.temperature.exception.CityNotFoundException;
import com.github.vsg.kyotu.temperature.storage.exception.DataNotAvailableException;
import com.github.vsg.kyotu.temperature.storage.exception.InvalidDataFormatException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    
    @ExceptionHandler(CityNotFoundException.class)
    public ProblemDetail handleCityNotFound(CityNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
    
    @ExceptionHandler(DataNotAvailableException.class)
    public ProblemDetail handleDataNotAvailable(DataNotAvailableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Temporarily unavailable");
    }

    @ExceptionHandler(InvalidDataFormatException.class)
    public ProblemDetail handleInvalidDataFormat(InvalidDataFormatException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Temporarily unavailable");
    }
    
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }
    
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");
    }
    
}
