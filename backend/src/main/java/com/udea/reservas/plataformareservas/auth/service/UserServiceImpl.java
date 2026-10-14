package com.udea.reservas.plataformareservas.auth.service;

import com.udea.reservas.plataformareservas.auth.dto.UserRegistrationRequest;
import com.udea.reservas.plataformareservas.auth.exception.EmailAlreadyRegisteredException;
import com.udea.reservas.plataformareservas.auth.exception.PasswordMismatchException;
import com.udea.reservas.plataformareservas.usuarios.model.User;
import com.udea.reservas.plataformareservas.usuarios.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ROLE = "usuario";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User register(UserRegistrationRequest registrationRequest) {
        if (
            !registrationRequest.password().equals(
                registrationRequest.confirmPassword()
            )
        ) {
            throw new PasswordMismatchException(
                "Las contraseñas no coinciden."
            );
        }

        String normalizedEmail = registrationRequest.email()
            .trim()
            .toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(
                "El correo electrónico ya está registrado."
            );
        }

        User user = User.builder()
            .name(registrationRequest.name().trim())
            .email(normalizedEmail)
            .passwordHash(passwordEncoder.encode(registrationRequest.password()))
            .role(DEFAULT_ROLE)
            .build();

        return userRepository.save(user);
    }
}
