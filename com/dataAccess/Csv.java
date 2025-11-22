package com.dataAccess;

import java.util.ArrayList;
import java.util.List;

/** Minimal CSV helpers with proper quote handling. */
public final class Csv {
    private Csv() {}

    public static String escape(String s) {
        if (s == null) return "";
        boolean needs = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String t = s.replace("\"", "\"\"");
        return needs ? ("\"" + t + "\"") : t;
    }
    public static String join(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            sb.append(escape(fields[i]));
            if (i < fields.length - 1) sb.append(',');
        }
        return sb.toString();
    }
    public static List<String> split(String line) {
        var out = new ArrayList<String>();
        if (line == null || line.isEmpty()) { out.add(""); return out; }
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else cur.append(ch);
            } else {
                if (ch == ',') { out.add(cur.toString()); cur.setLength(0); }
                else if (ch == '"') inQuotes = true;
                else cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out;
    }
}