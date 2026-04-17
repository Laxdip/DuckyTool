import java.util.ArrayList;
import java.util.List;

/**
 * Decoder - Converts a DuckyTool binary (.bin) file back to human-readable script.
 *
 * Reads the binary format produced by Encoder and reconstructs the original commands.
 * See Encoder.java for the full packet format specification.
 */
public class Decoder {

    private static final byte[] MAGIC   = {0x44, 0x55, 0x43, 0x4B}; // "DUCK"
    private static final byte   VERSION = 0x01;

    private int packetCount  = 0;
    private int warningCount = 0;

    /**
     * Decodes binary data into a list of human-readable script lines.
     *
     * @param data Raw bytes from the .bin file.
     * @return List of decoded script lines.
     * @throws DecoderException on format or structural errors.
     */
    public List<String> decode(byte[] data) throws DecoderException {
        if (data == null || data.length == 0) {
            throw new DecoderException("Binary file is empty.");
        }

        // Validate magic header
        if (data.length < MAGIC.length + 1) {
            throw new DecoderException("File too small to contain a valid header.");
        }
        for (int i = 0; i < MAGIC.length; i++) {
            if (data[i] != MAGIC[i]) {
                throw new DecoderException(String.format(
                    "Invalid magic header at byte %d. Expected 0x%02X, got 0x%02X. " +
                    "Is this a valid DuckyTool .bin file?", i, MAGIC[i] & 0xFF, data[i] & 0xFF));
            }
        }

        // Validate version
        byte fileVersion = data[MAGIC.length];
        if (fileVersion != VERSION) {
            throw new DecoderException(String.format(
                "Unsupported file version 0x%02X. This tool supports version 0x%02X.", fileVersion, VERSION));
        }

        List<String> lines = new ArrayList<>();
        lines.add("REM Decoded by DuckyTool Decoder");
        lines.add("REM ================================");

        int pos = MAGIC.length + 1; // skip header
        packetCount  = 0;
        warningCount = 0;

        while (pos < data.length) {
            if (pos >= data.length) break;

            byte packetType = data[pos];

            switch (packetType) {
                case KeyMapper.PACKET_DELAY:
                    pos = decodeDelay(data, pos, lines);
                    break;

                case KeyMapper.PACKET_STRING:
                    pos = decodeString(data, pos, lines);
                    break;

                case KeyMapper.PACKET_KEYPRESS:
                    pos = decodeKeyPress(data, pos, lines);
                    break;

                default:
                    System.out.printf("  [WARNING] Unknown packet type 0x%02X at offset %d - skipping byte.%n",
                        packetType & 0xFF, pos);
                    warningCount++;
                    pos++;
                    break;
            }
            packetCount++;
        }

        return lines;
    }

    /**
     * Decodes a DELAY packet.
     * Format: [0x02][0x00][high_byte][low_byte]
     */
    private int decodeDelay(byte[] data, int pos, List<String> lines) throws DecoderException {
        if (pos + 3 >= data.length) {
            throw new DecoderException(String.format(
                "Truncated DELAY packet at offset %d. Need 4 bytes, got %d.", pos, data.length - pos));
        }
        // byte 1 = reserved (0x00), bytes 2-3 = delay value big-endian
        int high  = data[pos + 2] & 0xFF;
        int low   = data[pos + 3] & 0xFF;
        int delay = (high << 8) | low;
        lines.add("DELAY " + delay);
        return pos + 4;
    }

    /**
     * Decodes a STRING packet.
     * Format: [0x03][length][char1_mod][char1_key]...[charN_mod][charN_key]
     */
    private int decodeString(byte[] data, int pos, List<String> lines) throws DecoderException {
        if (pos + 1 >= data.length) {
            throw new DecoderException(String.format(
                "Truncated STRING packet at offset %d (no length byte).", pos));
        }
        int length = data[pos + 1] & 0xFF;
        int requiredBytes = 2 + (length * 2); // type + length + (mod+key)*length

        if (pos + requiredBytes > data.length) {
            throw new DecoderException(String.format(
                "Truncated STRING packet at offset %d. Need %d bytes, got %d.",
                pos, requiredBytes, data.length - pos));
        }

        StringBuilder sb = new StringBuilder();
        int charPos = pos + 2;

        for (int i = 0; i < length; i++) {
            byte modifier = data[charPos];
            byte keycode  = data[charPos + 1];
            Character ch = KeyMapper.reverseCharLookup(modifier, keycode);
            if (ch != null) {
                sb.append(ch);
            } else {
                // Unknown mapping: emit placeholder
                sb.append(String.format("[0x%02X:0x%02X]", modifier & 0xFF, keycode & 0xFF));
                warningCount++;
            }
            charPos += 2;
        }

        lines.add("STRING " + sb.toString());
        return pos + requiredBytes;
    }

    /**
     * Decodes a KEYPRESS packet.
     * Format: [0x01][modifier][keycode]
     */
    private int decodeKeyPress(byte[] data, int pos, List<String> lines) throws DecoderException {
        if (pos + 2 >= data.length) {
            throw new DecoderException(String.format(
                "Truncated KEYPRESS packet at offset %d. Need 3 bytes, got %d.", pos, data.length - pos));
        }
        byte modifier = data[pos + 1];
        byte keycode  = data[pos + 2];

        String line = reconstructKeyPressCommand(modifier, keycode);
        lines.add(line);

        return pos + 3;
    }

    /**
     * Reconstructs a human-readable command from a modifier+keycode pair.
     */
    private String reconstructKeyPressCommand(byte modifier, byte keycode) {

        // Decompose modifier bits into names
        List<String> modNames = new ArrayList<>();
        if ((modifier & KeyMapper.MOD_LEFT_CTRL)  != 0) modNames.add("CTRL");
        if ((modifier & KeyMapper.MOD_RIGHT_CTRL) != 0) modNames.add("CTRL");
        if ((modifier & KeyMapper.MOD_LEFT_SHIFT) != 0) modNames.add("SHIFT");
        if ((modifier & KeyMapper.MOD_RIGHT_SHIFT)!= 0) modNames.add("SHIFT");
        if ((modifier & KeyMapper.MOD_LEFT_ALT)   != 0) modNames.add("ALT");
        if ((modifier & KeyMapper.MOD_RIGHT_ALT)  != 0) modNames.add("ALT");
        if ((modifier & KeyMapper.MOD_LEFT_GUI)   != 0) modNames.add("GUI");
        if ((modifier & KeyMapper.MOD_RIGHT_GUI)  != 0) modNames.add("GUI");

        // Deduplicate modifier names (left+right same key)
        List<String> dedupMods = new ArrayList<>();
        for (String m : modNames) {
            if (!dedupMods.contains(m)) dedupMods.add(m);
        }

        // Resolve keycode to a name
        String keyName = resolveKeycodeName(keycode);

        // Build command string
        if (dedupMods.isEmpty()) {
            // No modifier: it's a standalone key
            return (keyName != null) ? keyName : String.format("REM UNKNOWN_KEY(0x%02X)", keycode & 0xFF);
        } else {
            // Has modifier(s): use first modifier as command, rest + key as args
            String primaryMod = dedupMods.get(0);
            StringBuilder sb = new StringBuilder(primaryMod);

            // Additional modifiers
            for (int i = 1; i < dedupMods.size(); i++) {
                sb.append(" ").append(dedupMods.get(i));
            }

            // Append key if not KEY_NONE
            if (keycode != KeyMapper.KEY_NONE && keyName != null) {
                sb.append(" ").append(keyName);
            } else if (keycode != KeyMapper.KEY_NONE) {
                sb.append(String.format(" REM_UNKNOWN(0x%02X)", keycode & 0xFF));
            }

            return sb.toString();
        }
    }

    /**
     * Resolves a keycode byte to a human-readable key name.
     * Returns the name string, or null for KEY_NONE.
     */
    private String resolveKeycodeName(byte keycode) {
        if (keycode == KeyMapper.KEY_NONE)       return null;
        if (keycode == KeyMapper.KEY_ENTER)      return "ENTER";
        if (keycode == KeyMapper.KEY_ESCAPE)     return "ESCAPE";
        if (keycode == KeyMapper.KEY_BACKSPACE)  return "BACKSPACE";
        if (keycode == KeyMapper.KEY_TAB)        return "TAB";
        if (keycode == KeyMapper.KEY_SPACE)      return "SPACE";
        if (keycode == KeyMapper.KEY_DELETE)     return "DELETE";
        if (keycode == KeyMapper.KEY_INSERT)     return "INSERT";
        if (keycode == KeyMapper.KEY_HOME)       return "HOME";
        if (keycode == KeyMapper.KEY_END)        return "END";
        if (keycode == KeyMapper.KEY_PAGE_UP)    return "PAGEUP";
        if (keycode == KeyMapper.KEY_PAGE_DOWN)  return "PAGEDOWN";
        if (keycode == KeyMapper.KEY_UP)         return "UP";
        if (keycode == KeyMapper.KEY_DOWN)       return "DOWN";
        if (keycode == KeyMapper.KEY_LEFT)       return "LEFT";
        if (keycode == KeyMapper.KEY_RIGHT)      return "RIGHT";
        if (keycode == KeyMapper.KEY_CAPS_LOCK)  return "CAPS_LOCK";
        if (keycode == KeyMapper.KEY_PRINT_SCR)  return "PRINTSCREEN";
        if (keycode == KeyMapper.KEY_F1)         return "F1";
        if (keycode == KeyMapper.KEY_F2)         return "F2";
        if (keycode == KeyMapper.KEY_F3)         return "F3";
        if (keycode == KeyMapper.KEY_F4)         return "F4";
        if (keycode == KeyMapper.KEY_F5)         return "F5";
        if (keycode == KeyMapper.KEY_F6)         return "F6";
        if (keycode == KeyMapper.KEY_F7)         return "F7";
        if (keycode == KeyMapper.KEY_F8)         return "F8";
        if (keycode == KeyMapper.KEY_F9)         return "F9";
        if (keycode == KeyMapper.KEY_F10)        return "F10";
        if (keycode == KeyMapper.KEY_F11)        return "F11";
        if (keycode == KeyMapper.KEY_F12)        return "F12";

        // Lowercase a-z
        if (keycode >= 0x04 && keycode <= 0x1D) {
            return String.valueOf((char) ('a' + (keycode - 0x04)));
        }

        // Digits
        if (keycode >= 0x1E && keycode <= 0x26) {
            return String.valueOf((char) ('1' + (keycode - 0x1E)));
        }
        if (keycode == 0x27) return "0";

        return String.format("KEY_0x%02X", keycode & 0xFF);
    }

    public int getPacketCount()  { return packetCount; }
    public int getWarningCount() { return warningCount; }

    /**
     * Custom exception for decoding errors.
     */
    public static class DecoderException extends Exception {
        public DecoderException(String message) { super(message); }
    }
}
