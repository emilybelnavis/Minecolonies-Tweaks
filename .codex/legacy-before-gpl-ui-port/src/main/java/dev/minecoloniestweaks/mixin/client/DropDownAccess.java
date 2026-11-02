package dev.minecoloniestweaks.mixin.client;

import com.ldtteam.blockui.views.DropDownList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = DropDownList.class, remap = false)
public interface DropDownAccess {
    @Accessor("dataProvider") DropDownList.DataProvider tweaks$options();
}
