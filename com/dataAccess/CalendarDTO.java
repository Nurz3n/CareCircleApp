package com.dataAccess;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Appointment DTO persisted via CSV. */
public record CalendarDTO(
        UUID id,
        String patientId,
        String patientName,
        String professionalName,
        String professionalType,
        LocalDateTime appointmentTime,
        String reason,
        int durationMinutes,
        Instant createdAt
) {
    public static final String[] HEADER = {
            "id","patientId","patientName","professionalName","professionalType",
            "appointmentTimeISO","reason","durationMinutes","createdAt"
    };

    public String toCsvLine() {
        return Csv.join(
                id.toString(),
                nz(patientId), nz(patientName), nz(professionalName), nz(professionalType),
                appointmentTime.toString(),
                nz(reason),
                Integer.toString(durationMinutes),
                createdAt.toString()
        );
    }

    public static CalendarDTO fromCsvLine(String line) {
        List<String> c = Csv.split(line);
        UUID id = UUID.fromString(c.get(0));
        String patientId = g(c,1);
        String patientName = g(c,2);
        String professionalName = g(c,3);
        String professionalType = g(c,4);
        LocalDateTime at = LocalDateTime.parse(g(c,5));
        String reason = g(c,6);
        int duration = Integer.parseInt(g(c,7));
        Instant created = Instant.parse(g(c,8));
        return new CalendarDTO(id, patientId, patientName, professionalName, professionalType, at, reason, duration, created);
    }

    public static CalendarDTO newFromUI(String patientId, String patientName,
                                        String professionalName, String professionalType,
                                        LocalDateTime at, String reason, int durationMinutes) {
        return new CalendarDTO(UUID.randomUUID(), patientId, patientName, professionalName, professionalType, at, reason, durationMinutes, Instant.now());
    }

    private static String g(List<String> c, int i) { return i < c.size() ? c.get(i) : ""; }
    private static String nz(String s) { return s == null ? "" : s; }
}