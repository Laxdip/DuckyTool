import java.io.IOException;
import java.util.List;
import java.util.Scanner;


public class Main {

    // -------------------------------------------------------------------------
    // ANSI colour constants (degrade gracefully if terminal doesn't support them)
    // -------------------------------------------------------------------------
    private static final class Ansi {
        static final String RESET  = "\u001B[0m";
        static final String BOLD   = "\u001B[1m";
        static final String DIM    = "\u001B[2m";
        static final String RED    = "\u001B[31m";
        static final String GREEN  = "\u001B[32m";
        static final String YELLOW = "\u001B[33m";
        static final String CYAN   = "\u001B[36m";

        private Ansi() {} // utility class — no instances
    }

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        // Command-line shortcut: encode / decode without interactive menu
        if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "encode": runEncode(args[1], args[2]); break;
                case "decode": runDecode(args[1], args[2]); break;
                default:
                    printUsage();
                    System.exit(1);
            }
            return;
        }

        // Interactive menu
        printBanner();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                printMenu();
                System.out.print(Ansi.BOLD + "  Your choice: " + Ansi.RESET);
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1": interactiveEncode(scanner); break;
                    case "2": interactiveDecode(scanner); break;
                    case "3":
                        System.out.println(Ansi.DIM + "\n  Goodbye!\n" + Ansi.RESET);
                        System.exit(0);
                        break;
                    case "4": printHelp(); break;
                    default:
                        error("Invalid choice. Enter 1, 2, 3 or 4.");
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Interactive prompts
    // -------------------------------------------------------------------------

    private static void interactiveEncode(Scanner scanner) {
        System.out.println();
        System.out.println(Ansi.CYAN + "  ┌─ ENCODE ──────────────────────────────────────────┐" + Ansi.RESET);
        System.out.print(              "  │ Input script file  (.txt) : ");
        String inputFile  = scanner.nextLine().trim();
        System.out.print(              "  │ Output binary file (.bin) : ");
        String outputFile = scanner.nextLine().trim();
        System.out.println(Ansi.CYAN + "  └───────────────────────────────────────────────────┘" + Ansi.RESET);

        if (inputFile.isEmpty() || outputFile.isEmpty()) {
            error("File paths cannot be empty.");
            return;
        }
        runEncode(inputFile, outputFile);
    }

    private static void interactiveDecode(Scanner scanner) {
        System.out.println();
        System.out.println(Ansi.CYAN + "  ┌─ DECODE ──────────────────────────────────────────┐" + Ansi.RESET);
        System.out.print(              "  │ Input binary file  (.bin) : ");
        String inputFile  = scanner.nextLine().trim();
        System.out.print(              "  │ Output script file (.txt) : ");
        String outputFile = scanner.nextLine().trim();
        System.out.println(Ansi.CYAN + "  └───────────────────────────────────────────────────┘" + Ansi.RESET);

        if (inputFile.isEmpty() || outputFile.isEmpty()) {
            error("File paths cannot be empty.");
            return;
        }
        runDecode(inputFile, outputFile);
    }

    // -------------------------------------------------------------------------
    // Core encode / decode routines
    // -------------------------------------------------------------------------

    private static void runEncode(String inputPath, String outputPath) {
        System.out.println();
        System.out.println(Ansi.BOLD + "  Encoding: " + Ansi.RESET + inputPath + " → " + outputPath);

        hintExtension(inputPath,  ".txt", "Input");
        hintExtension(outputPath, ".bin", "Output");

        String[] scriptLines;
        try {
            scriptLines = FileHandler.readScriptFile(inputPath);
        } catch (IOException e) {
            error("Cannot read input file: " + e.getMessage());
            return;
        }

        System.out.println("  Read " + scriptLines.length + " script line(s).");

        Encoder encoder = new Encoder();
        byte[] binaryData;
        try {
            binaryData = encoder.encode(scriptLines);
        } catch (Encoder.EncoderException e) {
            error("Encoding failed: " + e.getMessage());
            return;
        }

        try {
            FileHandler.writeBinaryFile(outputPath, binaryData);
        } catch (IOException e) {
            error("Cannot write output file: " + e.getMessage());
            return;
        }

        System.out.println(Ansi.GREEN + "  ✓ Encoding complete!" + Ansi.RESET);
        System.out.println("  Lines processed : " + encoder.getLineCount());
        System.out.println("  Warnings        : " + encoder.getWarningCount());
        System.out.println("  Output size     : " + binaryData.length + " bytes → " + outputPath);
        System.out.println();
    }

    private static void runDecode(String inputPath, String outputPath) {
        System.out.println();
        System.out.println(Ansi.BOLD + "  Decoding: " + Ansi.RESET + inputPath + " → " + outputPath);

        hintExtension(inputPath, ".bin", "Input");

        byte[] binaryData;
        try {
            binaryData = FileHandler.readBinaryFile(inputPath);
        } catch (IOException e) {
            error("Cannot read binary file: " + e.getMessage());
            return;
        }

        System.out.println("  Read " + binaryData.length + " byte(s).");

        Decoder decoder = new Decoder();
        List<String> scriptLines;
        try {
            scriptLines = decoder.decode(binaryData);
        } catch (Decoder.DecoderException e) {
            error("Decoding failed: " + e.getMessage());
            return;
        }

        try {
            FileHandler.writeScriptFile(outputPath, scriptLines);
        } catch (IOException e) {
            error("Cannot write output file: " + e.getMessage());
            return;
        }

        System.out.println(Ansi.GREEN + "  ✓ Decoding complete!" + Ansi.RESET);
        System.out.println("  Packets decoded : " + decoder.getPacketCount());
        System.out.println("  Warnings        : " + decoder.getWarningCount());
        System.out.println("  Output lines    : " + scriptLines.size() + " → " + outputPath);
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Display helpers
    // -------------------------------------------------------------------------

    private static void printBanner() {
        System.out.println();
        String b = Ansi.CYAN + Ansi.BOLD;
        System.out.println(b + "  ██████╗ ██╗   ██╗ ██████╗██╗  ██╗██╗   ██╗    ████████╗ ██████╗  ██████╗ ██╗"  + Ansi.RESET);
        System.out.println(b + "  ██╔══██╗██║   ██║██╔════╝██║ ██╔╝╚██╗ ██╔╝    ╚══██╔══╝██╔═══██╗██╔═══██╗██║" + Ansi.RESET);
        System.out.println(b + "  ██║  ██║██║   ██║██║     █████╔╝  ╚████╔╝        ██║   ██║   ██║██║   ██║██║"  + Ansi.RESET);
        System.out.println(b + "  ██║  ██║██║   ██║██║     ██╔═██╗   ╚██╔╝         ██║   ██║   ██║██║   ██║██║"  + Ansi.RESET);
        System.out.println(b + "  ██████╔╝╚██████╔╝╚██████╗██║  ██╗   ██║          ██║   ╚██████╔╝╚██████╔╝███████╗" + Ansi.RESET);
        System.out.println(b + "  ╚═════╝  ╚═════╝  ╚═════╝╚═╝  ╚═╝   ╚═╝          ╚═╝    ╚═════╝  ╚═════╝ ╚══════╝" + Ansi.RESET);
        System.out.println(Ansi.DIM + "              USB Rubber Ducky Script Encoder / Decoder  v1.0" + Ansi.RESET);
        System.out.println(Ansi.DIM + "                    Created by: PRASAD");
        System.out.println(Ansi.DIM + "                    GitHub: github.com/Laxdip");
        System.out.println();
    }

    private static void printMenu() {
        String b = Ansi.BOLD;
        System.out.println(b + "  ┌─────────────────────────────┐" + Ansi.RESET);
        System.out.println(b + "  │         MAIN MENU           │" + Ansi.RESET);
        System.out.println(b + "  ├─────────────────────────────┤" + Ansi.RESET);
        menuItem("1", "Encode  (.txt → .bin)");
        menuItem("2", "Decode  (.bin → .txt)");
        menuItem("3", "Exit                 ");
        menuItem("4", "Help / Command List  ");
        System.out.println(b + "  └─────────────────────────────┘" + Ansi.RESET);
    }

    private static void printHelp() {
        System.out.println();
        System.out.println(Ansi.BOLD + Ansi.CYAN
                + "  ── Supported Commands ──────────────────────────────────────" + Ansi.RESET);
        System.out.println();

        helpLine("DELAY <ms>",         "Pause for <ms> milliseconds (0–65535)");
        helpLine("STRING <text>",      "Type the given text (max 255 chars)");
        helpLine("ENTER",              "Press Enter key");
        helpLine("ESCAPE / ESC",       "Press Escape");
        helpLine("BACKSPACE",          "Press Backspace");
        helpLine("TAB",                "Press Tab");
        helpLine("DELETE / DEL",       "Press Delete");
        helpLine("HOME / END",         "Press Home / End");
        helpLine("PAGEUP / PAGEDOWN",  "Page navigation");
        helpLine("UP/DOWN/LEFT/RIGHT", "Arrow keys");
        helpLine("CAPS_LOCK",          "Toggle Caps Lock");
        helpLine("F1 … F12",           "Function keys");
        helpLine("PRINTSCREEN",        "Print Screen");

        System.out.println();
        System.out.println(Ansi.BOLD + "  Modifier combos:" + Ansi.RESET);
        helpLine("GUI <key>",          "WIN/CMD + key  e.g. GUI r");
        helpLine("CTRL <key>",         "CTRL + key     e.g. CTRL c");
        helpLine("ALT <key>",          "ALT  + key     e.g. ALT F4");
        helpLine("SHIFT <key>",        "SHIFT+ key     e.g. SHIFT F10");
        helpLine("CTRL ALT DELETE",    "Chained modifiers supported");

        System.out.println();
        System.out.println(Ansi.BOLD + "  Comments:" + Ansi.RESET);
        helpLine("REM <text>",  "Comment line (ignored during encoding)");
        helpLine("// <text>",   "Also treated as a comment");

        System.out.println();
        System.out.println(Ansi.BOLD + "  CLI usage:          " + Ansi.RESET
                + Ansi.DIM + "java -cp src Main encode input.txt output.bin" + Ansi.RESET);
        System.out.println(Ansi.BOLD + "                      " + Ansi.RESET
                + Ansi.DIM + "java -cp src Main decode input.bin output.txt" + Ansi.RESET);
        System.out.println();
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -cp src Main                        (interactive mode)");
        System.out.println("  java -cp src Main encode <in.txt> <out.bin>");
        System.out.println("  java -cp src Main decode <in.bin> <out.txt>");
    }

    // -------------------------------------------------------------------------
    // Small private utilities (formatting / messaging)
    // -------------------------------------------------------------------------

    /** Prints a single menu row with a coloured number key. */
    private static void menuItem(String key, String label) {
        System.out.println(Ansi.BOLD + "  │  " + Ansi.GREEN + key + Ansi.RESET
                + "  " + label + Ansi.BOLD + "   │" + Ansi.RESET);
    }

    /** Prints a fixed-width help entry: bold command name + dim description. */
    private static void helpLine(String command, String description) {
        System.out.printf(Ansi.BOLD + "  %-20s" + Ansi.RESET
                + Ansi.DIM + "%s" + Ansi.RESET + "%n", command, description);
    }

    /** Prints a red error message in a consistent format. */
    private static void error(String message) {
        System.out.println(Ansi.RED + "  [ERROR] " + message + Ansi.RESET);
        System.out.println();
    }

    /** Warns if a file path does not carry the expected extension. */
    private static void hintExtension(String path, String expectedExt, String role) {
        if (!FileHandler.hasExtension(path, expectedExt)) {
            System.out.println(Ansi.YELLOW + "  [HINT] " + role
                    + " file does not have a " + expectedExt + " extension." + Ansi.RESET);
        }
    }
}
