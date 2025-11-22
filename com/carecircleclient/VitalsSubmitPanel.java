package com.carecircleclient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static com.dataAccess.Csv.join;
import static com.dataAccess.Settings.SERVER_HOST;
import static com.dataAccess.Settings.SERVER_PORT;

public final class VitalsSubmitPanel extends JPanel {
    private static final int ICON = Icons.DEFAULT_LABEL_ICON_SIZE;

    private final JTextField tfPatientId   = new JTextField(16);
    private final JTextField tfHeartRate   = new JTextField(16);
    private final JTextField tfBpSys       = new JTextField(16);
    private final JTextField tfBpDia       = new JTextField(16);
    private final JTextField tfTempC       = new JTextField(16);
    private final JComboBox<String> cbMood = new JComboBox<>(new String[]{"", "VERY_BAD", "BAD", "NEUTRAL", "GOOD", "VERY_GOOD"});
    private final JTextArea taDietNotes    = new JTextArea(3, 16);
    private final JTextField tfWeightKg    = new JTextField(16);
    private final JTextArea taLog          = new JTextArea(8, 48);

    public VitalsSubmitPanel() {
        setLayout(new BorderLayout(12,12));
        setBorder(new EmptyBorder(16,16,16,16));

        JPanel formGrid = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        int row = 0;
        addRow(formGrid, c, row++, label("Patient ID*",      "user", "👤"), tfPatientId);
        addRow(formGrid, c, row++, label("Heart Rate (bpm)", "heart", "❤️"), tfHeartRate);
        addRow(formGrid, c, row++, label("BP Systolic",      "cross", "➕"), tfBpSys);
        addRow(formGrid, c, row++, label("BP Diastolic",     "cross", "➖"), tfBpDia);
        addRow(formGrid, c, row++, label("Temperature (°C)", "thermometer", "🌡"), tfTempC);
        addRow(formGrid, c, row++, label("Mood",             "mood", "🙂"), cbMood);
        taDietNotes.setLineWrap(true); taDietNotes.setWrapStyleWord(true);
        addRow(formGrid, c, row++, label("Diet Notes",       "list", "≣"), new JScrollPane(taDietNotes));
        addRow(formGrid, c, row++, label("Weight (kg)",      "weight", "⚖"), tfWeightKg);

        JPanel formCard = titled("Submit Vitals", formGrid);

        JButton btnSubmit = new JButton("Submit Vitals", Icons.icon("check", "✔", ICON));
        JButton btnClear  = new JButton("Clear Form", Icons.icon("refresh", "🔄", ICON));
        JButton btnQuit   = new JButton("Quit Session", Icons.icon("cancel", "✖", ICON));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.add(btnSubmit); actions.add(btnClear); actions.add(btnQuit);

        taLog.setEditable(false);
        taLog.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(taLog);
        JPanel logCard = titled("Session Log", logScroll);

        JPanel north = new JPanel(new BorderLayout(12,12));
        north.add(formCard, BorderLayout.CENTER);
        north.add(actions, BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);
        add(logCard, BorderLayout.CENTER);

        btnSubmit.addActionListener(e -> onSubmit());
        btnClear.addActionListener(e -> clearForm());
        btnQuit.addActionListener(e -> onQuit());
    }

    private static JPanel titled(String title, JComponent content) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(content, BorderLayout.CENTER);
        return p;
    }
    private static JLabel label(String text, String iconName, String glyph) {
        JLabel l = new JLabel(text);
        Icon icon = Icons.icon(iconName, glyph, ICON);
        if (icon != null) { l.setIcon(icon); l.setIconTextGap(8); }
        return l;
    }
    private static void addRow(JPanel p, GridBagConstraints c, int row, JComponent label, JComponent field) {
        c.gridx = 0; c.gridy = row; c.weightx = 0; p.add(label, c);
        c.gridx = 1; c.weightx = 1; p.add(field, c);
    }

    private void onSubmit() {
        var patientId = tfPatientId.getText().trim();
        if (patientId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Patient ID is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        var line = join(
                txt(tfPatientId), txt(tfHeartRate), txt(tfBpSys), txt(tfBpDia),
                txt(tfTempC), val(cbMood), txt(taDietNotes), txt(tfWeightKg)
        );
        appendLog("> " + line);
        var resp = sendSingle(line);
        appendLog("< " + resp);
    }
    private void onQuit() { appendLog("> QUIT"); appendLog("< " + sendSingle("QUIT")); }

    private String sendSingle(String line) {
        try (var socket = new Socket(SERVER_HOST, SERVER_PORT);
             var br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            bw.write(line); bw.newLine(); bw.flush();
            socket.setSoTimeout(1500);
            var first = br.readLine(); if (first == null) return "<no response>";
            var second = br.ready() ? br.readLine() : null;
            return (second != null) ? (first + " | " + second) : first;
        } catch (IOException ex) { return "ERROR: " + ex.getMessage(); }
    }

    private static String txt(JTextField tf) { var s = tf.getText(); return s == null ? "" : s.trim(); }
    private static String txt(JTextArea ta)   { var s = ta.getText(); return s == null ? "" : s.trim(); }
    private static String val(JComboBox<String> cb) { var v = cb.getSelectedItem(); return v == null ? "" : v.toString(); }

    private void clearForm() {
        tfHeartRate.setText(""); tfBpSys.setText(""); tfBpDia.setText("");
        tfTempC.setText(""); cbMood.setSelectedIndex(0);
        taDietNotes.setText(""); tfWeightKg.setText("");
    }
    private void appendLog(String s) {
        taLog.append("[" + Instant.now() + "] " + s + "\n");
        taLog.setCaretPosition(taLog.getDocument().getLength());
    }
}