package com.udea.reservas.plataformareservas.auth.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;

@ExtendWith(MockitoExtension.class)
class LoginAttemptListenerTest {

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private LoginAttemptListener listener;

    @Test
    void badCredentialsAreRecordedAsFailure() {
        listener.onBadCredentials(
            new AuthenticationFailureBadCredentialsEvent(
                UsernamePasswordAuthenticationToken.unauthenticated("ana@example.com", "x"),
                new BadCredentialsException("x")
            )
        );

        verify(loginAttemptService).recordFailure("ana@example.com");
    }

    @Test
    void successfulLoginClearsTheAttempts() {
        UserPrincipal principal = new UserPrincipal(7, "Ana", "ana@example.com", "usuario", "hash", true);

        listener.onSuccess(
            new AuthenticationSuccessEvent(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, java.util.List.of())
            )
        );

        verify(loginAttemptService).recordSuccess("ana@example.com");
    }
}
