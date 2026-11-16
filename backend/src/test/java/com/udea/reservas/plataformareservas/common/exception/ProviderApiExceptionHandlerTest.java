package com.udea.reservas.plataformareservas.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.udea.reservas.plataformareservas.common.ApiErrorResponse;
import com.udea.reservas.plataformareservas.common.dto.ApiResponseDto;
import com.udea.reservas.plataformareservas.proveedores.controller.ProviderController;
import com.udea.reservas.plataformareservas.servicios.controller.ServiceCatalogController;
import com.udea.reservas.plataformareservas.servicios.controller.ServicioController;
import com.udea.reservas.plataformareservas.servicios.dto.validation.StrictIntegerDeserializer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.HandlerMethod;

class ProviderApiExceptionHandlerTest {

    private final ProviderApiExceptionHandler handler =
        new ProviderApiExceptionHandler(new GlobalExceptionHandler());

    @Test
    void catalogValidationRetainsGlobalContract() throws Exception {
        HandlerMethod method = catalogMethod();
        var binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "id", "inválido"));
        var exception = new MethodArgumentNotValidException(new MethodParameter(method.getMethod(), 0), binding);

        var response = handler.handleValidation(exception, method);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(
            ApiResponseDto.validationFailure("Hay campos obligatorios o inválidos.", java.util.Map.of("id", "inválido"))
        );
    }

    @Test
    void catalogMalformedRequestRetainsGlobalContract() throws Exception {
        var exception = new HttpMessageNotReadableException("invalid", mock(HttpInputMessage.class));

        var response = handler.handleMalformed(exception, catalogMethod());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(body -> {
            assertThat(body.errorCode()).isEqualTo("MALFORMED_REQUEST");
            assertThat(body.message()).isEqualTo("El cuerpo de la solicitud no es un JSON válido.");
            assertThat(body.details()).isEmpty();
            assertThat(body.traceId()).isNotBlank();
        });
    }

    @Test
    void integerDeserializationErrorOnlyChangesServiceValidation() throws Exception {
        var exception = new HttpMessageNotReadableException(
            StrictIntegerDeserializer.ERROR_MESSAGE, mock(HttpInputMessage.class)
        );
        var serviceMethod = new HandlerMethod(new ServicioController(null), "crearServicio",
            com.udea.reservas.plataformareservas.auth.service.UserPrincipal.class,
            com.udea.reservas.plataformareservas.servicios.dto.CrearServicioRequest.class);
        var providerMethod = new HandlerMethod(new ProviderController(null, null), "listProviders", int.class, int.class);

        ApiErrorResponse serviceError = handler.handleMalformed(exception, serviceMethod).getBody();
        ApiErrorResponse providerError = handler.handleMalformed(exception, providerMethod).getBody();

        assertThat(serviceError.errorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(serviceError.message()).isEqualTo("Datos inválidos.");
        assertThat(serviceError.details()).isEqualTo(List.of(StrictIntegerDeserializer.ERROR_MESSAGE));
        assertThat(providerError.errorCode()).isEqualTo("MALFORMED_REQUEST");
        assertThat(providerError.message()).isEqualTo("El cuerpo de la solicitud no es un JSON válido.");
        assertThat(providerError.details()).isEmpty();
    }

    private static HandlerMethod catalogMethod() throws NoSuchMethodException {
        return new HandlerMethod(new ServiceCatalogController(null), "listAvailableSlots", Integer.class);
    }
}
