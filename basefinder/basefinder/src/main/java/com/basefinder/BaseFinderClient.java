package com.basefinder;

import java.util.List;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

public class BaseFinderClient implements ClientModInitializer {
    public static final String MOD_ID = "basefinder";
    public static volatile boolean enabled = false;
    public static volatile boolean hudEnabled = true;
    public static volatile boolean radarEnabled = true;
    public static volatile boolean chunkMapEnabled = true;
    public static volatile boolean hotkeyListEnabled = true;

    public static volatile boolean containerEspEnabled = true;
    public static volatile boolean baseBoxesEnabled = true;
    public static volatile boolean chunkBoxesEnabled = true;
    public static volatile boolean tracersEnabled = false;
    public static volatile boolean transparentEnabled = true;
    public static volatile boolean labelsEnabled = true;

    public static volatile List<Marker> markers = List.of();
    private static KeyMapping toggleKey, menuKey;
    private static int tick;

    @Override public void onInitializeClient() {
        Config.load();
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.basefinder.toggle",
                InputConstants.Type.KEYBOARD, InputConstants.KEY_B, category));
        menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.basefinder.menu",
                InputConstants.Type.KEYBOARD, InputConstants.KEY_M, category));
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "hud"), HudOverlay::render);
        Renderer.init();
    }

    public static void setScan(boolean value) {
        enabled = value;
        if (!value) {
            markers = List.of();
            Scanner.clear();
        }
    }

    private void onTick(Minecraft client) {
        while (toggleKey.consumeClick()) {
            setScan(!enabled);
            if (client.player != null) client.player.sendSystemMessage(Component.literal(
                    "BaseFinder: " + (enabled ? "ACTIVADO" : "PAUSADO")));
        }
        while (menuKey.consumeClick()) {
            if (client.player != null && client.gui.screen() == null) client.gui.setScreen(new HudScreen());
        }
        if (!enabled || client.player == null || client.level == null) return;

        Scanner.tick(client);
        if (++tick % 5 == 0) {
            markers = Scanner.buildMarkers(client.player.getX(), client.player.getZ());
        }
    }
}
