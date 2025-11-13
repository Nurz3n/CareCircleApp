package com.carecircleclient;

import com.dataAccess.Csv;
import com.dataAccess.Settings;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Read-only vitals viewer for clinicians (LIST/LIST ALL). */
public final class HealthCareProvider extends JPanel {
    private final JTextField tfPatientId = new JTextField(18);
    private final JButton btnLoadAll = new JButton("Load All");
    private final JButton btnLoadById = new JButton("Load by Patient ID");
    private final DefaultTableModel model = new DefaultTableModel();
    private final JTable table = new JTable(model);

    public HealthCareProvider() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        var bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.add(new JLabel("Patient ID:"));
        bar.add(tfPatientId);
        bar.add(btnLoadById);
        bar.add(btnLoadAll);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(22);
        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnLoadAll.addActionListener(e -> fetchAsync("LIST ALL"));
        btnLoadById.addActionListener(e -> {
            var id = tfPatientId.getText().trim();
            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a Patient ID.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            fetchAsync("LIST " + id);
        });

        fetchAsync("LIST ALL");
    }

    private void fetchAsync(String cmd) {
        setBusy(true);
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return queryServer(cmd);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    renderCsv(get());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(HealthCareProvider.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private static List<String> queryServer(String cmd) throws IOException {
        try (var socket = new Socket(Settings.SERVER_HOST, Settings.SERVER_PORT);
             var br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            socket.setSoTimeout(3000);
            bw.write(cmd);
            bw.newLine();
            bw.flush();
            var lines = new ArrayList<String>();
            String line;
            while ((line = br.readLine()) != null) {
                if ("END".equalsIgnoreCase(line)) {
                    break;
                }
                lines.add(line);
            }
            return lines;
        }
    }

    private void renderCsv(List<String> lines) {
        if (lines.isEmpty()) {
            model.setDataVector(new Object[0][0], new Object[]{"(no data)"});
            return;
        }
        if (lines.size() == 1 && lines.get(0).startsWith("ERROR:")) {
            JOptionPane.showMessageDialog(this, lines.get(0), "Server Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        var header = Csv.split(lines.get(0));
        var cols = header.toArray(new String[0]);
        var data = new ArrayList<Object[]>();
        for (int i = 1; i < lines.size(); i++) {
            data.add(Csv.split(lines.get(i)).toArray(new String[0]));
        }
        model.setDataVector(data.toArray(Object[][]::new), cols);
        for (int i = 0; i < cols.length; i++) {
            try {
                table.getColumnModel().getColumn(i).setPreferredWidth(Math.min(260, Math.max(90, cols[i].length() * 9)));
            } catch (Exception ignored) {
            }
        }
    }

    private void setBusy(boolean busy) {
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
        btnLoadAll.setEnabled(!busy);
        btnLoadById.setEnabled(!busy);
    }
}
