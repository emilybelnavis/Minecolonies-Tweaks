package dev.minecoloniestweaks.names;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NamePoolTest {
    @Test void selectsNamesFromTheCorrectGenderPool() {
        var pool = new NamePool(List.of("Jean"), List.of("Jeanne"), List.of("Tremblay"), false);
        assertEquals("Jean Tremblay", pool.generate(new Random(1), false));
        assertEquals("Jeanne Tremblay", pool.generate(new Random(1), true));
    }

    @Test void preservesSurnameFirstOrder() {
        var pool = new NamePool(List.of("Wei"), List.of("Mei"), List.of("Wang"), true);
        assertEquals("Wang Wei", pool.generate(new Random(1), false));
        assertEquals("Wang", pool.surnameFrom("Wang Mei"));
    }

    @Test void boundedCollisionHandlingWorksEvenForASingleNamePool() {
        var pool = new NamePool(List.of("Jean"), List.of("Jeanne"), List.of("Roy"), false);
        assertEquals("Roy", pool.surnameFrom("Jean Roy 2"));
        assertEquals("Jean Roy 3", pool.generateUnique(new Random(1), false, Set.of("Jean Roy", "Jean Roy 2"), null));
    }

    @Test void childKeepsTheInheritedSurname() {
        var pool = new NamePool(List.of("Jean"), List.of("Jeanne"), List.of("Roy"), false);
        assertEquals("Jeanne Tremblay", pool.generateUnique(new Random(1), true, Set.of(), "Tremblay"));
    }

    @Test void recognizesCompoundCustomSurnames() {
        var pool = new NamePool(List.of("Jean"), List.of("Jeanne"), List.of("de la Tour"), false);
        assertEquals("de la Tour", pool.surnameFrom("Jeanne de la Tour"));
    }

    @Test void rejectsInvalidCustomData() {
        assertThrows(IllegalArgumentException.class, () -> new NamePool(List.of(), List.of("A"), List.of("B"), false));
        assertThrows(IllegalArgumentException.class, () -> new NamePool(null, List.of("A"), List.of("B"), false));
        assertThrows(IllegalArgumentException.class, () -> new NamePool(List.of(" "), List.of("A"), List.of("B"), false));
        assertThrows(IllegalArgumentException.class, () -> new NamePool(List.of("A\nB"), List.of("A"), List.of("B"), false));
        assertThrows(RuntimeException.class, () -> new Gson().fromJson("{\"male\":[],\"female\":[\"A\"],\"surnames\":[\"B\"]}", NamePool.class));
    }

    @Test void allSevenBundledLocalesAreValidAndGenerateNames() throws Exception {
        for (String locale : List.of("en_ca", "en_us", "en_gb", "fr_ca", "fr_fr", "zh_cn_latn", "zh_tw_latn")) {
            try (var stream = getClass().getResourceAsStream("/data/minecoloniestweaks/names/" + locale + ".json")) {
                assertNotNull(stream, locale);
                var pool = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), NamePool.class);
                assertEquals(locale.startsWith("zh_"), pool.surnameFirst(), locale);
                assertFalse(pool.generate(new Random(42), true).isBlank());
                assertFalse(pool.generate(new Random(42), false).isBlank());
            }
        }
    }
}
