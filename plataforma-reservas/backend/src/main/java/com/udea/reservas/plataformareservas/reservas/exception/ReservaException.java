package com.udea.reservas.plataformareservas.reservas.exception;

import org.springframework.http.HttpStatus;

public class ReservaException extends RuntimeException {

    private final HttpStatus status;

    public ReservaException(String mensaje, HttpStatus status) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}