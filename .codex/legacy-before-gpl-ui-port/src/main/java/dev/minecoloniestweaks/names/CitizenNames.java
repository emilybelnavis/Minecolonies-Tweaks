package dev.minecoloniestweaks.names;

import com.google.gson.Gson;
import com.mojang.logging.LogUtils;
import dev.minecoloniestweaks.config.ServerConfig;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.loading.FMLPaths;

public final class CitizenNames {
    public static final Set<String> LOCALES = Set.of("en_ca", "en_us", "en_gb", "fr_ca", "fr_fr", "zh_cn_latn", "zh_tw_latn");
    private static final Gson GSON = new Gson();
    private static final Map<String, CachedPool> CACHE = new HashMap<>();

    private CitizenNames() {}

    public static synchronized NamePool current() {
        String locale = ServerConfig.NAME_LOCALE.get().toLowerCase(Locale.ROOT);
        if (!LOCALES.contains(locale)) {
            locale = "en_ca";
        }
        var path = FMLPaths.CONFIGDIR.get().resolve("minecoloniestweaks/names/" + locale + ".json");
        long modified = 0;
        try {
            if (Files.isRegularFile(path)) {
                modified = Files.getLastModifiedTime(path).toMillis();
            }
            var cached = CACHE.get(locale);
            if (cached != null && cached.modified() == modified) {
                return cached.pool();
            }
            NamePool pool;
            if (modified != 0) {
                try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    if (Files.size(path) > 2_000_000) {
                        throw new IOException("Name list exceeds 2 MB");
                    }
                    pool = GSON.fromJson(reader, NamePool.class);
                    if (pool == null) throw new IOException("Name list is empty");
                }
            } else {
                pool = builtin(locale);
            }
            CACHE.put(locale, new CachedPool(modified, pool));
            return pool;
        } catch (IOException | RuntimeException exception) {
            LogUtils.getLogger().warn("Cannot load name list {}; using the bundled list", path, exception);
            NamePool fallback = builtin(locale);
            CACHE.put(locale, new CachedPool(modified, fallback));
            return fallback;
        }
    }

    private static NamePool builtin(String locale) {
        String resource = "/data/minecoloniestweaks/names/" + locale + ".json";
        try (var stream = CitizenNames.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing name list: " + resource);
            return GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), NamePool.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read bundled name list", exception);
        }
    }

    private record CachedPool(long modified, NamePool pool) {}
}
