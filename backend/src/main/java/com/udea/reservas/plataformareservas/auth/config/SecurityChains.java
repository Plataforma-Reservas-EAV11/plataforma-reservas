package com.udea.reservas.plataformareservas.auth.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Configuración común de las cadenas de filtros de la API: sesión HTTP, CSRF y
 * respuestas JSON de error. Cada cadena solo agrega lo que la distingue (a qué
 * rutas se aplica y quién puede entrar).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SecurityChains {

    private static final Set<String> SAFE_METHODS = Set.of(
        "GET",
        "HEAD",
        "TRACE",
        "OPTIONS"
    );

    /** Mismo criterio que el predeterminado de Spring Security: todo salvo los métodos seguros. */
    static boolean isUnsafeMethod(HttpServletRequest request) {
        return !SAFE_METHODS.contains(request.getMethod());
    }

    /**
     * Exige CSRF solo cuando hay una sesión autenticada que un tercero podría
     * aprovechar. Una solicitud sin sesión no necesita token: recibe 401.
     */
    static boolean isUnsafeMethodWithSession(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return isUnsafeMethod(request) && authentication != null && authentication.isAuthenticated();
    }

    static SecurityFilterChain build(
        HttpSecurity http,
        SecurityContextRepository securityContextRepository,
        JsonSecurityErrorHandler errorHandler,
        RequestMatcher csrfMatcher
    ) {
        http
            .csrf(csrf -> csrf.requireCsrfProtectionMatcher(csrfMatcher))
            .exceptionHandling(exceptionHandling ->
                exceptionHandling
                    .authenticationEntryPoint(errorHandler)
                    .accessDeniedHandler(errorHandler)
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
