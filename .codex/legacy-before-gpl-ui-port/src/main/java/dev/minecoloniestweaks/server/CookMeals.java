package dev.minecoloniestweaks.server;

import com.minecolonies.api.items.IMinecoloniesFoodItem;
import com.minecolonies.api.items.ModTags;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCook;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/** Only the assigned cook uses this policy; other citizens retain MineColonies food rules. */
public final class CookMeals {
    private static final TagKey<Item> PREPARED_FOOD = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("minecoloniestweaks", "prepared_food"));

    private CookMeals() {}

    public static void feedIfHungry(EntityCitizen citizen) {
        var data = citizen.getCitizenData();
        if (data.getSaturation() >= 60 || data.isAsleep()) return;
        if (!(data.getWorkBuilding() instanceof BuildingCook restaurant)) return;
        long now = citizen.level().getGameTime();
        var memory = citizen.getPersistentData();
        if (now < memory.getLong("minecoloniestweaksNextMeal")) return;
        memory.putLong("minecoloniestweaksNextMeal", now + 20);

        List<IItemHandler> inventories = new ArrayList<>();
        inventories.add(citizen.getInventoryCitizen());
        // Never extract restaurant food remotely. Normal work navigation brings the cook back here.
        if (restaurant.getPosition().distSqr(citizen.blockPosition()) <= 64) {
            var positions = new HashSet<>(restaurant.getContainers());
            positions.add(restaurant.getPosition());
            for (var position : positions) {
                if (!citizen.level().hasChunkAt(position)) continue;
                var handler = citizen.level().getCapability(Capabilities.ItemHandler.BLOCK, position, null);
                if (handler != null) inventories.add(handler);
            }
        }
        Meal best = null;
        for (var inventory : inventories) {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!isPrepared(stack, citizen)) continue;
                double value = FoodUtils.getFoodValue(stack, citizen);
                if (best == null || value > best.value()) {
                    best = new Meal(inventory, slot, value);
                }
            }
        }
        if (best == null) return; // Leave the cook's work AI free to light furnaces and prepare food.
        ItemStack meal = best.inventory().extractItem(best.slot(), 1, false);
        if (meal.isEmpty()) return;
        data.getCitizenFoodHandler().addLastEaten(meal.getItem());
        ItemStackUtils.consumeFood(meal, citizen, null);
        // consumeFood handles nutrition and container items; any unconsumed remainder goes back.
        if (!meal.isEmpty()) {
            ItemStack remainder = best.inventory().insertItem(best.slot(), meal, false);
            if (!remainder.isEmpty()) citizen.spawnAtLocation(remainder);
        }
        data.setJustAte(data.getSaturation() >= 60);
    }

    private static boolean isPrepared(ItemStack stack, EntityCitizen citizen) {
        if (stack.isEmpty() || !FoodUtils.EDIBLE.test(stack)) return false;
        if (stack.is(ModTags.poisonous_food)) return false;
        if (stack.is(PREPARED_FOOD)) return true;
        if (ItemStackUtils.ISCOOKABLE.test(stack)) return false;
        if (stack.getItem() instanceof IMinecoloniesFoodItem food && food.getTier() > 0) return true;
        var level = citizen.level();
        return level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING).stream()
                .anyMatch(recipe -> ItemStack.isSameItemSameComponents(recipe.value().getResultItem(level.registryAccess()), stack))
                || level.getRecipeManager().getAllRecipesFor(RecipeType.SMOKING).stream()
                .anyMatch(recipe -> ItemStack.isSameItemSameComponents(recipe.value().getResultItem(level.registryAccess()), stack))
                || level.getRecipeManager().getAllRecipesFor(RecipeType.CAMPFIRE_COOKING).stream()
                .anyMatch(recipe -> ItemStack.isSameItemSameComponents(recipe.value().getResultItem(level.registryAccess()), stack));
    }

    private record Meal(IItemHandler inventory, int slot, double value) {}
}
