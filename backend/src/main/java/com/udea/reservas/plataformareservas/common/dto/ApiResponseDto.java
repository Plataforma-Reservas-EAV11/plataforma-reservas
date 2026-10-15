package com.udea.reservas.plataformareservas.common.dto;

import java.util.Map;

public record ApiResponseDto(
    boolean success,
    String message,
    Map<String, String> errors
) {

    public ApiResponseDto {
        errors = Map.copyOf(errors);
    }

    public static ApiResponseDto success(String message) {
        return new ApiResponseDto(true, message, Map.of());
    }

    public static ApiResponseDto failure(String message) {
        return new ApiResponseDto(false, message, Map.of());
    }

    public static ApiResponseDto validationFailure(
        String message,
        Map<String, String> errors
    ) {
        return new ApiResponseDto(false, message, errors);
    }
}
