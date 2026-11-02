package dev.minecoloniestweaks.mixin.client;

import com.ldtteam.blockui.BOGuiGraphics;
import com.ldtteam.blockui.controls.ButtonImage;
import dev.minecoloniestweaks.client.screen.ScreenTheme;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ButtonImage.class, remap = false)
public abstract class ButtonThemeMixin {
    @Inject(method = "drawSelf", at = @At("HEAD"), cancellable = true)
    private void drawVanillaButton(BOGuiGraphics graphics, double mouseX, double mouseY, CallbackInfo callback) {
        var button = (ButtonImage) (Object) this;
        if (!ScreenTheme.enabled(button) || button.isTextEmpty()) return;
        var font = Minecraft.getInstance().font;
        boolean hovered = button.isPointInPane(mouseX, mouseY);
        graphics.blitSprite(ButtonImage.VANILLA_BUTTON.get(button.isEnabled(), hovered),
                button.getX(), button.getY(), button.getWidth(), button.getHeight());
        String text = font.plainSubstrByWidth(button.getTextAsString(), Math.max(0, button.getWidth() - 6));
        int color = !button.isEnabled() ? 0xA0A0A0 : hovered ? 0xFFFFA0 : 0xFFFFFF;
        graphics.drawCenteredString(font, text, button.getX() + button.getWidth() / 2,
                button.getY() + (button.getHeight() - 8) / 2, color);
        callback.cancel();
    }
}
