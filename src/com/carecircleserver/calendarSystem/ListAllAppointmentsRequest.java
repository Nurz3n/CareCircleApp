package com.carecircleserver.calendarSystem;

import com.dataAccess.CalendarDTO;

import java.util.List;

public final class ListAllAppointmentsRequest implements ServiceRequest<List<CalendarDTO>> {
    @Override
    public List<CalendarDTO> handleWith(CalendarService service) {
        return service.listAllAppointments();
    }
}
