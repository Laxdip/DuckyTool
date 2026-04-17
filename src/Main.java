import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Main - Entry point for the DuckyTool CLI application.
 *
 * Usage:
 *   java -cp src Main                    (interactive menu)
 *   java -cp src Main encode in.txt out.bin
 *   java -cp src Main decode in.bin out.txt
 */
public class Main {

    // ANSI colour codes (degraded gracefully if terminal doesn't support them)
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";
    private static final String RED    = "\u001B[31m";
    private static final String DIM    = "\u001B[2m";

    public static void main(String[] args) {
        // Command-line shortcut: encode / decode without interactive menu
        if (args.length == 3) {
            String mode = args[0].toLowerCase();
            if (mode.equals("encode")) {
                runEncode(args[1], args[2]);
            } else if (mode.equals("decode")) {
                runDecode(args[1], args[2]);
            } else {
                printUsage();
                System.exit(1);
            }
            return;
        }

        // Interactive menu
        printBanner();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            printMenu();
            System.out.print(BOLD + "  Your choice: " + RESET);
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    interactiveEncode(scanner);
                    break;
                case "2":
                    interactiveDecode(scanner);
                    break;
                case "3":
                    System.out.println(DIM + "\n  Goodbye!\n" + RESET);
                    scanner.close();
                    System.exit(0);
                    break;
                case "4":
                    printHelp();
                    break;
                default:
                    System.out.println(RED + "  [ERROR] Invalid choice. Enter 1, 2, 3 or 4.\n" + RESET);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Interactive prompts
    // -------------------------------------------------------------------------

    private static void interactiveEncode(Scanner scanner) {
        System.out.println();
        System.out.println(CYAN + "  ┌─ ENCODE ──────────────────────────────────────────┐" + RESET);
        System.out.print(  "  │ Input script file  (.txt) : " + RESET);
        String inputFile = scanner.nextLine().trim();

        System.out.print(  "  │ Output binary file (.bin) : " + RESET);
        String outputFile = scanner.nextLine().trim();
        System.out.println(CYAN + "  └────────────────────────────────────────────────────┘" + RESET);

        if (inputFile.isEmpty() || outputFile.isEmpty()) {
            System.out.println(RED + "\n  [ERROR] File paths cannot be empty.\n" + RESET);
            return;
        }
        runEncode(inputFile, outputFile);
    }

    private static void interactiveDecode(Scanner scanner) {
        System.out.println();
        System.out.println(CYAN + "  ┌─ DECODE ──────────────────────────────────────────┐" + RESET);
        System.out.print(  "  │ Input binary file  (.bin) : " + RESET);
        String inputFile = scanner.nextLine().trim();

        System.out.print(  "  │ Output script file (.txt) : " + RESET);
        String outputFile = scanner.nextLine().trim();
        System.out.println(CYAN + "  └────────────────────────────────────────────────────┘" + RESET);

        if (inputFile.isEmpty() || outputFile.isEmpty()) {
            System.out.println(RED + "\n  [ERROR] File paths cannot be empty.\n" + RESET);
            return;
        }
        runDecode(inputFile, outputFile);
    }

    // -------------------------------------------------------------------------
    // Core encode / decode routines
    // -------------------------------------------------------------------------

    private static void runEncode(String inputPath, String outputPath) {
        System.out.println();
        System.out.println(BOLD + "  Encoding: " + RESET + inputPath + " → " + outputPath);

        // Basic extension hints (non-fatal)
        if (!FileHandler.hasExtension(inputPath, ".txt")) {
            System.out.println(YELLOW + "  [HINT] Input file does not have a .txt extension." + RESET);
        }
        if (!FileHandler.hasExtension(outputPath, ".bin")) {
            System.out.println(YELLOW + "  [HINT] Output file does not have a .bin extension." + RESET);
        }

        String[] scriptLines;
        try {
            scriptLines = FileHandler.readScriptFile(inputPath);
        } catch (IOException e) {
            System.out.println(RED + "  [ERROR] Cannot read input file: " + e.getMessage() + RESET);
            System.out.println();
            return;
        }

        System.out.println("  Read " + scriptLines.length + " script line(s).");

        Encoder encoder = new Encoder();
        byte[] binaryData;
        try {
            binaryData = encoder.encode(scriptLines);
        } catch (Encoder.EncoderException e) {
            System.out.println(RED + "  [ERROR] Encoding failed: " + e.getMessage() + RESET);
            System.out.println();
            return;
        }

        try {
            FileHandler.writeBinaryFile(outputPath, binaryData);
        } catch (IOException e) {
            System.out.println(RED + "  [ERROR] Cannot write output file: " + e.getMessage() + RESET);
            System.out.println();
            return;
        }

        System.out.println(GREEN + "  ✓ Encoding complete!" + RESET);
        System.out.println("  Lines processed : " + encoder.getLineCount());
        System.out.println("  Warnings        : " + encoder.getWarningCount());
        System.out.println("  Output size     : " + binaryData.length + " bytes → " + outputPath);
        System.out.println();
    }

    private static void runDecode(String inputPath, String outputPath) {
        System.out.println();
        System.out.println(BOLD + "  Decoding: " + RESET + inputPath + " → " + outputPath);

        if (!FileHandler.hasExtension(inputPath, ".bin")) {
            System.out.println(YELLOW + "  [HINT] Input file does not have a .bin extension." + RESET);
        }

        byte[] binaryData;
        try {
            binaryData = FileHandler.readBinaryFile(inputPath);
        } catch (IOException e) {
            System.out.println(RED + "  [ERROR] Cannot read binary file: " + e.getMessage() + RESET);
            System.out.println();
            return;
        }

        System.out.println("  Read " + binaryData.length + " byte(s).");

        Decoder decoder = new Decoder();
        List<String> scriptLines;
        try {
            scriptLines = decoder.decode(binaryData);
        } catch (Decoder.DecoderException e) {
            System.out.println(RED + "  [ERROR] Decoding failed: " + e.getMessage() + RESET);
            System.out.println();
            return;
        }

        try {
            FileHandler.writeScriptFile(outputPath, scriptLines);
        } catch (IOException e) {
            System.out.println(RED + "  [ERROR] Cannot write output file: " + e.getMessage() + RESET);
            System.out.println();
            return;
        }

        System.out.println(GREEN + "  ✓ Decoding complete!" + RESET);
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
        System.out.println(CYAN + BOLD +
            "  ██████╗ ██╗   ██╗ ██████╗██╗  ██╗██╗   ██╗    ████████╗ ██████╗  ██████╗ ██╗" + RESET);
        System.out.println(CYAN + BOLD +
            "  ██╔══██╗██║   ██║██╔════╝██║ ██╔╝╚██╗ ██╔╝    ╚══██╔══╝██╔═══██╗██╔═══██╗██║" + RESET);
        System.out.println(CYAN + BOLD +
            "  ██║  ██║██║   ██║██║     █████╔╝  ╚████╔╝        ██║   ██║   ██║██║   ██║██║" + RESET);
        System.out.println(CYAN + BOLD +
            "  ██║  ██║██║   ██║██║     ██╔═██╗   ╚██╔╝         ██║   ██║   ██║██║   ██║██║" + RESET);
        System.out.println(CYAN + BOLD +
            "  ██████╔╝╚██████╔╝╚██████╗██║  ██╗   ██║          ██║   ╚██████╔╝╚██████╔╝███████╗" + RESET);
        System.out.println(CYAN + BOLD +
            "  ╚═════╝  ╚═════╝  ╚═════╝╚═╝  ╚═╝   ╚═╝          ╚═╝    ╚═════╝  ╚═════╝ ╚══════╝" + RESET);
        System.out.println(DIM + "              USB Rubber Ducky Script Encoder / Decoder  v1.0" + RESET);
        System.out.println(DIM + "                    Created by: PRASAD");
        System.out.println(DIM + "                    GitHub: github.com/Laxdip");
        System.out.println();
    }

    private static void printMenu() {
        System.out.println(BOLD + "  ┌─────────────────────────────┐" + RESET);
        System.out.println(BOLD + "  │         MAIN MENU           │" + RESET);
        System.out.println(BOLD + "  ├─────────────────────────────┤" + RESET);
        System.out.println(BOLD + "  │  " + GREEN + "1" + RESET + "  Encode  (.txt → .bin)" + BOLD + "  │" + RESET);
        System.out.println(BOLD + "  │  " + GREEN + "2" + RESET + "  Decode  (.bin → .txt)" + BOLD + "  │" + RESET);
        System.out.println(BOLD + "  │  " + GREEN + "3" + RESET + "  Exit                 " + BOLD + "  │" + RESET);
        System.out.println(BOLD + "  │  " + GREEN + "4" + RESET + "  Help / Command List  " + BOLD + "  │" + RESET);
        System.out.println(BOLD + "  └─────────────────────────────┘" + RESET);
    }

    private static void printHelp() {
        System.out.println();
        System.out.println(BOLD + CYAN + "  ── Supported Commands ──────────────────────────────────────" + RESET);
        System.out.println();
        System.out.println(BOLD + "  DELAY <ms>          " + RESET + DIM + "Pause for <ms> milliseconds (0–65535)" + RESET);
        System.out.println(BOLD + "  STRING <text>       " + RESET + DIM + "Type the given text (max 255 chars)" + RESET);
        System.out.println(BOLD + "  ENTER               " + RESET + DIM + "Press Enter key" + RESET);
        System.out.println(BOLD + "  ESCAPE / ESC        " + RESET + DIM + "Press Escape" + RESET);
        System.out.println(BOLD + "  BACKSPACE           " + RESET + DIM + "Press Backspace" + RESET);
        System.out.println(BOLD + "  TAB                 " + RESET + DIM + "Press Tab" + RESET);
        System.out.println(BOLD + "  DELETE / DEL        " + RESET + DIM + "Press Delete" + RESET);
        System.out.println(BOLD + "  HOME / END          " + RESET + DIM + "Press Home / End" + RESET);
        System.out.println(BOLD + "  PAGEUP / PAGEDOWN   " + RESET + DIM + "Page navigation" + RESET);
        System.out.println(BOLD + "  UP/DOWN/LEFT/RIGHT  " + RESET + DIM + "Arrow keys" + RESET);
        System.out.println(BOLD + "  CAPS_LOCK           " + RESET + DIM + "Toggle Caps Lock" + RESET);
        System.out.println(BOLD + "  F1 … F12            " + RESET + DIM + "Function keys" + RESET);
        System.out.println(BOLD + "  PRINTSCREEN         " + RESET + DIM + "Print Screen" + RESET);
        System.out.println();
        System.out.println(BOLD + "  Modifier combos:" + RESET);
        System.out.println(BOLD + "  GUI <key>           " + RESET + DIM + "WIN/CMD + key  e.g. GUI r" + RESET);
        System.out.println(BOLD + "  CTRL <key>          " + RESET + DIM + "CTRL + key     e.g. CTRL c" + RESET);
        System.out.println(BOLD + "  ALT <key>           " + RESET + DIM + "ALT  + key     e.g. ALT F4" + RESET);
        System.out.println(BOLD + "  SHIFT <key>         " + RESET + DIM + "SHIFT+ key     e.g. SHIFT F10" + RESET);
        System.out.println(BOLD + "  CTRL ALT DELETE     " + RESET + DIM + "Chained modifiers supported" + RESET);
        System.out.println();
        System.out.println(BOLD + "  Comments:" + RESET);
        System.out.println(BOLD + "  REM <text>          " + RESET + DIM + "Comment line (ignored during encoding)" + RESET);
        System.out.println(BOLD + "  // <text>           " + RESET + DIM + "Also treated as a comment" + RESET);
        System.out.println();
        System.out.println(BOLD + "  CLI usage:          " + RESET + DIM + "java -cp src Main encode input.txt output.bin" + RESET);
        System.out.println(BOLD + "                      " + RESET + DIM + "java -cp src Main decode input.bin output.txt" + RESET);
        System.out.println();
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -cp src Main                        (interactive mode)");
        System.out.println("  java -cp src Main encode <in.txt> <out.bin>");
        System.out.println("  java -cp src Main decode <in.bin> <out.txt>");
    }
}
