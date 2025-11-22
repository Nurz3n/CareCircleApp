package com.carecircleserver;

import com.dataAccess.Settings;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** TCP vitals server compatible with your client panels. */
public final class VitalsTcpServer {
    private static final int PORT = Settings.SERVER_PORT;
    private static final File CSV_FILE = new File(Settings.VITALS_CSV);
    private static final String QUIT = "QUIT";

    public static void main(String[] args) {
        System.out.println("Server listening on port " + PORT + " ...");
        ensureCsvHeader();
        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) pool.submit(() -> {
                try {
                    handleClient(serverSocket.accept());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) { e.printStackTrace(); } finally { pool.shutdown(); }
    }

    private static void handleClient(Socket socket) {
        String remote = socket.getRemoteSocketAddress().toString();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (QUIT.equalsIgnoreCase(trimmed)) { bw.write("Goodbye"); bw.newLine(); bw.flush(); break; }
                if ("LIST ALL".equalsIgnoreCase(trimmed)) { sendCsv(bw, null); continue; }
                if (trimmed.toUpperCase().startsWith("LIST ")) {
                    String patientId = trimmed.substring(5).trim();
                    if (patientId.isEmpty()) { bw.write("ERROR: Missing patientId after LIST"); bw.newLine(); bw.flush(); }
                    else { sendCsv(bw, patientId); }
                    continue;
                }
                if (trimmed.isEmpty()) { bw.write("ERROR: empty submission"); bw.newLine(); bw.flush(); continue; }
                appendCsvLine(line + "," + Instant.now());
                bw.write("OK saved to " + CSV_FILE.getName()); bw.newLine(); bw.flush();
            }
        } catch (IOException ignored) {} finally { try { socket.close(); } catch (IOException ignored) {} }
    }

    private static void sendCsv(BufferedWriter bw, String filterPatientId) throws IOException {
        ensureCsvHeader();
        try (BufferedReader csv = new BufferedReader(new InputStreamReader(new FileInputStream(CSV_FILE), StandardCharsets.UTF_8))) {
            String header = csv.readLine(); if (header != null) { bw.write(header); bw.newLine(); }
            String row; while ((row = csv.readLine()) != null) {
                if (filterPatientId == null) { bw.write(row); bw.newLine(); }
                else {
                    int comma = row.indexOf(','); String pid = (comma == -1) ? row : row.substring(0, comma);
                    if (filterPatientId.equalsIgnoreCase(pid.trim())) { bw.write(row); bw.newLine(); }
                }
            }
        } catch (IOException ex) { bw.write("ERROR: " + ex.getMessage()); bw.newLine(); }
        bw.write("END"); bw.newLine(); bw.flush();
    }

    private static synchronized void appendCsvLine(String csvLine) {
        try (BufferedWriter csv = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_FILE, true), StandardCharsets.UTF_8))) {
            csv.write(csvLine); csv.newLine();
        } catch (IOException e) { throw new RuntimeException("Failed to write CSV: " + e.getMessage(), e); }
    }

    private static void ensureCsvHeader() {
        try {
            boolean newFile = CSV_FILE.createNewFile();
            if (newFile || CSV_FILE.length() == 0L) {
                try (BufferedWriter csv = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_FILE, true), StandardCharsets.UTF_8))) {
                    csv.write("patientId,heartRateBpm,bpSystolic,bpDiastolic,temperatureC,mood,dietNotes,weightKg,submittedAt");
                    csv.newLine();
                }
            }
        } catch (IOException e) { throw new RuntimeException("Failed to init CSV: " + e.getMessage(), e); }
    }
}

