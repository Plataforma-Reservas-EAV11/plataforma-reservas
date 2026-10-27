package com.udea.reservas.plataformareservas.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Traduce los eventos de autenticación de Spring Security en el registro de
 * intentos de inicio de sesión. Solo cuenta las contraseñas incorrectas: una
 * cuenta ya bloqueada genera otro evento y no extiende el bloqueo.
 */
@Component
@RequiredArgsConstructor
public class LoginAttemptListener {

    private final LoginAttemptService loginAttemptService;

    @EventListener
    public void onBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
        loginAttemptService.recordFailure(event.getAuthentication().getName());
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        loginAttemptService.recordSuccess(event.getAuthentication().getName());
    }
}
