package com.udea.reservas.plataformareservas.common;

public record ApiSuccessResponse<T>(String message, T data) {}
