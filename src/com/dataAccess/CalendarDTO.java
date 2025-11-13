package com.dataAccess;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
            "id", "patientId", "patientName", "professionalName", "professionalType",
            "appointmentTimeISO", "reason", "durationMinutes", "createdAt"
    };

    public String toCsvLine() {
        return Csv.join(
                id.toString(),
                nz(patientId),
                nz(patientName),
                nz(professionalName),
                nz(professionalType),
                appointmentTime.toString(),
                nz(reason),
                Integer.toString(durationMinutes),
                createdAt.toString()
        );
    }

    public static CalendarDTO fromCsvLine(String line) {
        List<String> raw = Csv.split(line);
        if (raw.size() < HEADER.length) {
            raw = new ArrayList<>(raw);
            while (raw.size() < HEADER.length) {
                raw.add("");
            }
        }
        UUID id = parseUuid(g(raw, 0));
        String patientId = g(raw, 1);
        String patientName = g(raw, 2);
        String professionalName = g(raw, 3);
        String professionalType = g(raw, 4);
        LocalDateTime at = parseAppointmentTime(g(raw, 5));
        String reason = g(raw, 6);
        int duration = parseDuration(g(raw, 7));
        Instant created = parseCreatedAt(g(raw, 8));
        return new CalendarDTO(id, patientId, patientName, professionalName, professionalType, at, reason, duration, created);
    }

    public static CalendarDTO newFromUI(String patientId, String patientName,
                                        String professionalName, String professionalType,
                                        LocalDateTime at, String reason, int durationMinutes) {
        return new CalendarDTO(UUID.randomUUID(), patientId, patientName, professionalName, professionalType, at, reason, durationMinutes, Instant.now());
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid appointment id: " + raw, e);
        }
    }

    private static LocalDateTime parseAppointmentTime(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Missing appointment time");
        }
        try {
            return LocalDateTime.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid appointment time: " + raw, e);
        }
    }

    private static int parseDuration(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid duration minutes: " + raw, e);
        }
    }

    private static Instant parseCreatedAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid createdAt timestamp: " + raw, e);
        }
    }

    private static String g(List<String> c, int i) {
        return i < c.size() ? c.get(i) : "";
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
