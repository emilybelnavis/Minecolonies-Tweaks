package dev.minecoloniestweaks.mixin.client;

import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.client.gui.citizen.AbstractWindowCitizen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractWindowCitizen.class, remap = false)
public interface CitizenWindowAccess {
    @Accessor("citizen") ICitizenDataView tweaks$citizen();
}
