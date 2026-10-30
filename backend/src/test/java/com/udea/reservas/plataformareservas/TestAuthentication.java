package com.udea.reservas.plataformareservas;

import com.udea.reservas.plataformareservas.auth.service.UserPrincipal;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;

/**
 * Apoyo para las pruebas de controladores que reciben el usuario de la sesión
 * con {@code @AuthenticationPrincipal} (DEF-01): en un MockMvc independiente no
 * hay filtros de seguridad, así que se deja la identidad en el contexto y se
 * registra el resolutor del argumento.
 */
public final class TestAuthentication {

    private TestAuthentication() {}

    public static UserPrincipal principal(int id) {
        return new UserPrincipal(
            id,
            "Ana",
            "ana@example.com",
            "usuario",
            "hashed-secret",
            true
        );
    }

    public static void authenticateAs(UserPrincipal principal) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of()
            )
        );
        SecurityContextHolder.setContext(context);
    }

    public static AuthenticationPrincipalArgumentResolver argumentResolver() {
        return new AuthenticationPrincipalArgumentResolver();
    }
}
