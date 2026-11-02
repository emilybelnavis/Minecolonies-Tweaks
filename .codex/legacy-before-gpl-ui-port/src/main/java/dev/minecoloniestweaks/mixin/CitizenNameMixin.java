package dev.minecoloniestweaks.mixin;

import com.minecolonies.api.colony.CitizenNameFile;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.CitizenData;
import dev.minecoloniestweaks.config.ServerConfig;
import dev.minecoloniestweaks.names.CitizenNames;
import java.util.Random;
import java.util.stream.Collectors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CitizenData.class, remap = false)
public abstract class CitizenNameMixin {
    @Inject(method = "generateName(Ljava/util/Random;ZLcom/minecolonies/api/colony/IColony;Lcom/minecolonies/api/colony/CitizenNameFile;)Ljava/lang/String;",
            at = @At("HEAD"), cancellable = true)
    private static void generateCustomName(Random random, boolean female, IColony colony, CitizenNameFile source,
                                           CallbackInfoReturnable<String> callback) {
        if (!colony.isRemote() && ServerConfig.CUSTOM_NAMES.get()) {
            var occupied = colony.getCitizenManager().getCitizens().stream()
                    .map(ICitizenData::getName).collect(Collectors.toSet());
            callback.setReturnValue(CitizenNames.current().generateUnique(random, female, occupied, null));
        }
    }

    @Inject(method = "generateName(Ljava/util/Random;Ljava/lang/String;Ljava/lang/String;Lcom/minecolonies/api/colony/CitizenNameFile;)V",
            at = @At("HEAD"), cancellable = true)
    private void generateChildName(Random random, String firstParent, String secondParent, CitizenNameFile source,
                                   CallbackInfo callback) {
        var citizen = (CitizenData) (Object) this;
        if (citizen.getColony().isRemote() || !ServerConfig.CUSTOM_NAMES.get()) return;
        var pool = CitizenNames.current();
        String surname = pool.surnameFrom(random.nextBoolean() ? firstParent : secondParent);
        var occupied = citizen.getColony().getCitizenManager().getCitizens().stream()
                .filter(other -> other.getId() != citizen.getId())
                .map(ICitizenData::getName).collect(Collectors.toSet());
        citizen.setName(pool.generateUnique(random, citizen.isFemale(), occupied, surname));
        callback.cancel();
    }
}
