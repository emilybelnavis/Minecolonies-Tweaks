package dev.minecoloniestweaks.mixin.client;

import com.minecolonies.core.client.gui.WindowResourceList;
import com.minecolonies.core.colony.buildings.utils.BuildingBuilderResource;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = WindowResourceList.class, remap = false)
public interface ResourceScrollAccess {
    @Invoker("pullResourcesFromHut") void tweaks$refresh();
    @Accessor("resources") List<BuildingBuilderResource> tweaks$resources();
}
