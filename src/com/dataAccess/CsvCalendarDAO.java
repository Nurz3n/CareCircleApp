package com.dataAccess;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** DAO implementation backed by CsvCalendarDatabase. */
public final class CsvCalendarDAO implements CalendarDAO {
    private final Database db;

    public CsvCalendarDAO(Database db) {
        this.db = db;
    }

    @Override
    public boolean save(CalendarDTO dto) {
        try {
            db.append(dto);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean deleteById(UUID id) {
        try {
            List<CalendarDTO> all = db.readAll();
            boolean removed = all.removeIf(a -> a.id().equals(id));
            if (!removed) {
                return false;
            }
            db.rewriteAll(all);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<CalendarDTO> findById(UUID id) {
        try {
            return db.readAll().stream().filter(a -> a.id().equals(id)).findFirst();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<CalendarDTO> findByPatientId(String patientId) {
        try {
            return db.readAll().stream()
                    .filter(a -> Objects.equals(a.patientId(), patientId))
                    .sorted(Comparator.comparing(CalendarDTO::appointmentTime))
                    .collect(Collectors.toUnmodifiableList());
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<CalendarDTO> findAll() {
        try {
            List<CalendarDTO> list = db.readAll();
            list.sort(Comparator.comparing(CalendarDTO::appointmentTime));
            return List.copyOf(list);
        } catch (Exception e) {
            return List.of();
        }
    }
}
