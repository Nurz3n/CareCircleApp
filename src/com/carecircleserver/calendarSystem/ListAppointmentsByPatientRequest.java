package com.carecircleserver.calendarSystem;

import com.dataAccess.CalendarDTO;

import java.util.List;

public final class ListAppointmentsByPatientRequest implements ServiceRequest<List<CalendarDTO>> {
    private final String patientId;

    public ListAppointmentsByPatientRequest(String patientId) {
        this.patientId = patientId;
    }

    @Override
    public List<CalendarDTO> handleWith(CalendarService service) {
        return service.listAppointmentsByPatient(patientId);
    }
}
