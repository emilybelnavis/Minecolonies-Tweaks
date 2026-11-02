package dev.minecoloniestweaks.mixin.client;

import com.ldtteam.blockui.BOGuiGraphics;
import com.ldtteam.blockui.controls.Image;
import dev.minecoloniestweaks.client.screen.ScreenTheme;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Image.class, remap = false)
public abstract class ImageThemeMixin {
    @Shadow protected ResourceLocation resourceLocation;

    @Inject(method = "drawSelf", at = @At("HEAD"), cancellable = true)
    private void drawVanillaBackdrop(BOGuiGraphics graphics, double mouseX, double mouseY, CallbackInfo callback) {
        var image = (Image) (Object) this;
        if (ScreenTheme.isCitizenContent() && resourceLocation != null
                && resourceLocation.getPath().startsWith("textures/gui/citizen/")) {
            callback.cancel();
            return;
        }
        if (ScreenTheme.enabled(image) && ScreenTheme.isBackdrop(image, resourceLocation)) {
            ScreenTheme.panel(graphics, image.getX(), image.getY(), image.getWidth(), image.getHeight());
            callback.cancel();
        }
    }
}
