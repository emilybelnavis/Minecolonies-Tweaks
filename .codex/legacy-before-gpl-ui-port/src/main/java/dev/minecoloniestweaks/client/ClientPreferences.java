package dev.minecoloniestweaks.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;

/** Client preferences stay in the game config directory, never in a save's server config. */
public final class ClientPreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("minecoloniestweaks-client.json");
    public static final ClientPreferences INSTANCE = load();

    public boolean hudVisible = true;
    public boolean beamsVisible = true;
    public boolean showSatisfied = false;
    public boolean showInventory = true;
    public int panelWidth = 230;
    public int maxRows = 10;
    public double hudScale = 1;
    public int beamSeconds = 120;
    public Set<String> disabledScreens = new HashSet<>();
    public Map<String, List<PinnedBuilder>> profiles = new HashMap<>();

    private static ClientPreferences load() {
        if (Files.isRegularFile(FILE)) {
            try (var reader = Files.newBufferedReader(FILE)) {
                var result = GSON.fromJson(reader, ClientPreferences.class);
                if (result != null) {
                    if (result.disabledScreens == null) result.disabledScreens = new HashSet<>();
                    if (result.profiles == null) result.profiles = new HashMap<>();
                    result.panelWidth = Math.clamp(result.panelWidth, 160, 400);
                    result.maxRows = Math.clamp(result.maxRows, 3, 30);
                    result.hudScale = Double.isFinite(result.hudScale) ? Math.clamp(result.hudScale, 0.5, 2) : 1;
                    result.beamSeconds = Math.clamp(result.beamSeconds, 10, 3600);
                    result.profiles.values().removeIf(Objects::isNull);
                    result.profiles.values().forEach(list -> {
                        list.removeIf(pin -> pin == null || pin.dimension == null || pin.colony <= 0
                                || ResourceLocation.tryParse(pin.dimension) == null);
                        for (var pin : list) {
                            pin.x = Double.isFinite(pin.x) ? Math.clamp(pin.x, 0, 1) : 0.02;
                            pin.y = Double.isFinite(pin.y) ? Math.clamp(pin.y, 0, 1) : 0.06;
                        }
                        if (list.size() > 16) list.subList(16, list.size()).clear();
                    });
                    return result;
                }
            } catch (IOException | RuntimeException exception) {
                LogUtils.getLogger().warn("Cannot read MineColonies Tweaks client preferences", exception);
            }
        }
        return new ClientPreferences();
    }

    public List<PinnedBuilder> pins() {
        var client = Minecraft.getInstance();
        String profile;
        if (client.getCurrentServer() != null) {
            profile = "server:" + client.getCurrentServer().ip;
        } else if (client.getSingleplayerServer() != null) {
            profile = "world:" + client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).normalize().getParent().getFileName();
        } else {
            return List.of();
        }
        return profiles.computeIfAbsent(profile, key -> new ArrayList<>());
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            var temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(this));
            Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            LogUtils.getLogger().error("Cannot save MineColonies Tweaks client preferences", exception);
        }
    }

    public static final class PinnedBuilder {
        public String dimension;
        public int colony;
        public long position;
        public double x = 0.02;
        public double y = 0.06;

        public PinnedBuilder(String dimension, int colony, long position) {
            this.dimension = dimension;
            this.colony = colony;
            this.position = position;
        }
    }
}
