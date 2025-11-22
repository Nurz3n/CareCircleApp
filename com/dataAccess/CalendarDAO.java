package com.dataAccess;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalendarDAO {
    boolean save(CalendarDTO dto);
    boolean deleteById(UUID id);
    Optional<CalendarDTO> findById(UUID id);
    List<CalendarDTO> findByPatientId(String patientId);
    List<CalendarDTO> findAll();
}