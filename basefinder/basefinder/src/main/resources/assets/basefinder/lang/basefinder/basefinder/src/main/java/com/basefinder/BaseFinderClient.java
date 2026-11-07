package com.basefinder;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

public class BaseFinderClient implements ClientModInitializer {
    public static final String MOD_ID = "basefinder";

    /** ESP activo (tecla B o boton del menu). */
    public static volatile boolean enabled = false;
    /** Cajas a dibujar (lista inmutable, la lee el render). */
    public static volatile List<Marker> markers = List.of();

    private static KeyMapping toggleKey;
    private static KeyMapping guiKey;
    private static int tick;

    @Override
    public void onInitializeClient() {
        Settings.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.basefinder.toggle", InputConstants.Type.KEYBOARD, InputConstants.KEY_B, category));
        guiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.basefinder.gui", InputConstants.Type.KEYBOARD, InputConstants.KEY_RSHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        Renderer.init();
        Hud.init();
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            markers = List.of();
            Scanner.clear();
        }
        Hud.toast(value ? "ESP ACTIVADO" : "ESP DESACTIVADO");
    }

    private void onTick(Minecraft client) {
        while (toggleKey.consumeClick()) {
            setEnabled(!enabled);
        }
        while (guiKey.consumeClick()) {
            if (client.player != null) {
                client.gui.setScreen(new BaseFinderScreen());
            }
        }

        if (!enabled || client.player == null || client.level == null) return;

        Scanner.tick(client);
        if (++tick % 20 == 0) {
            markers = Scanner.buildMarkers(client.player.getX(), client.player.getZ());
        }
    }

    /** Fuerza un recalculo inmediato (al cambiar ajustes en el menu). */
    public static void refreshNow() {
        Minecraft mc = Minecraft.getInstance();
        if (enabled && mc.player != null) {
            markers = Scanner.buildMarkers(mc.player.getX(), mc.player.getZ());
        }
    }
}
