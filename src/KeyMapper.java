import java.util.HashMap;
import java.util.Map;

/**
 * KeyMapper - Maps characters and command strings to HID keycodes.
 * Based on the USB HID Usage Tables specification (US keyboard layout).
 */
public class KeyMapper {

    // HID modifier byte flags
    public static final byte MOD_NONE        = 0x00;
    public static final byte MOD_LEFT_CTRL   = 0x01;
    public static final byte MOD_LEFT_SHIFT  = 0x02;
    public static final byte MOD_LEFT_ALT    = 0x04;
    public static final byte MOD_LEFT_GUI    = 0x08;
    public static final byte MOD_RIGHT_CTRL  = 0x10;
    public static final byte MOD_RIGHT_SHIFT = 0x20;
    public static final byte MOD_RIGHT_ALT   = 0x40;
    public static final byte MOD_RIGHT_GUI   = (byte) 0x80;

    // HID keycodes
    public static final byte KEY_NONE       = 0x00;
    public static final byte KEY_ENTER      = 0x28;
    public static final byte KEY_ESCAPE     = 0x29;
    public static final byte KEY_BACKSPACE  = 0x2A;
    public static final byte KEY_TAB        = 0x2B;
    public static final byte KEY_SPACE      = 0x2C;
    public static final byte KEY_DELETE     = 0x4C;
    public static final byte KEY_INSERT     = 0x49;
    public static final byte KEY_HOME       = 0x4A;
    public static final byte KEY_END        = 0x4D;
    public static final byte KEY_PAGE_UP    = 0x4B;
    public static final byte KEY_PAGE_DOWN  = 0x4E;
    public static final byte KEY_UP         = 0x52;
    public static final byte KEY_DOWN       = 0x51;
    public static final byte KEY_LEFT       = 0x50;
    public static final byte KEY_RIGHT      = 0x4F;
    public static final byte KEY_CAPS_LOCK  = 0x39;
    public static final byte KEY_F1         = 0x3A;
    public static final byte KEY_F2         = 0x3B;
    public static final byte KEY_F3         = 0x3C;
    public static final byte KEY_F4         = 0x3D;
    public static final byte KEY_F5         = 0x3E;
    public static final byte KEY_F6         = 0x3F;
    public static final byte KEY_F7         = 0x40;
    public static final byte KEY_F8         = 0x41;
    public static final byte KEY_F9         = 0x42;
    public static final byte KEY_F10        = 0x43;
    public static final byte KEY_F11        = 0x44;
    public static final byte KEY_F12        = 0x45;
    public static final byte KEY_PRINT_SCR  = 0x46;

    // Packet type identifiers (used in .bin format)
    public static final byte PACKET_KEYPRESS = 0x01;
    public static final byte PACKET_DELAY    = 0x02;
    public static final byte PACKET_STRING   = 0x03;

    // Maps a character to [modifier, keycode]
    private static final Map<Character, byte[]> CHAR_MAP = new HashMap<>();

    // Maps special key names to [modifier, keycode]
    private static final Map<String, byte[]> KEY_NAME_MAP = new HashMap<>();

    static {
        buildCharMap();
        buildKeyNameMap();
    }

    private static void buildCharMap() {
        // Lowercase a-z (HID keycodes 0x04 - 0x1D)
        for (char c = 'a'; c <= 'z'; c++) {
            CHAR_MAP.put(c, new byte[]{MOD_NONE, (byte) (0x04 + (c - 'a'))});
        }
        // Uppercase A-Z (shift + a-z)
        for (char c = 'A'; c <= 'Z'; c++) {
            CHAR_MAP.put(c, new byte[]{MOD_LEFT_SHIFT, (byte) (0x04 + (c - 'A'))});
        }
        // Digits 1-9 (HID 0x1E - 0x26)
        for (char c = '1'; c <= '9'; c++) {
            CHAR_MAP.put(c, new byte[]{MOD_NONE, (byte) (0x1E + (c - '1'))});
        }
        // Digit 0 (HID 0x27)
        CHAR_MAP.put('0', new byte[]{MOD_NONE, (byte) 0x27});

        // Space
        CHAR_MAP.put(' ', new byte[]{MOD_NONE, KEY_SPACE});

        // Punctuation - no shift
        CHAR_MAP.put('-',  new byte[]{MOD_NONE, (byte) 0x2D});
        CHAR_MAP.put('=',  new byte[]{MOD_NONE, (byte) 0x2E});
        CHAR_MAP.put('[',  new byte[]{MOD_NONE, (byte) 0x2F});
        CHAR_MAP.put(']',  new byte[]{MOD_NONE, (byte) 0x30});
        CHAR_MAP.put('\\', new byte[]{MOD_NONE, (byte) 0x31});
        CHAR_MAP.put(';',  new byte[]{MOD_NONE, (byte) 0x33});
        CHAR_MAP.put('\'', new byte[]{MOD_NONE, (byte) 0x34});
        CHAR_MAP.put('`',  new byte[]{MOD_NONE, (byte) 0x35});
        CHAR_MAP.put(',',  new byte[]{MOD_NONE, (byte) 0x36});
        CHAR_MAP.put('.',  new byte[]{MOD_NONE, (byte) 0x37});
        CHAR_MAP.put('/',  new byte[]{MOD_NONE, (byte) 0x38});

        // Punctuation - shifted
        CHAR_MAP.put('!',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x1E});
        CHAR_MAP.put('@',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x1F});
        CHAR_MAP.put('#',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x20});
        CHAR_MAP.put('$',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x21});
        CHAR_MAP.put('%',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x22});
        CHAR_MAP.put('^',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x23});
        CHAR_MAP.put('&',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x24});
        CHAR_MAP.put('*',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x25});
        CHAR_MAP.put('(',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x26});
        CHAR_MAP.put(')',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x27});
        CHAR_MAP.put('_',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x2D});
        CHAR_MAP.put('+',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x2E});
        CHAR_MAP.put('{',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x2F});
        CHAR_MAP.put('}',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x30});
        CHAR_MAP.put('|',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x31});
        CHAR_MAP.put(':',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x33});
        CHAR_MAP.put('"',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x34});
        CHAR_MAP.put('~',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x35});
        CHAR_MAP.put('<',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x36});
        CHAR_MAP.put('>',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x37});
        CHAR_MAP.put('?',  new byte[]{MOD_LEFT_SHIFT, (byte) 0x38});
    }

    private static void buildKeyNameMap() {
        KEY_NAME_MAP.put("ENTER",      new byte[]{MOD_NONE, KEY_ENTER});
        KEY_NAME_MAP.put("ESCAPE",     new byte[]{MOD_NONE, KEY_ESCAPE});
        KEY_NAME_MAP.put("ESC",        new byte[]{MOD_NONE, KEY_ESCAPE});
        KEY_NAME_MAP.put("BACKSPACE",  new byte[]{MOD_NONE, KEY_BACKSPACE});
        KEY_NAME_MAP.put("TAB",        new byte[]{MOD_NONE, KEY_TAB});
        KEY_NAME_MAP.put("SPACE",      new byte[]{MOD_NONE, KEY_SPACE});
        KEY_NAME_MAP.put("DELETE",     new byte[]{MOD_NONE, KEY_DELETE});
        KEY_NAME_MAP.put("DEL",        new byte[]{MOD_NONE, KEY_DELETE});
        KEY_NAME_MAP.put("INSERT",     new byte[]{MOD_NONE, KEY_INSERT});
        KEY_NAME_MAP.put("HOME",       new byte[]{MOD_NONE, KEY_HOME});
        KEY_NAME_MAP.put("END",        new byte[]{MOD_NONE, KEY_END});
        KEY_NAME_MAP.put("PAGEUP",     new byte[]{MOD_NONE, KEY_PAGE_UP});
        KEY_NAME_MAP.put("PAGEDOWN",   new byte[]{MOD_NONE, KEY_PAGE_DOWN});
        KEY_NAME_MAP.put("UP",         new byte[]{MOD_NONE, KEY_UP});
        KEY_NAME_MAP.put("DOWN",       new byte[]{MOD_NONE, KEY_DOWN});
        KEY_NAME_MAP.put("LEFT",       new byte[]{MOD_NONE, KEY_LEFT});
        KEY_NAME_MAP.put("RIGHT",      new byte[]{MOD_NONE, KEY_RIGHT});
        KEY_NAME_MAP.put("CAPS_LOCK",  new byte[]{MOD_NONE, KEY_CAPS_LOCK});
        KEY_NAME_MAP.put("PRINTSCREEN",new byte[]{MOD_NONE, KEY_PRINT_SCR});
        KEY_NAME_MAP.put("F1",         new byte[]{MOD_NONE, KEY_F1});
        KEY_NAME_MAP.put("F2",         new byte[]{MOD_NONE, KEY_F2});
        KEY_NAME_MAP.put("F3",         new byte[]{MOD_NONE, KEY_F3});
        KEY_NAME_MAP.put("F4",         new byte[]{MOD_NONE, KEY_F4});
        KEY_NAME_MAP.put("F5",         new byte[]{MOD_NONE, KEY_F5});
        KEY_NAME_MAP.put("F6",         new byte[]{MOD_NONE, KEY_F6});
        KEY_NAME_MAP.put("F7",         new byte[]{MOD_NONE, KEY_F7});
        KEY_NAME_MAP.put("F8",         new byte[]{MOD_NONE, KEY_F8});
        KEY_NAME_MAP.put("F9",         new byte[]{MOD_NONE, KEY_F9});
        KEY_NAME_MAP.put("F10",        new byte[]{MOD_NONE, KEY_F10});
        KEY_NAME_MAP.put("F11",        new byte[]{MOD_NONE, KEY_F11});
        KEY_NAME_MAP.put("F12",        new byte[]{MOD_NONE, KEY_F12});
        // GUI / Windows / Command key
        KEY_NAME_MAP.put("GUI",        new byte[]{MOD_LEFT_GUI, KEY_NONE});
        KEY_NAME_MAP.put("WINDOWS",    new byte[]{MOD_LEFT_GUI, KEY_NONE});
        KEY_NAME_MAP.put("COMMAND",    new byte[]{MOD_LEFT_GUI, KEY_NONE});
        // Modifier names (used in combo commands)
        KEY_NAME_MAP.put("CTRL",       new byte[]{MOD_LEFT_CTRL, KEY_NONE});
        KEY_NAME_MAP.put("CONTROL",    new byte[]{MOD_LEFT_CTRL, KEY_NONE});
        KEY_NAME_MAP.put("SHIFT",      new byte[]{MOD_LEFT_SHIFT, KEY_NONE});
        KEY_NAME_MAP.put("ALT",        new byte[]{MOD_LEFT_ALT, KEY_NONE});
    }

    /**
     * Returns [modifier, keycode] for the given character, or null if unsupported.
     */
    public static byte[] getCharMapping(char c) {
        return CHAR_MAP.get(c);
    }

    /**
     * Returns [modifier, keycode] for the given key name (e.g. "ENTER"), or null if unknown.
     */
    public static byte[] getKeyNameMapping(String name) {
        return KEY_NAME_MAP.get(name.toUpperCase());
    }

    /**
     * Reverse lookup: given modifier + keycode, return the character or null.
     */
    public static Character reverseCharLookup(byte modifier, byte keycode) {
        for (Map.Entry<Character, byte[]> entry : CHAR_MAP.entrySet()) {
            byte[] val = entry.getValue();
            if (val[0] == modifier && val[1] == keycode) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Reverse lookup: given modifier + keycode, return the key name or null.
     */
    public static String reverseKeyNameLookup(byte modifier, byte keycode) {
        for (Map.Entry<String, byte[]> entry : KEY_NAME_MAP.entrySet()) {
            byte[] val = entry.getValue();
            if (val[0] == modifier && val[1] == keycode) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static boolean isCharSupported(char c) {
        return CHAR_MAP.containsKey(c);
    }
}
