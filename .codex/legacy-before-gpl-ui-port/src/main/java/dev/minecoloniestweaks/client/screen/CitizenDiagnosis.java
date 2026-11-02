package dev.minecoloniestweaks.client.screen;

import com.minecolonies.api.colony.ICitizenDataView;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class CitizenDiagnosis {
    private CitizenDiagnosis() {}

    public static List<Component> explain(ICitizenDataView citizen) {
        var reasons = new ArrayList<Component>();
        if (citizen.isChild()) reasons.add(Component.translatable("diagnosis.minecoloniestweaks.child"));
        if (citizen.isPaused()) reasons.add(Component.translatable("diagnosis.minecoloniestweaks.paused"));
        if (citizen.isSick()) reasons.add(Component.translatable("diagnosis.minecoloniestweaks.sick"));
        if (citizen.getSaturation() < 10) reasons.add(Component.translatable("diagnosis.minecoloniestweaks.hungry"));
        if (citizen.getWorkBuilding() == null || citizen.getWorkBuilding().equals(BlockPos.ZERO)) {
            reasons.add(Component.translatable("diagnosis.minecoloniestweaks.unemployed"));
        }
        if (citizen.hasBlockingInteractions()) reasons.add(Component.translatable("diagnosis.minecoloniestweaks.interaction"));
        if (citizen.getVisibleStatus() != null) reasons.add(Component.translatable(citizen.getVisibleStatus().getTranslationKey()));
        if (reasons.isEmpty()) reasons.add(Component.translatable("diagnosis.minecoloniestweaks.no_blocker"));
        return reasons;
    }
}
