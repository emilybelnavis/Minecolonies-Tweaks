package dev.minecoloniestweaks;

import com.mojang.logging.LogUtils;
import dev.minecoloniestweaks.client.KeyBindings;
import dev.minecoloniestweaks.client.ModSettings;
import dev.minecoloniestweaks.client.PinnedBuilders;
import dev.minecoloniestweaks.client.citizen.CitizenScreens;
import dev.minecoloniestweaks.client.townhall.ColonyFreshness;
import dev.minecoloniestweaks.client.townhall.TownHallScreens;
import dev.minecoloniestweaks.client.hut.HutScreens;
import dev.minecoloniestweaks.client.hut.TeachScreens;
import dev.minecoloniestweaks.client.buildtool.BuildToolScreens;
import dev.minecoloniestweaks.client.domum.CutterScreens;
import dev.minecoloniestweaks.client.university.UniversityScreens;
import dev.minecoloniestweaks.client.data.ResourceHudTracker;
import dev.minecoloniestweaks.client.fixes.BlockUiWindowHooks;
import dev.minecoloniestweaks.client.fixes.CitizenInventoryTabs;
import dev.minecoloniestweaks.client.hud.ResourceHudLayer;
import dev.minecoloniestweaks.client.locate.LocateBeams;
import dev.minecoloniestweaks.client.names.NameLibrary;
import dev.minecoloniestweaks.client.names.NameSync;
import dev.minecoloniestweaks.config.ConfigMigration;
import dev.minecoloniestweaks.config.HudConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * MineColonies Vanilla Tweaks, client entry point (the one for both sides is {@link MineColoniesTweaksCommon}).
 * Vanilla-style screens for MineColonies' windows, a HUD of the builders' resources, "locate" beams and the editor of
 * the citizen name lists.
 */
@Mod(value = MineColoniesTweaks.MOD_ID, dist = Dist.CLIENT)
public final class MineColoniesTweaks
{
    public static final String MOD_ID = "minecoloniestweaks";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static ModContainer modContainer;

    public MineColoniesTweaks(final IEventBus modBus, final ModContainer container)
    {
        modContainer = container;
        // Before registering: NeoForge drops the old top-level entries when it loads the new sections.
        ConfigMigration.readOldFile();
        container.registerConfig(ModConfig.Type.CLIENT, HudConfig.SPEC);
        modBus.addListener(ConfigMigration::onConfigLoading);
        // NeoForge's generic config screen (Mods > MineColonies Vanilla Tweaks > Config).
        container.registerExtensionPoint(IConfigScreenFactory.class, ModSettings::create);

        modBus.addListener(KeyBindings::register);
        modBus.addListener(ResourceHudLayer::register);
        modBus.addListener(LocateBeams::registerLayer);
        modBus.addListener(NameLibrary::registerReloadListener);

        NeoForge.EVENT_BUS.addListener(ResourceHudTracker::onClientTick);
        NeoForge.EVENT_BUS.addListener(ResourceHudTracker::onLoggingOut);
        // Fixes for long-standing MineColonies UI bugs, for its own windows.
        NeoForge.EVENT_BUS.addListener(BlockUiWindowHooks::onClientTick);
        NeoForge.EVENT_BUS.addListener(CitizenInventoryTabs::onScreenInit);
        // Vanilla-style citizen screen, replacing MineColonies' citizen windows as they open.
        NeoForge.EVENT_BUS.addListener(CitizenScreens::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(TownHallScreens::onScreenOpening);
        // When the player was last in each colony (the clipboard's dashboard says how old its data is).
        NeoForge.EVENT_BUS.addListener(ColonyFreshness::onClientTick);
        NeoForge.EVENT_BUS.addListener(ColonyFreshness::onLoggingOut);
        // Colony guide (key G): the huts of the colony the player stands in, and a message on entering someone else's.
        NeoForge.EVENT_BUS.addListener(dev.minecoloniestweaks.client.townhall.ColonyGuide::onClientTick);
        NeoForge.EVENT_BUS.addListener(dev.minecoloniestweaks.client.townhall.ColonyGuide::onLoggingOut);
        // "Locate" beams above citizens and huts.
        NeoForge.EVENT_BUS.addListener(LocateBeams::onClientTick);
        NeoForge.EVENT_BUS.addListener(dev.minecoloniestweaks.client.locate.DeathBeams::onClientTick);
        NeoForge.EVENT_BUS.addListener(dev.minecoloniestweaks.client.locate.DeathBeams::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(LocateBeams::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(LocateBeams::onRenderLevel);
        // Builders' "building finished" messages: a click beams the hut and/or gives its coordinates.
        NeoForge.EVENT_BUS.addListener(dev.minecoloniestweaks.client.locate.CompletionMessages::onChatReceived);
        NeoForge.EVENT_BUS.addListener(dev.minecoloniestweaks.client.locate.CompletionMessages::registerCommands);
        NeoForge.EVENT_BUS.addListener(UniversityScreens::onScreenOpening);
        // Every other hut: home page, workers, settings, statistics (and the university's research page).
        NeoForge.EVENT_BUS.addListener(HutScreens::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(BuildToolScreens::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(CutterScreens::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(TeachScreens::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(dev.minecoloniestweaks.client.dialog.DialogScreens::onScreenOpening);

        // Citizen name lists: sent to the server (single player included) on joining a world.
        NeoForge.EVENT_BUS.addListener(NameSync::onLoggingIn);
        NeoForge.EVENT_BUS.addListener(NameSync::onLoggingOut);

        PinnedBuilders.load();
        NameLibrary.load();
    }

    public static ModContainer container()
    {
        return modContainer;
    }
}
