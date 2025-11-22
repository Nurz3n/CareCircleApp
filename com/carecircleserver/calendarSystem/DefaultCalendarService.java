package com.carecircleserver.calendarSystem;

import com.dataAccess.CalendarDAO;
import com.dataAccess.CalendarDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class DefaultCalendarService implements CalendarService {
    private final CalendarDAO dao;
    public DefaultCalendarService(CalendarDAO dao) { this.dao = dao; }

    @Override public boolean createAppointment(CalendarDTO dto) {
        if (dto.patientId() == null || dto.patientId().isBlank()) return false;
        if (dto.appointmentTime() == null || dto.appointmentTime().isBefore(LocalDateTime.now())) return false;
        return dao.save(dto);
    }
    @Override public boolean cancelAppointment(UUID appointmentId) { return dao.deleteById(appointmentId); }
    @Override public List<CalendarDTO> listAppointmentsByPatient(String patientId) { return dao.findByPatientId(patientId); }
    @Override public List<CalendarDTO> listAllAppointments() { return dao.findAll(); }
}