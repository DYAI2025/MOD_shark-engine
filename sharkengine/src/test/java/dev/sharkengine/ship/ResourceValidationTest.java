package dev.sharkengine.ship;

import dev.sharkengine.ship.part.VehiclePartDefinition;
import dev.sharkengine.ship.part.VehiclePartRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    /**
     * AIR-030: recipe/loot table/blockstate/model/tag/lang data for
     * thruster/steering_wheel/bug now comes from
     * {@code dev.sharkengine.datagen.SharkEngineDataGenerator} (run via
     * {@code ./gradlew runDatagen}) instead of being hand-written. Fabric
     * Loom's {@code fabricApi.configureDataGeneration()} default output dir
     * — verified against the pinned Loom 1.7-SNAPSHOT plugin — and adds it
     * as an extra {@code main} resources source directory automatically, so
     * this is a real (committed) resource root the packaged jar reads from,
     * not a build/ scratch directory. Textures (AIR-032) are NOT part of
     * this migration and stay under {@link #RESOURCES_ROOT}.
     */
    private static final Path GENERATED_ROOT = Path.of("src/main/generated");

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
            Path correctPath = GENERATED_ROOT.resolve(
                    "data/sharkengine/tags/block/ship_eligible.json");
            assertTrue(Files.exists(correctPath),
                    "Tag file must exist at src/main/generated/data/sharkengine/tags/block/ship_eligible.json "
                            + "(singular 'block') — run ./gradlew runDatagen");
        }

        @Test
        @DisplayName("REGRESSION: plural 'blocks' directory must NOT exist")
        void noPluralBlocksDirectory() {
            // Ensure the old buggy path is gone
            Path wrongPath = GENERATED_ROOT.resolve(
                    "data/sharkengine/tags/blocks");
            assertFalse(Files.exists(wrongPath),
                    "Plural 'blocks' directory must not exist — MC 1.21.1 uses singular 'block'");
        }

        @Test
        @DisplayName("ship_eligible.json contains steering_wheel")
        void tagContainsSteeringWheel() throws IOException {
            Path tagFile = GENERATED_ROOT.resolve(
                    "data/sharkengine/tags/block/ship_eligible.json");
            String content = Files.readString(tagFile, StandardCharsets.UTF_8);
            assertTrue(content.contains("sharkengine:steering_wheel"),
                    "ship_eligible tag must include steering_wheel block");
        }

        @Test
        @DisplayName("ship_eligible.json contains thruster")
        void tagContainsThruster() throws IOException {
            Path tagFile = GENERATED_ROOT.resolve(
                    "data/sharkengine/tags/block/ship_eligible.json");
            String content = Files.readString(tagFile, StandardCharsets.UTF_8);
            assertTrue(content.contains("sharkengine:thruster"),
                    "ship_eligible tag must include thruster block");
        }

        @Test
        @DisplayName("ship_eligible.json contains building material tags")
        void tagContainsBuildingMaterials() throws IOException {
            Path tagFile = GENERATED_ROOT.resolve(
                    "data/sharkengine/tags/block/ship_eligible.json");
            String content = Files.readString(tagFile, StandardCharsets.UTF_8);
            assertTrue(content.contains("#minecraft:planks"),
                    "ship_eligible tag must include planks block tag");
            assertTrue(content.contains("#minecraft:logs"),
                    "ship_eligible tag must include logs block tag");
        }
    }

    @Nested
    @DisplayName("Recipe & Loot Table Paths (AIR-002 regression guard)")
    class RecipeAndLootTableTests {

        private static final String[] CRAFTABLE_BLOCK_IDS = {"steering_wheel", "thruster", "bug"};

        @Test
        @DisplayName("REGRESSION: recipe directory uses singular 'recipe' (MC 1.21.1)")
        void recipeUsesSingularDirectory() {
            // MC 1.21 renamed data/<ns>/recipes/ to data/<ns>/recipe/ (singular)
            Path correctPath = GENERATED_ROOT.resolve("data/sharkengine/recipe");
            assertTrue(Files.isDirectory(correctPath),
                    "Recipes must live under data/sharkengine/recipe/ (singular) — run ./gradlew runDatagen");
        }

        @Test
        @DisplayName("REGRESSION: plural 'recipes' directory must NOT exist")
        void noPluralRecipesDirectory() {
            Path wrongPath = GENERATED_ROOT.resolve("data/sharkengine/recipes");
            assertFalse(Files.exists(wrongPath),
                    "Plural 'recipes' directory must not exist — MC 1.21.1 silently ignores it");
        }

        @Test
        @DisplayName("REGRESSION: loot table directory uses singular 'loot_table' (MC 1.21.1)")
        void lootTableUsesSingularDirectory() {
            // MC 1.21 renamed data/<ns>/loot_tables/ to data/<ns>/loot_table/ (singular)
            Path correctPath = GENERATED_ROOT.resolve("data/sharkengine/loot_table");
            assertTrue(Files.isDirectory(correctPath),
                    "Loot tables must live under data/sharkengine/loot_table/ (singular) — run ./gradlew runDatagen");
        }

        @Test
        @DisplayName("REGRESSION: plural 'loot_tables' directory must NOT exist")
        void noPluralLootTablesDirectory() {
            Path wrongPath = GENERATED_ROOT.resolve("data/sharkengine/loot_tables");
            assertFalse(Files.exists(wrongPath),
                    "Plural 'loot_tables' directory must not exist — MC 1.21.1 silently ignores it");
        }

        @Test
        @DisplayName("every craftable block has a recipe file")
        void everyCraftableBlockHasRecipe() {
            for (String id : CRAFTABLE_BLOCK_IDS) {
                Path recipeFile = GENERATED_ROOT.resolve("data/sharkengine/recipe/" + id + ".json");
                assertTrue(Files.exists(recipeFile),
                        "Missing recipe for '" + id + "' at src/main/generated/data/sharkengine/recipe/"
                                + id + ".json — run ./gradlew runDatagen");
            }
        }

        @Test
        @DisplayName("every registered block has a loot table")
        void everyBlockHasLootTable() {
            for (String id : CRAFTABLE_BLOCK_IDS) {
                Path lootFile = GENERATED_ROOT.resolve("data/sharkengine/loot_table/blocks/" + id + ".json");
                assertTrue(Files.exists(lootFile),
                        "Missing loot table for '" + id + "' at src/main/generated/data/sharkengine/loot_table/blocks/"
                                + id + ".json — run ./gradlew runDatagen");
            }
        }

        @Test
        @DisplayName("REGRESSION: recipe result uses 'id' not 'item' (MC 1.20.5+ format)")
        void recipeResultUsesIdField() throws IOException {
            for (String id : CRAFTABLE_BLOCK_IDS) {
                Path recipeFile = GENERATED_ROOT.resolve("data/sharkengine/recipe/" + id + ".json");
                if (!Files.exists(recipeFile)) {
                    continue; // covered by everyCraftableBlockHasRecipe
                }
                String content = Files.readString(recipeFile, StandardCharsets.UTF_8);
                int resultIdx = content.indexOf("\"result\"");
                assertTrue(resultIdx >= 0, "Recipe for '" + id + "' must declare a \"result\"");
                String afterResult = content.substring(resultIdx);
                int closeIdx = afterResult.indexOf('}');
                String resultBlock = afterResult.substring(0, closeIdx + 1);
                assertTrue(resultBlock.contains("\"id\""),
                        "Recipe result for '" + id + "' must use \"id\" (MC 1.20.5+ format), found: " + resultBlock);
                assertFalse(resultBlock.contains("\"item\""),
                        "Recipe result for '" + id + "' must not use pre-1.20.5 \"item\" key, found: " + resultBlock);
            }
        }

        @Test
        @DisplayName("REGRESSION: stale 1.21.2+ assets/items/ directory must NOT exist")
        void noVersionedItemsDirectory() {
            // assets/<ns>/items/ is a 1.21.2+ item-model-definition path; ignored on our 1.21.1 target
            Path wrongPath = RESOURCES_ROOT.resolve("assets/sharkengine/items");
            assertFalse(Files.exists(wrongPath),
                    "assets/sharkengine/items/ is a 1.21.2+ path not used on MC 1.21.1 — must not exist");
            Path wrongGeneratedPath = GENERATED_ROOT.resolve("assets/sharkengine/items");
            assertFalse(Files.exists(wrongGeneratedPath),
                    "assets/sharkengine/items/ must not exist in generated output either");
        }
    }

    @Nested
    @DisplayName("AIR-030 datagen migration (hand-written resources fully retired)")
    class DatagenMigrationTests {

        private static final String[] MIGRATED_BLOCK_IDS = {"steering_wheel", "thruster", "bug"};

        @Test
        @DisplayName("every migrated block has a generated blockstate")
        void everyMigratedBlockHasBlockState() {
            for (String id : MIGRATED_BLOCK_IDS) {
                Path blockState = GENERATED_ROOT.resolve("assets/sharkengine/blockstates/" + id + ".json");
                assertTrue(Files.exists(blockState),
                        "Missing generated blockstate for '" + id + "' — run ./gradlew runDatagen");
            }
        }

        @Test
        @DisplayName("every migrated block has a generated block model")
        void everyMigratedBlockHasBlockModel() {
            for (String id : MIGRATED_BLOCK_IDS) {
                Path blockModel = GENERATED_ROOT.resolve("assets/sharkengine/models/block/" + id + ".json");
                assertTrue(Files.exists(blockModel),
                        "Missing generated block model for '" + id + "' — run ./gradlew runDatagen");
            }
        }

        @Test
        @DisplayName("every migrated block has a generated item model")
        void everyMigratedBlockHasItemModel() {
            for (String id : MIGRATED_BLOCK_IDS) {
                Path itemModel = GENERATED_ROOT.resolve("assets/sharkengine/models/item/" + id + ".json");
                assertTrue(Files.exists(itemModel),
                        "Missing generated item model for '" + id + "' — run ./gradlew runDatagen");
            }
        }

        @Test
        @DisplayName("hand-written recipe/loot_table/tags directories no longer exist under src/main/resources")
        void handWrittenDataDirectoriesRemoved() {
            assertFalse(Files.exists(RESOURCES_ROOT.resolve("data/sharkengine/recipe")),
                    "data/sharkengine/recipe/ must no longer be hand-written — it is now datagen output");
            assertFalse(Files.exists(RESOURCES_ROOT.resolve("data/sharkengine/loot_table")),
                    "data/sharkengine/loot_table/ must no longer be hand-written — it is now datagen output");
            assertFalse(Files.exists(RESOURCES_ROOT.resolve("data/sharkengine/tags")),
                    "data/sharkengine/tags/ must no longer be hand-written — it is now datagen output");
        }

        @Test
        @DisplayName("hand-written blockstates/models/lang no longer exist under src/main/resources")
        void handWrittenAssetDirectoriesRemoved() {
            assertFalse(Files.exists(RESOURCES_ROOT.resolve("assets/sharkengine/blockstates")),
                    "assets/sharkengine/blockstates/ must no longer be hand-written — it is now datagen output");
            assertFalse(Files.exists(RESOURCES_ROOT.resolve("assets/sharkengine/models")),
                    "assets/sharkengine/models/ must no longer be hand-written — it is now datagen output");
            assertFalse(Files.exists(RESOURCES_ROOT.resolve("assets/sharkengine/lang")),
                    "assets/sharkengine/lang/ must no longer be hand-written — it is now datagen output");
        }
    }

    @Nested
    @DisplayName("Localization Completeness")
    class LocalizationTests {

        @Test
        @DisplayName("English localization file exists")
        void englishLocalizationExists() {
            Path enFile = GENERATED_ROOT.resolve(
                    "assets/sharkengine/lang/en_us.json");
            assertTrue(Files.exists(enFile),
                    "English localization file must exist");
        }

        @Test
        @DisplayName("German localization file exists")
        void germanLocalizationExists() {
            Path deFile = GENERATED_ROOT.resolve(
                    "assets/sharkengine/lang/de_de.json");
            assertTrue(Files.exists(deFile),
                    "German localization file must exist");
        }

        @Test
        @DisplayName("German localization has all keys from English")
        void germanHasAllEnglishKeys() throws IOException {
            Path enFile = GENERATED_ROOT.resolve("assets/sharkengine/lang/en_us.json");
            Path deFile = GENERATED_ROOT.resolve("assets/sharkengine/lang/de_de.json");

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
            Path deFile = GENERATED_ROOT.resolve("assets/sharkengine/lang/de_de.json");
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
            Path enFile = GENERATED_ROOT.resolve("assets/sharkengine/lang/en_us.json");
            Path deFile = GENERATED_ROOT.resolve("assets/sharkengine/lang/de_de.json");

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

    @Nested
    @DisplayName("Aircraft-Extension Texture Palette Conformance (AIR-032)")
    class TexturePaletteTests {

        /**
         * tools/asset-gen/palette.json is the single source of truth (see that
         * file's own header comment) — this reads the same file the Python
         * generator does, not a duplicated copy of the hex values, so the two
         * can never silently drift apart.
         */
        private static final Path PALETTE_JSON = Path.of("../tools/asset-gen/palette.json");
        private static final Path TEXTURES_ROOT = RESOURCES_ROOT.resolve("assets/sharkengine/textures");
        private static final int TEXTURE_SIZE = 16;

        private Set<Integer> loadPaletteRgb() throws IOException {
            String json = Files.readString(PALETTE_JSON, StandardCharsets.UTF_8);
            Set<Integer> rgb = new TreeSet<>();
            Matcher m = Pattern.compile("#([0-9a-fA-F]{6})").matcher(json);
            while (m.find()) {
                rgb.add(Integer.parseInt(m.group(1), 16));
            }
            return rgb;
        }

        private List<Path> discoverTextures() throws IOException {
            if (!Files.isDirectory(TEXTURES_ROOT)) {
                return List.of();
            }
            try (var stream = Files.walk(TEXTURES_ROOT)) {
                return stream.filter(p -> p.toString().endsWith(".png")).sorted().toList();
            }
        }

        @Test
        @DisplayName("palette.json is readable and non-empty")
        void paletteFileIsReadable() throws IOException {
            assertTrue(Files.exists(PALETTE_JSON), "tools/asset-gen/palette.json must exist");
            Set<Integer> rgb = loadPaletteRgb();
            assertFalse(rgb.isEmpty(), "palette.json must define at least one #RRGGBB color");
        }

        @Test
        @DisplayName("at least one aircraft-extension texture has been generated (AIR-032 GREEN gate)")
        void atLeastOneTextureExists() throws IOException {
            List<Path> textures = discoverTextures();
            assertFalse(textures.isEmpty(),
                    "No textures found under assets/sharkengine/textures/ — run "
                            + "`python3 tools/asset-gen/generate.py` to generate at least airframe_panel");
        }

        @Test
        @DisplayName("every generated texture is exactly 16x16")
        void everyTextureIsCorrectSize() throws IOException {
            for (Path texturePath : discoverTextures()) {
                BufferedImage img = ImageIO.read(texturePath.toFile());
                assertNotNull(img, texturePath + " could not be read as an image");
                assertEquals(TEXTURE_SIZE, img.getWidth(),
                        texturePath + " must be " + TEXTURE_SIZE + "px wide, was " + img.getWidth());
                assertEquals(TEXTURE_SIZE, img.getHeight(),
                        texturePath + " must be " + TEXTURE_SIZE + "px tall, was " + img.getHeight());
            }
        }

        @Test
        @DisplayName("every generated texture uses only palette.json colors (+ transparency)")
        void everyTextureUsesOnlyPaletteColors() throws IOException {
            Set<Integer> paletteRgb = loadPaletteRgb();

            for (Path texturePath : discoverTextures()) {
                BufferedImage img = ImageIO.read(texturePath.toFile());
                assertNotNull(img, texturePath + " could not be read as an image");

                for (int y = 0; y < img.getHeight(); y++) {
                    for (int x = 0; x < img.getWidth(); x++) {
                        int argb = img.getRGB(x, y);
                        int alpha = (argb >>> 24) & 0xFF;
                        if (alpha == 0) {
                            continue; // fully transparent pixels are exempt — no color to check
                        }
                        int rgb = argb & 0xFFFFFF;
                        assertTrue(paletteRgb.contains(rgb),
                                texturePath + " pixel (" + x + "," + y + ") uses color #"
                                        + String.format("%06x", rgb)
                                        + " which is not in palette.json — add it there first, "
                                        + "never hardcode a new color in a part script");
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("AIR-031: Resource contract — every ModBlocks entry")
    class PerBlockResourceContractTests {

        /**
         * Every block currently registered in {@code dev.sharkengine.content.ModBlocks}.
         *
         * <p>Kept as a hand-written literal, not derived by reflecting over {@code ModBlocks}
         * itself: that class references un-stubbed Minecraft/Fabric types
         * ({@code net.minecraft.world.level.block.Block}, {@code BuiltInRegistries},
         * {@code Item}, {@code BlockItem}, {@code ItemGroupEvents}, ...) that do not exist on
         * this {@code test} source set's compile/runtime classpath — only {@code main}/
         * {@code client} pull in the Loom-mapped Minecraft jar (see {@code build.gradle}'s
         * {@code testImplementation} block, which has no Minecraft dependency). Referencing —
         * or reflectively {@code Class.forName}-loading — {@code ModBlocks} here fails at
         * class-verification/link time, the same classpath constraint already documented for
         * {@code ShipBlueprint} (AIR-015) and the network payload roundtrip (AIR-022). This
         * list must therefore be kept in sync by hand whenever a block is added to
         * {@code ModBlocks} — same convention as the pre-existing {@code CRAFTABLE_BLOCK_IDS}
         * / {@code MIGRATED_BLOCK_IDS} arrays above and {@code VehiclePartRegistryTest}.</p>
         */
        private static final String[] ALL_BLOCK_IDS =
                {"bug", "steering_wheel", "thruster", "airframe_panel", "fuselage_frame", "helicopter_engine",
                        "rotor_hub", "rotor_blade", "landing_skid"};

        /** Subset of {@link #ALL_BLOCK_IDS} that has a crafting recipe. All nine do today. */
        private static final String[] CRAFTABLE_IDS =
                {"bug", "steering_wheel", "thruster", "airframe_panel", "fuselage_frame", "helicopter_engine",
                        "rotor_hub", "rotor_blade", "landing_skid"};

        private static final Path TEXTURES_ROOT = RESOURCES_ROOT.resolve("assets/sharkengine/textures");

        private String readGenerated(String relativePath) throws IOException {
            return Files.readString(GENERATED_ROOT.resolve(relativePath), StandardCharsets.UTF_8);
        }

        /** Extracts every value of a {@code "model": "..."} field from a blockstate JSON. */
        private static Set<String> extractModelRefs(String blockstateJson) {
            Set<String> models = new TreeSet<>();
            Matcher m = Pattern.compile("\"model\"\\s*:\\s*\"([^\"]+)\"").matcher(blockstateJson);
            while (m.find()) {
                models.add(m.group(1));
            }
            return models;
        }

        /**
         * Extracts the values of the top-level {@code "textures": { ... }} object from a
         * block model JSON (brace-matched, so it doesn't accidentally pick up unrelated
         * quoted strings elsewhere in the file, e.g. inside {@code "elements"}).
         */
        private static Set<String> extractTextureRefs(String modelJson) {
            int keyIdx = modelJson.indexOf("\"textures\"");
            if (keyIdx < 0) {
                return Set.of();
            }
            int braceStart = modelJson.indexOf('{', keyIdx);
            int depth = 0;
            int i = braceStart;
            for (; i < modelJson.length(); i++) {
                char c = modelJson.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        break;
                    }
                }
            }
            String texturesBlock = modelJson.substring(braceStart, i + 1);
            Set<String> refs = new TreeSet<>();
            Matcher m = Pattern.compile("\"[^\"]+\"\\s*:\\s*\"([^\"]+)\"").matcher(texturesBlock);
            while (m.find()) {
                refs.add(m.group(1));
            }
            return refs;
        }

        @Test
        @DisplayName("every registered block has a blockstate")
        void everyBlockHasBlockstate() {
            for (String id : ALL_BLOCK_IDS) {
                Path path = GENERATED_ROOT.resolve("assets/sharkengine/blockstates/" + id + ".json");
                assertTrue(Files.exists(path),
                        "Missing blockstate for '" + id + "' at " + path + " — run ./gradlew runDatagen");
            }
        }

        @Test
        @DisplayName("every registered block's blockstate resolves to an existing block model")
        void everyBlockModelResolves() throws IOException {
            for (String id : ALL_BLOCK_IDS) {
                Path blockstatePath = GENERATED_ROOT.resolve("assets/sharkengine/blockstates/" + id + ".json");
                assertTrue(Files.exists(blockstatePath),
                        "Missing blockstate for '" + id + "' — cannot resolve its block model");
                String blockstateJson = Files.readString(blockstatePath, StandardCharsets.UTF_8);
                Set<String> modelRefs = extractModelRefs(blockstateJson);
                assertFalse(modelRefs.isEmpty(),
                        "Blockstate for '" + id + "' declares no \"model\" references at all: " + blockstatePath);
                for (String modelRef : modelRefs) {
                    assertTrue(modelRef.startsWith("sharkengine:"),
                            "Block model reference '" + modelRef + "' for '" + id
                                    + "' must be sharkengine-namespaced");
                    String modelPath = modelRef.substring("sharkengine:".length());
                    Path modelFile = GENERATED_ROOT.resolve("assets/sharkengine/models/" + modelPath + ".json");
                    assertTrue(Files.exists(modelFile),
                            "Blockstate for '" + id + "' references model '" + modelRef
                                    + "' which does not exist at " + modelFile);
                }
            }
        }

        @Test
        @DisplayName("every registered block has an item model")
        void everyBlockHasItemModel() {
            for (String id : ALL_BLOCK_IDS) {
                Path path = GENERATED_ROOT.resolve("assets/sharkengine/models/item/" + id + ".json");
                assertTrue(Files.exists(path),
                        "Missing item model for '" + id + "' at " + path + " — run ./gradlew runDatagen");
            }
        }

        @Test
        @DisplayName("every registered block has a loot table")
        void everyBlockHasLootTable() {
            for (String id : ALL_BLOCK_IDS) {
                Path path = GENERATED_ROOT.resolve("data/sharkengine/loot_table/blocks/" + id + ".json");
                assertTrue(Files.exists(path),
                        "Missing loot table for '" + id + "' at " + path + " — run ./gradlew runDatagen");
            }
        }

        @Test
        @DisplayName("every craftable block has a recipe")
        void everyCraftableBlockHasRecipe() {
            for (String id : CRAFTABLE_IDS) {
                Path path = GENERATED_ROOT.resolve("data/sharkengine/recipe/" + id + ".json");
                assertTrue(Files.exists(path),
                        "Missing recipe for craftable block '" + id + "' at " + path + " — run ./gradlew runDatagen");
            }
        }

        @Test
        @DisplayName("every registered block has non-blank English and German block translations")
        void everyBlockHasTranslations() throws IOException {
            String enContent = readGenerated("assets/sharkengine/lang/en_us.json");
            String deContent = readGenerated("assets/sharkengine/lang/de_de.json");
            for (String id : ALL_BLOCK_IDS) {
                String key = "block.sharkengine." + id;
                String enVal = extractJsonValue(enContent, key);
                String deVal = extractJsonValue(deContent, key);
                assertNotNull(enVal, "Missing English translation key '" + key + "' in en_us.json");
                assertFalse(enVal.isBlank(), "English translation for '" + key + "' must not be blank");
                assertNotNull(deVal, "Missing German translation key '" + key + "' in de_de.json");
                assertFalse(deVal.isBlank(), "German translation for '" + key + "' must not be blank");
            }
        }

        @Test
        @DisplayName("every registered block resolves a non-fallback VehiclePartDefinition")
        void everyBlockResolvesAVehiclePartDefinition() {
            for (String id : ALL_BLOCK_IDS) {
                String fullId = "sharkengine:" + id;
                VehiclePartDefinition def = VehiclePartRegistry.resolve(fullId);
                assertNotEquals(VehiclePartRegistry.FALLBACK, def,
                        "Block '" + fullId + "' resolves to the generic STRUCTURE fallback — every "
                                + "ModBlocks entry needs its own VehiclePartDefinition (AIR-020/AIR-031)");
            }
        }

        @Test
        @DisplayName("every registered block is a member of the ship_eligible tag")
        void everyBlockIsShipEligible() throws IOException {
            String tagJson = readGenerated("data/sharkengine/tags/block/ship_eligible.json");
            for (String id : ALL_BLOCK_IDS) {
                assertTrue(tagJson.contains("\"sharkengine:" + id + "\""),
                        "Block 'sharkengine:" + id + "' is not a member of the ship_eligible tag");
            }
        }

        @Test
        @DisplayName("every texture reference in a block model resolves to a real file (sharkengine) "
                + "or a recognized namespace (minecraft)")
        void everyTextureReferenceResolves() throws IOException {
            for (String id : ALL_BLOCK_IDS) {
                Path modelFile = GENERATED_ROOT.resolve("assets/sharkengine/models/block/" + id + ".json");
                assertTrue(Files.exists(modelFile), "Missing block model for '" + id + "'");
                String modelJson = Files.readString(modelFile, StandardCharsets.UTF_8);
                Set<String> textureRefs = extractTextureRefs(modelJson);
                assertFalse(textureRefs.isEmpty(),
                        "Block model for '" + id + "' declares no textures at all: " + modelFile);
                for (String textureRef : textureRefs) {
                    assertTrue(textureRef.contains(":"),
                            "Texture reference '" + textureRef + "' in '" + id + "' model must be namespace:path");
                    String namespace = textureRef.substring(0, textureRef.indexOf(':'));
                    String path = textureRef.substring(textureRef.indexOf(':') + 1);
                    assertEquals(path.toLowerCase(Locale.ROOT), path,
                            "Texture reference '" + textureRef + "' in '" + id + "' model must be lowercase");
                    if (namespace.equals("sharkengine")) {
                        Path textureFile = TEXTURES_ROOT.resolve(path + ".png");
                        assertTrue(Files.exists(textureFile),
                                "Block model for '" + id + "' references texture '" + textureRef
                                        + "' which does not exist at " + textureFile);
                    } else {
                        // Vanilla-namespaced textures (e.g. minecraft:block/iron_block) live inside
                        // the Minecraft client jar, which this test suite deliberately does not open
                        // (no such jar is a committed repo resource) — only the reference *format* is
                        // checked here, not on-disk existence.
                        assertEquals("minecraft", namespace,
                                "Texture reference '" + textureRef + "' in '" + id
                                        + "' model uses an unexpected namespace — expected 'sharkengine' "
                                        + "or 'minecraft'");
                    }
                }
            }
        }

        @Test
        @DisplayName("all generated and hand-written resource filenames are lowercase")
        void allResourceFilenamesAreLowercase() throws IOException {
            // MC's resource loader is case-sensitive on some platforms (Linux/macOS servers) —
            // an uppercase-letter filename that happens to work on a case-insensitive dev
            // machine (Windows/default macOS) can silently 404 in production. Scoped to actual
            // game resources: .cache/ is datagen's own hash cache (not a resource), and *.md
            // is human documentation that ships alongside resources (e.g. sounds/.../README.md)
            // but is never loaded by the game — neither is a "resource filename" in scope here.
            List<Path> roots = List.of(
                    GENERATED_ROOT,
                    RESOURCES_ROOT.resolve("assets"),
                    RESOURCES_ROOT.resolve("data"));
            for (Path root : roots) {
                if (!Files.isDirectory(root)) {
                    continue;
                }
                try (var stream = Files.walk(root)) {
                    List<Path> files = stream
                            .filter(Files::isRegularFile)
                            .filter(p -> !p.toString().contains(".cache"))
                            .filter(p -> !p.toString().endsWith(".md"))
                            .toList();
                    for (Path p : files) {
                        String filename = p.getFileName().toString();
                        assertEquals(filename.toLowerCase(Locale.ROOT), filename,
                                "Resource filename must be lowercase: " + p);
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("AIR-040: Item resource contract — crafting intermediates")
    class CraftingIntermediateResourceContractTests {

        /**
         * The 4 crafting-intermediate items AIR-040 registers first (concept doc §4
         * "Zwischenprodukte & Rezepte" — "intermediates FIRST" per the implementation
         * plan's own AIR-040 ordering note). These are plain {@code Item}s registered
         * via a new {@code ModItems}, NOT {@code ModBlocks} entries — so unlike
         * {@link PerBlockResourceContractTests#ALL_BLOCK_IDS} they have no blockstate,
         * no block model, no loot table, no {@code ship_eligible} tag membership, and
         * no {@code VehiclePartDefinition} (they are never placed or flown; they exist
         * only as crafting reagents for the real parts registered later in this task's
         * follow-on scope).
         */
        private static final String[] INTERMEDIATE_ITEM_IDS =
                {"metal_sheet", "rotor_shaft", "engine_core", "bearing_assembly"};

        /** Exact yield per craft, locked from the concept doc's recipe table (§4). */
        private static final Map<String, Integer> EXPECTED_YIELD = Map.of(
                "metal_sheet", 4,
                "rotor_shaft", 2,
                "engine_core", 1,
                "bearing_assembly", 2
        );

        private static final Path TEXTURES_ROOT = RESOURCES_ROOT.resolve("assets/sharkengine/textures");

        @Test
        @DisplayName("every intermediate has an item model")
        void everyIntermediateHasItemModel() {
            for (String id : INTERMEDIATE_ITEM_IDS) {
                Path path = GENERATED_ROOT.resolve("assets/sharkengine/models/item/" + id + ".json");
                assertTrue(Files.exists(path),
                        "Missing item model for intermediate '" + id + "' at " + path + " — run ./gradlew runDatagen");
            }
        }

        @Test
        @DisplayName("every intermediate's item model resolves its texture reference to an existing file")
        void everyIntermediateItemModelResolvesTexture() throws IOException {
            for (String id : INTERMEDIATE_ITEM_IDS) {
                Path modelFile = GENERATED_ROOT.resolve("assets/sharkengine/models/item/" + id + ".json");
                assertTrue(Files.exists(modelFile), "Missing item model for '" + id + "'");
                String modelJson = Files.readString(modelFile, StandardCharsets.UTF_8);
                Matcher m = Pattern.compile("\"layer0\"\\s*:\\s*\"([^\"]+)\"").matcher(modelJson);
                assertTrue(m.find(), "Item model for '" + id + "' declares no \"layer0\" texture: " + modelFile);
                String textureRef = m.group(1);
                assertTrue(textureRef.startsWith("sharkengine:"),
                        "Texture reference '" + textureRef + "' for '" + id + "' must be sharkengine-namespaced");
                String path = textureRef.substring("sharkengine:".length());
                Path textureFile = TEXTURES_ROOT.resolve(path + ".png");
                assertTrue(Files.exists(textureFile),
                        "Item model for '" + id + "' references texture '" + textureRef
                                + "' which does not exist at " + textureFile);
            }
        }

        @Test
        @DisplayName("every intermediate has a recipe producing the concept-doc yield")
        void everyIntermediateHasRecipeWithExpectedYield() throws IOException {
            for (String id : INTERMEDIATE_ITEM_IDS) {
                Path recipeFile = GENERATED_ROOT.resolve("data/sharkengine/recipe/" + id + ".json");
                assertTrue(Files.exists(recipeFile),
                        "Missing recipe for intermediate '" + id + "' at " + recipeFile + " — run ./gradlew runDatagen");
                String json = Files.readString(recipeFile, StandardCharsets.UTF_8);
                assertTrue(json.contains("\"id\": \"sharkengine:" + id + "\""),
                        "Recipe for '" + id + "' must produce result id 'sharkengine:" + id + "', found: " + json);
                int expectedCount = EXPECTED_YIELD.get(id);
                assertTrue(json.contains("\"count\": " + expectedCount),
                        "Recipe for '" + id + "' must yield " + expectedCount
                                + " per craft (concept doc §4 recipe table), found: " + json);
            }
        }

        @Test
        @DisplayName("every intermediate has non-blank English and German item translations")
        void everyIntermediateHasTranslations() throws IOException {
            String enContent = Files.readString(
                    GENERATED_ROOT.resolve("assets/sharkengine/lang/en_us.json"), StandardCharsets.UTF_8);
            String deContent = Files.readString(
                    GENERATED_ROOT.resolve("assets/sharkengine/lang/de_de.json"), StandardCharsets.UTF_8);
            for (String id : INTERMEDIATE_ITEM_IDS) {
                String key = "item.sharkengine." + id;
                String enVal = extractJsonValue(enContent, key);
                String deVal = extractJsonValue(deContent, key);
                assertNotNull(enVal, "Missing English translation key '" + key + "' in en_us.json");
                assertFalse(enVal.isBlank(), "English translation for '" + key + "' must not be blank");
                assertNotNull(deVal, "Missing German translation key '" + key + "' in de_de.json");
                assertFalse(deVal.isBlank(), "German translation for '" + key + "' must not be blank");
            }
        }
    }
}
