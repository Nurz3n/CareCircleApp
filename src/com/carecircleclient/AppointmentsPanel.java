package com.carecircleclient;

import com.carecircleserver.calendarSystem.CalendarWiring;
import com.dataAccess.CalendarDTO;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Appointments UI using ServiceDispatcher -> DAO -> CSV. */
public final class AppointmentsPanel extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTextField patientIdField = new JTextField(16);
    private final JTextField patientNameField = new JTextField(16);
    private final JTextField professionalNameField = new JTextField(16);
    private final JComboBox<String> professionalTypeCombo =
            new JComboBox<>(new String[]{"Select Type", "Doctor", "Nurse", "Caregiver", "Physiotherapist", "Specialist"});
    private final JTextField dateField = new JTextField(16); // YYYY-MM-DD
    private final JTextField timeField = new JTextField(16); // HH:MM
    private final JTextField reasonField = new JTextField(16);
    private final JSpinner durationSpinner = new JSpinner(new SpinnerNumberModel(30, 15, 240, 15));

    public AppointmentsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        tableModel = new DefaultTableModel(new String[]{"Patient ID", "Patient Name", "Professional", "Type", "Time", "Duration", "Reason"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        var table = new JTable(tableModel);
        table.setRowHeight(24);
        add(new JScrollPane(table), BorderLayout.CENTER);

        var form = new JPanel(new GridBagLayout());
        var gbc = new GridBagConstraints();
        gbc.insets = new java.awt.Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        int r = 0;
        addRow(form, gbc, r++, "Patient ID:", patientIdField);
        addRow(form, gbc, r++, "Patient Name:", patientNameField);
        addRow(form, gbc, r++, "Professional Name:", professionalNameField);
        addRow(form, gbc, r++, "Professional Type:", professionalTypeCombo);
        addRow(form, gbc, r++, "Date (YYYY-MM-DD):", dateField);
        addRow(form, gbc, r++, "Time (HH:MM):", timeField);
        addRow(form, gbc, r++, "Duration (minutes):", durationSpinner);
        addRow(form, gbc, r++, "Reason:", reasonField);

        var btnBook = new JButton("Book");
        var btnLoadAll = new JButton("Load All");
        var btnLoadByPid = new JButton("Load by Patient ID");
        var actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(btnBook);
        actions.add(btnLoadByPid);
        actions.add(btnLoadAll);

        var north = new JPanel(new BorderLayout());
        north.add(form, BorderLayout.CENTER);
        north.add(actions, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        btnBook.addActionListener(e -> onBook());
        btnLoadAll.addActionListener(e -> reloadAll());
        btnLoadByPid.addActionListener(e -> reloadByPid());
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, javax.swing.JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        p.add(field, gbc);
    }

    private void onBook() {
        try {
            String pid = patientIdField.getText().trim();
            String pname = patientNameField.getText().trim();
            String profName = professionalNameField.getText().trim();
            String ptype = (String) professionalTypeCombo.getSelectedItem();
            String date = dateField.getText().trim();
            String time = timeField.getText().trim();
            String reason = reasonField.getText().trim();
            int duration = (Integer) durationSpinner.getValue();

            if (pid.isEmpty() || pname.isEmpty() || profName.isEmpty() || "Select Type".equals(ptype) || date.isEmpty() || time.isEmpty() || reason.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fill all fields.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            LocalDateTime at = LocalDateTime.parse(date + "T" + time);
            CalendarDTO dto = CalendarDTO.newFromUI(pid, pname, profName, ptype, at, reason, duration);
            boolean ok = CalendarWiring.dispatcher().bookAppointment(dto);
            if (!ok) {
                JOptionPane.showMessageDialog(this, "Booking failed.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            tableModel.addRow(new Object[]{
                    pid,
                    pname,
                    profName,
                    ptype,
                    at.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                    duration + " minutes",
                    reason
            });
            clearForm();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid date/time. Use YYYY-MM-DD and HH:MM.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reloadAll() {
        tableModel.setRowCount(0);
        for (CalendarDTO a : CalendarWiring.dispatcher().listAllAppointments()) {
            tableModel.addRow(new Object[]{
                    a.patientId(),
                    a.patientName(),
                    a.professionalName(),
                    a.professionalType(),
                    a.appointmentTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                    a.durationMinutes() + " minutes",
                    a.reason()
            });
        }
    }

    private void reloadByPid() {
        String pid = patientIdField.getText().trim();
        if (pid.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter Patient ID.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        tableModel.setRowCount(0);
        for (CalendarDTO a : CalendarWiring.dispatcher().listAppointmentsByPatient(pid)) {
            tableModel.addRow(new Object[]{
                    a.patientId(),
                    a.patientName(),
                    a.professionalName(),
                    a.professionalType(),
                    a.appointmentTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                    a.durationMinutes() + " minutes",
                    a.reason()
            });
        }
    }

    private void clearForm() {
        patientIdField.setText("");
        patientNameField.setText("");
        professionalNameField.setText("");
        professionalTypeCombo.setSelectedIndex(0);
        dateField.setText("");
        timeField.setText("");
        reasonField.setText("");
        durationSpinner.setValue(30);
    }
}
