package si.terona.nullbrand.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConfigUtil {

    private final Path dataDirectory;
    private final Path configPath;
    private volatile boolean brandingEnabled = true;
    private volatile String brandingTextColored = "NullBrand";
    private volatile byte[] brandingPayload = new byte[0];
    private volatile boolean motdEnabled = true;
    private volatile List<String> motdLinesColored = List.of("NullBrand");
    private volatile String motdJoinedColored = "NullBrand";
    private volatile boolean hoverEnabled = false;
    private volatile List<String> hoverLinesColored = List.of();

    public ConfigUtil(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.configPath = dataDirectory.resolve("config.yml");
    }

    public synchronized void load(ClassLoader classLoader) throws IOException {
        ensureDefault(classLoader);
        byte[] bytes = Files.readAllBytes(configPath);
        String content = new String(bytes, StandardCharsets.UTF_8);
        if (!content.isEmpty() && content.charAt(0) == '\uFEFF') {
            content = content.substring(1);
        }
        List<String> lines = Arrays.asList(content.split("\\R", -1));
        parse(lines);
    }

    public boolean isBrandingEnabled() {
        return brandingEnabled;
    }

    public String getBrandingTextColored() {
        return brandingTextColored;
    }

    public byte[] getBrandingPayload() {
        return brandingPayload;
    }

    public boolean isMotdEnabled() {
        return motdEnabled;
    }

    public List<String> getMotdLinesColored() {
        return motdLinesColored;
    }

    public String getMotdJoinedColored() {
        return motdJoinedColored;
    }

    public boolean isHoverEnabled() {
        return hoverEnabled;
    }

    public List<String> getHoverLinesColored() {
        return hoverLinesColored;
    }

    private void ensureDefault(ClassLoader classLoader) throws IOException {
        if (!Files.exists(dataDirectory)) {
            Files.createDirectories(dataDirectory);
        }
        if (!Files.exists(configPath)) {
            try (InputStream in = classLoader.getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configPath);
                    return;
                }
            }
            writeDefault();
        }
    }

    private void writeDefault() throws IOException {
        String defaultConfig =
                "branding:\n"
                        + "  enabled: true\n"
                        + "  text: \"<#6affff>NullBrand</#6affff> &7| &fInvisible Proxy\"\n"
                        + "\n"
                        + "motd:\n"
                        + "  enabled: true\n"
                        + "  lines:\n"
                        + "    - \"<#6affff>NullBrand</#6affff>\"\n"
                        + "    - \"&7Secure \u2022 Fast \u2022 Invisible\"\n"
                        + "  hover:\n"
                        + "    enabled: false\n"
                        + "    lines:\n"
                        + "      - \"&7Welcome to &fNullBrand\"\n";
        Files.writeString(configPath, defaultConfig, StandardCharsets.UTF_8);
    }

    private void parse(List<String> lines) {
        boolean brandingEnabled = true;
        String brandingText = "NullBrand";
        boolean motdEnabled = true;
        String motdText = null;
        List<String> motdLines = new ArrayList<>();
        boolean hoverEnabled = false;
        List<String> hoverLines = new ArrayList<>();
        boolean inBranding = false;
        boolean inMotd = false;
        boolean inHover = false;
        boolean readingMotdLines = false;
        boolean readingHoverLines = false;

        for (String raw : lines) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int indent = countLeadingSpaces(raw);
            if (indent == 0 && trimmed.endsWith(":")) {
                String section = trimmed.substring(0, trimmed.length() - 1).toLowerCase();
                inBranding = section.equals("branding");
                inMotd = section.equals("motd");
                inHover = false;
                readingMotdLines = false;
                readingHoverLines = false;
                continue;
            }
            if (inMotd && indent == 2 && trimmed.endsWith(":")) {
                String key = trimmed.substring(0, trimmed.length() - 1).trim().toLowerCase();
                if (key.equals("hover")) {
                    inHover = true;
                    readingMotdLines = false;
                    readingHoverLines = false;
                    continue;
                }
            }
            if (inMotd && inHover && indent == 4 && trimmed.contains(":")) {
                int split = trimmed.indexOf(':');
                String key = trimmed.substring(0, split).trim().toLowerCase();
                String value = trimmed.substring(split + 1).trim();
                value = unquote(value);
                if (key.equals("enabled")) {
                    hoverEnabled = parseBoolean(value, hoverEnabled);
                } else if (key.equals("lines")) {
                    readingHoverLines = true;
                    hoverLines.clear();
                } else {
                    readingHoverLines = false;
                }
                continue;
            }
            if (inMotd && inHover && readingHoverLines && trimmed.startsWith("-")) {
                String item = trimmed.substring(1).trim();
                hoverLines.add(unquote(item));
                continue;
            }
            if (indent == 2 && trimmed.contains(":")) {
                inHover = false;
                int split = trimmed.indexOf(':');
                String key = trimmed.substring(0, split).trim().toLowerCase();
                String value = trimmed.substring(split + 1).trim();
                value = unquote(value);
                if (inBranding) {
                    if (key.equals("enabled")) {
                        brandingEnabled = parseBoolean(value, brandingEnabled);
                    } else if (key.equals("text")) {
                        brandingText = value;
                    }
                } else if (inMotd) {
                    if (key.equals("enabled")) {
                        motdEnabled = parseBoolean(value, motdEnabled);
                    } else if (key.equals("text")) {
                        motdText = value;
                    } else if (key.equals("lines")) {
                        readingMotdLines = true;
                        motdLines.clear();
                    } else {
                        readingMotdLines = false;
                    }
                }
                if (!key.equals("lines")) {
                    readingMotdLines = false;
                }
                continue;
            }
            if (inMotd && readingMotdLines && trimmed.startsWith("-")) {
                String item = trimmed.substring(1).trim();
                motdLines.add(unquote(item));
            }
        }

        if (motdLines.isEmpty()) {
            if (motdText != null) {
                motdLines.add(motdText);
            } else {
                motdLines.add("");
            }
        }
        if (hoverEnabled && hoverLines.isEmpty()) {
            hoverLines.addAll(motdLines);
        }

        List<String> motdLinesColored = new ArrayList<>(motdLines.size());
        for (String line : motdLines) {
            motdLinesColored.add(ColorUtil.translate(line));
        }
        List<String> hoverLinesColored = new ArrayList<>(hoverLines.size());
        for (String line : hoverLines) {
            hoverLinesColored.add(ColorUtil.translate(line));
        }
        String motdJoinedColored = String.join("\n", motdLinesColored);
        String brandingColored = ColorUtil.translate(brandingText);

        this.brandingEnabled = brandingEnabled;
        this.brandingTextColored = brandingColored;
        this.brandingPayload = createBrandPayload(brandingColored);
        this.motdEnabled = motdEnabled;
        this.motdLinesColored = Collections.unmodifiableList(motdLinesColored);
        this.motdJoinedColored = motdJoinedColored;
        this.hoverEnabled = hoverEnabled;
        this.hoverLinesColored = Collections.unmodifiableList(hoverLinesColored);
    }

    private static int countLeadingSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    private static String unquote(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }

    private static boolean parseBoolean(String value, boolean def) {
        if (value == null || value.isEmpty()) {
            return def;
        }
        String v = value.toLowerCase();
        if (v.equals("true") || v.equals("yes") || v.equals("on")) {
            return true;
        }
        if (v.equals("false") || v.equals("no") || v.equals("off")) {
            return false;
        }
        return def;
    }

    private static byte[] createBrandPayload(String brand) {
        byte[] text = brand.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[text.length + 5];
        int len = writeVarInt(text.length, payload, 0);
        System.arraycopy(text, 0, payload, len, text.length);
        int finalLen = len + text.length;
        if (finalLen == payload.length) {
            return payload;
        }
        byte[] trimmed = new byte[finalLen];
        System.arraycopy(payload, 0, trimmed, 0, finalLen);
        return trimmed;
    }

    private static int writeVarInt(int value, byte[] out, int index) {
        int i = value;
        while ((i & 0xFFFFFF80) != 0) {
            out[index++] = (byte) (i & 0x7F | 0x80);
            i >>>= 7;
        }
        out[index++] = (byte) i;
        return index;
    }
}
