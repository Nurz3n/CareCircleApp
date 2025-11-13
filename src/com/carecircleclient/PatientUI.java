package com.carecircleclient;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;

public final class PatientUI {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            var frame = new JFrame("CareCircle – Patient Portal");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setSize(980, 720);
            frame.setLocationRelativeTo(null);

            var tabs = new JTabbedPane();
            tabs.addTab("Vitals", new VitalsSubmitPanel());
            tabs.addTab("Appointments", new AppointmentsPanel());
            tabs.addTab("HealthCare Provider", new HealthCareProvider());

            frame.setLayout(new BorderLayout());
            frame.add(tabs, BorderLayout.CENTER);
            frame.setVisible(true);
        });
    }
}
