package dev.minecoloniestweaks.client.screen;

import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.tileentities.AbstractTileEntityGrave;
import com.minecolonies.api.tileentities.AbstractTileEntityNamedGrave;
import dev.minecoloniestweaks.client.ClientPreferences;
import dev.minecoloniestweaks.client.locate.LocateBeams;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    private final String menu;
    private int page;

    public SettingsScreen(Screen parent, String menu) {
        super(Component.translatable("settings.minecoloniestweaks." + menu));
        this.parent = parent;
        this.menu = menu;
    }

    private record Option(Component label, Runnable action) {}

    @Override protected void init() {
        var settings = ClientPreferences.INSTANCE;
        List<Option> options = new ArrayList<>();
        switch (menu) {
            case "root" -> {
                for (String child : List.of("screens", "hud", "beams")) {
                    options.add(new Option(Component.translatable("settings.minecoloniestweaks." + child),
                            () -> minecraft.setScreen(new SettingsScreen(this, child))));
                }
            }
            case "screens" -> {
                for (String key : ScreenTheme.SCREENS) {
                    options.add(new Option(Component.literal(settings.disabledScreens.contains(key) ? "[OFF] " : "[ON] ").append(ScreenTheme.label(key)), () -> {
                        if (!settings.disabledScreens.remove(key)) settings.disabledScreens.add(key);
                    }));
                }
            }
            case "hud" -> {
                options.add(toggle("visible", settings.hudVisible, () -> settings.hudVisible = !settings.hudVisible));
                for (String child : List.of("position", "content")) {
                    options.add(new Option(Component.translatable("settings.minecoloniestweaks." + child),
                            () -> minecraft.setScreen(new SettingsScreen(this, child))));
                }
            }
            case "position" -> {
                options.add(new Option(Component.translatable("screen.minecoloniestweaks.arrange"), () -> minecraft.setScreen(new HudLayoutScreen())));
                options.add(new Option(Component.translatable("settings.minecoloniestweaks.width", settings.panelWidth),
                        () -> settings.panelWidth = settings.panelWidth >= 360 ? 160 : settings.panelWidth + 20));
                options.add(new Option(Component.translatable("settings.minecoloniestweaks.scale", settings.hudScale),
                        () -> settings.hudScale = settings.hudScale >= 2 ? 0.5 : settings.hudScale + 0.25));
            }
            case "content" -> {
                options.add(toggle("satisfied", settings.showSatisfied, () -> settings.showSatisfied = !settings.showSatisfied));
                options.add(toggle("inventory", settings.showInventory, () -> settings.showInventory = !settings.showInventory));
                options.add(new Option(Component.translatable("settings.minecoloniestweaks.rows", settings.maxRows),
                        () -> settings.maxRows = settings.maxRows >= 30 ? 3 : settings.maxRows + 1));
            }
            case "beams" -> {
                options.add(new Option(Component.translatable("settings.minecoloniestweaks.locate_aimed"), SettingsScreen::locateAimed));
                options.add(toggle("visible", settings.beamsVisible, () -> settings.beamsVisible = !settings.beamsVisible));
                options.add(new Option(Component.translatable("settings.minecoloniestweaks.duration", settings.beamSeconds),
                        () -> settings.beamSeconds = settings.beamSeconds >= 600 ? 30 : settings.beamSeconds + 30));
                options.add(new Option(Component.translatable("settings.minecoloniestweaks.clear_beams"), LocateBeams::clear));
                options.add(new Option(Component.translatable("settings.minecoloniestweaks.locate"), () -> minecraft.setScreen(new SettingsScreen(this, "locate"))));
            }
            case "locate" -> {
                if (minecraft.level != null) {
                    for (var colony : IColonyManager.getInstance().getColonyViews(minecraft.level)) {
                        for (var citizen : colony.getCitizens().values()) {
                            options.add(new Option(Component.literal(citizen.getName()), () -> LocateBeams.citizen(citizen)));
                        }
                        for (var hut : colony.getClientBuildingManager().getBuildings().values()) {
                            options.add(new Option(Component.literal(hut.getBuildingDisplayName() + " " + hut.getPosition().toShortString()),
                                    () -> LocateBeams.block(hut.getBuildingDisplayName(), hut.getPosition(), false)));
                        }
                    }
                }
            }
            default -> throw new IllegalArgumentException("Unknown settings menu " + menu);
        }
        int perPage = Math.max(1, (height - 100) / 24);
        int pages = Math.max(1, (options.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        int buttonWidth = Math.min(460, width - 32);
        for (int i = page * perPage; i < Math.min(options.size(), (page + 1) * perPage); i++) {
            var option = options.get(i);
            var label = option.label();
            var button = Button.builder(label, clicked -> {
                option.action().run();
                settings.save();
                if (minecraft.screen == this) rebuildWidgets();
            }).bounds((width - buttonWidth) / 2, 48 + (i % perPage) * 24, buttonWidth, 20).build();
            button.setTooltip(Tooltip.create(label));
            addRenderableWidget(button);
        }
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { page = Math.floorMod(page - 1, pages); rebuildWidgets(); })
                .bounds(width / 2 - 140, height - 30, 35, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(width / 2 - 100, height - 30, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { page = (page + 1) % pages; rebuildWidgets(); })
                .bounds(width / 2 + 105, height - 30, 35, 20).build());
    }

    private static void locateAimed() {
        var client = Minecraft.getInstance();
        if (client.level == null) return;
        if (client.hitResult instanceof EntityHitResult hit
                && hit.getEntity() instanceof AbstractEntityCitizen citizen) {
            var data = citizen.getCitizenDataView();
            if (data != null) LocateBeams.citizen(data);
        } else if (client.hitResult instanceof BlockHitResult hit) {
            var position = hit.getBlockPos();
            var entity = client.level.getBlockEntity(position);
            boolean grave = entity instanceof AbstractTileEntityGrave
                    || entity instanceof AbstractTileEntityNamedGrave;
            var building = IColonyManager.getInstance().getBuildingView(client.level.dimension(), position);
            if (grave || building != null) {
                LocateBeams.block(grave ? Component.translatable("beam.minecoloniestweaks.grave").getString() : building.getBuildingDisplayName(), position, grave);
            }
        }
    }

    private static Option toggle(String name, boolean value, Runnable action) {
        return new Option(Component.translatable("settings.minecoloniestweaks." + name)
                .append(value ? ": ON" : ": OFF"), action);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFF);
        graphics.drawCenteredString(font, Integer.toString(page + 1), width / 2, 32, 0xAAAAAA);
    }

    @Override public void onClose() { ClientPreferences.INSTANCE.save(); minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
