package com.carecircleserver.calendarSystem;

import com.dataAccess.CalendarDTO;
import java.util.List;
import java.util.UUID;

public interface CalendarService {
    boolean createAppointment(CalendarDTO dto);
    boolean cancelAppointment(UUID appointmentId);
    List<CalendarDTO> listAppointmentsByPatient(String patientId);
    List<CalendarDTO> listAllAppointments();
}