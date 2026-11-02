package dev.minecoloniestweaks.mixin.client;

import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.core.client.gui.WindowResearchTree;
import com.minecolonies.core.client.gui.modules.building.UniversityModuleWindow;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = WindowResearchTree.class, remap = false)
public interface ResearchWindowAccess {
    @Accessor("branch") ResourceLocation tweaks$branch();
    @Accessor("building") IBuildingView tweaks$building();
    @Accessor("last") UniversityModuleWindow tweaks$parent();
}
