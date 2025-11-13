package com.carecircleserver;

import com.dataAccess.Csv;
import com.dataAccess.Settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class VitalsTcpServer {
    private static final int PORT = Settings.SERVER_PORT;
    private static final File CSV_FILE = new File(Settings.VITALS_CSV);
    private static final String QUIT = "QUIT";

    public static void main(String[] args) {
        System.out.println("Server listening on port " + PORT + " ...");
        ensureCsvHeader();
        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                pool.submit(() -> handleClient(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            pool.shutdown();
        }
    }

    private static void handleClient(Socket socket) {
        try (socket;
             BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (QUIT.equalsIgnoreCase(trimmed)) {
                    bw.write("Goodbye");
                    bw.newLine();
                    bw.flush();
                    break;
                }
                if ("LIST ALL".equalsIgnoreCase(trimmed)) {
                    sendCsv(bw, null);
                    continue;
                }
                if (trimmed.toUpperCase().startsWith("LIST ")) {
                    String patientId = trimmed.substring(5).trim();
                    if (patientId.isEmpty()) {
                        bw.write("ERROR: Missing patientId after LIST");
                        bw.newLine();
                        bw.flush();
                    } else {
                        sendCsv(bw, patientId);
                    }
                    continue;
                }
                if (trimmed.isEmpty()) {
                    bw.write("ERROR: empty submission");
                    bw.newLine();
                    bw.flush();
                    continue;
                }
                recordVitals(line, bw);
            }
        } catch (IOException ignored) {
        }
    }

    private static void recordVitals(String rawLine, BufferedWriter bw) throws IOException {
        List<String> fields = new ArrayList<>(Csv.split(rawLine));
        if (fields.isEmpty() || fields.get(0).trim().isEmpty()) {
            bw.write("ERROR: Missing patientId");
            bw.newLine();
            bw.flush();
            return;
        }
        fields.add(Instant.now().toString());
        appendCsvLine(fields);
        bw.write("OK saved to " + CSV_FILE.getName());
        bw.newLine();
        bw.flush();
    }

    private static void sendCsv(BufferedWriter bw, String filterPatientId) throws IOException {
        ensureCsvHeader();
        try (BufferedReader csv = new BufferedReader(new InputStreamReader(new FileInputStream(CSV_FILE), StandardCharsets.UTF_8))) {
            String header = csv.readLine();
            if (header != null) {
                bw.write(header);
                bw.newLine();
            }
            String row;
            while ((row = csv.readLine()) != null) {
                if (filterPatientId == null) {
                    bw.write(row);
                    bw.newLine();
                } else {
                    List<String> cols = Csv.split(row);
                    if (!cols.isEmpty() && filterPatientId.equalsIgnoreCase(cols.get(0).trim())) {
                        bw.write(row);
                        bw.newLine();
                    }
                }
            }
        } catch (IOException ex) {
            bw.write("ERROR: " + ex.getMessage());
            bw.newLine();
        }
        bw.write("END");
        bw.newLine();
        bw.flush();
    }

    private static synchronized void appendCsvLine(List<String> fields) {
        ensureCsvHeader();
        try (BufferedWriter csv = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_FILE, true), StandardCharsets.UTF_8))) {
            csv.write(Csv.join(fields.toArray(String[]::new)));
            csv.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write CSV: " + e.getMessage(), e);
        }
    }

    private static void ensureCsvHeader() {
        try {
            boolean newFile = CSV_FILE.createNewFile();
            if (newFile || CSV_FILE.length() == 0L) {
                try (BufferedWriter csv = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_FILE, false), StandardCharsets.UTF_8))) {
                    csv.write("patientId,heartRateBpm,bpSystolic,bpDiastolic,temperatureC,mood,dietNotes,weightKg,submittedAt");
                    csv.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to init CSV: " + e.getMessage(), e);
        }
    }

    private VitalsTcpServer() {
    }
}
