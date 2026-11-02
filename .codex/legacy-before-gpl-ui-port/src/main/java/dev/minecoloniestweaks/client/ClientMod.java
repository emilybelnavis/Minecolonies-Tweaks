package dev.minecoloniestweaks.client;

import com.ldtteam.blockui.BOScreen;
import com.ldtteam.domumornamentum.client.screens.ArchitectsCutterScreen;
import com.minecolonies.core.client.gui.AbstractBuildingWindow;
import com.minecolonies.core.client.gui.WindowResearchTree;
import com.minecolonies.core.client.gui.citizen.AbstractWindowCitizen;
import com.minecolonies.core.client.gui.containers.WindowGrave;
import com.minecolonies.core.client.gui.containers.WindowCitizenInventory;
import com.minecolonies.core.client.gui.citizen.MainWindowCitizen;
import com.minecolonies.api.colony.ICitizenDataView;
import dev.minecoloniestweaks.client.screen.CitizenInventoryScreen;
import com.mojang.logging.LogUtils;
import dev.minecoloniestweaks.MineColoniesTweaks;
import dev.minecoloniestweaks.client.hud.ResourceHud;
import dev.minecoloniestweaks.client.locate.LocateBeams;
import dev.minecoloniestweaks.client.screen.CutterScreen;
import dev.minecoloniestweaks.client.screen.BuildingScreen;
import dev.minecoloniestweaks.client.screen.CitizenScreen;
import dev.minecoloniestweaks.client.screen.HudLayoutScreen;
import dev.minecoloniestweaks.client.screen.ResearchScreen;
import dev.minecoloniestweaks.client.screen.ScreenTheme;
import dev.minecoloniestweaks.client.screen.SettingsScreen;
import dev.minecoloniestweaks.mixin.client.BuildingWindowAccess;
import dev.minecoloniestweaks.mixin.client.CitizenWindowAccess;
import dev.minecoloniestweaks.mixin.client.ResearchWindowAccess;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = MineColoniesTweaks.ID, dist = Dist.CLIENT)
public final class ClientMod {
    private static final String CATEGORY = "key.categories.minecoloniestweaks";
    private static final KeyMapping PIN = new KeyMapping("key.minecoloniestweaks.pin", GLFW.GLFW_KEY_J, CATEGORY);
    private static final KeyMapping ARRANGE = new KeyMapping("key.minecoloniestweaks.arrange", GLFW.GLFW_KEY_P, CATEGORY);
    private static final KeyMapping TOGGLE = new KeyMapping("key.minecoloniestweaks.toggle", GLFW.GLFW_KEY_H, CATEGORY);
    private static final KeyMapping SETTINGS = new KeyMapping("key.minecoloniestweaks.settings", GLFW.GLFW_KEY_K, CATEGORY);

    public ClientMod(IEventBus bus, ModContainer container) {
        bus.addListener(ClientMod::registerKeys);
        bus.addListener(ClientMod::registerLayers);
        NeoForge.EVENT_BUS.addListener(ClientMod::tick);
        NeoForge.EVENT_BUS.addListener(ClientMod::disconnect);
        NeoForge.EVENT_BUS.addListener(ClientMod::initScreen);
        NeoForge.EVENT_BUS.addListener(ClientMod::openScreen);
        NeoForge.EVENT_BUS.addListener(ClientMod::renderToolbar);
        NeoForge.EVENT_BUS.addListener(ClientMod::clickToolbar);
        NeoForge.EVENT_BUS.addListener(LocateBeams::renderWorld);
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new SettingsScreen(parent, "root"));
        try (var stream = ClientMod.class.getResourceAsStream("/assets/minecoloniestweaks/screens.txt")) {
            if (stream != null) {
                try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    reader.lines().filter(line -> !line.isBlank()).forEach(ScreenTheme.SCREENS::add);
                }
            }
        } catch (IOException exception) {
            LogUtils.getLogger().warn("Cannot read screen catalog", exception);
        }
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        for (var key : new KeyMapping[]{PIN, ARRANGE, TOGGLE, SETTINGS}) event.register(key);
    }

    private static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(MineColoniesTweaks.ID, "resources"),
                (graphics, deltaTracker) -> { ResourceHud.render(graphics, false); LocateBeams.renderLabels(graphics); });
    }

    private static void tick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        ResourceHud.tick();
        if (client.screen != null) return;
        while (PIN.consumeClick()) ResourceHud.toggleHeldScroll();
        while (TOGGLE.consumeClick()) {
            ClientPreferences.INSTANCE.hudVisible = !ClientPreferences.INSTANCE.hudVisible;
            ClientPreferences.INSTANCE.save();
        }
        if (ARRANGE.consumeClick()) client.setScreen(new HudLayoutScreen());
        if (SETTINGS.consumeClick()) client.setScreen(new SettingsScreen(null, "root"));
    }

    private static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        TOOLBARS.clear();
        ResourceHud.clear();
        LocateBeams.clear();
    }

    private static final Map<Screen, List<Button>> TOOLBARS = new WeakHashMap<>();

    private static void openScreen(ScreenEvent.Opening event) {
        var screen = event.getNewScreen();
        if (screen instanceof BOScreen citizenScreen && citizenScreen.getWindow() instanceof AbstractWindowCitizen citizenWindow
                && ScreenTheme.enabled(citizenWindow)
                && !citizenWindow.getXmlResourceLocation().getPath().endsWith("debug.xml")) {
            var citizen = ((CitizenWindowAccess) citizenWindow).tweaks$citizen();
            event.setNewScreen(new CitizenScreen(citizenWindow, citizen));
        } else if (screen instanceof BOScreen blockUi && blockUi.getWindow() instanceof WindowResearchTree
                && ScreenTheme.enabled(blockUi.getWindow())) {
            var access = (ResearchWindowAccess) blockUi.getWindow();
            event.setNewScreen(new ResearchScreen(access.tweaks$building(), access.tweaks$branch(), access.tweaks$parent()));
        } else if (screen instanceof BOScreen buildingScreen
                && buildingScreen.getWindow() instanceof AbstractBuildingWindow<?> window && ScreenTheme.enabled(window)) {
            event.setNewScreen(new BuildingScreen(window, ((BuildingWindowAccess) window).tweaks$building()));
        } else if (screen instanceof WindowCitizenInventory inventory
                && !(screen instanceof CitizenInventoryScreen)
                && inventory.getCitizenData() instanceof ICitizenDataView citizen
                && ScreenTheme.enabled(new MainWindowCitizen(citizen))) {
            event.setNewScreen(new CitizenInventoryScreen(inventory.getMenu(),
                    Minecraft.getInstance().player.getInventory(), inventory.getTitle(), citizen));
        } else if (screen instanceof ArchitectsCutterScreen cutter
                && !ClientPreferences.INSTANCE.disabledScreens.contains("domum_ornamentum:architects_cutter")) {
            event.setNewScreen(new CutterScreen(cutter.getMenu(), Minecraft.getInstance().player.getInventory(), cutter.getTitle()));
        }
    }

    private static void renderToolbar(ScreenEvent.Render.Post event) {
        var toolbar = TOOLBARS.get(event.getScreen());
        if (toolbar != null) for (var button : toolbar) button.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
    }

    private static void clickToolbar(ScreenEvent.MouseButtonPressed.Pre event) {
        var toolbar = TOOLBARS.get(event.getScreen());
        if (toolbar != null) {
            for (var button : toolbar) {
                if (button.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

    private static void initScreen(ScreenEvent.Init.Post event) {
        var screen = event.getScreen();
        TOOLBARS.clear();
        var toolbar = new ArrayList<Button>();
        TOOLBARS.put(screen, toolbar);
        if (screen instanceof BOScreen blockUi) {
            var window = blockUi.getWindow();
            if (!ScreenTheme.enabled(window)) return;
            if (window instanceof AbstractBuildingWindow<?>) {
                var building = ((BuildingWindowAccess) window).tweaks$building();
                toolbar.add(Button.builder(Component.translatable("button.minecoloniestweaks.locate"), b -> {
                    LocateBeams.block(building.getBuildingDisplayName(), building.getPosition(), false);
                    Minecraft.getInstance().setScreen(null);
                }).bounds(4, 4, 90, 20).build());
            }
        } else if (screen instanceof WindowGrave grave) {
            toolbar.add(Button.builder(Component.translatable("button.minecoloniestweaks.locate"), b -> {
                LocateBeams.block(grave.getTitle().getString(), grave.getMenu().grave.getBlockPos(), true);
                Minecraft.getInstance().setScreen(null);
            }).bounds(4, 4, 90, 20).build());
        }
    }
}
