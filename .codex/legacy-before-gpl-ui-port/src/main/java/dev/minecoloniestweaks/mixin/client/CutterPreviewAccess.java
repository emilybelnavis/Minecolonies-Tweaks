package dev.minecoloniestweaks.mixin.client;

import com.ldtteam.domumornamentum.client.screens.ArchitectsCutterScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ArchitectsCutterScreen.class, remap = false)
public interface CutterPreviewAccess {
    @Invoker("texturizeVariantUsingCurrentInput") void tweaks$applyMaterials(ItemStack preview);
}
