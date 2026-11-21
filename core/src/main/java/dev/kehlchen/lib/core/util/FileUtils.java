package dev.kehlchen.lib.core.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class FileUtils {
    private FileUtils() {}
    public static Path ensureDirectory(Path directory) {
        try { return Files.exists(directory) ? directory : Files.createDirectories(directory); }
        catch (IOException e) { throw new IllegalStateException("Could not create directory: " + directory, e); }
    }
    public static Path ensureFile(Path file) {
        ensureDirectory(file.toAbsolutePath().getParent());
        try { if (!Files.exists(file)) Files.createFile(file); return file; }
        catch (IOException e) { throw new IllegalStateException("Could not create file: " + file, e); }
    }
    public static String readString(Path file) {
        try { return Files.readString(file, StandardCharsets.UTF_8); }
        catch (IOException e) { throw new IllegalStateException("Could not read file: " + file, e); }
    }
    public static void writeString(Path file, String content) {
        ensureDirectory(file.toAbsolutePath().getParent());
        try { Files.writeString(file, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING); }
        catch (IOException e) { throw new IllegalStateException("Could not write file: " + file, e); }
    }
    public static List<String> readLines(Path file) {
        try { return Files.readAllLines(file, StandardCharsets.UTF_8); }
        catch (IOException e) { throw new IllegalStateException("Could not read file: " + file, e); }
    }
}
