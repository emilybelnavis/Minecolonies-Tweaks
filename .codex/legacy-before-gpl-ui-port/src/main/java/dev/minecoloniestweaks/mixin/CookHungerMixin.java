package dev.minecoloniestweaks.mixin;

import com.minecolonies.core.colony.jobs.JobCook;
import com.minecolonies.core.entity.ai.workers.CitizenAI;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import dev.minecoloniestweaks.config.ServerConfig;
import dev.minecoloniestweaks.server.CookMeals;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CitizenAI.class, remap = false)
public abstract class CookHungerMixin {
    @Shadow @Final private EntityCitizen citizen;

    @Inject(method = "shouldEat", at = @At("HEAD"), cancellable = true)
    private void letCookFeedThemselves(CallbackInfoReturnable<Boolean> callback) {
        if (ServerConfig.COOK_SELF_FEEDING.get() && citizen.getCitizenData().getJob() instanceof JobCook) {
            CookMeals.feedIfHungry(citizen);
            // Waiting for another cook creates a deadlock. Continue normal cooking when no meal is ready.
            callback.setReturnValue(false);
        }
    }
}
