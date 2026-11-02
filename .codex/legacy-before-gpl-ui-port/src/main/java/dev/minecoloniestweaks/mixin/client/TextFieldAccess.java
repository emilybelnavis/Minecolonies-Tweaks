package dev.minecoloniestweaks.mixin.client;

import com.ldtteam.blockui.controls.TextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = TextField.class, remap = false)
public interface TextFieldAccess {
    @Invoker("triggerHandler") void tweaks$notifyChanged();
}
