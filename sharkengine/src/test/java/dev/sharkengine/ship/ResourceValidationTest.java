package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates mod resource files are at correct paths and contain required data.
 * Prevents regressions like Bug #2 (tag file at wrong directory path).
 */
@DisplayName("Resource Validation Tests")
class ResourceValidationTest {

    private static final Path RESOURCES_ROOT = Path.of("src/main/resources");

    /** Extract all JSON string keys from a simple flat JSON object. */
    private static Set<String> extractJsonKeys(String json) {
        Set<String> keys = new TreeSet<>();
        // Matches "key": "value" patterns in flat JSON
        Matcher m = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"").matcher(json);
        while (m.find()) {
            keys.add(m.group(1));
        }
        return keys;
    }

    /** Extract value for a given key from flat JSON. */
    private static String extractJsonValue(String json, String key) {
        String escaped = Pattern.quote(key);
        Matcher m = Pattern.compile("\"" + escaped + "\"\\s*:\\s*\"([^\"]*?)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    @Nested
    @DisplayName("Block Tag Paths (Bug #2 regression guard)")
    class TagPathTests {

        @Test
        @DisplayName("REGRESSION: ship_eligible tag uses singular 'block' directory (MC 1.21.1)")
        void tagUseSingularBlockDirectory() {
            // MC 1.21.1 requires data/<namespace>/tags/block/ (singular)
            // Bug #2 was caused by using the plural 'blocks' directory
            Path correctPath = RESOURCES_ROOT.resolve(
                    "data/sharkengine/tags/block/ship_eligible.json");
            assertTrue(Files.exists(correctPath),
                    "Tag file must exist at data/sharkengine/tags/block/ship_eligible.json (singular 'block')");
        }

        @Test
        @DisplayName("REGRESSION: plural 'blocks' directory must NOT exist")
        void noPluralBlocksDirectory() {
            // Ensure the old buggy path is gone
            Path wrongPath = RESOURCES_ROOT.resolve(
                    "data/sharkengine/tags/blocks");
            assertFalse(Files.exists(wrongPath),
                    "Plural 'blocks' directory must not exist — MC 1.21.1 uses singular 'block'");
        }

        @Test
        @DisplayName("ship_eligible.json contains steering_wheel")
        void tagContainsSteeringWheel() throws IOException {
            Path tagFile = RESOURCES_ROOT.resolve(
                    "data/sharkengine/tags/block/ship_eligible.json");
            String content = Files.readString(tagFile, StandardCharsets.UTF_8);
            assertTrue(content.contains("sharkengine:steering_wheel"),
                    "ship_eligible tag must include steering_wheel block");
        }

        @Test
        @DisplayName("ship_eligible.json contains thruster")
        void tagContainsThruster() throws IOException {
            Path tagFile = RESOURCES_ROOT.resolve(
                    "data/sharkengine/tags/block/ship_eligible.json");
            String content = Files.readString(tagFile, StandardCharsets.UTF_8);
            assertTrue(content.contains("sharkengine:thruster"),
                    "ship_eligible tag must include thruster block");
        }

        @Test
        @DisplayName("ship_eligible.json contains building material tags")
        void tagContainsBuildingMaterials() throws IOException {
            Path tagFile = RESOURCES_ROOT.resolve(
                    "data/sharkengine/tags/block/ship_eligible.json");
            String content = Files.readString(tagFile, StandardCharsets.UTF_8);
            assertTrue(content.contains("#minecraft:planks"),
                    "ship_eligible tag must include planks block tag");
            assertTrue(content.contains("#minecraft:logs"),
                    "ship_eligible tag must include logs block tag");
        }
    }

    @Nested
    @DisplayName("Localization Completeness")
    class LocalizationTests {

        @Test
        @DisplayName("English localization file exists")
        void englishLocalizationExists() {
            Path enFile = RESOURCES_ROOT.resolve(
                    "assets/sharkengine/lang/en_us.json");
            assertTrue(Files.exists(enFile),
                    "English localization file must exist");
        }

        @Test
        @DisplayName("German localization file exists")
        void germanLocalizationExists() {
            Path deFile = RESOURCES_ROOT.resolve(
                    "assets/sharkengine/lang/de_de.json");
            assertTrue(Files.exists(deFile),
                    "German localization file must exist");
        }

        @Test
        @DisplayName("German localization has all keys from English")
        void germanHasAllEnglishKeys() throws IOException {
            Path enFile = RESOURCES_ROOT.resolve("assets/sharkengine/lang/en_us.json");
            Path deFile = RESOURCES_ROOT.resolve("assets/sharkengine/lang/de_de.json");

            String enContent = Files.readString(enFile, StandardCharsets.UTF_8);
            String deContent = Files.readString(deFile, StandardCharsets.UTF_8);

            Set<String> enKeys = extractJsonKeys(enContent);
            Set<String> deKeys = extractJsonKeys(deContent);

            Set<String> missingInDe = new TreeSet<>(enKeys);
            missingInDe.removeAll(deKeys);

            assertTrue(missingInDe.isEmpty(),
                    "German localization is missing keys: " + missingInDe);
        }

        @Test
        @DisplayName("German localization values are not empty")
        void germanValuesNotEmpty() throws IOException {
            Path deFile = RESOURCES_ROOT.resolve("assets/sharkengine/lang/de_de.json");
            String deContent = Files.readString(deFile, StandardCharsets.UTF_8);
            Set<String> deKeys = extractJsonKeys(deContent);

            for (String key : deKeys) {
                String value = extractJsonValue(deContent, key);
                assertNotNull(value, "German translation for '" + key + "' must exist");
                assertFalse(value.isBlank(),
                        "German translation for '" + key + "' must not be blank");
            }
        }

        @Test
        @DisplayName("German localization values differ from English (actually translated)")
        void germanValuesAreTranslated() throws IOException {
            Path enFile = RESOURCES_ROOT.resolve("assets/sharkengine/lang/en_us.json");
            Path deFile = RESOURCES_ROOT.resolve("assets/sharkengine/lang/de_de.json");

            String enContent = Files.readString(enFile, StandardCharsets.UTF_8);
            String deContent = Files.readString(deFile, StandardCharsets.UTF_8);
            Set<String> enKeys = extractJsonKeys(enContent);

            int identicalCount = 0;
            int totalKeys = enKeys.size();

            for (String key : enKeys) {
                String enVal = extractJsonValue(enContent, key);
                String deVal = extractJsonValue(deContent, key);
                if (enVal != null && enVal.equals(deVal)) {
                    identicalCount++;
                }
            }

            // Allow some keys to be identical (e.g., proper names, symbols)
            // but most should differ
            assertTrue(identicalCount < totalKeys / 2,
                    "Too many German translations identical to English (" +
                            identicalCount + "/" + totalKeys + "). Translations may be missing.");
        }
    }
}
