package dev.minecoloniestweaks.client.screen;

import com.minecolonies.api.colony.ICitizenDataView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DiagnosisScreen extends Screen {
    private final Screen parent;
    private final ICitizenDataView citizen;

    public DiagnosisScreen(Screen parent, ICitizenDataView citizen) {
        super(Component.translatable("screen.minecoloniestweaks.diagnosis"));
        this.parent = parent;
        this.citizen = citizen;
    }

    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(width / 2 - 75, height - 30, 150, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFF);
        graphics.drawCenteredString(font, citizen.getName(), width / 2, 34, 0xAAAAAA);
        int y = 60;
        for (var reason : CitizenDiagnosis.explain(citizen)) {
            for (var line : font.split(reason, width - 50)) {
                graphics.drawString(font, line, 25, y, 0xFFFFFF);
                y += 12;
            }
        }
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
