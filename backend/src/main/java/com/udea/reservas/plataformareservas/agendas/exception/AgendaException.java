package com.udea.reservas.plataformareservas.agendas.exception;

import org.springframework.http.HttpStatus;

public class AgendaException extends RuntimeException {

    private final HttpStatus status;

    public AgendaException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
