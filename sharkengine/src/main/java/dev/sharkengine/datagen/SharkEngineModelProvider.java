package dev.sharkengine.datagen;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * AIR-030: generates blockstate + block model + item model JSON for
 * thruster/steering_wheel/bug.
 *
 * <p>These blocks use fully custom multi-element geometry (a wedge + tip for
 * {@code bug}, eight elements for the {@code steering_wheel} spokes/rim/hub,
 * three elements for {@code thruster}) that does not correspond to any
 * vanilla {@code BlockModelGenerators}/{@code ModelTemplate} shape (cube,
 * cross, orientable, ...) — {@code net.minecraft.data.models.*} has no
 * public entry point for registering arbitrary inline {@code elements}.
 * {@link net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider}'s
 * {@code blockStateOutput}/{@code modelOutput} consumers are package-private
 * on {@code BlockModelGenerators} and unreachable from mod code.
 *
 * <p>Rather than fight that DSL, this provider is a direct
 * {@link DataProvider}: the exact JSON that used to be hand-written is kept
 * as the single source of truth (as a Java text block, so it survives
 * copy/paste without hand re-encoding numeric literals), parsed once, and
 * written out through the same {@link DataProvider#saveStable} path every
 * other provider uses. This is registered like any other provider via
 * {@link net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator.Pack#addProvider}.
 */
final class SharkEngineModelProvider implements DataProvider {

    private final FabricDataOutput output;
    private final PackOutput.PathProvider blockStatePathProvider;
    private final PackOutput.PathProvider modelPathProvider;

    SharkEngineModelProvider(FabricDataOutput output) {
        this.output = output;
        this.blockStatePathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.modelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput writer) {
        String ns = output.getModId();

        List<CompletableFuture<?>> futures = List.of(
                writeBlockState(writer, ns, "thruster", THRUSTER_BLOCKSTATE),
                writeBlockState(writer, ns, "steering_wheel", STEERING_WHEEL_BLOCKSTATE),
                writeBlockState(writer, ns, "bug", BUG_BLOCKSTATE),

                writeModel(writer, blockModelId(ns, "thruster"), THRUSTER_BLOCK_MODEL),
                writeModel(writer, blockModelId(ns, "steering_wheel"), STEERING_WHEEL_BLOCK_MODEL),
                writeModel(writer, blockModelId(ns, "bug"), BUG_BLOCK_MODEL),

                writeModel(writer, itemModelId(ns, "thruster"), THRUSTER_ITEM_MODEL),
                writeModel(writer, itemModelId(ns, "steering_wheel"), STEERING_WHEEL_ITEM_MODEL),
                writeModel(writer, itemModelId(ns, "bug"), BUG_ITEM_MODEL)
        );

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> writeBlockState(CachedOutput writer, String ns, String path, String json) {
        Path target = blockStatePathProvider.json(ResourceLocation.fromNamespaceAndPath(ns, path));
        return DataProvider.saveStable(writer, parse(json), target);
    }

    private CompletableFuture<?> writeModel(CachedOutput writer, ResourceLocation modelId, String json) {
        Path target = modelPathProvider.json(modelId);
        return DataProvider.saveStable(writer, parse(json), target);
    }

    private static ResourceLocation blockModelId(String ns, String path) {
        return ResourceLocation.fromNamespaceAndPath(ns, "block/" + path);
    }

    private static ResourceLocation itemModelId(String ns, String path) {
        return ResourceLocation.fromNamespaceAndPath(ns, "item/" + path);
    }

    private static JsonElement parse(String json) {
        return JsonParser.parseString(json);
    }

    @Override
    public String getName() {
        return "Shark Engine Block/Item Models";
    }

    // ---------------------------------------------------------------
    // Verbatim content of the former hand-written resource files.
    // ---------------------------------------------------------------

    private static final String THRUSTER_BLOCKSTATE = """
            {
              "variants": {
                "": { "model": "sharkengine:block/thruster" }
              }
            }
            """;

    private static final String STEERING_WHEEL_BLOCKSTATE = """
            {
                "variants": {
                    "": {
                        "model": "sharkengine:block/steering_wheel"
                    }
                }
            }
            """;

    private static final String BUG_BLOCKSTATE = """
            {
              "variants": {
                "facing=north": { "model": "sharkengine:block/bug", "y": 0 },
                "facing=east":  { "model": "sharkengine:block/bug", "y": 90 },
                "facing=south": { "model": "sharkengine:block/bug", "y": 180 },
                "facing=west":  { "model": "sharkengine:block/bug", "y": 270 }
              }
            }
            """;

    private static final String THRUSTER_BLOCK_MODEL = """
            {
              "parent": "minecraft:block/block",
              "textures": {
                "body": "minecraft:block/iron_block",
                "coil": "minecraft:block/copper_block",
                "fan": "minecraft:block/polished_deepslate"
              },
              "elements": [
                {
                  "from": [2, 0, 2],
                  "to": [14, 6, 14],
                  "faces": {
                    "north": { "uv": [0, 0, 12, 6], "texture": "#body" },
                    "south": { "uv": [0, 0, 12, 6], "texture": "#body" },
                    "east": { "uv": [0, 0, 12, 6], "texture": "#body" },
                    "west": { "uv": [0, 0, 12, 6], "texture": "#body" },
                    "up": { "uv": [0, 0, 12, 12], "texture": "#body" },
                    "down": { "uv": [0, 0, 12, 12], "texture": "#body" }
                  }
                },
                {
                  "from": [4, 6, 4],
                  "to": [12, 12, 12],
                  "faces": {
                    "north": { "uv": [0, 0, 8, 6], "texture": "#coil" },
                    "south": { "uv": [0, 0, 8, 6], "texture": "#coil" },
                    "east": { "uv": [0, 0, 8, 6], "texture": "#coil" },
                    "west": { "uv": [0, 0, 8, 6], "texture": "#coil" },
                    "up": { "uv": [0, 0, 8, 8], "texture": "#coil" },
                    "down": { "uv": [0, 0, 8, 8], "texture": "#coil" }
                  }
                },
                {
                  "from": [2, 12, 2],
                  "to": [14, 16, 14],
                  "faces": {
                    "north": { "uv": [0, 0, 12, 4], "texture": "#body" },
                    "south": { "uv": [0, 0, 12, 4], "texture": "#body" },
                    "east": { "uv": [0, 0, 12, 4], "texture": "#body" },
                    "west": { "uv": [0, 0, 12, 4], "texture": "#body" },
                    "up": { "uv": [0, 0, 12, 12], "texture": "#fan" },
                    "down": { "uv": [0, 0, 12, 12], "texture": "#body" }
                  }
                }
              ]
            }
            """;

    private static final String STEERING_WHEEL_BLOCK_MODEL = """
            {
              "parent": "minecraft:block/block",
              "textures": {
                "rim": "minecraft:block/stripped_oak_log",
                "spoke": "minecraft:block/dark_oak_planks",
                "hub": "minecraft:block/copper_block"
              },
              "elements": [
                {
                  "from": [2, 14, 6],
                  "to": [14, 16, 10],
                  "faces": {
                    "north": { "uv": [0, 0, 12, 2], "texture": "#rim" },
                    "south": { "uv": [0, 0, 12, 2], "texture": "#rim" },
                    "up": { "uv": [0, 0, 12, 4], "texture": "#rim" },
                    "down": { "uv": [0, 0, 12, 4], "texture": "#rim" }
                  }
                },
                {
                  "from": [2, 0, 6],
                  "to": [14, 2, 10],
                  "faces": {
                    "north": { "uv": [0, 0, 12, 2], "texture": "#rim" },
                    "south": { "uv": [0, 0, 12, 2], "texture": "#rim" },
                    "up": { "uv": [0, 0, 12, 4], "texture": "#rim" },
                    "down": { "uv": [0, 0, 12, 4], "texture": "#rim" }
                  }
                },
                {
                  "from": [0, 2, 6],
                  "to": [2, 14, 10],
                  "faces": {
                    "east": { "uv": [0, 0, 12, 4], "texture": "#rim" },
                    "west": { "uv": [0, 0, 12, 4], "texture": "#rim" },
                    "north": { "uv": [0, 0, 2, 12], "texture": "#rim" },
                    "south": { "uv": [0, 0, 2, 12], "texture": "#rim" }
                  }
                },
                {
                  "from": [14, 2, 6],
                  "to": [16, 14, 10],
                  "faces": {
                    "east": { "uv": [0, 0, 12, 4], "texture": "#rim" },
                    "west": { "uv": [0, 0, 12, 4], "texture": "#rim" },
                    "north": { "uv": [0, 0, 2, 12], "texture": "#rim" },
                    "south": { "uv": [0, 0, 2, 12], "texture": "#rim" }
                  }
                },
                {
                  "from": [6, 6, 6],
                  "to": [10, 10, 10],
                  "faces": {
                    "north": { "uv": [0, 0, 4, 4], "texture": "#hub" },
                    "south": { "uv": [0, 0, 4, 4], "texture": "#hub" },
                    "east": { "uv": [0, 0, 4, 4], "texture": "#hub" },
                    "west": { "uv": [0, 0, 4, 4], "texture": "#hub" },
                    "up": { "uv": [0, 0, 4, 4], "texture": "#hub" },
                    "down": { "uv": [0, 0, 4, 4], "texture": "#hub" }
                  }
                },
                {
                  "from": [2, 7, 7],
                  "to": [14, 9, 9],
                  "faces": {
                    "north": { "uv": [0, 0, 12, 2], "texture": "#spoke" },
                    "south": { "uv": [0, 0, 12, 2], "texture": "#spoke" },
                    "up": { "uv": [0, 0, 12, 2], "texture": "#spoke" },
                    "down": { "uv": [0, 0, 12, 2], "texture": "#spoke" }
                  }
                },
                {
                  "from": [7, 2, 7],
                  "to": [9, 14, 9],
                  "faces": {
                    "north": { "uv": [0, 0, 2, 12], "texture": "#spoke" },
                    "south": { "uv": [0, 0, 2, 12], "texture": "#spoke" },
                    "east": { "uv": [0, 0, 2, 12], "texture": "#spoke" },
                    "west": { "uv": [0, 0, 2, 12], "texture": "#spoke" }
                  }
                },
                {
                  "from": [3, 3, 7],
                  "to": [5, 5, 9],
                  "faces": {
                    "north": { "uv": [0, 0, 2, 2], "texture": "#spoke" },
                    "south": { "uv": [0, 0, 2, 2], "texture": "#spoke" },
                    "east": { "uv": [0, 0, 2, 2], "texture": "#spoke" },
                    "west": { "uv": [0, 0, 2, 2], "texture": "#spoke" }
                  }
                },
                {
                  "from": [11, 11, 7],
                  "to": [13, 13, 9],
                  "faces": {
                    "north": { "uv": [0, 0, 2, 2], "texture": "#spoke" },
                    "south": { "uv": [0, 0, 2, 2], "texture": "#spoke" },
                    "east": { "uv": [0, 0, 2, 2], "texture": "#spoke" },
                    "west": { "uv": [0, 0, 2, 2], "texture": "#spoke" }
                  }
                }
              ]
            }
            """;

    private static final String BUG_BLOCK_MODEL = """
            {
              "parent": "minecraft:block/block",
              "textures": {
                "side": "minecraft:block/gold_block",
                "front": "minecraft:block/target_top",
                "back": "minecraft:block/stripped_oak_log"
              },
              "elements": [
                {
                  "comment": "Main wedge body",
                  "from": [2, 0, 0],
                  "to": [14, 10, 16],
                  "faces": {
                    "north": { "uv": [0, 0, 12, 10], "texture": "#front" },
                    "south": { "uv": [0, 0, 12, 10], "texture": "#back" },
                    "east":  { "uv": [0, 0, 16, 10], "texture": "#side" },
                    "west":  { "uv": [0, 0, 16, 10], "texture": "#side" },
                    "up":    { "uv": [0, 0, 12, 16], "texture": "#side" },
                    "down":  { "uv": [0, 0, 12, 16], "texture": "#side" }
                  }
                },
                {
                  "comment": "Pointed tip (arrow shape pointing north = forward)",
                  "from": [5, 2, -3],
                  "to": [11, 8, 0],
                  "faces": {
                    "north": { "uv": [4, 4, 10, 10], "texture": "#front" },
                    "south": { "uv": [4, 4, 10, 10], "texture": "#front" },
                    "east":  { "uv": [0, 4, 3, 10], "texture": "#side" },
                    "west":  { "uv": [0, 4, 3, 10], "texture": "#side" },
                    "up":    { "uv": [4, 0, 10, 3], "texture": "#side" },
                    "down":  { "uv": [4, 0, 10, 3], "texture": "#side" }
                  }
                }
              ]
            }
            """;

    private static final String THRUSTER_ITEM_MODEL = """
            {
              "parent": "sharkengine:block/thruster"
            }
            """;

    private static final String STEERING_WHEEL_ITEM_MODEL = """
            {
                "parent": "sharkengine:block/steering_wheel"
            }
            """;

    private static final String BUG_ITEM_MODEL = """
            {
              "parent": "sharkengine:block/bug"
            }
            """;
}
