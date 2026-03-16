package si.terona.nullbrand.model;

import java.util.Collections;
import java.util.List;

public final class MotdProfile {
    private final List<String> lines;
    private final List<String> hoverLines;
    private final boolean hoverEnabled;
    private final String joined;

    public MotdProfile(List<String> lines, List<String> hoverLines, boolean hoverEnabled) {
        this.lines = Collections.unmodifiableList(lines);
        this.hoverLines = Collections.unmodifiableList(hoverLines);
        this.hoverEnabled = hoverEnabled;
        this.joined = String.join("\n", lines);
    }

    public List<String> getLines() {
        return lines;
    }

    public List<String> getHoverLines() {
        return hoverLines;
    }

    public boolean isHoverEnabled() {
        return hoverEnabled;
    }

    public String getJoined() {
        return joined;
    }
}
