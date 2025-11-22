package com.carecircleclient;

import javax.swing.*;

public final class PatientUI {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

            JFrame frame = new JFrame("CareCircle – Patient Portal");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setSize(1040, 720);
            frame.setLocationRelativeTo(null);

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Vitals", new VitalsSubmitPanel());
            tabs.addTab("Appointments", new AppointmentsPanel());

            frame.setContentPane(tabs);
            frame.setVisible(true);
        });
    }
}