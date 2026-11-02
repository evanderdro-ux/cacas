package com.basefinder;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Editor visual: arrastra cualquier módulo y suelta para guardar. */
public final class HudEditorScreen extends Screen {
    private int dragging = -1;

    public HudEditorScreen() {
        super(Component.literal("BaseFinder HUD Editor"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x5504000A);
        HudOverlay.renderEditor(g);

        int barW = 372, barH = 34;
        int bx = (width - barW) / 2;
        g.fill(bx, 8, bx + barW, 8 + barH, 0xF00D0917);
        g.fill(bx, 8, bx + barW, 10, 0xFFB55AFF);
        g.outline(bx, 8, barW, barH, 0xFF8A59B9);
        g.text(font, "EDITOR HUD  •  ARRASTRA LOS PANELES", bx + 12, 16, 0xFFFFF4FF, true);
        g.text(font, "ESC  CERRAR", bx + barW - 100, 16, 0xFFCFBCE6, false);

        int rx = width - 188;
        g.fill(rx, 8, rx + 84, 28, 0xE82B1C37);
        g.outline(rx, 8, 84, 20, 0xFF9868C3);
        g.text(font, "REINICIAR", rx + 9, 14, 0xFFFFE9FF, false);
        g.fill(rx + 90, 8, rx + 180, 28, 0xE82B1C37);
        g.outline(rx + 90, 8, 90, 20, 0xFF9868C3);
        g.text(font, "GUARDAR", rx + 103, 14, 0xFFFFE9FF, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.buttonInfo().button() == 1) {
            double mx = event.x(), my = event.y();
            int rx = width - 188;
            if (mx >= rx && mx <= rx + 84 && my >= 8 && my <= 28) {
                HudLayout.reset();
                return true;
            }
            if (mx >= rx + 90 && mx <= rx + 180 && my >= 8 && my <= 28) {
                HudLayout.save();
                onClose();
                return true;
            }

            for (int id = HudLayout.COUNT - 1; id >= 0; id--) {
                int x = HudLayout.x(id, width), y = HudLayout.y(id, height);
                if (mx >= x && mx <= x + HudLayout.width(id)
                        && my >= y && my <= y + HudLayout.height(id)) {
                    dragging = id;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging >= 0 && event.buttonInfo().button() == 1) {
            int id = dragging;
            HudLayout.move(id,
                    HudLayout.x(id, width) + (int)Math.round(dx),
                    HudLayout.y(id, height) + (int)Math.round(dy),
                    width, height);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging >= 0) {
            dragging = -1;
            HudLayout.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void removed() {
        HudLayout.save();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
