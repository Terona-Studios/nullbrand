package si.terona.nullbrand.listener;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import si.terona.nullbrand.util.ConfigUtil;

public final class MotdListener {

    private MotdListener() {
    }

    public static final class Velocity {

        private static final net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer LEGACY =
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.builder()
                        .character('\u00A7')
                        .hexColors()
                        .build();

        private final ConfigUtil config;
        private String cachedMotd;
        private net.kyori.adventure.text.Component cachedComponent;
        private List<String> cachedHoverLines;
        private List<com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer> cachedSample;

        public Velocity(ConfigUtil config) {
            this.config = config;
        }

        @com.velocitypowered.api.event.Subscribe
        public void onProxyPing(com.velocitypowered.api.event.proxy.ProxyPingEvent event) {
            if (!config.isMotdEnabled()) {
                return;
            }
            si.terona.nullbrand.model.MotdProfile profile = config.getActiveProfile();
            String motd = profile.getJoined();
            if (cachedComponent == null || !motd.equals(cachedMotd)) {
                cachedMotd = motd;
                cachedComponent = LEGACY.deserialize(motd);
            }
            com.velocitypowered.api.proxy.server.ServerPing.Builder builder =
                    event.getPing().asBuilder().description(cachedComponent);

            Object favicon = config.getCachedFaviconVelocity();
            if (favicon instanceof com.velocitypowered.api.util.Favicon) {
                builder.favicon((com.velocitypowered.api.util.Favicon) favicon);
            }

            if (profile.isHoverEnabled()) {
                List<String> hoverLines = profile.getHoverLines();
                if (cachedSample == null || !hoverLines.equals(cachedHoverLines)) {
                    cachedHoverLines = hoverLines;
                    cachedSample = new ArrayList<>(hoverLines.size());
                    int index = 0;
                    for (String line : hoverLines) {
                        UUID id = UUID.nameUUIDFromBytes(("nb-hover-" + index).getBytes(StandardCharsets.UTF_8));
                        cachedSample.add(new com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer(line, id));
                        index++;
                    }
                }
                builder.samplePlayers(cachedSample.toArray(new com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer[0]));
            }
            event.setPing(builder.build());
        }
    }

    public static final class Bungee implements net.md_5.bungee.api.plugin.Listener {

        private final ConfigUtil config;
        private List<String> cachedHoverLines;
        private net.md_5.bungee.api.ServerPing.PlayerInfo[] cachedSample;

        public Bungee(ConfigUtil config) {
            this.config = config;
        }

        @net.md_5.bungee.event.EventHandler
        public void onProxyPing(net.md_5.bungee.api.event.ProxyPingEvent event) {
            if (!config.isMotdEnabled()) {
                return;
            }
            if (event.getResponse() == null) {
                return;
            }
            si.terona.nullbrand.model.MotdProfile profile = config.getActiveProfile();
            event.getResponse().setDescription(profile.getJoined());

            Object favicon = config.getCachedFaviconBungee();
            if (favicon instanceof net.md_5.bungee.api.Favicon) {
                event.getResponse().setFavicon((net.md_5.bungee.api.Favicon) favicon);
            }

            if (!profile.isHoverEnabled()) {
                return;
            }
            List<String> hoverLines = profile.getHoverLines();
            if (cachedSample == null || !hoverLines.equals(cachedHoverLines)) {
                cachedHoverLines = hoverLines;
                cachedSample = new net.md_5.bungee.api.ServerPing.PlayerInfo[hoverLines.size()];
                int index = 0;
                for (String line : hoverLines) {
                    UUID id = UUID.nameUUIDFromBytes(("nb-hover-" + index).getBytes(StandardCharsets.UTF_8));
                    cachedSample[index] = new net.md_5.bungee.api.ServerPing.PlayerInfo(line, id);
                    index++;
                }
            }
            net.md_5.bungee.api.ServerPing.Players players = event.getResponse().getPlayers();
            if (players != null) {
                players.setSample(cachedSample);
            }
        }
    }

    public static final class Spigot implements org.bukkit.event.Listener {

        private final ConfigUtil config;

        public Spigot(ConfigUtil config) {
            this.config = config;
        }

        @org.bukkit.event.EventHandler
        public void onProxyPing(org.bukkit.event.server.ServerListPingEvent event) {
            handle(event);
        }

        @org.bukkit.event.EventHandler
        public void onPaperProxyPing(com.destroystokyo.paper.event.server.PaperServerListPingEvent event) {
            handle(event);
        }

        private void handle(org.bukkit.event.server.ServerListPingEvent event) {
            if (!config.isMotdEnabled()) {
                return;
            }
            si.terona.nullbrand.model.MotdProfile profile = config.getActiveProfile();
            event.setMotd(profile.getJoined());

            Object favicon = config.getCachedFaviconSpigot();
            if (favicon instanceof org.bukkit.util.CachedServerIcon) {
                event.setServerIcon((org.bukkit.util.CachedServerIcon) favicon);
            }
        }
    }
}
