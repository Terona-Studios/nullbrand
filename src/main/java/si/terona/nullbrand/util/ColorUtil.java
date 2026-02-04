package si.terona.nullbrand.util;

public final class ColorUtil {

    private static final char SECTION = '\u00A7';
    private static final char[] LEGACY_CODES = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };
    private static final int[] LEGACY_RGB = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private ColorUtil() {
    }

    public static String translate(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String value = replaceHexTags(input, false);
        value = replaceHexCodes(value, false);
        return translateLegacy(value);
    }

    public static String translateLegacyOnly(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String value = replaceHexTags(input, true);
        value = replaceHexCodes(value, true);
        return translateLegacy(value);
    }

    private static String replaceHexTags(String input, boolean legacyOnly) {
        StringBuilder out = new StringBuilder(input.length());
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '<' && i + 8 < input.length() && input.charAt(i + 1) == '#') {
                String hex = input.substring(i + 2, i + 8);
                if (isHex(hex) && input.charAt(i + 8) == '>') {
                    out.append(legacyOnly ? toNearestLegacy(hex) : toLegacyHex(hex));
                    i += 9;
                    continue;
                }
            }
            if (c == '<' && i + 3 < input.length() && input.charAt(i + 1) == '/' && input.charAt(i + 2) == '#') {
                int end = input.indexOf('>', i + 3);
                if (end != -1) {
                    out.append(SECTION).append('r');
                    i = end + 1;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static String replaceHexCodes(String input, boolean legacyOnly) {
        StringBuilder out = new StringBuilder(input.length());
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '&' && i + 7 < input.length() && input.charAt(i + 1) == '#') {
                String hex = input.substring(i + 2, i + 8);
                if (isHex(hex)) {
                    out.append(legacyOnly ? toNearestLegacy(hex) : toLegacyHex(hex));
                    i += 8;
                    continue;
                }
            }
            if (c == '#' && i + 6 < input.length()) {
                String hex = input.substring(i + 1, i + 7);
                if (isHex(hex)) {
                    out.append(legacyOnly ? toNearestLegacy(hex) : toLegacyHex(hex));
                    i += 7;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static String translateLegacy(String input) {
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '&' && i + 1 < input.length()) {
                char code = input.charAt(i + 1);
                if (isLegacy(code)) {
                    out.append(SECTION).append(Character.toLowerCase(code));
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String toLegacyHex(String hex) {
        StringBuilder out = new StringBuilder(14);
        out.append(SECTION).append('x');
        for (int i = 0; i < hex.length(); i++) {
            out.append(SECTION).append(Character.toLowerCase(hex.charAt(i)));
        }
        return out.toString();
    }

    private static String toNearestLegacy(String hex) {
        int rgb = Integer.parseInt(hex, 16);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int bestIndex = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < LEGACY_RGB.length; i++) {
            int crgb = LEGACY_RGB[i];
            int dr = r - ((crgb >> 16) & 0xFF);
            int dg = g - ((crgb >> 8) & 0xFF);
            int db = b - (crgb & 0xFF);
            int dist = dr * dr + dg * dg + db * db;
            if (dist < bestDist) {
                bestDist = dist;
                bestIndex = i;
            }
        }
        return new String(new char[]{SECTION, LEGACY_CODES[bestIndex]});
    }

    private static boolean isHex(String hex) {
        if (hex.length() != 6) {
            return false;
        }
        for (int i = 0; i < hex.length(); i++) {
            if (!isHexChar(hex.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }

    private static boolean isLegacy(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F')
                || (c >= 'k' && c <= 'o')
                || (c >= 'K' && c <= 'O')
                || c == 'r'
                || c == 'R';
    }
}
