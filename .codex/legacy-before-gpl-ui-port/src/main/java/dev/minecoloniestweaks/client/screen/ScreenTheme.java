package dev.minecoloniestweaks.client.screen;

import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.views.BOWindow;
import dev.minecoloniestweaks.client.ClientPreferences;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A shared skin keeps upstream actions, permission checks, and dynamically added pages intact. */
public final class ScreenTheme {
    public static final Set<String> SCREENS = new TreeSet<>();
    private static final Set<String> NAMESPACES = Set.of("minecolonies", "structurize", "domum_ornamentum");

    private static boolean citizenContent;

    public static void beginCitizenContent() { citizenContent = true; }
    public static void endCitizenContent() { citizenContent = false; }
    public static boolean isCitizenContent() { return citizenContent; }

    private ScreenTheme() {}

    public static String key(BOWindow window) {
        var xml = window.getXmlResourceLocation();
        return xml == null ? "" : xml.toString();
    }

    private static final Map<Pane, ResourceLocation> SOURCES = new WeakHashMap<>();

    public static void remember(Pane pane, ResourceLocation source) {
        if (pane != null && NAMESPACES.contains(source.getNamespace())) {
            SOURCES.put(pane, source);
            SCREENS.add(source.toString());
        }
    }

    public static boolean enabled(Pane pane) {
        boolean supported = false;
        for (Pane current = pane; current != null; current = current.getParent()) {
            ResourceLocation source = SOURCES.get(current);
            if (source == null && current instanceof BOWindow window) source = window.getXmlResourceLocation();
            if (source == null || !NAMESPACES.contains(source.getNamespace())) continue;
            supported = true;
            if (ClientPreferences.INSTANCE.disabledScreens.contains(source.toString())) return false;
        }
        var window = pane.getWindow();
        if (window != null && window.getXmlResourceLocation() != null) {
            var source = window.getXmlResourceLocation();
            if (NAMESPACES.contains(source.getNamespace())) {
                supported = true;
                if (ClientPreferences.INSTANCE.disabledScreens.contains(source.toString())) return false;
            }
        }
        return supported;
    }

    public static Component label(String key) {
        String translation = "screen.minecoloniestweaks.catalog." + key.replace(':', '.').replace('/', '.').replace(".xml", "");
        return I18n.exists(translation)
                ? Component.translatable(translation)
                : Component.literal("Colony window");
    }

    public static boolean isBackdrop(Pane pane, ResourceLocation image) {
        if (image == null || !NAMESPACES.contains(image.getNamespace())) return false;
        String path = image.getPath();
        return path.contains("background") || path.contains("paper") || path.contains("papper") || path.contains("clipboard") || path.contains("dialog") || path.contains("window")
                || path.contains("builderhut/builderhut") || path.contains("townhall/townhall")
                || (pane.getID().toLowerCase(Locale.ROOT).contains("background"));
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        roundedFill(graphics, x, y, width, height, 4, 0xFF373737);
        roundedFill(graphics, x + 1, y + 1, width - 2, height - 2, 3, 0xFFFFFFFF);
        roundedFill(graphics, x + 2, y + 2, width - 3, height - 3, 3, 0xFF555555);
        roundedFill(graphics, x + 2, y + 2, width - 5, height - 5, 2, 0xFFC6C6C6);
    }

    /** Small stepped corners retain Minecraft's pixel-art appearance at every GUI scale. */
    public static void roundedFill(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        int corner = Math.min(radius, Math.min(width, height) / 2);
        graphics.fill(x, y + corner, x + width, y + height - corner, color);
        for (int row = 0; row < corner; row++) {
            int inset = corner - row;
            graphics.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
            graphics.fill(x + inset, y + height - row - 1, x + width - inset, y + height - row, color);
        }
    }
}
