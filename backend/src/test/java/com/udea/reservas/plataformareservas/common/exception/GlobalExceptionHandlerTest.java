package com.udea.reservas.plataformareservas.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.udea.reservas.plataformareservas.auth.exception.EmailAlreadyRegisteredException;
import com.udea.reservas.plataformareservas.auth.exception.PasswordMismatchException;
import com.udea.reservas.plataformareservas.common.ApiErrorResponse;
import com.udea.reservas.plataformareservas.common.dto.ApiResponseDto;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Agrupa los errores de validación por campo conservando el primero")
    void validacionDevuelveErroresPorCampo() {
        // Arrange
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
            new FieldError("request", "email", "El correo electrónico es obligatorio."),
            new FieldError("request", "email", "El correo electrónico no es válido."),
            new FieldError("request", "name", "El nombre es obligatorio.")
        ));
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        // Act
        ResponseEntity<ApiResponseDto> response = handler.handleValidationException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().errors())
            .containsEntry("email", "El correo electrónico es obligatorio.")
            .containsEntry("name", "El nombre es obligatorio.")
            .hasSize(2);
    }

    @Test
    @DisplayName("Responde 409 cuando el correo ya está registrado")
    void correoRegistradoDevuelveConflicto() {
        // Arrange
        EmailAlreadyRegisteredException exception =
            new EmailAlreadyRegisteredException("El correo electrónico ya está registrado.");

        // Act
        ResponseEntity<ApiResponseDto> response = handler.handleEmailAlreadyRegistered(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message())
            .isEqualTo("El correo electrónico ya está registrado.");
    }

    @Test
    @DisplayName("Responde 400 cuando las contraseñas no coinciden")
    void contrasenasDistintasDevuelveBadRequest() {
        // Arrange
        PasswordMismatchException exception =
            new PasswordMismatchException("Las contraseñas no coinciden.");

        // Act
        ResponseEntity<ApiResponseDto> response = handler.handlePasswordMismatch(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Las contraseñas no coinciden.");
    }

    @Test
    @DisplayName("Responde 400 MALFORMED_REQUEST cuando el JSON no se puede leer")
    void jsonMalformadoDevuelveBadRequest() {
        // Arrange
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
            "JSON parse error: detalle interno",
            new MockHttpInputMessage(new byte[0])
        );

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleMalformedRequest(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorCode()).isEqualTo("MALFORMED_REQUEST");
        assertThat(response.getBody().message()).doesNotContain("detalle interno");
        assertThat(response.getBody().traceId()).isNotBlank();
    }

    @Test
    @DisplayName("Responde 400 INVALID_PARAMETER cuando un parámetro tiene un tipo inválido")
    void tipoInvalidoDevuelveBadRequest() {
        // Arrange
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
            "abc",
            Integer.class,
            "usuarioId",
            null,
            new NumberFormatException("For input string: abc")
        );

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleTypeMismatch(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorCode()).isEqualTo("INVALID_PARAMETER");
        assertThat(response.getBody().message())
            .isEqualTo("El parámetro 'usuarioId' no tiene un formato válido.");
    }

    @Test
    @DisplayName("Responde 404 cuando el recurso no existe, sin exponer la ruta")
    void recursoInexistenteDevuelveNotFound() {
        // Arrange
        NoResourceFoundException exception =
            new NoResourceFoundException(HttpMethod.GET, "/api/no-existe", "api/no-existe");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpectedException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().errorCode()).isEqualTo("HTTP_404");
        assertThat(response.getBody().message()).isEqualTo("El recurso solicitado no existe.");
    }

    @Test
    @DisplayName("Responde 405 cuando el método HTTP no está soportado")
    void metodoNoSoportadoDevuelve405() {
        // Arrange
        HttpRequestMethodNotSupportedException exception =
            new HttpRequestMethodNotSupportedException("PATCH", List.of("POST"));

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpectedException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().message())
            .isEqualTo("El método HTTP no está soportado para este recurso.");
    }

    @Test
    @DisplayName("Responde 415 cuando el tipo de contenido no está soportado")
    void tipoDeContenidoNoSoportadoDevuelve415() {
        // Arrange
        HttpMediaTypeNotSupportedException exception = new HttpMediaTypeNotSupportedException(
            MediaType.TEXT_PLAIN,
            List.of(MediaType.APPLICATION_JSON)
        );

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpectedException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody().message())
            .isEqualTo("El tipo de contenido de la solicitud no está soportado.");
    }

    @Test
    @DisplayName("Responde 400 genérico cuando falta un parámetro obligatorio")
    void parametroFaltanteDevuelveBadRequest() {
        // Arrange
        MissingServletRequestParameterException exception =
            new MissingServletRequestParameterException("usuarioId", "Integer");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpectedException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorCode()).isEqualTo("HTTP_400");
        assertThat(response.getBody().message()).isEqualTo("La solicitud no pudo procesarse.");
    }

    @Test
    @DisplayName("Responde 500 sin exponer el mensaje interno de la excepción")
    void errorInesperadoNoExponeDetallesInternos() {
        // Arrange
        RuntimeException exception =
            new IllegalStateException("password cannot be more than 72 bytes");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpectedException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message())
            .isEqualTo("Ocurrió un error inesperado. Intenta de nuevo más tarde.")
            .doesNotContain("password");
        assertThat(response.getBody().traceId()).isNotBlank();
    }
}
