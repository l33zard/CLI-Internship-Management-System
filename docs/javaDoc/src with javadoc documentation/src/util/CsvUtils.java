package util;

import java.util.*;

/** Minimal CSV helpers shared by repositories. */
public class CsvUtils {
    /** Split a CSV line into cells (quoted fields supported, no multiline). */
    public static String[] splitCsv(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') { 
                    sb.append('"'); 
                    i++; 
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                parts.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }
        parts.add(sb.toString().trim());
        return parts.toArray(new String[0]);
    }

    /** Build a lowercase index map for headers. */
    public static Map<String, Integer> indexMap(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            if (headers[i] != null) {
                map.put(headers[i].toLowerCase().trim(), i);
            }
        }
        return map;
    }

    /** Get a cell by any header variant. */
    public static String val(String[] row, Map<String, Integer> idx, String[] variants) {
        for (String v : variants) {
            Integer i = idx.get(v.toLowerCase());
            if (i != null && i < row.length && row[i] != null) {
                String value = row[i].trim();
                return value.isEmpty() ? null : value;
            }
        }
        return null; // Return null instead of throwing exception for better error handling
    }

    /** Escape when writing CSV (quotes if needed). */
    public static String esc(String v) {
        if (v == null) return "";
        boolean needsQuotes = v.contains(",") || v.contains("\"") || v.contains("\n");
        String out = v.replace("\"", "\"\"");
        return needsQuotes ? "\"" + out + "\"" : out;
    }
}