package com.udea.reservas.plataformareservas.auth.controller;

import com.udea.reservas.plataformareservas.auth.dto.CsrfTokenResponse;
import com.udea.reservas.plataformareservas.auth.dto.LoginRequest;
import com.udea.reservas.plataformareservas.auth.dto.LoginResponse;
import com.udea.reservas.plataformareservas.auth.dto.UserRegistrationRequest;
import com.udea.reservas.plataformareservas.auth.service.UserService;
import com.udea.reservas.plataformareservas.auth.service.UserPrincipal;
import com.udea.reservas.plataformareservas.common.ApiSuccessResponse;
import com.udea.reservas.plataformareservas.common.dto.ApiResponseDto;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las respuestas de error de este controlador (credenciales incorrectas, cuenta
 * bloqueada y correo duplicado) las construye {@code AuthExceptionHandler}.
 */
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
        // El correo ya llega recortado y en minúsculas (ver LoginRequest).
        Authentication authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                loginRequest.email(),
                loginRequest.password()
            )
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);

        // Protección contra fijación de sesión: el identificador siempre se
        // renueva al autenticarse (getSession(true) garantiza que exista una).
        request.getSession(true);
        request.changeSessionId();

        securityContextRepository.saveContext(
            securityContext,
            request,
            response
        );
        SecurityContextHolder.setContext(securityContext);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        LoginResponse loginResponse = new LoginResponse(
            principal.getId(),
            principal.getName(),
            principal.getEmail(),
            principal.getRole()
        );
        return ResponseEntity.ok(
            new ApiSuccessResponse<>(
                "Inicio de sesión exitoso.",
                loginResponse
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
}
