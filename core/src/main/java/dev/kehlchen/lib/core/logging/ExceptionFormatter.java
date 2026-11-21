package dev.kehlchen.lib.core.logging;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class ExceptionFormatter {
    private ExceptionFormatter() {}
    public static String format(Throwable error) {
        if (error == null) return "";
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
    public static String formatOneLine(Throwable error) {
        if (error == null) return "";
        String message = error.getMessage();
        return error.getClass().getName() + (message == null ? "" : ": " + message);
    }
}
