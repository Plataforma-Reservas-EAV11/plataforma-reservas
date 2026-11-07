package com.udea.reservas.plataformareservas.recursos.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservas.plataformareservas.TestAuthentication;
import com.udea.reservas.plataformareservas.auth.service.UserPrincipal;
import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.common.exception.GlobalExceptionHandler;
import com.udea.reservas.plataformareservas.common.exception.ProviderApiExceptionHandler;
import com.udea.reservas.plataformareservas.recursos.dto.GuardarRecursoRequest;
import com.udea.reservas.plataformareservas.recursos.dto.RecursoResponse;
import com.udea.reservas.plataformareservas.recursos.service.RecursoService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RecursoControllerTest {

    private static final String RUTA = "/api/v1/providers/me/resources";

    @Mock
    private RecursoService recursoService;

    @InjectMocks
    private RecursoController recursoController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(recursoController)
            .setControllerAdvice(new ProviderApiExceptionHandler(new GlobalExceptionHandler()))
            .setCustomArgumentResolvers(TestAuthentication.argumentResolver())
            .build();
        objectMapper = new ObjectMapper();
        TestAuthentication.authenticateAs(
            new UserPrincipal(7, "Proveedor", "proveedor@example.com", "proveedor", "hash", true)
        );
    }

    @Test
    @DisplayName("Registra un recurso con el usuario autenticado")
    void crearRecurso() throws Exception {
        GuardarRecursoRequest request = new GuardarRecursoRequest("Consultorio", "sala");
        when(recursoService.crearRecurso(7, request))
            .thenReturn(new RecursoResponse(41, "Consultorio", "sala", true));

        mockMvc.perform(
            post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").value(41))
            .andExpect(jsonPath("$.data.activo").value(true));

        verify(recursoService).crearRecurso(7, request);
    }

    @Test
    @DisplayName("Lista recursos del proveedor autenticado")
    void listarRecursos() throws Exception {
        when(recursoService.listarRecursos(7)).thenReturn(
            List.of(new RecursoResponse(41, "Consultorio", "sala", true))
        );

        mockMvc.perform(get(RUTA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(41));

        verify(recursoService).listarRecursos(7);
    }

    @Test
    @DisplayName("Consulta el recurso específico del proveedor autenticado")
    void consultarRecurso() throws Exception {
        when(recursoService.consultarRecurso(7, 41))
            .thenReturn(new RecursoResponse(41, "Consultorio", "sala", true));

        mockMvc.perform(get(RUTA + "/41"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nombre").value("Consultorio"));

        verify(recursoService).consultarRecurso(7, 41);
    }

    @Test
    @DisplayName("Actualiza datos de un recurso propio")
    void actualizarRecurso() throws Exception {
        GuardarRecursoRequest request = new GuardarRecursoRequest("Sala 2", "sala");
        when(recursoService.actualizarRecurso(7, 41, request))
            .thenReturn(new RecursoResponse(41, "Sala 2", "sala", true));

        mockMvc.perform(
            put(RUTA + "/41")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nombre").value("Sala 2"));

        verify(recursoService).actualizarRecurso(7, 41, request);
    }

    @Test
    @DisplayName("Desactiva recurso mediante actualización lógica")
    void desactivarRecurso() throws Exception {
        when(recursoService.cambiarEstado(7, 41, false))
            .thenReturn(new RecursoResponse(41, "Consultorio", "sala", false));

        mockMvc.perform(
            patch(RUTA + "/41/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":false}")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.activo").value(false));

        verify(recursoService).cambiarEstado(7, 41, false);
    }

    @Test
    @DisplayName("Asocia y retira un servicio del recurso sin exponer IDs de propietario")
    void gestionarAsociacionServicio() throws Exception {
        mockMvc.perform(put(RUTA + "/41/servicios/51"))
            .andExpect(status().isNoContent());
        mockMvc.perform(delete(RUTA + "/41/servicios/51"))
            .andExpect(status().isNoContent());

        verify(recursoService).asociarServicio(7, 41, 51);
        verify(recursoService).retirarServicio(7, 41, 51);
    }

    @Test
    void activateResourceReturnsActiveMessage() throws Exception {
        when(recursoService.cambiarEstado(7, 41, true))
            .thenReturn(new RecursoResponse(41, "Consultorio", "sala", true));

        mockMvc.perform(patch(RUTA + "/41/estado")
                .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Recurso activado exitosamente."))
            .andExpect(jsonPath("$.data.activo").value(true));
        verify(recursoService).cambiarEstado(7, 41, true);
    }

    @Test
    void invalidResourceBodyReturnsValidationContract() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\" \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("Datos inválidos."))
            .andExpect(jsonPath("$.details").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
        verify(recursoService, never()).crearRecurso(any(), any());
    }

    @Test
    void malformedResourceBodyReturnsMalformedContract() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"))
            .andExpect(jsonPath("$.message").value("El cuerpo de la solicitud no es un JSON válido."))
            .andExpect(jsonPath("$.details").isEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
        verify(recursoService, never()).crearRecurso(any(), any());
    }

    @Test
    void missingResourceReturnsBusinessError() throws Exception {
        when(recursoService.consultarRecurso(7, 99)).thenThrow(
            new BusinessRuleException("RESOURCE_NOT_FOUND", "El recurso no existe.", HttpStatus.NOT_FOUND.value())
        );
        mockMvc.perform(get(RUTA + "/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("El recurso no existe."))
            .andExpect(jsonPath("$.details").isEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }
}
