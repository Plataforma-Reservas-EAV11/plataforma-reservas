package com.udea.reservas.plataformareservas.servicios.dto.validation;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public final class StrictIntegerDeserializer extends ValueDeserializer<Integer> {

    public static final String ERROR_MESSAGE =
        "duracion debe ser un número entero.";

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context)
        throws tools.jackson.core.JacksonException {
        if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
            return context.reportInputMismatch(this, ERROR_MESSAGE);
        }

        return parser.getIntValue();
    }
}
