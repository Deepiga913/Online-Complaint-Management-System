package com.cms.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralizes all disk I/O using java.nio.file. Two write strategies are offered:
 *  - appendLine: fast, non-destructive append (used for logs and inserts)
 *  - writeCsvAtomic: writes the full dataset to a temp file then atomically
 *    swaps it into place, so a rewrite (e.g. editing one complaint's status)
 *    can never leave the file half-written or corrupt neighboring rows.
 */
public final class FileHandler {

    private FileHandler() {
    }

    public static void ensureFileWithHeader(Path path, String header) throws IOException {
        if (!Files.exists(path)) {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, header + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    /** Reads a CSV file and returns each data row (header excluded) as a String[]. */
    public static List<String[]> readCsv(Path path) throws IOException {
        List<String[]> rows = new ArrayList<>();
        if (!Files.exists(path)) {
            return rows;
        }
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) { // skip header
            String line = lines.get(i);
            if (line.isBlank()) continue;
            rows.add(parseLine(line));
        }
        return rows;
    }

    /** Appends a single already-escaped CSV line (used for logs & simple inserts). */
    public static void appendLine(Path path, String csvLine) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, csvLine + System.lineSeparator(),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Atomically rewrites the entire CSV file with the given header + rows.
     * Writes to a sibling temp file first, then performs an atomic move so
     * readers never observe a partially-written file.
     */
    public static synchronized void writeCsvAtomic(Path path, String header, List<String[]> rows) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Path tempFile = Files.createTempFile(path.getParent(), "tmp_", ".csv");
        StringBuilder sb = new StringBuilder();
        sb.append(header).append(System.lineSeparator());
        for (String[] row : rows) {
            sb.append(toCsvLine(row)).append(System.lineSeparator());
        }
        Files.writeString(tempFile, sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        try {
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static String toCsvLine(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(fields[i] == null ? "" : fields[i]));
        }
        return sb.toString();
    }

    private static String escape(String field) {
        boolean needsQuoting = field.contains(",") || field.contains("\"") || field.contains("\n");
        if (!needsQuoting) {
            return field;
        }
        return "\"" + field.replace("\"", "\"\"") + "\"";
    }

    /** Minimal state-machine CSV line parser supporting quoted fields with embedded commas/quotes. */
    private static String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
