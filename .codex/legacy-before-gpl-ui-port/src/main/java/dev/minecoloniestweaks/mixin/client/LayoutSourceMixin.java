package dev.minecoloniestweaks.mixin.client;

import com.ldtteam.blockui.Loader;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.views.View;
import dev.minecoloniestweaks.client.screen.ScreenTheme;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Loader.class, remap = false)
public abstract class LayoutSourceMixin {
    @Inject(method = "createFromXMLFile", at = @At("RETURN"))
    private static void rememberLayout(ResourceLocation source, View parent, CallbackInfoReturnable<Pane> callback) {
        ScreenTheme.remember(callback.getReturnValue(), source);
    }
}
