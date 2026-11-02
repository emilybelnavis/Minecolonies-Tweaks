package dev.minecoloniestweaks.client.screen;

import com.ldtteam.domumornamentum.block.ModBlocks;
import com.ldtteam.domumornamentum.client.screens.ArchitectsCutterScreen;
import com.ldtteam.domumornamentum.container.ArchitectsCutterContainer;
import dev.minecoloniestweaks.mixin.client.CutterPreviewAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** The original menu owns crafting and inventory transfer; this screen only selects its groups and variants. */
public final class CutterScreen extends AbstractContainerScreen<ArchitectsCutterContainer> {
    private final CutterPreviewAccess previews;
    private List<ResourceLocation> groups = List.of();
    private int group;
    private int page;
    private int columns;
    private int rows;

    public CutterScreen(ArchitectsCutterContainer menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 202;
        previews = (CutterPreviewAccess) new ArchitectsCutterScreen(menu, inventory, title);
    }

    @Override protected void init() {
        imageWidth = Math.min(width - 8, 420);
        columns = Math.max(3, (imageWidth - 232) / 18);
        rows = 8;
        super.init();
        groups = new ArrayList<>(ModBlocks.getInstance().getOrComputeItemGroups().keySet());
        var selected = menu.getCurrentGroup();
        if (groups.contains(selected)) group = groups.indexOf(selected);
        addRenderableWidget(Button.builder(Component.literal("<"), button -> changeGroup(-1)).bounds(leftPos + 8, topPos + 18, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> changeGroup(1)).bounds(leftPos + 194, topPos + 18, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1)).bounds(leftPos + 232, topPos + 176, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1)).bounds(leftPos + imageWidth - 28, topPos + 176, 20, 20).build());
    }

    private List<ItemStack> variants() {
        return groups.isEmpty() ? List.of() : ModBlocks.getInstance().getOrComputeItemGroups().getOrDefault(groups.get(group), List.of());
    }

    private void changeGroup(int delta) {
        if (groups.isEmpty()) return;
        group = Math.floorMod(group + delta, groups.size());
        page = 0;
        select(group);
    }

    private void select(int button) {
        if (minecraft.player != null && minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, button)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    private void changePage(int delta) {
        int count = Math.max(1, (variants().size() + columns * rows - 1) / (columns * rows));
        page = Math.floorMod(page + delta, count);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ScreenTheme.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
            graphics.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
        }
        var variants = variants();
        for (int i = 0; i < columns * rows; i++) {
            int index = page * columns * rows + i;
            int x = leftPos + 232 + (i % columns) * 18;
            int y = topPos + 26 + (i / columns) * 18;
            graphics.fill(x, y, x + 18, y + 18, 0xFF555555);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
            if (index < variants.size()) {
                var preview = variants.get(index).copy();
                previews.tweaks$applyMaterials(preview);
                graphics.renderItem(preview, x + 1, y + 1);
            }
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        graphics.drawString(font, Component.translatable("cutter.minecoloniestweaks.materials"), 85, 54, 0x404040, false);
        graphics.drawString(font, Component.translatable("cutter.minecoloniestweaks.result"), 172, 65, 0x404040, false);
        graphics.drawString(font, Component.translatable("container.inventory"), 40, 108, 0x404040, false);
        if (!groups.isEmpty()) {
            var id = groups.get(group);
            String translation = "cuttergroup." + id.getNamespace() + "." + id.getPath();
            String name = I18n.exists(translation)
                    ? Component.translatable(translation).getString() : titleCase(id.getPath());
            graphics.drawString(font, font.plainSubstrByWidth(name, 156), 32, 24, 0x404040, false);
        }
        graphics.drawString(font, Component.translatable("cutter.minecoloniestweaks.variants", page + 1), 232, 9, 0x404040, false);
    }

    private static String titleCase(String value) {
        StringBuilder text = new StringBuilder();
        for (String word : value.replace('/', '_').split("_")) {
            if (!word.isEmpty()) text.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.ROOT)).append(' ');
        }
        return text.toString().strip();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        int index = indexAt(mouseX, mouseY);
        if (index >= 0 && index < variants().size()) graphics.renderTooltip(font, variants().get(index), mouseX, mouseY);
    }

    private int indexAt(double mouseX, double mouseY) {
        int x = (int) mouseX - leftPos - 232;
        int y = (int) mouseY - topPos - 26;
        if (x < 0 || x >= columns * 18 || y < 0 || y >= rows * 18) return -1;
        return page * columns * rows + (y / 18) * columns + x / 18;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = indexAt(mouseX, mouseY);
        if (button == 0 && index >= 0 && index < variants().size()) {
            select(groups.size() + index);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseX >= leftPos + 230) changePage(vertical > 0 ? -1 : 1);
        else changeGroup(vertical > 0 ? -1 : 1);
        return true;
    }
}
