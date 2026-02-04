package si.terona.nullbrand.listener;

import si.terona.nullbrand.util.ConfigUtil;

public final class BrandingListener {

    private static final String BRAND_MODERN = "minecraft:brand";
    private static final String BRAND_LEGACY = "MC|Brand";

    private BrandingListener() {
    }

    public static final class Velocity {

        private final ConfigUtil config;
        private final com.velocitypowered.api.proxy.messages.ChannelIdentifier modern;
        private final com.velocitypowered.api.proxy.messages.ChannelIdentifier legacy;

        public Velocity(ConfigUtil config,
                        com.velocitypowered.api.proxy.messages.ChannelIdentifier modern,
                        com.velocitypowered.api.proxy.messages.ChannelIdentifier legacy) {
            this.config = config;
            this.modern = modern;
            this.legacy = legacy;
        }

        @com.velocitypowered.api.event.Subscribe
        public void onPluginMessage(com.velocitypowered.api.event.connection.PluginMessageEvent event) {
            if (!config.isBrandingEnabled()) {
                return;
            }
            if (!(event.getSource() instanceof com.velocitypowered.api.proxy.ServerConnection)) {
                return;
            }
            if (!(event.getTarget() instanceof com.velocitypowered.api.proxy.Player)) {
                return;
            }
            com.velocitypowered.api.proxy.messages.ChannelIdentifier id = event.getIdentifier();
            if (!id.equals(modern) && !id.equals(legacy)) {
                return;
            }
            event.setResult(com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult.handled());
            com.velocitypowered.api.proxy.Player player = (com.velocitypowered.api.proxy.Player) event.getTarget();
            player.sendPluginMessage(id, config.getBrandingPayload());
        }
    }

    public static final class Bungee implements net.md_5.bungee.api.plugin.Listener {

        private final ConfigUtil config;

        public Bungee(ConfigUtil config) {
            this.config = config;
        }

        @net.md_5.bungee.event.EventHandler
        public void onPluginMessage(net.md_5.bungee.api.event.PluginMessageEvent event) {
            if (!config.isBrandingEnabled()) {
                return;
            }
            String tag = event.getTag();
            if (!BRAND_MODERN.equalsIgnoreCase(tag) && !BRAND_LEGACY.equalsIgnoreCase(tag)) {
                return;
            }
            if (!(event.getSender() instanceof net.md_5.bungee.api.connection.Server)) {
                return;
            }
            if (!(event.getReceiver() instanceof net.md_5.bungee.api.connection.ProxiedPlayer)) {
                return;
            }
            event.setCancelled(true);
            net.md_5.bungee.api.connection.ProxiedPlayer player =
                    (net.md_5.bungee.api.connection.ProxiedPlayer) event.getReceiver();
            player.sendData(tag, config.getBrandingPayload());
        }
    }

    public static final class Spigot implements org.bukkit.event.Listener {

        private final org.bukkit.plugin.Plugin plugin;
        private final ConfigUtil config;

        public Spigot(org.bukkit.plugin.Plugin plugin, ConfigUtil config) {
            this.plugin = plugin;
            this.config = config;
        }

        @org.bukkit.event.EventHandler
        public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
            if (!config.isBrandingEnabled()) {
                return;
            }
            sendBrand(event.getPlayer());
        }

        private void sendBrand(org.bukkit.entity.Player player) {
            byte[] payload = config.getBrandingPayload();
            player.sendPluginMessage(plugin, BRAND_MODERN, payload);
            try {
                player.sendPluginMessage(plugin, BRAND_LEGACY, payload);
            } catch (IllegalArgumentException ignored) {
                // Legacy channel not supported on this version
            }
        }
    }
}
