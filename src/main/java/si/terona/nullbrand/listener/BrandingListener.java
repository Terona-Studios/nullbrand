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
        private boolean protocolLibEnabled = false;

        public Spigot(org.bukkit.plugin.Plugin plugin, ConfigUtil config) {
            this.plugin = plugin;
            this.config = config;
            setupProtocolLib();
        }

        private void setupProtocolLib() {
            if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
                try {
                    Class.forName("com.comphenix.protocol.ProtocolLibrary");
                    registerProtocolLibListener();
                    protocolLibEnabled = true;
                } catch (ClassNotFoundException e) {
                    if (config.isDebugEnabled()) {
                        plugin.getLogger().warning("ProtocolLib class not found despite plugin being enabled.");
                    }
                } catch (Exception e) {
                    if (config.isDebugEnabled()) {
                        plugin.getLogger().warning("Failed to register ProtocolLib listener: " + e.getMessage());
                    }
                }
            } else if (config.isDebugEnabled()) {
                plugin.getLogger().warning("ProtocolLib not found. F3 branding override disabled.");
            }
        }

        private void registerProtocolLibListener() throws Exception {
            Class<?> protocolLibraryClass = Class.forName("com.comphenix.protocol.ProtocolLibrary");
            Object protocolManager = protocolLibraryClass.getMethod("getProtocolManager").invoke(null);
            
            Class<?> packetAdapterClass = Class.forName("com.comphenix.protocol.events.PacketAdapter");
            Class<?> packetTypeClass = Class.forName("com.comphenix.protocol.PacketType");
            Class<?> serverClass = Class.forName("com.comphenix.protocol.PacketType$Play$Server");
            Object customPayloadType = serverClass.getField("CUSTOM_PAYLOAD").get(null);
            
            // Use reflection to create the PacketListener to avoid compile-time dependency
            java.lang.reflect.Method addPacketListener = protocolManager.getClass().getMethod("addPacketListener", Class.forName("com.comphenix.protocol.events.PacketListener"));

            Object packetAdapter = java.lang.reflect.Proxy.newProxyInstance(
                plugin.getClass().getClassLoader(),
                new Class<?>[]{Class.forName("com.comphenix.protocol.events.PacketListener")},
                (proxy, method, args) -> {
                    if (method.getName().equals("onPacketSending")) {
                        Object event = args[0];
                        if (!config.isBrandingEnabled()) return null;
                        
                        try {
                            Object packet = event.getClass().getMethod("getPacket").invoke(event);
                            Object structures = packet.getClass().getMethod("getStructures").invoke(packet);
                            Object modifier = structures.getClass().getMethod("read", int.class).invoke(structures, 0);
                            
                            String identifier = null;
                            if (modifier != null) {
                                Object handle = modifier.getClass().getMethod("getHandle").invoke(modifier);
                                if (handle != null) identifier = handle.toString();
                            }
                            
                            if (identifier == null) {
                                Object strings = packet.getClass().getMethod("getStrings").invoke(packet);
                                identifier = (String) strings.getClass().getMethod("read", int.class).invoke(strings, 0);
                            }
                            
                            if (BRAND_MODERN.equals(identifier) || BRAND_LEGACY.equals(identifier) || 
                                (identifier != null && (identifier.endsWith(":brand") || identifier.equals("MC|Brand")))) {
                                Object byteArrays = packet.getClass().getMethod("getByteArrays").invoke(packet);
                                byteArrays.getClass().getMethod("write", int.class, Object.class).invoke(byteArrays, 0, config.getBrandingPayload());
                            }
                        } catch (Exception ignored) {}
                    } else if (method.getName().equals("getPlugin")) {
                        return plugin;
                    } else if (method.getName().equals("getSendingPriority")) {
                        return Class.forName("com.comphenix.protocol.events.ListenerPriority").getField("NORMAL").get(null);
                    } else if (method.getName().equals("getConnectionSide")) {
                        return Class.forName("com.comphenix.protocol.events.ConnectionSide").getField("SERVER_SIDE").get(null);
                    }
                    return null;
                }
            );
            
            addPacketListener.invoke(protocolManager, packetAdapter);
        }

        @org.bukkit.event.EventHandler
        public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
            if (protocolLibEnabled || !config.isBrandingEnabled()) {
                return;
            }
            org.bukkit.entity.Player player = event.getPlayer();
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    sendBrand(player);
                }
            }, 1L);
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
