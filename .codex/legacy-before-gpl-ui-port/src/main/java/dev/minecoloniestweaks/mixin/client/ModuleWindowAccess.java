package dev.minecoloniestweaks.mixin.client;

import com.minecolonies.api.colony.buildings.modules.IBuildingModuleView;
import com.minecolonies.core.client.gui.AbstractModuleWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractModuleWindow.class, remap = false)
public interface ModuleWindowAccess {
    @Accessor("moduleView") IBuildingModuleView tweaks$module();
}
