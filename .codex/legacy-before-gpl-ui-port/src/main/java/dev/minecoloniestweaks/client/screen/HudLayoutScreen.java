package dev.minecoloniestweaks.client.screen;

import dev.minecoloniestweaks.client.ClientPreferences;
import dev.minecoloniestweaks.client.hud.ResourceHud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class HudLayoutScreen extends Screen {
    private ClientPreferences.PinnedBuilder dragging;
    private double offsetX;
    private double offsetY;

    public HudLayoutScreen() { super(Component.translatable("screen.minecoloniestweaks.arrange")); }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x55000000);
        ResourceHud.render(graphics, true);
        graphics.drawCenteredString(font, Component.translatable("screen.minecoloniestweaks.arrange_help"), width / 2, height - 20, 0xFFFFFF);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var settings = ClientPreferences.INSTANCE;
        double scale = settings.hudScale;
        double x = mouseX / scale;
        double y = mouseY / scale;
        for (var pin : settings.pins().reversed()) {
            if (!minecraft.level.dimension().location().toString().equals(pin.dimension)) continue;
            double px = Math.clamp(pin.x * width / scale, 0, Math.max(0, width / scale - settings.panelWidth));
            double py = Math.clamp(pin.y * height / scale, 0, Math.max(0, height / scale - ResourceHud.panelHeight(pin)));
            if (x >= px && x < px + settings.panelWidth && y >= py && y < py + ResourceHud.panelHeight(pin)) {
                if (button == 1) {
                    settings.pins().remove(pin);
                    settings.save();
                } else if (button == 0) {
                    dragging = pin;
                    offsetX = x - px;
                    offsetY = y - py;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging == null) return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        double scale = ClientPreferences.INSTANCE.hudScale;
        dragging.x = Math.clamp((mouseX - offsetX * scale) / width, 0, 1);
        dragging.y = Math.clamp((mouseY - offsetY * scale) / height, 0, 1);
        return true;
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = null;
        ClientPreferences.INSTANCE.save();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public void removed() { ClientPreferences.INSTANCE.save(); }
    @Override public boolean isPauseScreen() { return false; }
}
