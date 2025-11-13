package com.carecircleclient;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static com.dataAccess.Csv.join;
import static com.dataAccess.Settings.SERVER_HOST;
import static com.dataAccess.Settings.SERVER_PORT;

/** Vitals submit UI that talks to VitalsTcpServer. */
public final class VitalsSubmitPanel extends JPanel {
    private final JTextField tfPatientId = new JTextField(16);
    private final JTextField tfHeartRate = new JTextField(16);
    private final JTextField tfBpSys = new JTextField(16);
    private final JTextField tfBpDia = new JTextField(16);
    private final JTextField tfTempC = new JTextField(16);
    private final JComboBox<String> cbMood = new JComboBox<>(new String[]{"", "VERY_BAD", "BAD", "NEUTRAL", "GOOD", "VERY_GOOD"});
    private final JTextArea taDietNotes = new JTextArea(3, 16);
    private final JTextField tfWeightKg = new JTextField(16);
    private final JTextArea taLog = new JTextArea(6, 40);

    public VitalsSubmitPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        var form = new JPanel(new GridBagLayout());
        var c = new GridBagConstraints();
        c.insets = new java.awt.Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        int row = 0;
        addRow(form, c, row++, "Patient ID*", tfPatientId);
        addRow(form, c, row++, "Heart Rate (bpm)", tfHeartRate);
        addRow(form, c, row++, "BP Systolic", tfBpSys);
        addRow(form, c, row++, "BP Diastolic", tfBpDia);
        addRow(form, c, row++, "Temperature (°C)", tfTempC);
        addRow(form, c, row++, "Mood", cbMood);
        taDietNotes.setLineWrap(true);
        taDietNotes.setWrapStyleWord(true);
        addRow(form, c, row++, "Diet Notes", new JScrollPane(taDietNotes));
        addRow(form, c, row++, "Weight (kg)", tfWeightKg);

        var btnSubmit = new JButton("Submit Vitals");
        var btnClear = new JButton("Clear Form");
        var btnQuit = new JButton("Quit Session");

        var buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnSubmit);
        buttons.add(btnClear);
        buttons.add(btnQuit);

        taLog.setEditable(false);
        var center = new JPanel(new BorderLayout(8, 8));
        center.add(buttons, BorderLayout.NORTH);
        center.add(new JScrollPane(taLog), BorderLayout.CENTER);

        add(form, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        btnSubmit.addActionListener(e -> onSubmit());
        btnClear.addActionListener(e -> clearForm());
        btnQuit.addActionListener(e -> onQuit());
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, String label, Component field) {
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        p.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        p.add(field, c);
    }

    private void onSubmit() {
        var patientId = tfPatientId.getText().trim();
        if (patientId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Patient ID is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        var line = join(
                txt(tfPatientId),
                txt(tfHeartRate),
                txt(tfBpSys),
                txt(tfBpDia),
                txt(tfTempC),
                val(cbMood),
                txt(taDietNotes),
                txt(tfWeightKg)
        );
        appendLog("> " + line);
        var resp = sendSingle(line);
        appendLog("< " + resp);
    }

    private void onQuit() {
        appendLog("> QUIT");
        var resp = sendSingle("QUIT");
        appendLog("< " + resp);
    }

    private String sendSingle(String line) {
        try (var socket = new Socket(SERVER_HOST, SERVER_PORT);
             var br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            bw.write(line);
            bw.newLine();
            bw.flush();
            socket.setSoTimeout(1500);
            var first = br.readLine();
            if (first == null) {
                return "<no response>";
            }
            var second = br.ready() ? br.readLine() : null;
            return (second != null) ? (first + " | " + second) : first;
        } catch (IOException ex) {
            return "ERROR: " + ex.getMessage();
        }
    }

    private static String txt(JTextField tf) {
        var s = tf.getText();
        return s == null ? "" : s.trim();
    }

    private static String txt(JTextArea ta) {
        var s = ta.getText();
        return s == null ? "" : s.trim();
    }

    private static String val(JComboBox<String> cb) {
        var v = cb.getSelectedItem();
        return v == null ? "" : v.toString();
    }

    private void clearForm() {
        tfHeartRate.setText("");
        tfBpSys.setText("");
        tfBpDia.setText("");
        tfTempC.setText("");
        cbMood.setSelectedIndex(0);
        taDietNotes.setText("");
        tfWeightKg.setText("");
    }

    private void appendLog(String s) {
        taLog.append("[" + Instant.now() + "] " + s + "\n");
        taLog.setCaretPosition(taLog.getDocument().getLength());
    }
}
