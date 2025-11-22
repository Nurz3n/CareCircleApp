package com.carecircleclient;

import com.carecircleserver.calendarSystem.CalendarWiring;
import com.dataAccess.CalendarDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/** Provider: Upcoming Appointments (two-column) with larger icons. */
public final class ProviderAppointmentsPanel extends JPanel {
    private static final int ICON = Icons.DEFAULT_LABEL_ICON_SIZE;

    private final JTextField tfPatientId = new JTextField(16);
    private final JSpinner   spDaysAhead = new JSpinner(new SpinnerNumberModel(30, 1, 365, 1));
    private final JButton btnLoadUpcoming    = new JButton("Load Upcoming", Icons.icon("appointment", "📅", ICON));
    private final JButton btnLoadAllUpcoming = new JButton("Load All Upcoming", Icons.icon("refresh", "🔄", ICON));
    private final JButton btnReset           = makeLinkButton("Reset Filters");

    private final DefaultTableModel model = new DefaultTableModel(new String[]{
            "Patient ID","Patient Name","Professional","Type","Time","Duration","Reason"
    }, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    private final JTable table = new JTable(model);

    public ProviderAppointmentsPanel() {
        setLayout(new BorderLayout(12,12));
        setBorder(new EmptyBorder(16,16,16,16));

        // Left: filters
        JPanel filters = new JPanel();
        filters.setOpaque(false);
        filters.setLayout(new BoxLayout(filters, BoxLayout.Y_AXIS));
        filters.setBorder(new EmptyBorder(8,8,8,8));

        filters.add(row("Patient ID:", tfPatientId, Icons.icon("user", "👤", ICON)));
        filters.add(Box.createVerticalStrut(10));
        filters.add(row("Next days:",  spDaysAhead, Icons.icon("calendar", "📅", ICON)));
        filters.add(Box.createVerticalStrut(14));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(btnLoadUpcoming);
        actions.add(btnLoadAllUpcoming);
        filters.add(actions);
        filters.add(btnReset);

        JPanel filterCard = titled("Filters", filters);

        // Right: table
        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JPanel tableCard = titled("Upcoming Appointments", new JScrollPane(table));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, filterCard, tableCard);
        split.setResizeWeight(0.25);
        split.setDividerSize(12);
        add(split, BorderLayout.CENTER);

        btnLoadUpcoming.addActionListener(e -> load(false));
        btnLoadAllUpcoming.addActionListener(e -> load(true));
        btnReset.addActionListener(e -> { tfPatientId.setText(""); spDaysAhead.setValue(30); load(true); });

        load(true);
    }

    private static JPanel titled(String title, JComponent content) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(content, BorderLayout.CENTER);
        return p;
    }
    private static JPanel row(String label, JComponent field, Icon icon) {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JLabel l = new JLabel(label);
        if (icon != null) { l.setIcon(icon); l.setIconTextGap(8); }
        r.add(l); r.add(field);
        return r;
    }
    private static JButton makeLinkButton(String text) {
        JButton b = new JButton(text);
        b.setBorderPainted(false); b.setContentAreaFilled(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void load(boolean allPatients) {
        String pid  = tfPatientId.getText().trim();
        int days    = (Integer) spDaysAhead.getValue();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plusDays(days);

        List<CalendarDTO> source = (allPatients || pid.isEmpty())
                ? CalendarWiring.dispatcher().listAllAppointments()
                : CalendarWiring.dispatcher().listAppointmentsByPatient(pid);

        model.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

        for (CalendarDTO a : source) {
            LocalDateTime at = a.appointmentTime();
            if (at == null || at.isBefore(now) || at.isAfter(until)) continue;
            model.addRow(new Object[]{
                    nz(a.patientId()), nz(a.patientName()), nz(a.professionalName()), nz(a.professionalType()),
                    at.format(fmt), a.durationMinutes() + " minutes", nz(a.reason())
            });
        }
        autosize();

        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No upcoming appointments in next " + days +
                    " days" + ((allPatients || pid.isEmpty()) ? "" : " for patient " + pid) + ".", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    private void autosize() {
        for (int i = 0; i < model.getColumnCount(); i++) {
            try {
                int width = 120;
                for (int r = 0; r < model.getRowCount(); r++) {
                    Object v = model.getValueAt(r, i);
                    width = Math.max(width, Objects.toString(v, "").length() * 7);
                }
                table.getColumnModel().getColumn(i).setPreferredWidth(Math.min(260, Math.max(90, width)));
            } catch (Exception ignored) {}
        }
    }
    private static String nz(String s) { return s == null ? "" : s; }
}