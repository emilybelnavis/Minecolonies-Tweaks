package dev.minecoloniestweaks.client.screen;

import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.controls.TextField;
import com.ldtteam.blockui.views.BOWindow;
import com.ldtteam.blockui.views.DropDownList;
import com.ldtteam.blockui.views.ScrollingList;
import com.ldtteam.blockui.views.SwitchView;
import com.ldtteam.blockui.views.View;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Runtime regressions for native controls retaining their upstream handlers and row identities. */
public final class NativeControlsSmoke {
    private NativeControlsSmoke() {}

    public static void run() {
        var root = new BOWindow(ResourceLocation.fromNamespaceAndPath("minecoloniestweaks", "gui/native_controls_smoke.xml"));
        var list = root.findPaneOfTypeByID("rows", ScrollingList.class);
        int[] dispatched = {-1};
        list.setDataProvider(new ScrollingList.DataProvider() {
            @Override public int getElementCount() { return 30; }
            @Override public void updateElement(int index, com.ldtteam.blockui.Pane row) {
                var button = row.findPaneOfTypeByID("rowAction", Button.class);
                button.setText(Component.literal("Row " + index));
                button.setHandler(clicked -> dispatched[0] = list.getListElementIndexByPane(clicked));
            }
        });
        var controls = new NativeBuildingControls();
        controls.refresh(root);
        require(controls.height() == 30 * 25, "Native list dropped off-viewport rows");
        var client = Minecraft.getInstance();
        var graphics = new GuiGraphics(client, client.renderBuffers().bufferSource());
        controls.render(graphics, 0, -29 * 25, 160, 0, 25, 10, 10);
        require(controls.click(10, 10) && dispatched[0] == 29, "Native last-row click lost its upstream list index");

        var fields = new View();
        var field = new TextField();
        field.setSize(160, 20);
        int[] changes = {0};
        field.setHandler(ignored -> changes[0]++);
        fields.addChild(field);
        controls.refresh(fields);
        controls.render(graphics, 0, 0, 160, 0, 25, 10, 10);
        controls.click(10, 10);
        controls.character('x', 0);
        require(field.getText().equals("x") && changes[0] == 1, "Native input failed to notify upstream");

        var choicesRoot = new BOWindow(ResourceLocation.fromNamespaceAndPath("minecoloniestweaks", "gui/native_dropdown_smoke.xml"));
        var dropdown = choicesRoot.findPaneOfTypeByID("choices", DropDownList.class);
        dropdown.setDataProvider(new DropDownList.DataProvider() { @Override public int getElementCount() { return 3; } });
        int[] selected = {-1};
        dropdown.setHandler(value -> selected[0] = value.getSelectedIndex());

        controls.refresh(choicesRoot);
        controls.render(graphics, 0, 0, 160, 0, 25, 10, 10);
        controls.click(10, 10);
        controls.clickChoices(10, 35, 0, 0, 160, 80);
        require(selected[0] == 1, "Native dropdown did not notify upstream selection");

        var pages = new SwitchView();
        for (int i = 0; i < 2; i++) {
            var page = new View();
            var text = new Text();
            text.setText(Component.literal("Page " + i));
            page.addChild(text);
            pages.addChild(page);
        }
        controls.refresh(pages);
        require(controls.height() == 25, "Native controls included an inactive SwitchView page");
        graphics.flush();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
