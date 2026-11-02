package dev.minecoloniestweaks.client.hud;

import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.items.component.BuildingId;
import com.minecolonies.core.client.gui.WindowResourceList;
import com.minecolonies.core.colony.buildings.moduleviews.BuildingResourcesModuleView;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import com.minecolonies.core.items.ItemResourceScroll;
import com.minecolonies.core.network.messages.server.colony.building.MarkBuildingDirtyMessage;
import com.mojang.logging.LogUtils;
import dev.minecoloniestweaks.client.ClientPreferences;
import dev.minecoloniestweaks.client.ClientPreferences.PinnedBuilder;
import dev.minecoloniestweaks.mixin.client.ResourceScrollAccess;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ResourceHud {
    private static final Map<PinnedBuilder, TrackedScroll> TRACKED = new HashMap<>();
    private static final Map<PinnedBuilder, Panel> PANELS = new HashMap<>();
    private static int ticks;

    private ResourceHud() {}

    public record Row(String name, int required, int available, int carried, int delivery, int missing) {}
    public record Panel(String title, List<Row> rows, String status) {}
    private record TrackedScroll(BuildingBuilder.View builder, WindowResourceList window) {}

    public static Panel panel(PinnedBuilder pin) {
        return PANELS.getOrDefault(pin, new Panel(text("builder"), List.of(), text("waiting")));
    }

    public static void clear() {
        TRACKED.clear();
        PANELS.clear();
        ticks = 0;
    }

    public static void toggleHeldScroll() {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        ItemStack scroll = client.player.getMainHandItem();
        if (!(scroll.getItem() instanceof ItemResourceScroll)) scroll = client.player.getOffhandItem();
        if (!(scroll.getItem() instanceof ItemResourceScroll)) {
            client.player.displayClientMessage(Component.translatable("message.minecoloniestweaks.hold_scroll"), true);
            return;
        }
        if (!(BuildingId.readBuildingViewFromItemStack(scroll) instanceof BuildingBuilder.View builder)) {
            client.player.displayClientMessage(Component.translatable("message.minecoloniestweaks.unbound_scroll"), true);
            return;
        }
        var preferences = ClientPreferences.INSTANCE;
        var pins = preferences.pins();
        String dimension = client.level.dimension().location().toString();
        boolean removed = pins.removeIf(pin -> pin.colony == builder.getColony().getID()
                && dimension.equals(pin.dimension) && pin.position == builder.getID().asLong());
        if (!removed && pins.size() < 16) {
            var pin = new PinnedBuilder(dimension, builder.getColony().getID(), builder.getID().asLong());
            pin.y = Math.min(0.85, 0.06 + pins.size() * 0.15);
            pins.add(pin);
        }
        preferences.save();
        refresh();
    }

    public static void tick() {
        if (++ticks % 20 == 0) refresh();
    }

    private static void refresh() {
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        var pins = ClientPreferences.INSTANCE.pins();
        TRACKED.keySet().retainAll(pins);
        PANELS.keySet().retainAll(pins);
        for (var pin : pins) {
            if (!client.level.dimension().location().toString().equals(pin.dimension)) continue;
            var colony = IColonyManager.getInstance().getColonyView(pin.colony, client.level.dimension());
            var building = colony == null ? null : colony.getClientBuildingManager().getBuilding(BlockPos.of(pin.position));
            if (!(building instanceof BuildingBuilder.View builder)
                    || builder.getModuleViewByType(BuildingResourcesModuleView.class) == null) {
                PANELS.put(pin, new Panel(text("builder"), List.of(), text("waiting")));
                continue;
            }
            try {
                var tracked = TRACKED.get(pin);
                if (tracked == null || tracked.builder() != builder) {
                    tracked = new TrackedScroll(builder, new WindowResourceList(builder, Map.of()));
                    TRACKED.put(pin, tracked);
                }
                // The upstream scroll calculates carried items and deliveries. Use its result directly.
                var access = (ResourceScrollAccess) tracked.window();
                access.tweaks$refresh();
                List<Row> rows = new ArrayList<>();
                for (var resource : access.tweaks$resources()) {
                    rows.add(new Row(resource.getName(), resource.getAmount(), resource.getAvailable(),
                            resource.getPlayerAmount(), resource.getAmountInDelivery(), resource.getMissingFromPlayer()));
                }
                rows.sort(Comparator.comparing(Row::name));
                PANELS.put(pin, new Panel(builder.getBuildingDisplayName(), List.copyOf(rows), ""));
                if (ticks % 100 == 0) new MarkBuildingDirtyMessage(builder).sendToServer();
            } catch (RuntimeException exception) {
                if (ticks % 200 == 0) LogUtils.getLogger().warn("Cannot refresh pinned builder {}", pin.position, exception);
                PANELS.put(pin, new Panel(builder.getBuildingDisplayName(), List.of(), text("unavailable")));
            }
        }
    }

    private static String text(String key, Object... arguments) {
        return I18n.get("hud.minecoloniestweaks." + key, arguments);
    }

    public static int panelHeight(PinnedBuilder pin) {
        var settings = ClientPreferences.INSTANCE;
        long visible = panel(pin).rows().stream().filter(row -> settings.showSatisfied || row.missing() > 0).count();
        return 34 + (int) Math.min(visible, rowLimit()) * (settings.showInventory ? 22 : 12);
    }

    private static int rowLimit() {
        var settings = ClientPreferences.INSTANCE;
        int screenHeight = (int) (Minecraft.getInstance().getWindow().getGuiScaledHeight() / settings.hudScale);
        int fittingRows = Math.max(1, (screenHeight - 34) / (settings.showInventory ? 22 : 12));
        return Math.min(settings.maxRows, fittingRows);
    }

    public static void render(GuiGraphics graphics, boolean arranging) {
        var client = Minecraft.getInstance();
        var settings = ClientPreferences.INSTANCE;
        if (client.level == null || client.player == null || (!arranging && (!settings.hudVisible || client.options.hideGui))) return;
        double scale = settings.hudScale;
        graphics.pose().pushPose();
        graphics.pose().scale((float) scale, (float) scale, 1);
        int screenWidth = (int) (graphics.guiWidth() / scale);
        int screenHeight = (int) (graphics.guiHeight() / scale);
        for (var pin : settings.pins()) {
            if (!client.level.dimension().location().toString().equals(pin.dimension)) continue;
            var panel = panel(pin);
            int width = Math.min(settings.panelWidth, screenWidth - 4);
            int height = panelHeight(pin);
            int x = Math.clamp((int) (pin.x * screenWidth), 0, Math.max(0, screenWidth - width));
            int y = Math.clamp((int) (pin.y * screenHeight), 0, Math.max(0, screenHeight - height));
            graphics.fill(x, y, x + width, y + height, arranging ? 0xEE202020 : 0xB0101010);
            graphics.drawString(client.font, client.font.plainSubstrByWidth(panel.title(), width - 12), x + 6, y + 6, 0xFFFFFF);
            int rowY = y + 21;
            var visible = panel.rows().stream().filter(row -> settings.showSatisfied || row.missing() > 0).toList();
            for (var row : visible.stream().limit(rowLimit()).toList()) {
                String count = row.missing() + "/" + row.required();
                int labelWidth = width - client.font.width(count) - 20;
                graphics.drawString(client.font, client.font.plainSubstrByWidth(row.name(), labelWidth), x + 6, rowY, 0xEEEEEE);
                graphics.drawString(client.font, count, x + width - client.font.width(count) - 6, rowY, row.missing() == 0 ? 0x88DD88 : 0xFFDD88);
                if (settings.showInventory) {
                    String detail = text("counts", row.available(), row.carried(), row.delivery());
                    graphics.drawString(client.font, client.font.plainSubstrByWidth(detail, width - 12), x + 6, rowY + 10, 0xAAAAAA);
                }
                rowY += settings.showInventory ? 22 : 12;
            }
            String status = panel.status();
            if (status.isEmpty()) {
                if (visible.isEmpty()) status = text("supplied");
                else if (visible.size() > rowLimit()) status = text("more", visible.size() - rowLimit());
                else status = text("missing");
            }
            graphics.drawString(client.font, status, x + 6, y + height - 11, 0xAAAAAA);
        }
        graphics.pose().popPose();
    }
}
