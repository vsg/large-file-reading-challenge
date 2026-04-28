package com.github.vsg.kyotu.temperature.exception;

public class CityNotFoundException extends RuntimeException {

    private final String city;

    public CityNotFoundException(String city) {
        super(String.format("City '%s' not found", city));
        this.city = city;
    }

    public String getCity() {
        return city;
    }
    
}
