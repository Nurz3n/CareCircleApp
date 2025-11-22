package com.dataAccess;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** CSV database for CalendarDTO using Settings.APPOINTMENTS_CSV. */
public final class CsvCalendarDatabase implements Database {
    private final File file = new File(Settings.APPOINTMENTS_CSV);
    public CsvCalendarDatabase() { ensureHeader(); }

    @Override
    public synchronized void append(CalendarDTO dto) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
            bw.write(dto.toCsvLine()); bw.newLine();
        }
    }

    @Override
    public synchronized List<CalendarDTO> readAll() throws IOException {
        List<CalendarDTO> out = new ArrayList<>();
        if (!file.exists()) return out;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            br.readLine(); // header
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isBlank()) out.add(CalendarDTO.fromCsvLine(line));
            }
        }
        return out;
    }

    @Override
    public synchronized void rewriteAll(List<CalendarDTO> all) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8))) {
            bw.write(String.join(",", CalendarDTO.HEADER)); bw.newLine();
            for (CalendarDTO dto : all) { bw.write(dto.toCsvLine()); bw.newLine(); }
        }
    }

    private void ensureHeader() {
        try {
            boolean created = file.createNewFile();
            if (created || file.length() == 0L) {
                try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8))) {
                    bw.write(String.join(",", CalendarDTO.HEADER)); bw.newLine();
                }
            }
        } catch (IOException e) { throw new RuntimeException("Failed to init " + file.getName() + ": " + e.getMessage(), e); }
    }
}