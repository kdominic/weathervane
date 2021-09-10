package com.assignment.spring.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenericApiException extends RuntimeException {
    private String errorCode;

    public GenericApiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
