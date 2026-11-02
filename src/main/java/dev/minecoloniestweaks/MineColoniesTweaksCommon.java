package dev.minecoloniestweaks;

import dev.minecoloniestweaks.names.NameListsPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Entry point on both sides (the client one is {@link MineColoniesTweaks}). The citizen names need the
 * server: the colony owner's lists reach it through {@link NameListsPayload}, and {@code CitizenDataMixin} uses them
 * where MineColonies names citizens. {@code EntityAIEatTaskMixin} (hungry cook fix) needs nothing registered here.
 * Must not touch client classes: it also loads on dedicated servers.
 */
@Mod(MineColoniesTweaks.MOD_ID)
public final class MineColoniesTweaksCommon
{
    public MineColoniesTweaksCommon(final IEventBus modBus)
    {
        modBus.addListener(NameListsPayload::register);
    }
}
