package com.udea.reservas.plataformareservas.auth.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Configuración común de las cadenas de filtros de la API: sesión HTTP, CSRF
 * (protección predeterminada de Spring Security, sin matcher propio) y
 * respuestas JSON de error. Cada cadena solo agrega lo que la distingue (a qué
 * rutas se aplica y quién puede entrar).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SecurityChains {

    /**
     * Para las cadenas que exigen autenticación: un rechazo del filtro CSRF
     * sin usuario autenticado es una solicitud sin sesión (401); con sesión y
     * token inválido o ausente sigue siendo 403.
     */
    static AccessDeniedHandler unauthenticatedAs401(JsonSecurityErrorHandler errorHandler) {
        return (request, response, exception) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
                errorHandler.commence(
                    request,
                    response,
                    new InsufficientAuthenticationException("Sin sesión", exception)
                );
            } else {
                errorHandler.handle(request, response, exception);
            }
        };
    }

    static SecurityFilterChain build(
        HttpSecurity http,
        SecurityContextRepository securityContextRepository,
        JsonSecurityErrorHandler errorHandler,
        AccessDeniedHandler accessDeniedHandler
    ) {
        http
            .csrf(Customizer.withDefaults())
            .exceptionHandling(exceptionHandling ->
                exceptionHandling
                    .authenticationEntryPoint(errorHandler)
                    .accessDeniedHandler(accessDeniedHandler)
            )
            .securityContext(securityContext ->
                securityContext.securityContextRepository(
                    securityContextRepository
                )
            )
            .sessionManagement(sessionManagement ->
                sessionManagement
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .sessionFixation(
                        SessionManagementConfigurer.SessionFixationConfigurer::changeSessionId
                    )
            );
        return http.build();
    }
}
