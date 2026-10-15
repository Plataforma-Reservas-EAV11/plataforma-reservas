package com.udea.reservas.plataformareservas.auth.config;

import com.udea.reservas.plataformareservas.common.ApiErrorResponse;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfiguration {

    @Bean
    public AuthenticationManager authenticationManager(
        UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(
            userDetailsService
        );
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain authenticationFilterChain(
        HttpSecurity http,
        SecurityContextRepository securityContextRepository,
        ObjectMapper objectMapper
    ) {
        AccessDeniedHandler csrfErrorHandler = (request, response, exception) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(
                response.getOutputStream(),
                ApiErrorResponse.of(
                    "CSRF_INVALID",
                    "La solicitud no pudo validarse.",
                    List.of()
                )
            );
        };

        http
            .securityMatcher("/api/v1/auth/login", "/api/v1/auth/csrf")
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
            .csrf(Customizer.withDefaults())
            .exceptionHandling(exceptionHandling ->
                exceptionHandling.accessDeniedHandler(csrfErrorHandler)
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
