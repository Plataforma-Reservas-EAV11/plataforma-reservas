package com.udea.reservas.plataformareservas.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limita el tamaño de un texto en bytes UTF-8. BCrypt solo admite contraseñas
 * de hasta 72 bytes, y un límite en caracteres no basta con letras como ñ o tildes.
 */
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

    int value();

    String message() default "supera el tamaño máximo permitido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
