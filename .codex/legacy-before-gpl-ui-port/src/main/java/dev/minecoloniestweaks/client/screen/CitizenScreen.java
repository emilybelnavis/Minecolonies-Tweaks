package dev.minecoloniestweaks.client.screen;

import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.views.View;
import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.core.client.gui.citizen.FamilyWindowCitizen;
import com.minecolonies.core.client.gui.citizen.HappinessWindowCitizen;
import com.minecolonies.core.client.gui.citizen.JobWindowCitizen;
import com.minecolonies.core.client.gui.citizen.MainWindowCitizen;
import com.minecolonies.core.client.gui.citizen.RequestWindowCitizen;
import com.minecolonies.core.colony.buildings.moduleviews.WorkerBuildingModuleView;
import com.minecolonies.core.debug.DebugPlayerManager;
import com.minecolonies.core.debug.gui.DebugWindowCitizen;
import com.minecolonies.core.network.messages.server.colony.OpenInventoryMessage;
import dev.minecoloniestweaks.client.locate.LocateBeams;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** A compact citizen frame measured in Minecraft GUI pixels, with native pages backed by upstream controllers. */
public final class CitizenScreen extends Screen {
    private static final int FRAME_WIDTH = 260;
    private static final int FRAME_HEIGHT = 210;
    private static final int TAB_HEIGHT = 28;
    private static final int LEFT = 12;
    private static final int LEFT_WIDTH = 140;
    private static final int RIGHT = 160;
    private static final int RIGHT_WIDTH = 88;
    private static final Set<String> SIDEBAR_IDS = Set.of(
            "mainTab", "mainIcon", "requestTab", "requestIcon", "inventoryTab", "inventoryIcon",
            "happinessTab", "happinessIcon", "familyTab", "familyIcon", "jobTab", "jobIcon", "debugTab", "debugIcon");

    private enum Page { OVERVIEW, SKILLS, REQUESTS, HAPPINESS, FAMILY, JOB, DEBUG }
    private record Action(int x, int y, int width, int height, Component tooltip, Runnable run) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private ICitizenDataView citizen;
    private BOWindow source;
    private Page page;
    private boolean opened;
    private double scale;
    private double originX;
    private double originY;
    private int contentWidth;
    private int contentScroll;
    private final NativeBuildingControls controls = new NativeBuildingControls();
    private final List<Action> actions = new ArrayList<>();

    public CitizenScreen(BOWindow source, ICitizenDataView citizen) {
        super(Component.literal(citizen.getName()));
        this.source = source;
        this.citizen = citizen;
        page = source instanceof DebugWindowCitizen ? Page.DEBUG
                : source instanceof RequestWindowCitizen ? Page.REQUESTS
                : source instanceof HappinessWindowCitizen ? Page.HAPPINESS
                : source instanceof FamilyWindowCitizen ? Page.FAMILY
                : source instanceof JobWindowCitizen ? Page.JOB : Page.OVERVIEW;
    }

    @Override protected void init() {
        // Screen dimensions already use the player's GUI scale. Only shrink when the
        // complete frame cannot fit; never enlarge it to fill the viewport.
        scale = Math.min(1.0, Math.min((width - 16.0) / FRAME_WIDTH,
                (height - 16.0) / (FRAME_HEIGHT + TAB_HEIGHT)));
        scale = Math.max(0.01, scale);
        originX = (width - FRAME_WIDTH * scale) / 2;
        originY = (height - (FRAME_HEIGHT + TAB_HEIGHT) * scale) / 2;
        if (!opened) {
            source.onOpened();
            opened = true;
        }
        hideSidebar(source);
        updateContentTransform();
    }

    private static void hideSidebar(Pane pane) {
        if (SIDEBAR_IDS.contains(pane.getID())) pane.hide();
        if (pane instanceof View view) {
            for (var child : List.copyOf(view.getChildren())) hideSidebar(child);
        }
    }

    @Override public void tick() {
        var current = citizen.getColony().getCitizen(citizen.getId());
        if (current != null) citizen = current;
        source.onUpdate();
        if (embeddedPage()) controls.refresh(source, SIDEBAR_IDS);
    }

    private boolean debugEnabled() {
        return minecraft.player != null && DebugPlayerManager.hasDebugEnabled(minecraft.player);
    }

    private void select(Page next) {
        if (next == page || next == Page.DEBUG && !debugEnabled()) return;
        source.onClosed();
        BOWindow.clearFocus();
        opened = false;
        source = switch (next) {
            case REQUESTS -> new RequestWindowCitizen(citizen);
            case HAPPINESS -> new HappinessWindowCitizen(citizen);
            case FAMILY -> new FamilyWindowCitizen(citizen);
            case JOB -> new JobWindowCitizen(citizen);
            case DEBUG -> new DebugWindowCitizen(citizen);
            default -> new MainWindowCitizen(citizen);
        };
        page = next;
        if (next != Page.DEBUG && !ScreenTheme.enabled(source)) {
            source.open();
            return;
        }
        source.onOpened();
        opened = true;
        hideSidebar(source);
        updateContentTransform();
    }

    private void upstream(String id) {
        Pane pane = source.findPaneByID(id);
        if (pane instanceof Button button && button.isEnabled()) button.handleClick(0, 0);
        else if (page == Page.DEBUG && id.equals("inventoryTab")) {
            new OpenInventoryMessage(citizen.getColony(), citizen.getName(), citizen.getEntityId()).sendToServer();
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        double mx = (mouseX - originX) / scale;
        double my = (mouseY - originY) / scale;
        actions.clear();
        graphics.pose().pushPose();
        graphics.pose().translate(originX, originY, 0);
        graphics.pose().scale((float) scale, (float) scale, 1);
        drawTab(graphics, 0, Items.NAME_TAG, "overview", page == Page.OVERVIEW, () -> select(Page.OVERVIEW));
        drawTab(graphics, 28, Items.EXPERIENCE_BOTTLE, "skills", page == Page.SKILLS || page == Page.JOB, () -> select(Page.SKILLS));
        drawTab(graphics, 56, Items.CHEST, "inventory", false, () -> upstream("inventoryTab"));
        drawTab(graphics, 84, Items.WRITABLE_BOOK, "requests", page == Page.REQUESTS, () -> select(Page.REQUESTS));
        drawTab(graphics, 112, Items.CAKE, "happiness", page == Page.HAPPINESS, () -> select(Page.HAPPINESS));
        drawTab(graphics, 140, Items.OAK_SAPLING, "family", page == Page.FAMILY, () -> select(Page.FAMILY));
        if (debugEnabled()) {
            drawTab(graphics, FRAME_WIDTH - 26, Items.COMMAND_BLOCK, "debug", page == Page.DEBUG, () -> select(Page.DEBUG));
        }
        ScreenTheme.panel(graphics, 0, TAB_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);
        // Like the creative inventory, the active tab overlaps the frame's top border.
        int selected = switch (page) {
            case OVERVIEW -> 0;
            case SKILLS, JOB -> 28;
            case REQUESTS -> 84;
            case HAPPINESS -> 112;
            case FAMILY -> 140;
            case DEBUG -> FRAME_WIDTH - 26;
        };
        Item selectedIcon = switch (page) {
            case OVERVIEW -> Items.NAME_TAG;
            case SKILLS, JOB -> Items.EXPERIENCE_BOTTLE;
            case REQUESTS -> Items.WRITABLE_BOOK;
            case HAPPINESS -> Items.CAKE;
            case FAMILY -> Items.OAK_SAPLING;
            case DEBUG -> Items.COMMAND_BLOCK;
        };
        drawTabSurface(graphics, selected, selectedIcon, true);
        graphics.pose().translate(0, TAB_HEIGHT, 0);
        drawHeader(graphics);
        if (page == Page.OVERVIEW) drawOverview(graphics);
        else if (page == Page.SKILLS) drawSkills(graphics);
        else drawUpstreamPage(graphics, mx, my - TAB_HEIGHT);
        if (page != Page.DEBUG) drawPortrait(graphics, (float) mx, (float) (my - TAB_HEIGHT));
        graphics.pose().popPose();
        for (var action : actions) {
            if (action.contains(mx, my)) {
                graphics.renderTooltip(font, action.tooltip(), mouseX, mouseY);
                break;
            }
        }
    }

    private void drawTab(GuiGraphics graphics, int x, Item icon, String label, boolean selected, Runnable click) {
        if (!selected) drawTabSurface(graphics, x, icon, false);
        actions.add(new Action(x, 0, 26, TAB_HEIGHT, text(label), click));
    }

    private void drawTabSurface(GuiGraphics graphics, int x, Item icon, boolean selected) {
        int column = x == 0 ? 1 : x == FRAME_WIDTH - 26 ? 7 : 2;
        String sprite = "container/creative_inventory/tab_top_"
                + (selected ? "selected_" : "unselected_") + column;
        graphics.blitSprite(ResourceLocation.withDefaultNamespace(sprite), x, 0, 26, 32);
        graphics.renderItem(new ItemStack(icon), x + 5, 9);
    }

    private void drawHeader(GuiGraphics graphics) {
        String name = font.plainSubstrByWidth(citizen.getName(), FRAME_WIDTH - 76);
        graphics.drawString(font, name, LEFT, 9, 0x404040, false);
        int nameWidth = font.width(name) + 5;
        graphics.drawString(font, citizen.isFemale() ? "♀" : "♂", LEFT + nameWidth, 9,
                citizen.isFemale() ? 0xE65AAA : 0x5599DD, false);
        if (page == Page.DEBUG) {
            Component back = text("back");
            graphics.blitSprite(ResourceLocation.withDefaultNamespace("widget/button"), LEFT, 23, 114, 18);
            graphics.drawCenteredString(font, back, LEFT + 57, 28, 0xFFFFFF);
            actions.add(new Action(LEFT, TAB_HEIGHT + 23, 114, 18, back, () -> select(Page.OVERVIEW)));
        } else {
            graphics.drawString(font, font.plainSubstrByWidth(citizen.getJobComponent().getString(), FRAME_WIDTH - 62), LEFT, 26, 0x555555, false);
        }
        ScreenTheme.panel(graphics, FRAME_WIDTH - 38, 7, 26, 26);
        graphics.renderItem(new ItemStack(Items.COMPASS), FRAME_WIDTH - 33, 12);
        actions.add(new Action(FRAME_WIDTH - 38, TAB_HEIGHT + 7, 26, 26,
                Component.translatable("button.minecoloniestweaks.locate"), () -> {
                    LocateBeams.citizen(citizen);
                    minecraft.setScreen(null);
                }));
    }

    private void drawOverview(GuiGraphics graphics) {
        inset(graphics, LEFT, 44, LEFT_WIDTH, 108);
        var status = citizen.getVisibleStatus();
        row(graphics, "job", citizen.getJobComponent(), 51, null);
        row(graphics, "work", buildingName(citizen.getWorkBuilding(), "none"), 67,
                () -> openBuilding(citizen.getWorkBuilding()));
        row(graphics, "home", buildingName(citizen.getHomeBuilding(), "homeless"), 83,
                () -> openBuilding(citizen.getHomeBuilding()));
        row(graphics, "activity", status == null ? text("none") : Component.translatable(status.getTranslationKey()), 99, null);
        var reasons = CitizenDiagnosis.explain(citizen);
        var verdict = !valid(citizen.getWorkBuilding()) && !citizen.isChild()
                ? citizen.getJobComponent() : reasons.getFirst();
        row(graphics, "verdict", verdict, 115,
                () -> minecraft.setScreen(new DiagnosisScreen(this, citizen)));
        row(graphics, "requests", citizen.hasPendingInteractions() ? text("pending") : text("none"), 131,
                () -> select(Page.REQUESTS));
        graphics.drawString(font, text("job_skills"), LEFT, 160, 0x404040, false);
        inset(graphics, LEFT, 174, LEFT_WIDTH, 30);
        var work = valid(citizen.getWorkBuilding())
                ? citizen.getColony().getClientBuildingManager().getBuilding(citizen.getWorkBuilding()) : null;
        var worker = work == null ? null : work.getModuleViewByType(WorkerBuildingModuleView.class);
        if (worker == null) {
            drawWrapped(graphics, text("no_job_bonus"), LEFT + 6, 179, LEFT_WIDTH - 12, 0xFFFFFF);
        } else {
            skillRow(graphics, worker.getPrimarySkill(), LEFT + 6, 179, LEFT_WIDTH - 12);
            skillRow(graphics, worker.getSecondarySkill(), LEFT + 6, 192, LEFT_WIDTH - 12);
            actions.add(new Action(LEFT, TAB_HEIGHT + 174, LEFT_WIDTH, 30, text("job_skills"), () -> select(Page.JOB)));
        }
    }

    private void row(GuiGraphics graphics, String label, Component value, int y, Runnable click) {
        Component caption = text(label);
        String captionText = font.plainSubstrByWidth(caption.getString(), LEFT_WIDTH / 2 - 10);
        graphics.drawString(font, captionText, LEFT + 6, y, 0xB0B0B0, false);
        String shown = font.plainSubstrByWidth(value.getString(), LEFT_WIDTH - font.width(captionText) - 23);
        boolean blocked = !valid(citizen.getWorkBuilding()) || citizen.isPaused() || citizen.isSick()
                || citizen.getSaturation() < 10 || citizen.hasBlockingInteractions();
        int color = label.equals("verdict") && blocked ? 0xFF5555 : 0xFFFFFF;
        graphics.drawString(font, shown, LEFT + LEFT_WIDTH - 7 - font.width(shown), y, color, false);
        if (click != null) actions.add(new Action(LEFT + 3, TAB_HEIGHT + y - 3, LEFT_WIDTH - 6, 15, value, click));
    }

    private void drawSkills(GuiGraphics graphics) {
        inset(graphics, LEFT, 44, LEFT_WIDTH, 160);
        int y = 51;
        for (var skill : Skill.values()) {
            skillRow(graphics, skill, LEFT + 8, y, LEFT_WIDTH - 16);
            y += 14;
        }
    }

    private void skillRow(GuiGraphics graphics, Skill skill, int x, int y, int width) {
        if (skill == null) return;
        String id = skill.name().toLowerCase(Locale.ROOT);
        var label = Component.translatable("com.minecolonies.coremod.gui.citizen.skills." + id);
        graphics.drawString(font, label, x, y, 0xFFFFFF, false);
        String level = Integer.toString(citizen.getCitizenSkillHandler().getLevel(skill));
        graphics.drawString(font, level, x + width - font.width(level), y, 0xFFFFFF, false);
    }

    private void drawPortrait(GuiGraphics graphics, float mouseX, float mouseY) {
        ScreenTheme.roundedFill(graphics, RIGHT, 44, 48, 78, 3, 0xFF101010);
        var entity = minecraft.level == null ? null : minecraft.level.getEntity(citizen.getEntityId());
        if (entity instanceof LivingEntity living) {
            graphics.pose().pushPose();
            graphics.pose().setIdentity();
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                    (int) (originX + RIGHT * scale), (int) (originY + (TAB_HEIGHT + 44) * scale),
                    (int) (originX + (RIGHT + 48) * scale), (int) (originY + (TAB_HEIGHT + 122) * scale),
                    Math.max(1, (int) ((citizen.isChild() ? 30 : 27) * scale)), 0.0625F,
                    (float) (originX + mouseX * scale), (float) (originY + (TAB_HEIGHT + mouseY) * scale), living);
            graphics.pose().popPose();
        } else {
            drawWrapped(graphics, text("not_loaded"), RIGHT + 3, 61, 42, 0xAAAAAA);
        }
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] empty = {"helmet", "chestplate", "leggings", "boots"};
        for (int i = 0; i < slots.length; i++) {
            equipment(graphics, RIGHT + 50, 44 + i * 20, citizen.getDisplayArmor(slots[i]), empty[i]);
        }
        var inventory = citizen.getInventory();
        equipment(graphics, RIGHT + 70, 44, inventory.getHeldItem(InteractionHand.MAIN_HAND), "sword");
        equipment(graphics, RIGHT + 70, 64, inventory.getHeldItem(InteractionHand.OFF_HAND), "shield");
        drawMeter(graphics, "heart/container", "heart/full", "heart/half", 130,
                citizen.getHealth() / Math.max(1, citizen.getMaxHealth()));
        drawMeter(graphics, "food_empty", "food_full", "food_half", 142, citizen.getSaturation() / 100.0);
        graphics.fill(RIGHT, 160, RIGHT + RIGHT_WIDTH, 166, 0xFF333B22);
        graphics.fill(RIGHT + 1, 161, RIGHT + 1 + (int) ((RIGHT_WIDTH - 2) * Math.clamp(citizen.getHappiness() / 10, 0, 1)), 165, 0xFF80B445);
        actions.add(new Action(RIGHT, TAB_HEIGHT + 158, RIGHT_WIDTH, 10,
                text("happiness_value", String.format(Locale.ROOT, "%.1f", citizen.getHappiness())), () -> select(Page.HAPPINESS)));
        var status = citizen.getVisibleStatus();
        if (status != null) drawWrapped(graphics, Component.translatable(status.getTranslationKey()), RIGHT, 176, RIGHT_WIDTH, 0x404040);
    }

    private void equipment(GuiGraphics graphics, int x, int y, ItemStack stack, String placeholder) {
        graphics.fill(x - 1, y - 1, x + 18, y + 18, 0xFF555555);
        graphics.fill(x, y, x + 17, y + 17, 0xFF8B8B8B);
        graphics.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 1, y + 1);
            actions.add(new Action(x, TAB_HEIGHT + y, 18, 18, stack.getHoverName(), () -> upstream("inventoryTab")));
        } else {
            String texture = placeholder.equals("sword") ? "empty_slot_sword" : "empty_armor_slot_" + placeholder;
            graphics.blit(ResourceLocation.withDefaultNamespace("textures/item/" + texture + ".png"),
                    x + 1, y + 1, 0, 0, 16, 16, 16, 16);
        }
    }

    private void drawMeter(GuiGraphics graphics, String empty, String full, String half, int y, double fraction) {
        double count = Math.clamp(fraction, 0, 1) * 20;
        for (int i = 0; i < 10; i++) {
            int x = RIGHT + (RIGHT_WIDTH - 81) / 2 + i * 8;
            graphics.blitSprite(ResourceLocation.withDefaultNamespace("hud/" + empty), x, y, 9, 9);
            if (count > i * 2) graphics.blitSprite(ResourceLocation.withDefaultNamespace("hud/" + (count >= i * 2 + 2 ? full : half)), x, y, 9, 9);
        }
    }

    private boolean embeddedPage() { return page != Page.OVERVIEW && page != Page.SKILLS; }

    private void updateContentTransform() {
        contentWidth = page == Page.DEBUG ? FRAME_WIDTH - LEFT * 2 : LEFT_WIDTH;
        contentScroll = 0;
        if (embeddedPage()) controls.refresh(source, SIDEBAR_IDS);
    }

    private void drawUpstreamPage(GuiGraphics graphics, double mouseX, double mouseY) {
        inset(graphics, LEFT, 44, contentWidth, 160);
        graphics.fill(LEFT + 1, 45, LEFT + contentWidth - 1, 205, 0xFFC6C6C6);
        contentScroll = Math.clamp(contentScroll, 0, Math.max(0, controls.height(contentWidth - 12) - 150));
        // GuiGraphics scissors use absolute GUI coordinates, independent of the pose stack.
        graphics.enableScissor((int) Math.floor(originX + (LEFT + 2) * scale),
                (int) Math.floor(originY + (TAB_HEIGHT + 46) * scale),
                (int) Math.ceil(originX + (LEFT + contentWidth - 2) * scale),
                (int) Math.ceil(originY + (TAB_HEIGHT + 204) * scale));
        controls.render(graphics, LEFT + 6, 48 - contentScroll, contentWidth - 12, 46, 204, (int) mouseX, (int) mouseY);
        graphics.disableScissor();
        if (controls.hasChoices()) controls.renderChoices(graphics, LEFT + 4, 46, contentWidth - 8, 158, (int) mouseX, (int) mouseY);
        else controls.tooltip(graphics, (int) mouseX, (int) mouseY, 46, 204);
    }

    private Component buildingName(BlockPos position, String fallback) {
        var building = valid(position) ? citizen.getColony().getClientBuildingManager().getBuilding(position) : null;
        return building == null ? text(fallback) : Component.translatable(building.getBuildingDisplayName());
    }

    private void openBuilding(BlockPos position) {
        if (!valid(position)) return;
        var building = citizen.getColony().getClientBuildingManager().getBuilding(position);
        if (building != null) building.openGui(false);
    }

    private static boolean valid(BlockPos position) { return position != null && !position.equals(BlockPos.ZERO); }
    private static Component text(String key, Object... args) { return Component.translatable("citizen.minecoloniestweaks." + key, args); }

    private void drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        for (var line : font.split(text, width)) {
            if (y + font.lineHeight > FRAME_HEIGHT - 8) break;
            graphics.drawString(font, line, x, y, color, false);
            y += 11;
        }
    }

    private static void inset(GuiGraphics graphics, int x, int y, int width, int height) {
        ScreenTheme.roundedFill(graphics, x, y, width, height, 3, 0xFF555555);
        ScreenTheme.roundedFill(graphics, x + 1, y + 1, width - 1, height - 1, 3, 0xFFFFFFFF);
        ScreenTheme.roundedFill(graphics, x + 1, y + 1, width - 2, height - 2, 2, 0xFF404040);
    }

    private boolean embedded(double x, double y) {
        return page != Page.OVERVIEW && page != Page.SKILLS && x >= LEFT && x < LEFT + contentWidth && y >= 44 && y < 206;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = (mouseX - originX) / scale;
        double y = (mouseY - originY) / scale;
        if (button == 0) {
            for (var action : actions) {
                if (action.contains(x, y)) { action.run().run(); return true; }
            }
        }
        y -= TAB_HEIGHT;
        if (button == 0 && embedded(x, y)) {
            if (controls.clickChoices(x, y, LEFT + 4, 46, contentWidth - 8, 158)) return true;
            return controls.click(x, y);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double x = (mouseX - originX) / scale;
        double y = (mouseY - originY) / scale - TAB_HEIGHT;
        if (embedded(x, y)) {
            if (controls.hasChoices()) controls.scrollChoices(vertical);
            else contentScroll = Math.clamp(contentScroll - (int) (vertical * 20), 0, Math.max(0, controls.height(contentWidth - 12) - 150));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override public boolean charTyped(char character, int modifiers) {
        return controls.character(character, modifiers) || super.charTyped(character, modifiers);
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 256 && controls.closeChoices()) return true;
        return controls.key(key, scanCode, modifiers) || super.keyPressed(key, scanCode, modifiers);
    }

    @Override public void removed() {
        if (opened) { source.onClosed(); BOWindow.clearFocus(); opened = false; }
    }

    @Override public boolean isPauseScreen() { return false; }
}
