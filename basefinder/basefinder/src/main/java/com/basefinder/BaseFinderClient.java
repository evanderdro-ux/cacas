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

public class BaseFinderClient implements ClientModInitializer {
    public static final String MOD_ID = "basefinder";

    /** Se activa/desactiva con la tecla (por defecto B). */
    public static volatile boolean enabled = false;
    /** Cajas a dibujar (lista inmutable, la lee el render). */
    public static volatile List<Marker> markers = List.of();

    private static KeyMapping toggleKey;
    private static int tick;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.basefinder.toggle",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_B,
                category));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        Renderer.init();
    }

    private void onTick(Minecraft client) {
        while (toggleKey.consumeClick()) {
            enabled = !enabled;
            if (!enabled) {
                markers = List.of();
                Scanner.clear();
            }
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal(
                        "Base Finder: " + (enabled ? "ACTIVADO" : "DESACTIVADO")));
            }
        }

        if (!enabled || client.player == null || client.level == null) return;

        Scanner.tick(client);
        if (++tick % 20 == 0) {
            markers = Scanner.buildMarkers(client.player.getX(), client.player.getZ());
        }
    }
}
