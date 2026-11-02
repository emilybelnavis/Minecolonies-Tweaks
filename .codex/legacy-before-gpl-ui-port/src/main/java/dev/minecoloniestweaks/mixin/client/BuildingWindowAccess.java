package dev.minecoloniestweaks.mixin.client;

import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.core.client.gui.AbstractBuildingWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractBuildingWindow.class, remap = false)
public interface BuildingWindowAccess {
    @Accessor("buildingView") IBuildingView tweaks$building();
}
