package com.assignment.spring.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CityNotFoundException extends RuntimeException {
    private String city;

    public CityNotFoundException(String city) {
        this.city = city;
    }
}
