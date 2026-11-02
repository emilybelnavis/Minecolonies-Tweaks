package dev.minecoloniestweaks.client.screen;

import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.AbstractTextElement;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.CheckBox;
import com.ldtteam.blockui.controls.ItemIcon;
import com.ldtteam.blockui.controls.TextField;
import com.ldtteam.blockui.views.DropDownList;
import com.ldtteam.blockui.views.ScrollingList;
import com.ldtteam.blockui.views.SwitchView;
import com.ldtteam.blockui.views.View;
import dev.minecoloniestweaks.mixin.client.DropDownAccess;
import dev.minecoloniestweaks.mixin.client.TextFieldAccess;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Reflows live upstream controls into native rows without drawing or reparenting their panes. */
final class NativeBuildingControls {
    private static final Set<String> NAVIGATION = Set.of("actions", "infopage", "permissions", "citizens",
            "happiness", "alliances", "settings");
    private final List<List<Pane>> rows = new ArrayList<>();
    private final Map<TextField, EditBox> inputs = new IdentityHashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private final Minecraft client = Minecraft.getInstance();
    private EditBox focused;
    private boolean syncing;
    private Set<String> excluded = Set.of();
    private DropDownList choices;
    private int choiceScroll;
    private int layoutWidth = 160;
    private record Hit(int x, int y, int width, int height, Pane pane, Runnable click) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + width && my >= y && my < y + height; }
    }

    void refresh(View root) { refresh(root, Set.of()); }

    void refresh(View root, Set<String> excluded) {
        this.excluded = excluded;
        rows.clear();
        collect(root);
        var visible = rows.stream().flatMap(List::stream).toList();
        inputs.keySet().removeIf(input -> !visible.contains(input));
        if (focused != null && !inputs.containsValue(focused)) focused = null;
        if (choices != null && !visible.contains(choices)) choices = null;
    }

    private void collect(Pane pane) {
        if (!pane.isVisible() || excluded(pane) || decoration(pane)) return;
        if (pane instanceof SwitchView pages) {
            if (pages.getCurrentView() != null) collect(pages.getCurrentView());
        } else if (pane instanceof DropDownList || pane instanceof TextField || pane instanceof Button
                || pane instanceof ItemIcon || pane instanceof AbstractTextElement) {
            if (!(pane instanceof AbstractTextElement text) || !text.isTextEmpty() || pane instanceof Button) rows.add(List.of(pane));
        } else if (pane instanceof View view) {
            if (view instanceof ScrollingList list) {
                // BlockUI normally materializes only visible rows. Our own scroll viewport owns clipping.
                list.setMaxHeight(Math.max(list.getHeight(), list.getContentHeight()));
                list.refreshElementPanes();
            }
            var children = new ArrayList<>(view.getChildren());
            children.sort(Comparator.comparingInt(Pane::getY).thenComparingInt(Pane::getX));
            List<Pane> line = new ArrayList<>();
            int previousY = Integer.MIN_VALUE;
            for (var child : children) {
                if (!child.isVisible() || excluded(child) || decoration(child)) continue;
                boolean atomic = child instanceof DropDownList || child instanceof TextField || child instanceof Button
                        || child instanceof ItemIcon || child instanceof AbstractTextElement;
                if (!atomic || child.getY() != previousY) {
                    if (!line.isEmpty()) { rows.add(List.copyOf(line)); line.clear(); }
                }
                if (atomic) {
                    if (!(child instanceof AbstractTextElement text) || !text.isTextEmpty() || child instanceof Button) line.add(child);
                    previousY = child.getY();
                } else collect(child);
            }
            if (!line.isEmpty()) rows.add(List.copyOf(line));
        }
    }

    private boolean excluded(Pane pane) {
        if (!excluded.contains(pane.getID())) return false;
        // Row IDs are local to their list, not aliases of the dashboard's controls.
        for (Pane parent = pane.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof ScrollingList) return false;
        }
        return true;
    }

    static boolean decoration(Pane pane) {
        if (pane instanceof com.ldtteam.blockui.controls.Tooltip) return true;
        String id = pane.getID();
        if (id.endsWith("_view") && id.contains("textures/")) return true;
        for (String navigation : pane.getWindow() instanceof com.minecolonies.core.client.gui.townhall.AbstractWindowTownHall ? NAVIGATION : Set.<String>of()) {
            if (id.equals(navigation) || id.equals(navigation + "0") || id.equals(navigation + "1")
                    || id.equals(navigation + "Ext")) return true;
        }
        return false;
    }

    int height(int width) { layoutWidth = width; return height(); }
    int height() { return rows.stream().mapToInt(row -> rowHeight(row, layoutWidth)).sum(); }

    private int rowHeight(List<Pane> row, int width) {
        int cellWidth = Math.max(1, (width - (row.size() - 1) * 4) / row.size());
        int height = 25;
        for (var pane : row) if (pane instanceof AbstractTextElement && !(pane instanceof Button)) {
            height = Math.max(height, client.font.split(label(pane), Math.max(1, cellWidth - 4)).size() * client.font.lineHeight + 12);
        }
        return height;
    }

    void render(GuiGraphics graphics, int x, int y, int width, int top, int bottom, int mouseX, int mouseY) {
        hits.clear();
        layoutWidth = width;
        int rowY = y;
        for (var row : rows) {
            int cellWidth = (width - (row.size() - 1) * 4) / row.size();
            for (int i = 0; i < row.size(); i++) {
                var pane = row.get(i);
                int cellX = x + i * (cellWidth + 4);
                if (rowY + rowHeight(row, width) > top && rowY < bottom) renderCell(graphics, pane, cellX, rowY, cellWidth, mouseX, mouseY);
            }
            rowY += rowHeight(row, width);
        }
    }

    private void renderCell(GuiGraphics graphics, Pane pane, int x, int y, int width, int mx, int my) {
        Component label = label(pane);
        boolean enabled = enabled(pane);
        boolean hover = mx >= x && mx < x + width && my >= y && my < y + 20;
        if (pane instanceof TextField field) {
            var edit = inputs.computeIfAbsent(field, input -> {
                var box = new EditBox(client.font, 0, 0, width, 20, Component.literal(input.getID()));
                box.setMaxLength(input.getMaxTextLength());
                box.setValue(input.getText());
                box.setFilter(value -> input.getFilter() == null || (value.chars().allMatch(c -> input.getFilter().isAllowedCharacter((char) c))
                        && input.getFilter().filter(value).equals(value)));
                box.setResponder(value -> {
                    if (syncing || value.equals(input.getText())) return;
                    input.setText(value);
                    ((TextFieldAccess) input).tweaks$notifyChanged();
                });
                return box;
            });
            if (edit != focused && !edit.getValue().equals(field.getText())) {
                syncing = true;
                try { edit.setValue(field.getText()); } finally { syncing = false; }
            }
            edit.setX(x); edit.setY(y); edit.setWidth(width); edit.active = enabled;
            edit.render(graphics, mx, my, 0);
            hits.add(new Hit(x, y, width, 20, pane, () -> {
                unfocus(); focused = edit; edit.setFocused(true); edit.mouseClicked(mx, my, 0);
            }));
        } else if (pane instanceof Button button || pane instanceof DropDownList) {
            graphics.blitSprite(ResourceLocation.withDefaultNamespace(!enabled ? "widget/button_disabled" : hover ? "widget/button_highlighted" : "widget/button"), x, y, width, 20);
            String text = client.font.plainSubstrByWidth(label.getString(), Math.max(0, width - 8));
            if (pane instanceof CheckBox check) text = (check.isChecked() ? "[x] " : "[ ] ") + text;
            graphics.drawCenteredString(client.font, text, x + width / 2, y + 6, enabled ? 0xFFFFFF : 0xA0A0A0);
            hits.add(new Hit(x, y, width, 20, pane, () -> {
                unfocus();
                if (pane instanceof DropDownList dropdown) { choices = dropdown; choiceScroll = 0; }
                else ((Button) pane).handleClick(0, 0);
            }));
        } else if (pane instanceof ItemIcon item) {
            graphics.renderItem(item.getItem(), x + 2, y + 2);
            graphics.drawString(client.font, client.font.plainSubstrByWidth(item.getItem().getHoverName().getString(), Math.max(0, width - 22)), x + 22, y + 6, 0x404040, false);
            hits.add(new Hit(x, y, width, 20, pane, () -> {}));
        } else {
            int lineY = y + 6;
            for (var line : client.font.split(label, Math.max(1, width - 4))) {
                graphics.drawString(client.font, line, x + 2, lineY, 0x404040, false);
                lineY += client.font.lineHeight;
            }
            hits.add(new Hit(x, y, width, Math.max(20, lineY - y + 6), pane, () -> {}));
        }
    }

    boolean hasChoices() { return choices != null; }

    void renderChoices(GuiGraphics graphics, int x, int y, int width, int height, int mx, int my) {
        if (choices == null) return;
        ScreenTheme.panel(graphics, x, y, width, height);
        var provider = ((DropDownAccess) choices).tweaks$options();
        if (provider == null) { choices = null; return; }
        int count = Math.max(1, (height - 12) / 22);
        choiceScroll = Math.clamp(choiceScroll, 0, Math.max(0, provider.getElementCount() - count));
        for (int row = 0; row < count && row + choiceScroll < provider.getElementCount(); row++) {
            int index = row + choiceScroll;
            int optionY = y + 6 + row * 22;
            boolean hover = mx >= x + 5 && mx < x + width - 5 && my >= optionY && my < optionY + 20;
            graphics.blitSprite(ResourceLocation.withDefaultNamespace(hover ? "widget/button_highlighted" : "widget/button"), x + 5, optionY, width - 10, 20);
            graphics.drawCenteredString(client.font, client.font.plainSubstrByWidth(provider.getLabel(index).getString(), width - 20), x + width / 2, optionY + 6, 0xFFFFFF);
        }
    }

    boolean clickChoices(double mx, double my, int x, int y, int width, int height) {
        if (choices == null) return false;
        int row = (int) (my - y - 6) / 22;
        var provider = ((DropDownAccess) choices).tweaks$options();
        if (provider != null && enabled(choices) && mx >= x + 5 && mx < x + width - 5 && my >= y + 6
                && my < y + height - 6 && row + choiceScroll < provider.getElementCount()) {
            choices.setSelectedIndex(row + choiceScroll);
        }
        choices = null;
        return true;
    }

    void scrollChoices(double delta) { choiceScroll -= (int) Math.signum(delta); }
    boolean closeChoices() { if (choices == null) return false; choices = null; return true; }

    void tooltip(GuiGraphics graphics, int mx, int my, int top, int bottom) {
        if (my < top || my >= bottom || choices != null) return;
        for (var hit : hits) if (hit.contains(mx, my)) {
            Component text = label(hit.pane());
            if (!text.getString().isBlank()) graphics.renderTooltip(client.font, text, mx, my);
            break;
        }
    }

    boolean click(double mx, double my) {
        for (var hit : List.copyOf(hits)) if (hit.contains(mx, my)) {
            if (enabled(hit.pane())) hit.click().run();
            return true;
        }
        unfocus();
        return false;
    }

    boolean key(int key, int scan, int modifiers) { return focused != null && focused.keyPressed(key, scan, modifiers); }
    boolean character(char c, int modifiers) { return focused != null && focused.charTyped(c, modifiers); }
    private void unfocus() { if (focused != null) focused.setFocused(false); focused = null; }

    static boolean enabled(Pane pane) {
        for (Pane current = pane; current != null; current = current.getParent()) {
            if (!current.isEnabled() || !current.isVisible()) return false;
        }
        return true;
    }

    static Component label(Pane pane) {
        if (pane instanceof DropDownList dropdown) {
            var provider = ((DropDownAccess) dropdown).tweaks$options();
            if (provider != null && dropdown.getSelectedIndex() >= 0 && dropdown.getSelectedIndex() < provider.getElementCount()) return provider.getLabel(dropdown.getSelectedIndex()).copy().append(" ▾");
        }
        if (pane instanceof AbstractTextElement text && !text.isTextEmpty()) return text.getText();
        if (pane instanceof ItemIcon item) return item.getItem().getHoverName();
        if (pane.getHoverPane() instanceof AbstractTextElement hover && !hover.isTextEmpty()) return hover.getText();
        String id = pane.getID().replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' ');
        return Component.literal(id.isBlank() ? "…" : id);
    }
}
