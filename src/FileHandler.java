import java.io.*;
import java.nio.file.*;

/**
 * FileHandler - Utility class for reading/writing text and binary files.
 */
public class FileHandler {

    /**
     * Reads all lines from a text file and returns them as an array.
     * Skips blank lines and comment lines (starting with REM or //).
     *
     * @param filePath Path to the input .txt script file.
     * @return Array of non-blank, non-comment lines.
     * @throws IOException if the file cannot be read.
     */
    public static String[] readScriptFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Script file not found: " + filePath);
        }
        if (!Files.isReadable(path)) {
            throw new IOException("Script file is not readable: " + filePath);
        }

        return Files.lines(path)
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("//") && !line.toUpperCase().startsWith("REM "))
                .toArray(String[]::new);
    }

    /**
     * Writes a byte array to a binary file.
     *
     * @param filePath    Destination file path.
     * @param data        Bytes to write.
     * @throws IOException if the file cannot be written.
     */
    public static void writeBinaryFile(String filePath, byte[] data) throws IOException {
        Path path = Paths.get(filePath);
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Files.write(path, data);
    }

    /**
     * Reads a binary file and returns its contents as a byte array.
     *
     * @param filePath Path to the .bin file.
     * @return Byte array of file contents.
     * @throws IOException if the file cannot be read.
     */
    public static byte[] readBinaryFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Binary file not found: " + filePath);
        }
        if (!Files.isReadable(path)) {
            throw new IOException("Binary file is not readable: " + filePath);
        }
        return Files.readAllBytes(path);
    }

    /**
     * Writes a decoded script (list of lines) to a text file.
     *
     * @param filePath Destination file path.
     * @param lines    Lines of decoded script.
     * @throws IOException if the file cannot be written.
     */
    public static void writeScriptFile(String filePath, java.util.List<String> lines) throws IOException {
        Path path = Paths.get(filePath);
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Files.write(path, lines);
    }

    /**
     * Validates that a file path ends with the expected extension.
     *
     * @param filePath  File path to check.
     * @param extension Expected extension (e.g. ".txt" or ".bin").
     * @return true if valid, false otherwise.
     */
    public static boolean hasExtension(String filePath, String extension) {
        return filePath != null && filePath.toLowerCase().endsWith(extension.toLowerCase());
    }

    /**
     * Returns the file size in bytes, or -1 if the file does not exist.
     */
    public static long fileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            return -1;
        }
    }
}
