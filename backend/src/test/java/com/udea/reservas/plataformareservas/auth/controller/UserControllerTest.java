package com.udea.reservas.plataformareservas.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservas.plataformareservas.auth.dto.UserRegistrationRequest;
import com.udea.reservas.plataformareservas.auth.exception.EmailAlreadyRegisteredException;
import com.udea.reservas.plataformareservas.auth.exception.PasswordMismatchException;
import com.udea.reservas.plataformareservas.auth.service.UserService;
import com.udea.reservas.plataformareservas.common.exception.GlobalExceptionHandler;
import com.udea.reservas.plataformareservas.usuarios.model.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void registerReturnsCreatedForValidRequest() throws Exception {
        // Arrange
        UserRegistrationRequest registrationRequest = new UserRegistrationRequest(
            "Ana",
            "ana@example.com",
            "clave-segura",
            "clave-segura"
        );
        when(userService.register(registrationRequest)).thenReturn(new User());

        // Act
        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest))
        )
            // Assert
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value(
                "Registro exitoso. Tu cuenta fue creada."
            ));

        verify(userService).register(registrationRequest);
    }

    @Test
    void registerReturnsFieldErrorsAndDoesNotCallServiceForBlankFields()
        throws Exception {
        // Arrange
        UserRegistrationRequest registrationRequest = new UserRegistrationRequest(
            "",
            "",
            "",
            ""
        );

        // Act
        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest))
        )
            // Assert
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors.name").exists())
            .andExpect(jsonPath("$.errors.email").exists())
            .andExpect(jsonPath("$.errors.password").exists())
            .andExpect(jsonPath("$.errors.confirmPassword").exists());

        verify(userService, never()).register(registrationRequest);
    }

    @Test
    void registerReturnsConflictWhenEmailAlreadyExists() throws Exception {
        // Arrange
        UserRegistrationRequest registrationRequest = new UserRegistrationRequest(
            "Ana",
            "ana@example.com",
            "clave-segura",
            "clave-segura"
        );
        when(userService.register(registrationRequest)).thenThrow(
            new EmailAlreadyRegisteredException(
                "El correo electrónico ya está registrado."
            )
        );

        // Act
        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest))
        )
            // Assert
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(
                "El correo electrónico ya está registrado."
            ));
    }

    @Test
    void registerReturnsBadRequestWhenPasswordsDoNotMatch() throws Exception {
        // Arrange
        UserRegistrationRequest registrationRequest = new UserRegistrationRequest(
            "Ana",
            "ana@example.com",
            "clave-segura",
            "otra-clave"
        );
        when(userService.register(registrationRequest)).thenThrow(
            new PasswordMismatchException("Las contraseñas no coinciden.")
        );

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest))
        )
            // Assert
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Las contraseñas no coinciden."
            ));
    }

    @Test
    void registerRejectsPasswordShorterThanEightCharacters() throws Exception {
        // Arrange
        UserRegistrationRequest registrationRequest = new UserRegistrationRequest(
            "Ana",
            "ana@example.com",
            "corta1",
            "corta1"
        );

        // Act
        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest))
        )
            // Assert
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.password").value(
                "La contraseña debe tener al menos 8 caracteres."
            ));

        verify(userService, never()).register(any());
    }

    @Test
    void registerRejectsPasswordLongerThanSeventyTwoBytes() throws Exception {
        // Arrange
        String password = "ñ".repeat(40);
        UserRegistrationRequest registrationRequest = new UserRegistrationRequest(
            "Ana",
            "ana@example.com",
            password,
            password
        );

        // Act
        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest))
        )
            // Assert
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.password").value(
                "La contraseña es demasiado larga."
            ));

        verify(userService, never()).register(any());
    }

    @Test
    void registerReturnsConflictForDatabaseIntegrityViolation() throws Exception {
        // Arrange
        UserRegistrationRequest registrationRequest = new UserRegistrationRequest(
            "Ana",
            "ana@example.com",
            "clave-segura",
            "clave-segura"
        );
        when(userService.register(registrationRequest)).thenThrow(
            new DataIntegrityViolationException("duplicate email")
        );

        // Act
        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest))
        )
            // Assert
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(
                "El correo electrónico ya está registrado."
            ));
    }
}
