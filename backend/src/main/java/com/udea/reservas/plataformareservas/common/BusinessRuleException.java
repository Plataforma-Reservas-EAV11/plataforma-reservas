package com.udea.reservas.plataformareservas.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessRuleException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public BusinessRuleException(String errorCode, String message, int status) {
        super(message);
        this.errorCode = errorCode;
        this.status = HttpStatus.valueOf(status);
    }
}
