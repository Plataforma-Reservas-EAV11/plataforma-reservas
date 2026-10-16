package com.udea.reservas.plataformareservas.auth.controller;

import com.udea.reservas.plataformareservas.auth.dto.CsrfTokenResponse;
import com.udea.reservas.plataformareservas.auth.dto.LoginRequest;
import com.udea.reservas.plataformareservas.auth.dto.LoginResponse;
import com.udea.reservas.plataformareservas.auth.dto.UserRegistrationRequest;
import com.udea.reservas.plataformareservas.auth.service.UserService;
import com.udea.reservas.plataformareservas.auth.service.UserPrincipal;
import com.udea.reservas.plataformareservas.common.ApiErrorResponse;
import com.udea.reservas.plataformareservas.common.ApiSuccessResponse;
import com.udea.reservas.plataformareservas.common.dto.ApiResponseDto;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    @GetMapping("/csrf")
    public CsrfTokenResponse csrfToken(CsrfToken csrfToken) {
        return new CsrfTokenResponse(
            csrfToken.getHeaderName(),
            csrfToken.getParameterName(),
            csrfToken.getToken()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiSuccessResponse<LoginResponse>> login(
        @Valid @RequestBody LoginRequest loginRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String normalizedEmail = loginRequest.email()
            .trim()
            .toLowerCase(Locale.ROOT);
        Authentication authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                normalizedEmail,
                loginRequest.password()
            )
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);

        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        securityContextRepository.saveContext(
            securityContext,
            request,
            response
        );
        SecurityContextHolder.setContext(securityContext);

        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException(
                "The authenticated principal is not a UserPrincipal."
            );
        }
        LoginResponse loginResponse = new LoginResponse(
            principal.getId(),
            principal.getName(),
            principal.getEmail()
        );
        return ResponseEntity.ok(
            new ApiSuccessResponse<>(
                "Inicio de sesión exitoso.",
                loginResponse
            )
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
        BadCredentialsException exception
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiErrorResponse.of(
                "INVALID_CREDENTIALS",
                "El correo electrónico o la contraseña no son correctos.",
                List.of()
            )
        );
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDto> register(
        @Valid @RequestBody UserRegistrationRequest registrationRequest
    ) {
        userService.register(registrationRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponseDto.success(
                "Registro exitoso. Tu cuenta fue creada."
            )
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponseDto> handleRegistrationConflict(
        DataIntegrityViolationException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiResponseDto.failure(
                "El correo electrónico ya está registrado."
            )
        );
    }
}
