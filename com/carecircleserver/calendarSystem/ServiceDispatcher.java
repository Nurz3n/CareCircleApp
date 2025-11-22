package com.carecircleserver.calendarSystem;

import com.dataAccess.CalendarDTO;
import java.util.List;
import java.util.UUID;

public interface ServiceDispatcher {
    <R> R dispatch(ServiceRequest<R> request);

    default boolean bookAppointment(CalendarDTO dto) {
        return dispatch(new CreateAppointmentRequest(dto));
    }
    default boolean cancelAppointment(UUID id) {
        return dispatch(new CancelAppointmentRequest(id));
    }
    default List<CalendarDTO> listAppointmentsByPatient(String patientId) {
        return dispatch(new ListAppointmentsByPatientRequest(patientId));
    }
    default List<CalendarDTO> listAllAppointments() {
        return dispatch(new ListAllAppointmentsRequest());
    }
}