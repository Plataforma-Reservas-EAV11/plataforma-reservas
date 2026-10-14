package com.udea.reservas.plataformareservas.proveedores.service;

import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.proveedores.dto.ProviderRegistrationRequest;
import com.udea.reservas.plataformareservas.proveedores.dto.ProviderRegistrationResponse;
import com.udea.reservas.plataformareservas.proveedores.model.Proveedor;
import com.udea.reservas.plataformareservas.proveedores.repository.IProveedorRepository;
import com.udea.reservas.plataformareservas.usuarios.model.Usuario;
import com.udea.reservas.plataformareservas.usuarios.repository.IUsuarioRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderRegistrationServiceImpl implements ProviderRegistrationService {

    private static final String PROVIDER_ROLE = "proveedor";

    private final IUsuarioRepository usuarioRepository;
    private final IProveedorRepository proveedorRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ProviderRegistrationResponse register(ProviderRegistrationRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessRuleException(
                "PASSWORD_MISMATCH",
                "Las contraseñas no coinciden.",
                HttpStatus.BAD_REQUEST.value()
            );
        }

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessRuleException(
                "EMAIL_ALREADY_REGISTERED",
                "El correo ya está registrado.",
                HttpStatus.CONFLICT.value()
            );
        }

        Usuario usuario = Usuario.builder()
            .nombre(request.name().trim())
            .email(normalizedEmail)
            .passwordHash(passwordEncoder.encode(request.password()))
            .rol(PROVIDER_ROLE)
            .build();
        Usuario savedUser = usuarioRepository.save(usuario);

        Proveedor proveedor = Proveedor.builder()
            .usuario(savedUser)
            .nombreNegocio(request.businessName())
            .telefono(request.phone())
            .build();
        Proveedor savedProvider = proveedorRepository.save(proveedor);

        log.info(
            "Provider registration completed: userId={}, providerId={}",
            savedUser.getId(),
            savedProvider.getId()
        );

        return new ProviderRegistrationResponse(
            savedProvider.getId(),
            savedUser.getId(),
            savedUser.getNombre(),
            savedUser.getEmail(),
            savedProvider.getNombreNegocio(),
            savedProvider.getTelefono()
        );
    }
}
