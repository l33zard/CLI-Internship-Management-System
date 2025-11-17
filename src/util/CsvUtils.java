package util;

import java.util.*;

/**
 * Minimal CSV helpers shared by repository implementations.
 *
 * Provides small utility methods for splitting and escaping CSV rows used by
 * the lightweight CSV load/save code in the repositories.
 */
public class CsvUtils {
    /**
     * Split a CSV line into fields.
     *
     * This supports quoted fields and doubled-quote escaping but does not support
     * multiline fields.
     *
     * @param line the CSV line to split (not null)
     * @return an array of field values (trimmed)
     */
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

    /**
     * Build a lowercase header-to-index map.
     *
     * @param headers header row tokens
     * @return a map from lowercased header name to column index
     */
    public static Map<String, Integer> indexMap(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            if (headers[i] != null) {
                map.put(headers[i].toLowerCase().trim(), i);
            }
        }
        return map;
    }

    /**
     * Get a cell value by trying multiple header name variants.
     *
     * This convenience attempts several possible header names (e.g. "email",
     * "mail") and returns the first non-empty value or null if none found.
     *
     * @param row CSV row tokens
     * @param idx header index map (lowercased names)
     * @param variants possible header names to try
     * @return the cell value (trimmed) or null if missing/empty
     */
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

    /**
     * Escape a value for CSV output (adds quotes when necessary).
     *
     * @param v raw value (may be null)
     * @return CSV-escaped string
     */
    public static String esc(String v) {
        if (v == null) return "";
        boolean needsQuotes = v.contains(",") || v.contains("\"") || v.contains("\n");
        String out = v.replace("\"", "\"\"");
        return needsQuotes ? "\"" + out + "\"" : out;
    }
}