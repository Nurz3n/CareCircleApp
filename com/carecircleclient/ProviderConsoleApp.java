package com.carecircleclient;

import javax.swing.*;

public final class ProviderConsoleApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

            JFrame f = new JFrame("CareCircle – HealthCare Provider");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Vitals", new ProviderVitalsPanel());
            tabs.addTab("Upcoming Appointments", new ProviderAppointmentsPanel());

            f.setContentPane(tabs);
            f.setSize(1100, 760);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}