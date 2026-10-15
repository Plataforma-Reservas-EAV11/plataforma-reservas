package com.udea.reservas.plataformareservas.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.udea.reservas.plataformareservas.auth.dto.UserRegistrationRequest;
import com.udea.reservas.plataformareservas.auth.exception.EmailAlreadyRegisteredException;
import com.udea.reservas.plataformareservas.auth.exception.PasswordMismatchException;
import com.udea.reservas.plataformareservas.usuarios.model.User;
import com.udea.reservas.plataformareservas.usuarios.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void registerStoresNormalizedEmailAndEncodedPassword() {
        UserRegistrationRequest registrationRequest = new UserRegistrationRequest(
            "  Ana Pérez ",
            " ANA@EXAMPLE.COM ",
            "secret",
            "secret"
        );
        when(userRepository.existsByEmailIgnoreCase("ana@example.com"))
            .thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation ->
            invocation.getArgument(0)
        );

        userService.register(registrationRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("Ana Pérez", savedUser.getName());
        assertEquals("ana@example.com", savedUser.getEmail());
        assertEquals("hashed-secret", savedUser.getPasswordHash());
        assertEquals("usuario", savedUser.getRole());
    }

    @Test
    void registerRejectsAnAlreadyRegisteredEmail() {
        when(userRepository.existsByEmailIgnoreCase("ana@example.com"))
            .thenReturn(true);

        assertThrows(
            EmailAlreadyRegisteredException.class,
            () ->
                userService.register(
                    new UserRegistrationRequest(
                        "Ana",
                        "ana@example.com",
                        "secret",
                        "secret"
                    )
                )
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerRejectsDifferentPasswordsBeforeRepositoryAccess() {
        assertThrows(
            PasswordMismatchException.class,
            () ->
                userService.register(
                    new UserRegistrationRequest(
                        "Ana",
                        "ana@example.com",
                        "secret",
                        "different"
                    )
                )
        );

        verify(userRepository, never()).existsByEmailIgnoreCase(any());
        verify(userRepository, never()).save(any(User.class));
    }
}
