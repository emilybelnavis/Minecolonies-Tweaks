package dev.minecoloniestweaks.client.screen;

import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.AbstractTextElement;
import com.minecolonies.api.colony.buildings.modules.IBuildingModuleView;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.core.client.gui.AbstractBuildingWindow;
import com.minecolonies.core.client.gui.AbstractBuildingMainWindow;
import com.minecolonies.core.client.gui.AbstractModuleWindow;
import com.minecolonies.core.client.gui.citizen.MainWindowCitizen;
import com.minecolonies.core.client.gui.townhall.AbstractWindowTownHall;
import com.minecolonies.core.colony.buildings.moduleviews.BuildingResourcesModuleView;
import com.minecolonies.core.colony.buildings.moduleviews.WorkerBuildingModuleView;
import dev.minecoloniestweaks.client.locate.LocateBeams;
import dev.minecoloniestweaks.mixin.client.ModuleWindowAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Native building presentation; upstream windows remain controllers, never render surfaces. */
public final class BuildingScreen extends Screen {
    private final AbstractBuildingWindow<?> source;
    private final IBuildingView building;
    private final NativeBuildingControls controls = new NativeBuildingControls();
    private final List<Tab> tabs = new ArrayList<>();
    private final List<Action> actions = new ArrayList<>();
    private boolean opened;
    private int left;
    private int top;
    private int frameWidth;
    private int frameHeight;
    private int tabOffset;
    private int scroll;
    private int contentHeight;
    private boolean workersOnly;
    private static final Set<String> DASHBOARD_CONTROLS = Set.of("workers", "name", "hire", "recall", "build", "inventory",
            "allinventory", "editName", "info", "prioValue", "deliveryPrioDown", "deliveryPrioUp", "forcePickup");
    private record Tab(Component title, ItemStack icon, Runnable click, boolean selected) {}
    private record Action(int x, int y, int width, int height, Component tooltip, Runnable click, boolean content) {
        Action(int x, int y, int width, int height, Component tooltip, Runnable click) {
            this(x, y, width, height, tooltip, click, false);
        }
        boolean contains(double mx, double my) { return mx >= x && mx < x + width && my >= y && my < y + height; }
    }

    public BuildingScreen(AbstractBuildingWindow<?> source, IBuildingView building) {
        super(Component.translatable(building.getBuildingDisplayName()));
        this.source = source;
        this.building = building;
    }

    @Override protected void init() {
        // All sizes are Minecraft GUI pixels: the game's GUI Scale is the sole magnification.
        frameWidth = Math.min(360, width - 16);
        frameHeight = Math.min(270, height - 48);
        left = (width - frameWidth) / 2;
        top = (height - frameHeight - 28) / 2 + 28;
        if (!opened) {
            opened = true;
            source.onOpened();
            if (minecraft.screen != this) return; // Upstream may open onboarding or a dialog.
        }
        rebuildTabs();
        refreshControls();
    }

    private void rebuildTabs() {
        tabs.clear();
        ItemStack hut = minecraft.level == null ? ItemStack.EMPTY
                : new ItemStack(minecraft.level.getBlockState(building.getPosition()).getBlock().asItem());
        if (hut.isEmpty()) hut = new ItemStack(Items.CRAFTING_TABLE);
        if (source instanceof AbstractWindowTownHall) {
            String[] ids = {"actions", "infopage", "permissions", "citizens", "happiness", "alliances", "settings"};
            String[] types = {"WindowMainPage", "WindowInfoPage", "WindowPermissionsPage", "WindowCitizenPage", "WindowStatsPage", "WindowAlliancePage", "WindowSettings"};
            Item[] icons = {Items.BELL, Items.PAPER, Items.IRON_DOOR, Items.PLAYER_HEAD, Items.CAKE, Items.WHITE_BANNER, Items.COMPARATOR};
            String[] labels = {"actions", "information", "permissions", "citizens", "stats", "alliances", "settings"};
            for (int i = 0; i < ids.length; i++) {
                String id = ids[i];
                tabs.add(new Tab(Component.translatable("com.minecolonies.coremod.gui.townhalltab." + labels[i]),
                        new ItemStack(icons[i]), () -> clickUpstream(id), source.getClass().getSimpleName().equals(types[i])));
            }
        } else {
            tabs.add(new Tab(text("overview"), hut, () -> building.getWindow().open(), dashboard() && !workersOnly));
            if (!building.getModuleViews(WorkerBuildingModuleView.class).isEmpty()) {
                tabs.add(new Tab(text("workers"), new ItemStack(Items.IRON_PICKAXE), () -> {
                    if (!dashboard()) {
                        var main = building.getWindow();
                        if (main instanceof AbstractBuildingWindow<?> window && ScreenTheme.enabled(window)) {
                            var workers = new BuildingScreen(window, building);
                            workers.workersOnly = true;
                            minecraft.setScreen(workers);
                        } else main.open();
                    }
                    else { workersOnly = !workersOnly; scroll = 0; rebuildTabs(); }
                }, workersOnly));
            }
            IBuildingModuleView current = source instanceof AbstractModuleWindow<?> ? ((ModuleWindowAccess) source).tweaks$module() : null;
            for (var module : building.getAllModuleViews()) if (module.isPageVisible()) {
                Component title = module.getDesc() == null ? text("details") : module.getDesc();
                tabs.add(new Tab(title, new ItemStack(moduleIcon(module)), () -> module.getWindow().open(), module == current));
            }
        }
        int selected = 0;
        for (int i = 0; i < tabs.size(); i++) if (tabs.get(i).selected()) selected = i;
        int capacity = tabCapacity();
        if (selected < tabOffset || selected >= tabOffset + capacity) tabOffset = selected / capacity * capacity;
    }

    private static Item moduleIcon(IBuildingModuleView module) {
        String icon = module.getIconResourceLocation().getPath().toLowerCase(Locale.ROOT);
        if (icon.contains("recipe") || icon.contains("craft")) return Items.CRAFTING_TABLE;
        if (icon.contains("stock") || icon.contains("item")) return Items.ITEM_FRAME;
        if (icon.contains("stat")) return Items.CLOCK;
        if (icon.contains("setting")) return Items.COMPARATOR;
        if (icon.contains("task") || icon.contains("request")) return Items.WRITABLE_BOOK;
        if (icon.contains("resource")) return Items.PAPER;
        if (icon.contains("worker")) return Items.IRON_PICKAXE;
        return Items.BOOK;
    }

    private int tabCapacity() { return Math.max(1, (frameWidth - 42) / 28); }
    private boolean dashboard() { return source instanceof AbstractBuildingMainWindow<?> && !(source instanceof AbstractWindowTownHall); }
    private void refreshControls() { controls.refresh(source, dashboard() ? DASHBOARD_CONTROLS : Set.of()); }

    @Override public void tick() {
        source.onUpdate();
        refreshControls();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        actions.clear();
        int count = Math.min(tabCapacity(), tabs.size() - tabOffset);
        for (int i = 0; i < count; i++) if (!tabs.get(i + tabOffset).selected()) drawTab(graphics, i, tabs.get(i + tabOffset));
        ScreenTheme.panel(graphics, left, top, frameWidth, frameHeight);
        for (int i = 0; i < count; i++) if (tabs.get(i + tabOffset).selected()) drawTab(graphics, i, tabs.get(i + tabOffset));
        if (tabs.size() > tabCapacity()) {
            smallButton(graphics, left + frameWidth - 40, top - 23, 18, Component.literal("‹"), () -> tabOffset = Math.max(0, tabOffset - tabCapacity()));
            smallButton(graphics, left + frameWidth - 20, top - 23, 18, Component.literal("›"), () -> tabOffset = Math.min(((tabs.size() - 1) / tabCapacity()) * tabCapacity(), tabOffset + tabCapacity()));
        }
        drawText(graphics, title, left + 10, top + 10, frameWidth - 47, 0x404040);
        drawText(graphics, text("level", building.getBuildingLevel(), building.getBuildingMaxLevel()), left + 10, top + 25, frameWidth - 47, 0x555555);
        smallButton(graphics, left + frameWidth - 32, top + 8, 22, Component.literal("⌖"), () -> {
            LocateBeams.block(title.getString(), building.getPosition(), false); minecraft.setScreen(null);
        });
        graphics.renderItem(new ItemStack(Items.COMPASS), left + frameWidth - 29, top + 10);
        int areaTop = top + 46;
        int areaBottom = top + frameHeight - 8;
        int availableHeight = areaBottom - areaTop;
        contentHeight = dashboard() ? dashboardHeight() : controls.height(frameWidth - 25);
        scroll = Math.clamp(scroll, 0, Math.max(0, contentHeight - availableHeight));
        graphics.enableScissor(left + 6, areaTop, left + frameWidth - 6, areaBottom);
        if (dashboard()) {
            drawDashboard(graphics, areaTop - scroll);
            if (!workersOnly) controls.render(graphics, left + 10, areaTop - scroll + dashboardBaseHeight() + 8,
                    frameWidth - 25, areaTop, areaBottom, mouseX, mouseY);
        }
        else controls.render(graphics, left + 10, areaTop - scroll, frameWidth - 25, areaTop, areaBottom, mouseX, mouseY);
        graphics.disableScissor();
        if (contentHeight > availableHeight) {
            int trackX = left + frameWidth - 10;
            graphics.fill(trackX, areaTop, trackX + 3, areaBottom, 0xFF777777);
            int thumb = Math.max(12, availableHeight * availableHeight / contentHeight);
            int thumbY = areaTop + scroll * (availableHeight - thumb) / Math.max(1, contentHeight - availableHeight);
            graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumb, 0xFFDDDDDD);
        }
        if (controls.hasChoices()) {
            graphics.pose().pushPose(); graphics.pose().translate(0, 0, 300);
            controls.renderChoices(graphics, left + 20, areaTop, frameWidth - 40, availableHeight, mouseX, mouseY);
            graphics.pose().popPose();
        } else {
            if (!workersOnly) controls.tooltip(graphics, mouseX, mouseY, areaTop, areaBottom);
            for (var action : actions) if (action.contains(mouseX, mouseY)
                    && (!action.content() || mouseY >= areaTop && mouseY < areaBottom)) {
                graphics.renderTooltip(font, action.tooltip(), mouseX, mouseY); break;
            }
        }
    }

    private void drawTab(GuiGraphics graphics, int column, Tab tab) {
        int x = left + column * 28;
        String sprite = "container/creative_inventory/tab_top_" + (tab.selected() ? "selected_" : "unselected_") + (column == 0 ? "1" : "2");
        graphics.blitSprite(ResourceLocation.withDefaultNamespace(sprite), x, top - 28, 26, 32);
        graphics.renderItem(tab.icon(), x + 5, top - 19);
        actions.add(new Action(x, top - 28, 26, 28, tab.title(), tab.click()));
    }

    private int workerCount() { return building.getModuleViews(WorkerBuildingModuleView.class).stream().mapToInt(module -> module.getAssignedCitizens().size()).sum(); }
    private int dashboardBaseHeight() { return Math.max(62, 18 + Math.max(1, workerCount()) * 36 + 8) + 152; }
    private int dashboardHeight() { return workersOnly ? Math.max(1, workerCount()) * 36 + 75 : dashboardBaseHeight() + (controls.height() > 0 ? 8 + controls.height() : 0); }

    private void drawDashboard(GuiGraphics graphics, int y) {
        int x = left + 10;
        int columnWidth = (frameWidth - 30) / 2;
        int right = x + columnWidth + 10;
        int capacity = building.getModuleViews(WorkerBuildingModuleView.class).stream().mapToInt(WorkerBuildingModuleView::getMaxInhabitants).sum();
        drawText(graphics, text("workers_count", workerCount(), capacity), x, y + 2, workersOnly ? frameWidth - 24 : columnWidth, 0x404040);
        int workerY = y + 18;
        for (var module : building.getModuleViews(WorkerBuildingModuleView.class)) for (int id : module.getAssignedCitizens()) {
            var citizen = building.getColony().getCitizen(id);
            if (citizen == null) continue;
            int cardWidth = workersOnly ? frameWidth - 26 : columnWidth;
            inset(graphics, x, workerY, cardWidth, 32);
            drawText(graphics, Component.literal(citizen.getName()), x + 5, workerY + 5, cardWidth - 10, 0xFFFFFF);
            drawText(graphics, citizen.getJobComponent(), x + 5, workerY + 18, cardWidth - 10, 0xAAAAAA);
            actions.add(new Action(x, workerY, cardWidth, 32, Component.literal(citizen.getName()), () -> new MainWindowCitizen(citizen).open(), true));
            workerY += 36;
        }
        if (workerCount() == 0) {
            inset(graphics, x, workerY, workersOnly ? frameWidth - 26 : columnWidth, 28);
            drawText(graphics, text("no_workers"), x + 5, workerY + 10, columnWidth - 10, 0xAAAAAA);
            workerY += 32;
        }
        if (workersOnly) {
            upstreamButton(graphics, "hire", Items.WRITABLE_BOOK, x, workerY + 8, frameWidth - 26, text("manage_workers"));
            upstreamButton(graphics, "recall", Items.BELL, x, workerY + 34, frameWidth - 26, text("recall"));
            return;
        }
        int constructionY = Math.max(y + 62, workerY + 8);
        drawText(graphics, text("construction"), x, constructionY, columnWidth, 0x404040);
        inset(graphics, x, constructionY + 15, columnWidth, 82);
        var resources = building.getModuleViewByType(BuildingResourcesModuleView.class);
        if (resources != null && resources.getWorkOrderId() > 0) {
            var work = building.getColony().getWorkOrder(resources.getWorkOrderId());
            drawText(graphics, work == null ? text("construction") : work.getDisplayName(), x + 5, constructionY + 22, columnWidth - 10, 0xFFFFFF);
            drawText(graphics, text("stage", resources.getCurrentStage(), resources.getTotalStages()), x + 5, constructionY + 35, columnWidth - 10, 0xAAAAAA);
            int total = resources.getResources().values().stream().mapToInt(resource -> resource.getAmount()).sum();
            int available = resources.getResources().values().stream().mapToInt(resource -> Math.min(resource.getAmount(), resource.getAvailable())).sum();
            int supplied = total == 0 ? 100 : (int) (100.0 * available / total);
            drawText(graphics, text("supplied", supplied), x + 5, constructionY + 48, columnWidth - 10, 0xFFFFFF);
            bar(graphics, x + 5, constructionY + 60, (columnWidth - 15) / 2, supplied);
            bar(graphics, x + 10 + (columnWidth - 15) / 2, constructionY + 60, (columnWidth - 15) / 2, resources.getProgress());
            long missing = resources.getResources().values().stream().filter(resource -> resource.getAvailable() < resource.getAmount()).count();
            drawText(graphics, text("missing", missing), x + 5, constructionY + 73, columnWidth - 10, 0xFFAA00);
        } else {
            Component status = building.isBuilding() ? text("building") : building.isRepairing() ? text("repairing") : building.isDeconstructing() ? text("removing") : text("no_construction");
            drawText(graphics, status, x + 5, constructionY + 26, columnWidth - 10, 0xFFFFFF);
        }
        upstreamButton(graphics, "hire", Items.WRITABLE_BOOK, x, constructionY + 104, columnWidth, text("manage_workers"));
        upstreamButton(graphics, "recall", Items.BELL, x, constructionY + 130, columnWidth, text("recall"));
        drawText(graphics, text("building"), right, y + 2, columnWidth, 0x404040);
        upstreamButton(graphics, "build", Items.CRAFTING_TABLE, right, y + 18, columnWidth, text("build_options"));
        upstreamButton(graphics, "inventory", Items.CHEST, right, y + 44, columnWidth, Component.translatable("container.inventory"));
        upstreamButton(graphics, "allinventory", Items.BARREL, right, y + 70, columnWidth, text("storage"));
        upstreamButton(graphics, "editName", Items.NAME_TAG, right, y + 96, columnWidth / 2 - 2, text("rename"));
        upstreamButton(graphics, "info", Items.BOOK, right + columnWidth / 2 + 2, y + 96, columnWidth / 2 - 2, text("info"));
        Pane priority = source.findPaneByID("prioValue");
        if (priority instanceof AbstractTextElement label) drawText(graphics, label.getText(), right, y + 127, columnWidth - 46, 0x404040);
        upstreamButton(graphics, "deliveryPrioDown", null, right + columnWidth - 44, y + 122, 20, Component.literal("−"));
        upstreamButton(graphics, "deliveryPrioUp", null, right + columnWidth - 20, y + 122, 20, Component.literal("+"));
        upstreamButton(graphics, "forcePickup", Items.HOPPER, right, y + 148, columnWidth, text("pickup"));
    }

    private void upstreamButton(GuiGraphics graphics, String id, Item icon, int x, int y, int width, Component fallback) {
        Pane pane = source.findPaneByID(id);
        if (!(pane instanceof Button button) || !button.isVisible()) return;
        Component label = button.isTextEmpty() ? fallback : button.getText();
        boolean enabled = NativeBuildingControls.enabled(button);
        graphics.blitSprite(ResourceLocation.withDefaultNamespace(enabled ? "widget/button" : "widget/button_disabled"), x, y, width, 22);
        if (icon != null && width > 90) graphics.renderItem(new ItemStack(icon), x + 4, y + 3);
        int iconWidth = icon != null && width > 90 ? 22 : 0;
        String shown = font.plainSubstrByWidth(label.getString(), width - iconWidth - 6);
        graphics.drawCenteredString(font, shown, x + iconWidth + (width - iconWidth) / 2, y + 7, enabled ? 0xFFFFFF : 0xA0A0A0);
        actions.add(new Action(x, y, width, 22, label, () -> { if (NativeBuildingControls.enabled(button)) button.handleClick(0, 0); }, true));
    }

    private void clickUpstream(String id) {
        Pane pane = source.findPaneByID(id);
        if (pane instanceof Button button && button.isEnabled()) button.handleClick(0, 0);
    }

    private void smallButton(GuiGraphics graphics, int x, int y, int width, Component label, Runnable click) {
        graphics.blitSprite(ResourceLocation.withDefaultNamespace("widget/button"), x, y, width, 20);
        if (!label.getString().equals("⌖")) graphics.drawCenteredString(font, label, x + width / 2, y + 6, 0xFFFFFF);
        actions.add(new Action(x, y, width, 20, label.getString().equals("⌖") ? Component.translatable("button.minecoloniestweaks.locate") : label, click));
    }

    private void drawText(GuiGraphics graphics, Component value, int x, int y, int width, int color) {
        graphics.drawString(font, font.plainSubstrByWidth(value.getString(), Math.max(0, width)), x, y, color, false);
    }
    private static void inset(GuiGraphics graphics, int x, int y, int width, int height) {
        ScreenTheme.roundedFill(graphics, x, y, width, height, 3, 0xFF777777);
        ScreenTheme.roundedFill(graphics, x + 1, y + 1, width - 1, height - 1, 3, 0xFFFFFFFF);
        ScreenTheme.roundedFill(graphics, x + 1, y + 1, width - 2, height - 2, 2, 0xFF404040);
    }
    private static void bar(GuiGraphics graphics, int x, int y, int width, int percent) {
        graphics.fill(x, y, x + width, y + 5, 0xFF233525);
        graphics.fill(x + 1, y + 1, x + 1 + (width - 2) * Math.clamp(percent, 0, 100) / 100, y + 4, 0xFF80B445);
    }
    private static Component text(String key, Object... args) { return Component.translatable("building.minecoloniestweaks." + key, args); }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button != 0) return super.mouseClicked(x, y, button);
        if (controls.clickChoices(x, y, left + 20, top + 46, frameWidth - 40, frameHeight - 54)) return true;
        int areaTop = top + 46;
        int areaBottom = top + frameHeight - 8;
        for (var action : List.copyOf(actions)) if (action.contains(x, y)
                && (!action.content() || y >= areaTop && y < areaBottom)) {
            action.click().run(); return true;
        }
        return !workersOnly && y >= areaTop && y < areaBottom && controls.click(x, y) || super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (controls.hasChoices()) controls.scrollChoices(vertical);
        else scroll = Math.clamp(scroll - (int) (vertical * 22), 0, Math.max(0, contentHeight - (frameHeight - 54)));
        return true;
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 256 && controls.closeChoices()) return true;
        return controls.key(key, scan, modifiers) || super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean charTyped(char character, int modifiers) { return controls.character(character, modifiers) || super.charTyped(character, modifiers); }
    @Override public void removed() {
        if (opened) { opened = false; source.onClosed(); }
        Pane.clearFocus();
    }
    @Override public boolean isPauseScreen() { return false; }
}
