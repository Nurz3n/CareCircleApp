package com.carecircleclient;

import com.carecircleserver.calendarSystem.CalendarWiring;
import com.dataAccess.CalendarDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AppointmentsPanel extends JPanel {
    private final DefaultTableModel tableModel;

    private final JTextField patientIdField = new JTextField(16);
    private final JTextField patientNameField = new JTextField(16);
    private final JTextField professionalNameField = new JTextField(16);
    private final JComboBox<String> professionalTypeCombo =
            new JComboBox<>(new String[]{"Select Type", "Doctor", "Nurse", "Caregiver", "Physiotherapist", "Specialist"});
    private final JTextField dateField = new JTextField(12); // YYYY-MM-DD
    private final JTextField timeField = new JTextField(8);  // HH:MM
    private final JTextField reasonField = new JTextField(20);
    private final JSpinner durationSpinner = new JSpinner(new SpinnerNumberModel(30, 15, 240, 15));

    private final JTable table;

    public AppointmentsPanel() {
        setLayout(new BorderLayout(12,12));
        setBorder(BorderFactory.createEmptyBorder(16,16,16,16));

        // Table with hidden ID col
        tableModel = new DefaultTableModel(new String[]{
                "ID", "Patient ID","Patient Name","Professional","Type","Time","Duration","Reason"
        }, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel tableCard = titled("Appointments", new JScrollPane(table));

        // Hide ID column visually
        SwingUtilities.invokeLater(this::hideIdColumn);

        // Form
        JPanel formGrid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6); gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 1.0;

        int r = 0;
        addRow(formGrid, gbc, r++, "Patient ID:", patientIdField);
        addRow(formGrid, gbc, r++, "Patient Name:", patientNameField);
        addRow(formGrid, gbc, r++, "Professional Name:", professionalNameField);
        addRow(formGrid, gbc, r++, "Professional Type:", professionalTypeCombo);

        JPanel dt = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dt.add(new JLabel("Date (YYYY-MM-DD):")); dt.add(dateField);
        dt.add(new JLabel("Time (HH:MM):")); dt.add(timeField);
        addRow(formGrid, gbc, r++, "When:", dt);

        JPanel dur = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dur.add(new JLabel("Duration (minutes):")); dur.add(durationSpinner);
        addRow(formGrid, gbc, r++, "Duration:", dur);

        addRow(formGrid, gbc, r++, "Reason:", reasonField);

        JButton btnBook        = new JButton("Book Appointment");
        JButton btnLoadByPid   = new JButton("Load by Patient ID");
        JButton btnLoadAll     = new JButton("Load All");
        JButton btnCancelSel   = new JButton("Cancel Selected");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.add(btnBook);
        buttons.add(btnLoadByPid);
        buttons.add(btnLoadAll);
        buttons.add(btnCancelSel);

        JPanel formCard = titled("New Appointment", formGrid);
        JPanel north = new JPanel(new BorderLayout(12,12));
        north.add(formCard, BorderLayout.CENTER);
        north.add(buttons, BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);

        // Actions
        btnBook.addActionListener(e -> onBook());
        btnLoadAll.addActionListener(e -> reloadAll());
        btnLoadByPid.addActionListener(e -> reloadByPid());
        btnCancelSel.addActionListener(e -> onCancelSelected());
    }

    private static JPanel titled(String title, JComponent content) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(content, BorderLayout.CENTER);
        return p;
    }
    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; p.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1; p.add(field, gbc);
    }

    private void onBook() {
        try {
            String pid = txt(patientIdField);
            String pname = txt(patientNameField);
            String profName = txt(professionalNameField);
            String ptype = Objects.toString(professionalTypeCombo.getSelectedItem(), "");
            String date = txt(dateField);
            String time = txt(timeField);
            String reason = txt(reasonField);
            int duration = (Integer) durationSpinner.getValue();

            if (pid.isEmpty() || pname.isEmpty() || profName.isEmpty() || "Select Type".equals(ptype) || date.isEmpty() || time.isEmpty() || reason.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fill all fields.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            LocalDateTime at = LocalDateTime.parse(date + "T" + time);
            CalendarDTO dto = CalendarDTO.newFromUI(pid, pname, profName, ptype, at, reason, duration);

            boolean ok = CalendarWiring.dispatcher().bookAppointment(dto);
            if (!ok) { JOptionPane.showMessageDialog(this, "Booking failed.", "Error", JOptionPane.ERROR_MESSAGE); return; }

            appendRow(dto);
            clearForm();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid date/time. Use YYYY-MM-DD and HH:MM.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancelSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select an appointment row to cancel.", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
        String idStr = Objects.toString(tableModel.getValueAt(row, 0), "");
        if (idStr.isEmpty()) { JOptionPane.showMessageDialog(this, "Selected row has no ID.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        try {
            UUID id = UUID.fromString(idStr);
            int confirm = JOptionPane.showConfirmDialog(this, "Cancel the selected appointment?", "Confirm", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.OK_OPTION) return;

            boolean ok = CalendarWiring.dispatcher().cancelAppointment(id);
            if (!ok) { JOptionPane.showMessageDialog(this, "Cancel failed.", "Error", JOptionPane.ERROR_MESSAGE); return; }
            tableModel.removeRow(row);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Invalid appointment ID.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reloadAll() {
        tableModel.setRowCount(0);
        List<CalendarDTO> list = CalendarWiring.dispatcher().listAllAppointments();
        for (CalendarDTO a : list) appendRow(a);
    }

    private void reloadByPid() {
        String pid = txt(patientIdField);
        if (pid.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter Patient ID.", "Validation", JOptionPane.WARNING_MESSAGE); return; }
        tableModel.setRowCount(0);
        List<CalendarDTO> list = CalendarWiring.dispatcher().listAppointmentsByPatient(pid);
        for (CalendarDTO a : list) appendRow(a);
    }

    private void appendRow(CalendarDTO a) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
        tableModel.addRow(new Object[]{
                a.id().toString(),
                nz(a.patientId()), nz(a.patientName()), nz(a.professionalName()), nz(a.professionalType()),
                a.appointmentTime().format(fmt),
                a.durationMinutes() + " minutes",
                nz(a.reason())
        });
        hideIdColumn();
    }

    private void hideIdColumn() {
        if (table.getColumnModel().getColumnCount() == 0) return;
        try {
            TableColumn idCol = table.getColumnModel().getColumn(0);
            idCol.setMinWidth(0); idCol.setMaxWidth(0); idCol.setPreferredWidth(0); idCol.setResizable(false);
        } catch (Exception ignored) {}
    }

    private void clearForm() {
        patientIdField.setText(""); patientNameField.setText(""); professionalNameField.setText("");
        professionalTypeCombo.setSelectedIndex(0); dateField.setText(""); timeField.setText("");
        reasonField.setText(""); durationSpinner.setValue(30);
    }

    private static String txt(JTextField tf) { String s = tf.getText(); return s == null ? "" : s.trim(); }
    private static String nz(String s) { return s == null ? "" : s; }
}
