package dev.minecoloniestweaks.client.screen;

import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.research.IGlobalResearch;
import com.minecolonies.api.research.IGlobalResearchTree;
import com.minecolonies.core.client.gui.modules.building.UniversityModuleWindow;
import com.minecolonies.core.network.messages.server.colony.building.university.TryResearchMessage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Advancements-style research map. Research requests still use the upstream permission-checked packet. */
public final class ResearchScreen extends Screen {
    private final IBuildingView building;
    private final UniversityModuleWindow parent;
    private final ResourceLocation branch;
    private final Map<ResourceLocation, Node> nodes = new LinkedHashMap<>();
    private double panX = 12;
    private double panY = 12;
    private double zoom = 1;
    private int left;
    private int top;
    private int mapWidth;
    private int mapHeight;
    private Node hovered;
    private int row;

    private record Node(IGlobalResearch research, int x, int y) {}

    public ResearchScreen(IBuildingView building, ResourceLocation branch, UniversityModuleWindow parent) {
        super(Component.translatable(IGlobalResearchTree.getInstance().getBranchData(branch).getName().getKey()));
        this.building = building;
        this.branch = branch;
        this.parent = parent;
    }

    @Override protected void init() {
        mapWidth = Math.min(540, width - 24);
        mapHeight = Math.min(300, height - 70);
        left = (width - mapWidth) / 2;
        top = (height - mapHeight) / 2;
        if (nodes.isEmpty()) {
            for (var root : IGlobalResearchTree.getInstance().getPrimaryResearch(branch)) layout(root, new HashSet<>());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(left + mapWidth - 75, top - 24, 75, 20).build());
    }

    private void layout(ResourceLocation id, Set<ResourceLocation> visited) {
        if (!visited.add(id)) return;
        var research = IGlobalResearchTree.getInstance().getResearch(branch, id);
        if (research == null || research.isHidden()) return;
        int y = row++ * 42;
        nodes.put(id, new Node(research, Math.max(0, research.getDepth() - 1) * 84, y));
        for (var child : research.getChildren()) layout(child, visited);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        ScreenTheme.panel(graphics, left - 5, top - 5, mapWidth + 10, mapHeight + 10);
        graphics.fill(left, top, left + mapWidth, top + mapHeight, 0xFF202020);
        graphics.drawString(font, title, left, top - 18, 0xFFFFFF);
        graphics.enableScissor(left, top, left + mapWidth, top + mapHeight);
        graphics.pose().pushPose();
        graphics.pose().translate(left + panX, top + panY, 0);
        graphics.pose().scale((float) zoom, (float) zoom, 1);
        hovered = null;
        double mx = (mouseX - left - panX) / zoom;
        double my = (mouseY - top - panY) / zoom;
        var local = building.getColony().getResearchManager().getResearchTree();
        for (var node : nodes.values()) {
            var parentNode = nodes.get(node.research().getParent());
            if (parentNode == null) continue;
            int ax = parentNode.x() + 26;
            int ay = parentNode.y() + 13;
            int bx = node.x();
            int by = node.y() + 13;
            int middle = (ax + bx) / 2;
            graphics.fill(ax, ay - 1, middle + 1, ay + 1, 0xFF888888);
            graphics.fill(middle - 1, Math.min(ay, by), middle + 1, Math.max(ay, by) + 1, 0xFF888888);
            graphics.fill(middle, by - 1, bx, by + 1, 0xFF888888);
        }
        for (var node : nodes.values()) {
            boolean complete = local.isComplete(node.research().getId());
            var progress = local.getResearch(branch, node.research().getId());
            String frame = complete ? "obtained" : "unobtained";
            graphics.blitSprite(ResourceLocation.withDefaultNamespace("advancements/task_frame_" + frame), node.x(), node.y(), 26, 26);
            ItemStack icon = new ItemStack(Items.BOOK);
            if (!node.research().getCostList().isEmpty()) {
                var items = node.research().getCostList().getFirst().getItems();
                if (items.length > 0) icon = items[0];
            }
            graphics.renderItem(icon, node.x() + 5, node.y() + 5);
            if (progress != null && !complete) graphics.fill(node.x(), node.y() + 27, node.x() + 26, node.y() + 29, 0xFF55AAFF);
            if (mx >= node.x() && mx < node.x() + 26 && my >= node.y() && my < node.y() + 26
                    && mouseX >= left && mouseX < left + mapWidth && mouseY >= top && mouseY < top + mapHeight) hovered = node;
        }
        graphics.pose().popPose();
        graphics.disableScissor();
        if (hovered != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(hovered.research().getName().getKey()));
            lines.add(Component.translatable(hovered.research().getSubtitle().getKey()));
            lines.add(Component.translatable("research.minecoloniestweaks.level", hovered.research().getDepth()));
            for (var cost : hovered.research().getCostList()) {
                var items = cost.getItems();
                if (items.length > 0) lines.add(Component.literal(cost.count() + " × ").append(items[0].getHoverName()));
            }
            for (var requirement : hovered.research().getResearchRequirements()) lines.add(requirement.getDesc());
            lines.add(Component.translatable(local.isComplete(hovered.research().getId()) ? "research.minecoloniestweaks.completed" : "research.minecoloniestweaks.start"));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hovered == null || (button != 0 && button != 1)) return super.mouseClicked(mouseX, mouseY, button);
        var research = hovered.research();
        boolean reset = button == 1;
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) new TryResearchMessage(building, research.getId(), branch, reset).sendToServer();
            minecraft.setScreen(this);
        }, Component.translatable(research.getName().getKey()),
                Component.translatable(reset ? "research.minecoloniestweaks.confirm_reset" : "research.minecoloniestweaks.confirm_start")));
        return true;
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button == 0 && mouseY >= top && mouseY < top + mapHeight) {
            panX += dx;
            panY += dy;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double previous = zoom;
        zoom = Math.clamp(zoom + vertical * 0.1, 0.5, 2);
        panX = mouseX - left - (mouseX - left - panX) * zoom / previous;
        panY = mouseY - top - (mouseY - top - panY) * zoom / previous;
        return true;
    }

    @Override public void onClose() { if (parent != null) parent.open(); else minecraft.setScreen(null); }
    @Override public boolean isPauseScreen() { return false; }
}
