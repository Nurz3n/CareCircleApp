package com.carecircleclient;

import com.dataAccess.Csv;
import com.dataAccess.Settings;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/** Provider vitals viewer with larger icons + Monthly Ave dialog. */
public final class ProviderVitalsPanel extends JPanel {
    private static final int ICON = Icons.DEFAULT_LABEL_ICON_SIZE;

    private final JTextField tfPatientId = new JTextField(16);
    private final JButton btnLoadAll     = new JButton("Load All", Icons.icon("refresh", "🔄", ICON));
    private final JButton btnLoadById    = new JButton("Load by Patient ID", Icons.icon("user", "👤", ICON));
    private final JButton btnMonthlyAvg  = new JButton("Monthly Ave", Icons.icon("calendar", "📆", ICON));

    private final DefaultTableModel model = new DefaultTableModel();
    private final JTable table = new JTable(model);

    private final JLabel lblCount   = bold("—");
    private final JLabel lblAvgHr   = bold("—");
    private final JLabel lblAvgBp   = bold("—");
    private final JLabel lblAvgTemp = bold("—");
    private final JLabel lblAvgWgt  = bold("—");
    private final JLabel lblMood    = bold("—");

    public ProviderVitalsPanel() {
        setLayout(new BorderLayout(12,12));
        setBorder(new EmptyBorder(16,16,16,16));

        JPanel ctrl = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        JLabel pidLabel = new JLabel("Patient ID:");
        pidLabel.setIcon(Icons.icon("user", "👤", ICON));
        pidLabel.setIconTextGap(8);

        c.gridx=0; c.gridy=0; ctrl.add(pidLabel, c);
        c.gridx=1; c.gridy=0; ctrl.add(tfPatientId, c);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btns.add(btnLoadById);
        btns.add(btnMonthlyAvg);
        btns.add(btnLoadAll);
        c.gridx=2; c.gridy=0; c.weightx = 0; ctrl.add(btns, c);

        JPanel ctrlCard = titled("Vitals Filters", ctrl);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(26);
        JPanel tableCard = titled("Patient Vitals", new JScrollPane(table));

        JPanel sumGrid = new JPanel(new GridBagLayout());
        GridBagConstraints sg = new GridBagConstraints();
        sg.insets = new Insets(4,10,4,10);
        sg.anchor = GridBagConstraints.WEST;

        int r = 0;
        addRow(sumGrid, sg, r++, labelWithIcon("Records:", Icons.icon("list", "≣", ICON)), lblCount);
        addRow(sumGrid, sg, r++, labelWithIcon("Avg Heart Rate (bpm):", Icons.icon("heart", "❤️", ICON)), lblAvgHr);
        addRow(sumGrid, sg, r++, labelWithIcon("Avg BP (Sys/Dia):", Icons.icon("cross", "➕", ICON)), lblAvgBp);
        addRow(sumGrid, sg, r++, labelWithIcon("Avg Temperature (°C):", Icons.icon("thermometer", "🌡", ICON)), lblAvgTemp);
        addRow(sumGrid, sg, r++, labelWithIcon("Avg Weight (kg):", Icons.icon("weight", "⚖", ICON)), lblAvgWgt);
        addRow(sumGrid, sg, r,   labelWithIcon("Most Common Mood:", Icons.icon("mood", "🙂", ICON)), lblMood);

        JPanel sumCard = titled("Summary (current view)", sumGrid);

        add(ctrlCard, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
        add(sumCard, BorderLayout.SOUTH);

        btnLoadAll.addActionListener(e -> fetchAsync("LIST ALL"));
        btnLoadById.addActionListener(e -> {
            var id = tfPatientId.getText().trim();
            if (id.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter a Patient ID.", "Validation", JOptionPane.WARNING_MESSAGE); return; }
            fetchAsync("LIST " + id);
        });
        btnMonthlyAvg.addActionListener(e -> monthlyAvgAsync());

        fetchAsync("LIST ALL");
    }

    private static JPanel titled(String title, JComponent content) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(content, BorderLayout.CENTER);
        return p;
    }
    private static void addRow(JPanel p, GridBagConstraints gbc, int row, JComponent label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; p.add(label, gbc);
        gbc.gridx = 1; gbc.weightx = 1; p.add(field, gbc);
    }
    private static JLabel labelWithIcon(String text, Icon icon) {
        JLabel l = new JLabel(text);
        if (icon != null) { l.setIcon(icon); l.setIconTextGap(8); }
        return l;
    }
    private static JLabel bold(String s) { var l = new JLabel(s); l.setFont(l.getFont().deriveFont(Font.BOLD)); return l; }

    private void fetchAsync(String cmd) {
        setBusy(true);
        new SwingWorker<List<String>, Void>() {
            @Override protected List<String> doInBackground() throws Exception { return queryServer(cmd); }
            @Override protected void done() {
                setBusy(false);
                try { var lines = get(); renderCsv(lines); updateSummary(lines); }
                catch (Exception ex) { JOptionPane.showMessageDialog(ProviderVitalsPanel.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); clearSummary(); }
            }
        }.execute();
    }

    private static List<String> queryServer(String cmd) throws IOException {
        try (var socket = new Socket(Settings.SERVER_HOST, Settings.SERVER_PORT);
             var br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            socket.setSoTimeout(3000);
            bw.write(cmd); bw.newLine(); bw.flush();
            var lines = new ArrayList<String>(); String line;
            while ((line = br.readLine()) != null) { if ("END".equalsIgnoreCase(line)) break; lines.add(line); }
            return lines;
        }
    }

    private void renderCsv(List<String> lines) {
        if (lines.isEmpty()) { model.setDataVector(new Object[0][0], new Object[]{"(no data)"}); return; }
        if (lines.size() == 1 && lines.get(0).startsWith("ERROR:")) {
            JOptionPane.showMessageDialog(this, lines.get(0), "Server Error", JOptionPane.ERROR_MESSAGE);
            model.setDataVector(new Object[0][0], new Object[]{"(error)"}); return;
        }
        var header = Csv.split(lines.get(0));
        var cols = header.toArray(new String[0]);
        var data = new ArrayList<Object[]>();
        for (int i = 1; i < lines.size(); i++) data.add(Csv.split(lines.get(i)).toArray(new String[0]));
        model.setDataVector(data.toArray(Object[][]::new), cols);
        for (int i = 0; i < cols.length; i++) {
            int width = Math.min(260, Math.max(100, cols[i].length() * 9));
            try { table.getColumnModel().getColumn(i).setPreferredWidth(width); } catch (Exception ignored) {}
        }
    }

    private void updateSummary(List<String> lines) {
        if (lines.isEmpty() || lines.get(0).startsWith("ERROR:")) { clearSummary(); return; }
        var header = Csv.split(lines.get(0));
        Map<String,Integer> idx = new HashMap<>();
        for (int i=0;i<header.size();i++) idx.put(header.get(i).toLowerCase(), i);
        int iHr   = idx.getOrDefault("heartratebpm", 1);
        int iSys  = idx.getOrDefault("bpsystolic",   2);
        int iDia  = idx.getOrDefault("bpdiastolic",  3);
        int iTemp = idx.getOrDefault("temperaturec", 4);
        int iMood = idx.getOrDefault("mood",         5);
        int iWgt  = idx.getOrDefault("weightkg",     7);

        var statsHr = new Avg(); var statsSys = new Avg(); var statsDia = new Avg();
        var statsTemp = new Avg(); var statsWgt = new Avg(); var moodFreq = new HashMap<String, Integer>();
        int records = 0;
        for (int r = 1; r < lines.size(); r++) {
            var row = Csv.split(lines.get(r));
            records++;
            statsHr.add(parseDoubleSafe(row, iHr));
            statsSys.add(parseDoubleSafe(row, iSys));
            statsDia.add(parseDoubleSafe(row, iDia));
            statsTemp.add(parseDoubleSafe(row, iTemp));
            statsWgt.add(parseDoubleSafe(row, iWgt));
            String mood = getSafe(row, iMood);
            if (!mood.isEmpty()) moodFreq.merge(mood, 1, Integer::sum);
        }
        lblCount.setText(Integer.toString(records));
        lblAvgHr.setText(statsHr.present() ? f1(statsHr.mean()) + " (n=" + statsHr.n + ")" : "—");
        lblAvgBp.setText((statsSys.present() || statsDia.present())
                ? (f1(statsSys.mean()) + "/" + f1(statsDia.mean()) + " (n=" + Math.max(statsSys.n, statsDia.n) + ")")
                : "—");
        lblAvgTemp.setText(statsTemp.present() ? f1(statsTemp.mean()) : "—");
        lblAvgWgt.setText(statsWgt.present() ? f1(statsWgt.mean()) : "—");
        lblMood.setText(mode(moodFreq));
    }

    private void monthlyAvgAsync() {
        var id = tfPatientId.getText().trim();
        if (id.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter a Patient ID to compute monthly averages.", "Validation", JOptionPane.WARNING_MESSAGE); return; }
        setBusy(true);
        new SwingWorker<List<String>, Void>() {
            @Override protected List<String> doInBackground() throws Exception { return queryServer("LIST " + id); }
            @Override protected void done() {
                setBusy(false);
                try { showMonthlyDialog(id, get()); }
                catch (Exception ex) { JOptionPane.showMessageDialog(ProviderVitalsPanel.this, "Failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
            }
        }.execute();
    }

    private void showMonthlyDialog(String patientId, List<String> lines) {
        if (lines.isEmpty() || lines.get(0).startsWith("ERROR:")) {
            JOptionPane.showMessageDialog(this, lines.isEmpty() ? "No data." : lines.get(0), "Monthly Ave", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        var header = Csv.split(lines.get(0));
        Map<String,Integer> idx = new HashMap<>();
        for (int i=0;i<header.size();i++) idx.put(header.get(i).toLowerCase(), i);
        int iHr=idx.getOrDefault("heartratebpm",1), iSys=idx.getOrDefault("bpsystolic",2), iDia=idx.getOrDefault("bpdiastolic",3),
                iTemp=idx.getOrDefault("temperaturec",4), iMood=idx.getOrDefault("mood",5), iWgt=idx.getOrDefault("weightkg",7),
                iSub=idx.getOrDefault("submittedat",8);

        Map<YearMonth, MonthlyAgg> byMonth = new TreeMap<>();
        ZoneId zone = ZoneId.systemDefault();
        for (int r = 1; r < lines.size(); r++) {
            var row = Csv.split(lines.get(r));
            YearMonth ym = parseYearMonth(getSafe(row, iSub), zone);
            if (ym == null) continue;
            var agg = byMonth.computeIfAbsent(ym, k -> new MonthlyAgg());
            agg.hr.add(parseDoubleSafe(row, iHr));
            agg.sys.add(parseDoubleSafe(row, iSys));
            agg.dia.add(parseDoubleSafe(row, iDia));
            agg.temp.add(parseDoubleSafe(row, iTemp));
            agg.wgt.add(parseDoubleSafe(row, iWgt));
            String mood = getSafe(row, iMood);
            if (!mood.isEmpty()) agg.mood.merge(mood, 1, Integer::sum);
            agg.count++;
        }
        if (byMonth.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No dated records to compute monthly averages.", "Monthly Ave", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] cols = {"Month", "Count", "Avg HR", "Avg Sys", "Avg Dia", "Avg Temp", "Avg Wgt", "Top Mood"};
        var data = new ArrayList<Object[]>();
        DateTimeFormatter ymFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (var e : byMonth.entrySet()) {
            var m = e.getValue();
            data.add(new Object[]{
                    e.getKey().format(ymFmt),
                    m.count,
                    m.hr.present()  ? f1(m.hr.mean())  : "—",
                    m.sys.present() ? f1(m.sys.mean()) : "—",
                    m.dia.present() ? f1(m.dia.mean()) : "—",
                    m.temp.present()? f1(m.temp.mean()): "—",
                    m.wgt.present() ? f1(m.wgt.mean()) : "—",
                    mode(m.mood)
            });
        }
        var dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Monthly Ave — " + patientId, Dialog.ModalityType.MODELESS);
        var tModel = new DefaultTableModel(data.toArray(Object[][]::new), cols);
        var t = new JTable(tModel);
        t.setRowHeight(24);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        var sp = new JScrollPane(t);
        int[] widths = {100, 70, 100, 100, 100, 100, 100, 130};
        for (int i = 0; i < cols.length; i++) try { t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]); } catch (Exception ignored) {}
        var close = new JButton("Close");
        close.addActionListener(e -> dlg.dispose());
        var south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(close);
        dlg.getContentPane().add(sp, BorderLayout.CENTER);
        dlg.getContentPane().add(south, BorderLayout.SOUTH);
        dlg.setSize(860, 340);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private static YearMonth parseYearMonth(String ts, ZoneId zone) {
        try { Instant inst = Instant.parse(ts); var ld = inst.atZone(zone).toLocalDate(); return YearMonth.of(ld.getYear(), ld.getMonth()); }
        catch (Exception e) { return null; }
    }
    private static String mode(Map<String, Integer> freq) {
        if (freq.isEmpty()) return "—";
        String best = null; int bestN = -1;
        for (var e : freq.entrySet()) if (e.getValue() > bestN) { best = e.getKey(); bestN = e.getValue(); }
        return best + " (n=" + bestN + ")";
    }
    private static double parseDoubleSafe(List<String> row, int i) {
        try { if (i < 0 || i >= row.size()) return Double.NaN; var s = row.get(i).trim(); if (s.isEmpty()) return Double.NaN; return Double.parseDouble(s); }
        catch (Exception e) { return Double.NaN; }
    }
    private static String getSafe(List<String> row, int i) { return (i >= 0 && i < row.size()) ? row.get(i).trim() : ""; }
    private static String f1(double v) { return String.format("%.1f", v); }

    private void clearSummary() { lblCount.setText("—"); lblAvgHr.setText("—"); lblAvgBp.setText("—"); lblAvgTemp.setText("—"); lblAvgWgt.setText("—"); lblMood.setText("—"); }
    private void setBusy(boolean busy) { setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor()); btnLoadAll.setEnabled(!busy); btnLoadById.setEnabled(!busy); btnMonthlyAvg.setEnabled(!busy); }

    private static final class Avg { double sum=0.0; int n=0; void add(double v){ if(!Double.isNaN(v)){ sum+=v; n++; }} boolean present(){ return n>0; } double mean(){ return n==0?Double.NaN:sum/n; } }
    private static final class MonthlyAgg { final Avg hr=new Avg(), sys=new Avg(), dia=new Avg(), temp=new Avg(), wgt=new Avg(); final Map<String,Integer> mood=new HashMap<>(); int count=0; }
}
