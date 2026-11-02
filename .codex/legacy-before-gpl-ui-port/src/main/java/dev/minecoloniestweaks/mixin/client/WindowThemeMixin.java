package dev.minecoloniestweaks.mixin.client;

import com.ldtteam.blockui.BOGuiGraphics;
import com.ldtteam.blockui.views.BOWindow;
import dev.minecoloniestweaks.client.screen.ScreenTheme;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BOWindow.class, remap = false)
public abstract class WindowThemeMixin {
    @Inject(method = "drawSelf", at = @At("HEAD"))
    private void drawVanillaFrame(BOGuiGraphics graphics, double mouseX, double mouseY, CallbackInfo callback) {
        var window = (BOWindow) (Object) this;
        if (ScreenTheme.enabled(window)) {
            ScreenTheme.panel(graphics, 0, 0, window.getWidth(), window.getHeight());
        }
    }
}
