package dev.minecoloniestweaks;

import dev.minecoloniestweaks.config.ServerConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(MineColoniesTweaks.ID)
public final class MineColoniesTweaks {
    public static final String ID = "minecoloniestweaks";

    public MineColoniesTweaks(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }
}
