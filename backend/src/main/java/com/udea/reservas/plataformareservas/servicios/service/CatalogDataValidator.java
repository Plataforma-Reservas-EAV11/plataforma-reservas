package com.udea.reservas.plataformareservas.servicios.service;

import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.proveedores.model.Proveedor;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import com.udea.reservas.plataformareservas.servicios.model.Servicio;
import java.time.LocalTime;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

final class CatalogDataValidator {

    private static final String SERVICE_MESSAGE =
        "A stored service must have a provider and a positive duration.";
    private static final String SCHEDULE_MESSAGE =
        "A stored schedule must have a valid day, time range and positive slot duration.";
    private static final String RESERVATION_MESSAGE =
        "A stored active reservation must reference a schedule and date.";

    private CatalogDataValidator() {}

    static void validatePagination(int page, int size, int maxSize) {
        if (page < 0 || size < 1 || size > maxSize) {
            throw new BusinessRuleException(
                "INVALID_PAGINATION",
                "La paginación debe tener una página no negativa y un tamaño entre 1 y 100.",
                HttpStatus.BAD_REQUEST.value()
            );
        }
    }

    static void requirePositiveId(Integer id, String parameterName) {
        if (id == null || id < 1) {
            throw new BusinessRuleException(
                "INVALID_PARAMETER",
                "El parámetro '" + parameterName + "' debe ser un entero positivo.",
                HttpStatus.BAD_REQUEST.value()
            );
        }
    }

    static void validateService(Servicio service) {
        Servicio stored = requirePresent(service, SERVICE_MESSAGE);
        Proveedor provider = requirePresent(stored.getProveedor(), SERVICE_MESSAGE);
        Assert.state(stored.getId() != null, SERVICE_MESSAGE);
        Assert.state(provider.getId() != null, SERVICE_MESSAGE);
        Assert.state(stored.getNombre() != null, SERVICE_MESSAGE);
        Assert.state(stored.getDuracionMinutos() != null, SERVICE_MESSAGE);
        Assert.state(stored.getDuracionMinutos() > 0, SERVICE_MESSAGE);
    }

    static void validateSchedule(HorarioDisponible schedule) {
        HorarioDisponible stored = requirePresent(schedule, SCHEDULE_MESSAGE);
        Integer weekday = requirePresent(stored.getDiaSemana(), SCHEDULE_MESSAGE);
        LocalTime startTime = requirePresent(stored.getHoraInicio(), SCHEDULE_MESSAGE);
        LocalTime endTime = requirePresent(stored.getHoraFin(), SCHEDULE_MESSAGE);
        Integer slotDuration = requirePresent(stored.getDuracionSlotMin(), SCHEDULE_MESSAGE);

        Assert.state(stored.getId() != null, SCHEDULE_MESSAGE);
        Assert.state(weekday >= 0, SCHEDULE_MESSAGE);
        Assert.state(weekday <= 6, SCHEDULE_MESSAGE);
        Assert.state(endTime.isAfter(startTime), SCHEDULE_MESSAGE);
        Assert.state(slotDuration > 0, SCHEDULE_MESSAGE);
    }

    static void validateActiveReservation(Reserva reservation) {
        Reserva stored = requirePresent(reservation, RESERVATION_MESSAGE);
        HorarioDisponible schedule = requirePresent(stored.getHorario(), RESERVATION_MESSAGE);

        Assert.state(schedule.getId() != null, RESERVATION_MESSAGE);
        Assert.state(stored.getFecha() != null, RESERVATION_MESSAGE);
    }

    static <T> T requirePresent(T value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return value;
    }
}
