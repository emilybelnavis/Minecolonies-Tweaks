package dev.minecoloniestweaks.client.screen;

import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.api.inventory.container.ContainerCitizenInventory;
import com.minecolonies.core.client.gui.citizen.MainWindowCitizen;
import com.minecolonies.core.client.gui.containers.WindowCitizenInventory;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Keeps the upstream container intact and provides a route back to the citizen. */
public final class CitizenInventoryScreen extends WindowCitizenInventory {
    private final ICitizenDataView citizen;

    public CitizenInventoryScreen(ContainerCitizenInventory menu, Inventory inventory, Component title,
                                  ICitizenDataView citizen) {
        super(menu, inventory, title);
        this.citizen = citizen;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("citizen.minecoloniestweaks.back"), button -> {
            // Close the server container first, including any item held by the cursor.
            onClose();
            var current = citizen.getColony().getCitizen(citizen.getId());
            if (current != null) new MainWindowCitizen(current).open();
        }).bounds(leftPos, Math.max(4, topPos - 24), 120, 20).build());
    }
}
