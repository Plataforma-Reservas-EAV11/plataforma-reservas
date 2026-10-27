package com.udea.reservas.plataformareservas.auth.config;

import com.udea.reservas.plataformareservas.common.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Respuestas JSON de los filtros de seguridad (que se ejecutan antes de los
 * controladores y por eso no pasan por el GlobalExceptionHandler):
 * 401 cuando falta la sesión y 403 cuando el token CSRF no es válido.
 */
@Component
@RequiredArgsConstructor
public class JsonSecurityErrorHandler
    implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {
        write(
            response,
            HttpStatus.UNAUTHORIZED,
            "UNAUTHENTICATED",
            "Debes iniciar sesión para realizar esta operación."
        );
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException exception
    ) throws IOException {
        write(
            response,
            HttpStatus.FORBIDDEN,
            "CSRF_INVALID",
            "La solicitud no pudo validarse."
        );
    }

    private void write(
        HttpServletResponse response,
        HttpStatus status,
        String errorCode,
        String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
            response.getOutputStream(),
            ApiErrorResponse.of(errorCode, message, List.of())
        );
    }
}
