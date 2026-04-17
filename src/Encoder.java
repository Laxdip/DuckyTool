import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Encoder - Converts a human-readable Ducky Script (.txt) to binary (.bin) format.
 *
 * Binary Packet Format:
 * -----------------------------------------------------------------------
 * DELAY packet   : [0x02][0x00][delay_high][delay_low]  (4 bytes)
 *                  delay is a 16-bit unsigned int (big-endian), max 65535 ms
 *
 * STRING packet  : [0x03][length][char1_mod][char1_key]...[charN_mod][charN_key]
 *                  length = number of characters (1 byte, max 255)
 *                  each char is 2 bytes: modifier + keycode
 *
 * KEYPRESS packet: [0x01][modifier][keycode]  (3 bytes)
 *                  Used for ENTER, GUI x, CTRL ALT DEL, SHIFT F10, etc.
 *
 * File header    : [0x44][0x55][0x43][0x4B] = "DUCK" magic bytes (4 bytes)
 * File version   : [0x01] (1 byte)
 * -----------------------------------------------------------------------
 */
public class Encoder {

    private static final byte[] MAGIC = {0x44, 0x55, 0x43, 0x4B}; // "DUCK"
    private static final byte   VERSION = 0x01;

    private int warningCount = 0;
    private int lineCount    = 0;

    /**
     * Encodes the script lines into a byte array representing the binary payload.
     *
     * @param scriptLines Lines read from the .txt script file.
     * @return Encoded byte array.
     * @throws EncoderException on fatal encoding errors.
     */
    public byte[] encode(String[] scriptLines) throws EncoderException {
        if (scriptLines == null || scriptLines.length == 0) {
            throw new EncoderException("Script file is empty or contains no valid commands.");
        }

        List<byte[]> packets = new ArrayList<>();
        warningCount = 0;
        lineCount = 0;

        for (String rawLine : scriptLines) {
            lineCount++;
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            byte[] packet = parseLine(line, lineCount);
            if (packet != null) {
                packets.add(packet);
            }
        }

        if (packets.isEmpty()) {
            throw new EncoderException("No valid packets generated. Check your script commands.");
        }

        return assembleFile(packets);
    }

    private byte[] parseLine(String line, int lineNum) throws EncoderException {
        // Tokenize: command is first word, rest is argument
        int spaceIdx = line.indexOf(' ');
        String command = (spaceIdx == -1) ? line.toUpperCase() : line.substring(0, spaceIdx).toUpperCase();
        String argument = (spaceIdx == -1) ? "" : line.substring(spaceIdx + 1).trim();

        switch (command) {
            case "DELAY":
                return encodeDelay(argument, lineNum);

            case "STRING":
                return encodeString(argument, lineNum);

            case "ENTER":
                return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_ENTER);

            case "ESCAPE":
            case "ESC":
                return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_ESCAPE);

            case "BACKSPACE":
                return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_BACKSPACE);

            case "TAB":
                return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_TAB);

            case "DELETE":
            case "DEL":
                return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_DELETE);

            case "INSERT":
                return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_INSERT);

            case "HOME":
                return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_HOME);

            case "END":
                return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_END);

            case "PAGEUP":
                return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_PAGE_UP);

            case "PAGEDOWN":
                return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_PAGE_DOWN);

            case "UP":
                return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_UP);

            case "DOWN":
                return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_DOWN);

            case "LEFT":
                return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_LEFT);

            case "RIGHT":
                return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_RIGHT);

            case "CAPS_LOCK":
                return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_CAPS_LOCK);

            case "PRINTSCREEN":
                return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_PRINT_SCR);

            case "F1":  return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_F1);
            case "F2":  return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_F2);
            case "F3":  return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_F3);
            case "F4":  return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_F4);
            case "F5":  return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_F5);
            case "F6":  return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_F6);
            case "F7":  return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_F7);
            case "F8":  return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_F8);
            case "F9":  return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_F9);
            case "F10": return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_F10);
            case "F11": return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_F11);
            case "F12": return encodeKeyPress(KeyMapper.MOD_NONE, KeyMapper.KEY_F12);

            case "GUI":
            case "WINDOWS":
            case "COMMAND":
                return encodeModifierCombo(KeyMapper.MOD_LEFT_GUI, argument, lineNum, command);

            case "CTRL":
            case "CONTROL":
                return encodeModifierCombo(KeyMapper.MOD_LEFT_CTRL, argument, lineNum, command);

            case "ALT":
                return encodeModifierCombo(KeyMapper.MOD_LEFT_ALT, argument, lineNum, command);

            case "SHIFT":
                return encodeModifierCombo(KeyMapper.MOD_LEFT_SHIFT, argument, lineNum, command);

            default:
                System.out.printf("  [WARNING] Line %d: Unknown command '%s' - skipped.%n", lineNum, command);
                warningCount++;
                return null;
        }
    }

    /**
     * Encodes a DELAY command.
     * Format: [0x02][0x00][high_byte][low_byte]
     */
    private byte[] encodeDelay(String argument, int lineNum) throws EncoderException {
        if (argument.isEmpty()) {
            throw new EncoderException(String.format("Line %d: DELAY requires a numeric argument.", lineNum));
        }
        int delay;
        try {
            delay = Integer.parseInt(argument.trim());
        } catch (NumberFormatException e) {
            throw new EncoderException(String.format("Line %d: DELAY value '%s' is not a valid integer.", lineNum, argument));
        }
        if (delay < 0) {
            throw new EncoderException(String.format("Line %d: DELAY value must be >= 0 (got %d).", lineNum, delay));
        }
        if (delay > 65535) {
            System.out.printf("  [WARNING] Line %d: DELAY %d exceeds max 65535 ms. Clamped to 65535.%n", lineNum, delay);
            warningCount++;
            delay = 65535;
        }
        return new byte[]{
            KeyMapper.PACKET_DELAY,
            0x00,
            (byte) ((delay >> 8) & 0xFF),
            (byte) (delay & 0xFF)
        };
    }

    /**
     * Encodes a STRING command.
     * Format: [0x03][length][char1_mod][char1_key]...[charN_mod][charN_key]
     */
    private byte[] encodeString(String text, int lineNum) throws EncoderException {
        if (text.isEmpty()) {
            throw new EncoderException(String.format("Line %d: STRING requires text argument.", lineNum));
        }
        if (text.length() > 255) {
            throw new EncoderException(String.format(
                "Line %d: STRING length %d exceeds maximum of 255 characters.", lineNum, text.length()));
        }

        List<Byte> payload = new ArrayList<>();
        payload.add(KeyMapper.PACKET_STRING);
        payload.add((byte) text.length());

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            byte[] mapping = KeyMapper.getCharMapping(c);
            if (mapping == null) {
                System.out.printf(
                    "  [WARNING] Line %d: Character '%c' (0x%02X) has no HID mapping - skipped.%n",
                    lineNum, c, (int) c);
                warningCount++;
                // Adjust the length byte
                payload.set(1, (byte) (payload.get(1) - 1));
                continue;
            }
            payload.add(mapping[0]); // modifier
            payload.add(mapping[1]); // keycode
        }

        // If all chars were skipped, nothing to encode
        if (payload.get(1) == 0) {
            throw new EncoderException(String.format(
                "Line %d: STRING '%s' contains no encodable characters.", lineNum, text));
        }

        byte[] result = new byte[payload.size()];
        for (int i = 0; i < payload.size(); i++) result[i] = payload.get(i);
        return result;
    }

    /**
     * Encodes a single key press (no modifier, or standalone modifier key).
     * Format: [0x01][modifier][keycode]
     */
    private byte[] encodeKeyPress(byte modifier, byte keycode) {
        return new byte[]{KeyMapper.PACKET_KEYPRESS, modifier, keycode};
    }

    /**
     * Encodes a modifier + key combination (e.g. CTRL c, GUI r, ALT F4).
     * Supports chained modifiers: CTRL ALT DELETE, CTRL SHIFT ESCAPE, etc.
     */
    private byte[] encodeModifierCombo(byte baseMod, String argument, int lineNum, String command)
            throws EncoderException {

        byte combinedMod = baseMod;
        byte keycode = KeyMapper.KEY_NONE;

        if (argument.isEmpty()) {
            // Standalone modifier (e.g. just "GUI" on its own line)
            return encodeKeyPress(combinedMod, KeyMapper.KEY_NONE);
        }

        // Argument may be: single char, key name, or additional modifier + key
        // e.g. "r", "F4", "ALT DELETE", "SHIFT ESCAPE"
        String[] parts = argument.split("\\s+");
        int partIndex = 0;

        // Consume additional modifiers
        while (partIndex < parts.length) {
            String part = parts[partIndex].toUpperCase();
            byte[] modMapping = getModifierByte(part);
            if (modMapping != null) {
                combinedMod |= modMapping[0];
                partIndex++;
            } else {
                break;
            }
        }

        // Remaining part is the key
        if (partIndex < parts.length) {
            String keyStr = parts[partIndex].toUpperCase();

            // Try as named key first
            byte[] keyMapping = KeyMapper.getKeyNameMapping(keyStr);
            if (keyMapping != null) {
                // If it's a modifier-only mapping, merge the modifier
                combinedMod |= keyMapping[0];
                keycode = keyMapping[1];
            } else if (keyStr.length() == 1) {
                // Single character key (e.g. GUI r -> WIN+R)
                char c = parts[partIndex].charAt(0);
                byte[] charMap = KeyMapper.getCharMapping(Character.toLowerCase(c));
                if (charMap != null) {
                    keycode = charMap[1]; // use keycode only, modifier already set
                } else {
                    throw new EncoderException(String.format(
                        "Line %d: %s argument '%s' has no HID keycode mapping.", lineNum, command, keyStr));
                }
            } else {
                throw new EncoderException(String.format(
                    "Line %d: Unknown key '%s' for %s command.", lineNum, keyStr, command));
            }
        }

        return encodeKeyPress(combinedMod, keycode);
    }

    /**
     * Returns modifier byte for known modifier names, or null if not a modifier.
     */
    private byte[] getModifierByte(String name) {
        switch (name) {
            case "CTRL":
            case "CONTROL": return new byte[]{KeyMapper.MOD_LEFT_CTRL};
            case "SHIFT":   return new byte[]{KeyMapper.MOD_LEFT_SHIFT};
            case "ALT":     return new byte[]{KeyMapper.MOD_LEFT_ALT};
            case "GUI":
            case "WINDOWS":
            case "COMMAND": return new byte[]{KeyMapper.MOD_LEFT_GUI};
            default:        return null;
        }
    }

    /**
     * Assembles the final file: magic header + version + all packets.
     */
    private byte[] assembleFile(List<byte[]> packets) {
        // Calculate total size
        int totalSize = MAGIC.length + 1; // header + version
        for (byte[] pkt : packets) totalSize += pkt.length;

        ByteBuffer buf = ByteBuffer.allocate(totalSize);
        buf.put(MAGIC);
        buf.put(VERSION);
        for (byte[] pkt : packets) buf.put(pkt);

        return buf.array();
    }

    public int getWarningCount() { return warningCount; }
    public int getLineCount()    { return lineCount; }

    /**
     * Custom exception for encoding errors.
     */
    public static class EncoderException extends Exception {
        public EncoderException(String message) { super(message); }
    }
}
