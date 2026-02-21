package com.example.quanlysinhvien.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CsvUtil {
    public static File saveCsv(Context ctx, String filename, List<String[]> rows) throws Exception {
        File dir = ctx.getExternalFilesDir(null);
        if (dir == null) dir = ctx.getFilesDir();
        File f = new File(dir, filename);
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            for (String[] row : rows) {
                w.write(escapeCsvRow(row));
                w.write('\n');
            }
            w.flush();
        }
        return f;
    }

    private static String escapeCsvRow(String[] row) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < row.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(row[i]));
        }
        return sb.toString();
    }

    private static String escape(String v) {
        if (v == null) return "";
        boolean needQuote = v.contains(",") || v.contains("\n") || v.contains("\r") || v.contains("\"");
        String out = v.replace("\"", "\"\"");
        if (needQuote) return '"' + out + '"';
        return out;
    }
}

