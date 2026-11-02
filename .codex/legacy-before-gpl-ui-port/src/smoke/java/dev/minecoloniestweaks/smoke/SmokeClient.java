package dev.minecoloniestweaks.smoke;

import com.ldtteam.blockui.views.BOWindow;
import com.ldtteam.domumornamentum.container.ArchitectsCutterContainer;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.jobs.ModJobs;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.entity.ModEntities;
import com.minecolonies.api.research.IGlobalResearchTree;
import com.minecolonies.core.client.gui.WindowResourceList;
import com.minecolonies.core.client.gui.citizen.MainWindowCitizen;
import com.minecolonies.core.debug.gui.DebugWindowCitizen;
import com.minecolonies.core.client.gui.huts.WindowHutBuilderModule;
import com.minecolonies.core.client.gui.modules.building.WindowBuilderResModule;
import com.minecolonies.core.client.gui.townhall.WindowMainPage;
import com.minecolonies.core.colony.CitizenData;
import com.minecolonies.core.colony.ColonyView;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.buildings.moduleviews.BuildingResourcesModuleView;
import com.minecolonies.core.colony.buildings.utils.BuildingBuilderResource;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCook;
import com.minecolonies.core.colony.jobs.JobCook;
import com.minecolonies.core.entity.ai.workers.CitizenAI;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenColonyHandler;
import com.mojang.logging.LogUtils;
import dev.minecoloniestweaks.client.locate.LocateBeams;
import dev.minecoloniestweaks.client.screen.CutterScreen;
import dev.minecoloniestweaks.client.screen.BuildingScreen;
import dev.minecoloniestweaks.client.screen.ResearchScreen;
import dev.minecoloniestweaks.client.screen.SettingsScreen;
import dev.minecoloniestweaks.client.screen.NativeControlsSmoke;
import dev.minecoloniestweaks.mixin.client.ResourceScrollAccess;
import dev.minecoloniestweaks.server.CookMeals;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Exercises real game classes in a new disposable world. Never included in the release jar. */
@EventBusSubscriber(modid = "minecoloniestweaks", value = Dist.CLIENT)
public final class SmokeClient {
    private static int phase;
    private static int ticks;
    private static CompletableFuture<Void> serverTests;
    private static int originalGuiScale = -1;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("minecoloniestweaks.smoke")) return;
        var client = Minecraft.getInstance();
        try {
            ticks++;
            if (phase == 0 && client.screen instanceof TitleScreen && ticks > 100) {
                phase = 1;
                client.getTutorial().setStep(TutorialSteps.NONE);
                var settings = new LevelSettings("Tweaks smoke test", GameType.CREATIVE, false,
                        Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                client.createWorldOpenFlows().createFreshLevel("smoke-" + System.currentTimeMillis(), settings,
                        new WorldOptions(42, false, false),
                        registry -> registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                        new TitleScreen());
            } else if (phase == 1 && client.player != null && client.level != null) {
                phase = 2;
                ticks = 0;
                serverTests = CompletableFuture.runAsync(SmokeClient::testCook, client.getSingleplayerServer());
                client.setScreen(new SettingsScreen(null, "root"));
            } else if (phase == 2 && ticks > 40 && serverTests.isDone()) {
                serverTests.join();
                testResourceScroll();
                capture("settings.png");
                phase = 3;
                ticks = 0;
                new BOWindow(ResourceLocation.fromNamespaceAndPath("minecolonies", "gui/windowconfirm.xml")).open();
            } else if (phase == 3 && ticks > 40) {
                capture("blockui-vanilla.png");
                phase = 4;
                ticks = 0;
                client.setScreen(new CutterScreen(new ArchitectsCutterContainer(0, client.player.getInventory()),
                        client.player.getInventory(), Component.literal("Architect's Cutter")));
            } else if (phase == 4 && ticks > 40) {
                capture("cutter.png");
                phase = 5;
                ticks = 0;
                var colony = ColonyView.createFromNetwork(1, client.level.dimension());
                var building = new BuildingBuilder.View(colony, client.player.blockPosition());
                var branches = IGlobalResearchTree.getInstance().getBranches();
                require(!branches.isEmpty(), "No research branches were synchronized");
                client.setScreen(new ResearchScreen(building, branches.getFirst(), null));
            } else if (phase == 5 && ticks > 40) {
                capture("research.png");
                phase = 6;
                ticks = 0;
                client.setScreen(null);
                LocateBeams.block("Smoke hut", client.player.blockPosition().south(8), false);
            } else if (phase == 6 && ticks > 40) {
                capture("beams.png");
                NativeControlsSmoke.run();
                phase = 7;
                ticks = 0;
                var builder = new BuildingBuilder.View(smokeColony(101), client.player.blockPosition());
                builder.setBuildingType(ModBuildings.builder.get());
                client.setScreen(new BuildingScreen(new WindowHutBuilderModule(builder, false), builder));
            } else if (phase == 7 && ticks > 40) {
                capture("native-builder-main.png");
                phase = 8;
                ticks = 0;
                var builder = new BuildingBuilder.View(smokeColony(102), client.player.blockPosition());
                builder.setBuildingType(ModBuildings.builder.get());
                var stone = new BuildingBuilderResource(new ItemStack(Items.COBBLESTONE), 12);
                stone.setAvailable(3);
                var resources = new BuildingResourcesModuleView() {
                    @Override public Map<String, BuildingBuilderResource> getResources() { return Map.of("stone", stone); }
                };
                resources.setProducer(BuildingModules.BUILDING_RESOURCES);
                builder.registerModule(resources);
                client.setScreen(new BuildingScreen(new WindowBuilderResModule(resources), builder));
            } else if (phase == 8 && ticks > 40) {
                capture("native-builder-resources.png");
                phase = 9;
                ticks = 0;
                var townHall = new com.minecolonies.core.colony.buildings.workerbuildings.BuildingTownHall.View(
                        smokeColony(103), client.player.blockPosition());
                townHall.setBuildingType(ModBuildings.townHall.get());
                client.setScreen(new BuildingScreen(new WindowMainPage(townHall), townHall));
            } else if (phase == 9 && ticks > 40) {
                capture("native-townhall.png");
                phase = 10;
                ticks = 0;
                var colony = smokeColony(104);
                var citizen = smokeCitizen(colony);
                originalGuiScale = client.options.guiScale().get();
                client.options.guiScale().set(1);
                client.resizeDisplay();
                client.setScreen(new dev.minecoloniestweaks.client.screen.CitizenScreen(new MainWindowCitizen(citizen), citizen));
            } else if (phase == 10 && ticks > 40) {
                capture("native-citizen-scale-1.png");
                phase = 11;
                ticks = 0;
                var colony = smokeColony(104);
                var citizen = smokeCitizen(colony);
                require(client.getWindow().getGuiScale() == 1, "GUI scale 1 was not applied");
                client.options.guiScale().set(2);
                client.resizeDisplay();
                client.setScreen(new dev.minecoloniestweaks.client.screen.CitizenScreen(new MainWindowCitizen(citizen), citizen));
            } else if (phase == 11 && ticks > 40) {
                require(client.getWindow().getGuiScale() == 2, "GUI scale 2 was not applied");
                capture("native-citizen-scale-2.png");
                phase = 12;
                ticks = 0;
                var colony = smokeColony(104);
                var citizen = smokeCitizen(colony);
                client.setScreen(new dev.minecoloniestweaks.client.screen.CitizenScreen(new DebugWindowCitizen(citizen), citizen));
            } else if (phase == 12 && ticks > 40) {
                capture("native-citizen-debug.png");
                phase = 13;
                ticks = 0;
                var screen = client.screen;
                require(screen instanceof dev.minecoloniestweaks.client.screen.CitizenScreen, "Debug page escaped native screen");
                screen.mouseClicked((screen.width - 260) / 2.0 + 20, (screen.height - 238) / 2.0 + 58, 0);
                var source = screen.getClass().getDeclaredField("source");
                source.setAccessible(true);
                require(source.get(screen) instanceof MainWindowCitizen, "Debug Back button did not return to overview");
            } else if (phase == 13 && ticks > 40) {
                capture("native-citizen-overview-return.png");
                client.options.guiScale().set(originalGuiScale);
                client.resizeDisplay();
                originalGuiScale = -1;
                phase = 14;
                ticks = 0;
                Files.writeString(client.gameDirectory.toPath().resolve("smoke-result.txt"), "PASS");
                LogUtils.getLogger().info("MCT_SMOKE_PASS: cook policy, names, settings, BlockUI skin, cutter, research, resource scroll adapter, beams, native builder, town hall, citizen scales and debug page");
                client.setScreen(null);
            } else if (phase == 14 && ticks > 40) {
                phase = 15;
                client.stop();
            }
        } catch (Throwable failure) {
            LogUtils.getLogger().error("MCT_SMOKE_FAIL", failure);
            phase = 8;
            if (originalGuiScale >= 0) client.options.guiScale().set(originalGuiScale);
            try {
                Files.writeString(client.gameDirectory.toPath().resolve("smoke-result.txt"), "FAIL: " + failure);
            } catch (IOException ignored) {}
            client.stop();
        }
    }

    private static void capture(String name) {
        var client = Minecraft.getInstance();
        Screenshot.grab(client.gameDirectory, name, client.getMainRenderTarget(), message -> {});
    }



    private static ColonyView smokeColony(int id) {
        var colony = ColonyView.createFromNetwork(id, Minecraft.getInstance().level.dimension());
        try {
            // Production views receive this through deserialize; these isolated UI fixtures do not.
            var dimension = ColonyView.class.getDeclaredField("dimensionId");
            dimension.setAccessible(true);
            dimension.set(colony, Minecraft.getInstance().level.dimension());
            var owner = colony.getPermissions().getClass().getDeclaredField("colonyOwner");
            owner.setAccessible(true);
            owner.set(colony.getPermissions(), Minecraft.getInstance().player.getUUID());
            colony.setTextureStyle("default");
            colony.setNameStyle("default");
            colony.setName("Tweaks QA");
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
        return colony;
    }

    private static ICitizenDataView smokeCitizen(com.minecolonies.api.colony.IColonyView colony) {
        var inventory = new com.minecolonies.api.inventory.InventoryCitizen("Smoke Citizen", true);
        var skills = new com.minecolonies.core.entity.citizen.citizenhandlers.CitizenSkillHandler();
        return (ICitizenDataView) Proxy.newProxyInstance(ICitizenDataView.class.getClassLoader(),
                new Class<?>[]{ICitizenDataView.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> "Smoke Citizen";
                    case "getJobComponent" -> Component.literal("Builder");
                    case "getColony" -> colony;
                    case "getColonyId" -> colony.getID();
                    case "getId" -> 9001;
                    case "getEntityId" -> Minecraft.getInstance().player.getId();
                    case "isFemale" -> false;
                    case "getHomeBuilding", "getWorkBuilding" -> BlockPos.ZERO;
                    case "getVisibleStatus" -> null;
                    case "getInventory" -> inventory;
                    case "getDisplayArmor" -> ItemStack.EMPTY;
                    case "getCitizenSkillHandler" -> skills;
                    case "getHealth", "getMaxHealth" -> 20.0;
                    case "getHappiness" -> 8.0;
                    case "getSaturation" -> 75.0;
                    case "getJob" -> "";
                    case "toString" -> "Smoke Citizen";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0F;
        if (type == long.class) return 0L;
        return null;
    }

    private static void testResourceScroll() {
        var client = Minecraft.getInstance();
        var colony = ColonyView.createFromNetwork(100, client.level.dimension());
        var builder = new BuildingBuilder.View(colony, client.player.blockPosition()) {
            @Override public Map<Integer, Collection<IToken<?>>> getOpenRequestsByCitizen() {
                return Map.of();
            }
        };
        var stone = new BuildingBuilderResource(new ItemStack(Items.COBBLESTONE), 12);
        stone.setAvailable(3);
        var resources = new BuildingResourcesModuleView() {
            @Override public Map<String, BuildingBuilderResource> getResources() {
                return Map.of("stone", stone);
            }
        };
        resources.setProducer(BuildingModules.BUILDING_RESOURCES);
        builder.registerModule(resources);
        var scroll = new WindowResourceList(builder, Map.of());
        var adapter = (ResourceScrollAccess) scroll;
        adapter.tweaks$refresh();
        require(adapter.tweaks$resources().size() == 1, "Resource Scroll adapter lost a resource");
        require(adapter.tweaks$resources().getFirst().getAmount() == 12, "Resource Scroll adapter changed requirements");
        require(adapter.tweaks$resources().getFirst().getAvailable() == 3, "Resource Scroll adapter changed hut stock");
        LogUtils.getLogger().info("MCT_RESOURCE_ADAPTER_PASS");
    }

    private static void testCook() {
        var server = Minecraft.getInstance().getSingleplayerServer();
        var level = server.overworld();
        var player = server.getPlayerList().getPlayers().getFirst();
        BlockPos position = player.blockPosition();
        var colony = IColonyManager.getInstance().createColony(level, position, player, "Tweaks QA", "medievaloak");
        var storagePosition = position.east();
        level.setBlockAndUpdate(storagePosition, Blocks.CHEST.defaultBlockState());
        var storage = level.getCapability(Capabilities.ItemHandler.BLOCK, storagePosition, null);
        require(storage != null, "Test restaurant chest has no inventory");
        var restaurant = new BuildingCook(colony, position) {
            @Override public List<BlockPos> getContainers() { return List.of(storagePosition); }
            @Override public int getBuildingLevel() { return 5; }
        };
        var data = new CitizenData(10000, colony) {
            @Override public IBuilding getWorkBuilding() { return restaurant; }
            @Override public IBuilding getHomeBuilding() { return restaurant; }
        };
        var cook = new EntityCitizen(ModEntities.CITIZEN, level);
        final var testColony = colony;
        cook.setCitizenColonyHandler(new CitizenColonyHandler(cook) {
            @Override public IColony getColonyOrRegister() { return testColony; }
            @Override public IColony getColony() { return testColony; }
            @Override public int getColonyId() { return testColony.getID(); }
        });
        data.initForNewCivilian();
        data.setEntity(cook);
        cook.setCivilianData(data);
        cook.setPos(position.getX(), position.getY(), position.getZ());
        var job = new JobCook(data);
        job.setRegistryEntry(ModJobs.cook.get());
        data.setJob(job);
        data.setSaturation(0);
        var inventory = data.getInventory();
        inventory.setStackInSlot(0, new ItemStack(Items.BEEF, 2));
        inventory.setStackInSlot(1, new ItemStack(Items.BREAD, 2));
        inventory.setStackInSlot(2, new ItemStack(Items.COOKED_BEEF, 2));
        CookMeals.feedIfHungry(cook);
        require(inventory.getStackInSlot(0).getCount() == 2, "Cook ate raw beef");
        require(inventory.getStackInSlot(1).getCount() == 2, "Cook skipped the better cooked meal");
        require(inventory.getStackInSlot(2).getCount() == 1, "Cook did not eat exactly one cooked meal");
        require(data.getSaturation() > 0, "Meal did not restore saturation");
        inventory.setStackInSlot(1, ItemStack.EMPTY);
        inventory.setStackInSlot(2, ItemStack.EMPTY);
        data.setSaturation(0);
        cook.getPersistentData().remove("minecoloniestweaksNextMeal");
        storage.insertItem(0, new ItemStack(Items.COOKED_COD, 2), false);
        CookMeals.feedIfHungry(cook);
        require(storage.getStackInSlot(0).getCount() == 1, "Cook did not eat from restaurant storage");
        require(data.getSaturation() > 0, "Restaurant meal did not restore saturation");
        data.setSaturation(0);
        cook.getPersistentData().remove("minecoloniestweaksNextMeal");
        cook.setPos(position.getX() + 100, position.getY(), position.getZ());
        CookMeals.feedIfHungry(cook);
        require(storage.getStackInSlot(0).getCount() == 1, "Cook took restaurant food remotely");
        storage.extractItem(0, 64, false);
        cook.setPos(position.getX(), position.getY(), position.getZ());
        data.setSaturation(0);
        require(!new CitizenAI(cook).shouldEat(), "Cook entered the restaurant waiting loop");
        require(inventory.getStackInSlot(0).getCount() == 2, "No-meal path consumed raw food");
        data.setGenderAndGenerateName(false);
        require(data.getName() != null && !data.getName().isBlank(), "Custom name generation failed");
        LogUtils.getLogger().info("MCT_SERVER_POLICY_PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
