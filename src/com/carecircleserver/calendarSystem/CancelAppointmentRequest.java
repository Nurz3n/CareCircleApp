package com.carecircleserver.calendarSystem;

import java.util.UUID;

public final class CancelAppointmentRequest implements ServiceRequest<Boolean> {
    private final UUID id;

    public CancelAppointmentRequest(UUID id) {
        this.id = id;
    }

    @Override
    public Boolean handleWith(CalendarService service) {
        return service.cancelAppointment(id);
    }
}
