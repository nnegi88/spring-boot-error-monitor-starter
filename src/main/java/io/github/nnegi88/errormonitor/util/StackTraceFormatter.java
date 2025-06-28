package io.github.nnegi88.errormonitor.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StackTraceFormatter {
    
    public static String format(Throwable throwable, int maxLines) {
        if (throwable == null) {
            return "";
        }
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        
        String fullStackTrace = sw.toString();
        String[] lines = fullStackTrace.split(System.lineSeparator());
        
        if (maxLines <= 0 || lines.length > maxLines) {
            StringBuilder truncated = new StringBuilder();
            int linesToShow = Math.max(0, Math.min(maxLines, lines.length));
            
            for (int i = 0; i < linesToShow; i++) {
                truncated.append(lines[i]);
                if (i < linesToShow - 1) {
                    truncated.append(System.lineSeparator());
                }
            }
            
            if (linesToShow > 0 && lines.length > linesToShow) {
                truncated.append(System.lineSeparator());
            }
            
            if (lines.length > linesToShow) {
                truncated.append("... ")
                        .append(lines.length - linesToShow)
                        .append(" more lines truncated");
            }
            
            return truncated.toString();
        }
        
        return fullStackTrace;
    }
}