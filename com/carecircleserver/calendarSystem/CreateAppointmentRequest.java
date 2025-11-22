package com.carecircleserver.calendarSystem;

import com.dataAccess.CalendarDTO;

public final class CreateAppointmentRequest implements ServiceRequest<Boolean> {
    private final CalendarDTO dto;
    public CreateAppointmentRequest(CalendarDTO dto) { this.dto = dto; }
    @Override public Boolean handleWith(CalendarService service) { return service.createAppointment(dto); }
}