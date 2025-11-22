package com.carecircleserver.calendarSystem;

import com.dataAccess.CalendarDTO;
import java.util.UUID;

public final class AppointmentManager {
    public UUID schedule(CalendarDTO dto) { return dto.id(); }
    public boolean cancel(UUID id) { return true; }
}
