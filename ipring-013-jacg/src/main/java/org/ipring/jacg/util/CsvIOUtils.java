package org.ipring.jacg.util;

import java.io.File;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public final class CsvIOUtils {

    private CsvIOUtils() {
    }

    public static String csvAlwaysQuote(String value) {
        String text = value == null ? "" : value;
        String escaped = text.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    public static String csvAutoQuote(String value) {
        if (value == null) {
            return "";
        }
        boolean needQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needQuote ? "\"" + escaped + "\"" : escaped;
    }

    public static void ensureParentDirectory(Path path) throws Exception {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    public static void ensureParentDirectory(File file) {
        File parent = file.getParentFile();
        if (parent == null || parent.exists()) {
            return;
        }
        if (!parent.mkdirs()) {
            throw new IllegalStateException("Failed to create output directory: " + parent.getAbsolutePath());
        }
    }

    public static void writeUtf8(Path outputPath, String content) throws Exception {
        ensureParentDirectory(outputPath);
        writeBytes(outputPath, content.getBytes(StandardCharsets.UTF_8));
    }

    public static void writeLines(Path filePath, List<String> lines) throws Exception {
        String lineSeparator = System.lineSeparator();
        String text = String.join(lineSeparator, lines);
        if (!text.isEmpty() && !text.endsWith(lineSeparator)) {
            text = text + lineSeparator;
        }
        writeUtf8(filePath, text);
    }

    public static void writeBytes(Path filePath, byte[] bytes) throws Exception {
        try (FileChannel channel = FileChannel.open(
                filePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        )) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
    }

    public static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    sb.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (c == ',' && !inQuotes) {
                fields.add(sb.toString());
                sb.setLength(0);
                continue;
            }
            sb.append(c);
        }
        fields.add(sb.toString());
        return fields;
    }

    public static void writeLines(File output, List<String> lines) throws Exception {
        ensureParentDirectory(output);
        try (PrintWriter writer = new PrintWriter(output, StandardCharsets.UTF_8.name())) {
            for (String line : lines) {
                writer.println(line);
            }
        }
    }
}
