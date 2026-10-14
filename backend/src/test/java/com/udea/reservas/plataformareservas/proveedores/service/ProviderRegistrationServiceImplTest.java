package com.udea.reservas.plataformareservas.proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.proveedores.dto.ProviderRegistrationRequest;
import com.udea.reservas.plataformareservas.proveedores.dto.ProviderRegistrationResponse;
import com.udea.reservas.plataformareservas.proveedores.model.Proveedor;
import com.udea.reservas.plataformareservas.proveedores.repository.IProveedorRepository;
import com.udea.reservas.plataformareservas.usuarios.model.Usuario;
import com.udea.reservas.plataformareservas.usuarios.repository.IUsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ProviderRegistrationServiceImplTest {

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private IProveedorRepository proveedorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProviderRegistrationServiceImpl registrationService;

    @Test
    @DisplayName("Registra proveedor, usuario y contraseña cifrada correctamente")
    void registerValidRequestSavesUserAndProvider() {
        // Arrange
        ProviderRegistrationRequest request = validRequest(
            "  Ana Pérez  ",
            "  ANA@EXAMPLE.COM ",
            "MiNegocio"
        );
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("clave-segura")).thenReturn("bcrypt-hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(31);
            return usuario;
        });
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(invocation -> {
            Proveedor proveedor = invocation.getArgument(0);
            proveedor.setId(47);
            return proveedor;
        });

        // Act
        ProviderRegistrationResponse response = registrationService.register(request);

        // Assert
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        ArgumentCaptor<Proveedor> proveedorCaptor = ArgumentCaptor.forClass(Proveedor.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        verify(proveedorRepository).save(proveedorCaptor.capture());
        Usuario savedUser = usuarioCaptor.getValue();
        Proveedor savedProvider = proveedorCaptor.getValue();

        assertThat(savedUser.getNombre()).isEqualTo("Ana Pérez");
        assertThat(savedUser.getEmail()).isEqualTo("ana@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(savedUser.getRol()).isEqualTo("proveedor");
        assertThat(savedProvider.getUsuario()).isSameAs(savedUser);
        assertThat(savedProvider.getNombreNegocio()).isEqualTo("MiNegocio");
        assertThat(response).isEqualTo(
            new ProviderRegistrationResponse(
                47,
                31,
                "Ana Pérez",
                "ana@example.com",
                "MiNegocio",
                "3001234567"
            )
        );
        InOrder saveOrder = inOrder(usuarioRepository, proveedorRepository);
        saveOrder.verify(usuarioRepository).save(savedUser);
        saveOrder.verify(proveedorRepository).save(savedProvider);
    }

    @Test
    @DisplayName("Rechaza el correo registrado sin guardar entidades")
    void registerDuplicateEmailDoesNotSaveEntities() {
        // Arrange
        ProviderRegistrationRequest request = validRequest(
            "Ana Pérez",
            "ANA@example.com",
            "MiNegocio"
        );
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(true);

        // Act and Assert
        assertThatThrownBy(() -> registrationService.register(request))
            .isInstanceOf(BusinessRuleException.class)
            .satisfies(exception -> {
                BusinessRuleException businessException = (BusinessRuleException) exception;
                assertThat(businessException.getErrorCode())
                    .isEqualTo("EMAIL_ALREADY_REGISTERED");
                assertThat(businessException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            });
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verifyNoInteractions(proveedorRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Rechaza contraseñas distintas antes de consultar repositorios")
    void registerPasswordMismatchDoesNotInteractWithRepositories() {
        // Arrange
        ProviderRegistrationRequest request = new ProviderRegistrationRequest(
            "Ana Pérez",
            "MiNegocio",
            "ana@example.com",
            "clave-segura",
            "otra-clave",
            "3001234567"
        );

        // Act and Assert
        assertThatThrownBy(() -> registrationService.register(request))
            .isInstanceOf(BusinessRuleException.class)
            .satisfies(exception -> {
                BusinessRuleException businessException = (BusinessRuleException) exception;
                assertThat(businessException.getErrorCode()).isEqualTo("PASSWORD_MISMATCH");
                assertThat(businessException.getMessage())
                    .isEqualTo("Las contraseñas no coinciden.");
                assertThat(businessException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            });
        verifyNoInteractions(usuarioRepository, proveedorRepository, passwordEncoder);
    }

    private ProviderRegistrationRequest validRequest(
        String name,
        String email,
        String businessName
    ) {
        return new ProviderRegistrationRequest(
            name,
            businessName,
            email,
            "clave-segura",
            "clave-segura",
            "3001234567"
        );
    }
}
