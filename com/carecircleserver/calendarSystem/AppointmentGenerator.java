package com.carecircleserver.calendarSystem;

import com.dataAccess.CalendarDTO;

public interface AppointmentGenerator {
    CalendarDTO generateNextAppointment(String patientId);
}