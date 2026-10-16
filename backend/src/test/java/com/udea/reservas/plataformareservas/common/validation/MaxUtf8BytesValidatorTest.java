package com.udea.reservas.plataformareservas.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MaxUtf8BytesValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    private record Muestra(@MaxUtf8Bytes(72) String valor) {}

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest(name = "{0} veces \"{1}\" -> válido: {2}")
    @CsvSource({
        "72, a, true",
        "73, a, false",
        "36, ñ, true",
        "37, ñ, false",
        "0, a, true"
    })
    @DisplayName("Valida el límite de 72 bytes UTF-8 en los valores límite")
    void validaLimiteDeBytes(int repeticiones, String caracter, boolean esperado) {
        // Arrange
        Muestra muestra = new Muestra(caracter.repeat(repeticiones));

        // Act
        Set<ConstraintViolation<Muestra>> violaciones = validator.validate(muestra);

        // Assert
        assertThat(violaciones.isEmpty()).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "null es válido")
    @CsvSource(nullValues = "NULL", value = "NULL")
    @DisplayName("Un valor nulo se considera válido; @NotBlank se encarga de exigirlo")
    void nuloEsValido(String valor) {
        // Arrange
        Muestra muestra = new Muestra(valor);

        // Act
        Set<ConstraintViolation<Muestra>> violaciones = validator.validate(muestra);

        // Assert
        assertThat(violaciones).isEmpty();
    }
}
