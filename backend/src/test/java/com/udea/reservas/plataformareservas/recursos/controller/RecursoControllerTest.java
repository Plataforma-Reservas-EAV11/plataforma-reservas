package com.udea.reservas.plataformareservas.recursos.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservas.plataformareservas.TestAuthentication;
import com.udea.reservas.plataformareservas.auth.service.UserPrincipal;
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
}
