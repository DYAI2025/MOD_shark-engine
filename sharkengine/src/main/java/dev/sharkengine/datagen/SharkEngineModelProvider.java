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
                writeBlockState(writer, ns, "airframe_panel", AIRFRAME_PANEL_BLOCKSTATE),
                writeBlockState(writer, ns, "fuselage_frame", FUSELAGE_FRAME_BLOCKSTATE),
                writeBlockState(writer, ns, "helicopter_engine", HELICOPTER_ENGINE_BLOCKSTATE),
                writeBlockState(writer, ns, "rotor_hub", ROTOR_HUB_BLOCKSTATE),
                writeBlockState(writer, ns, "rotor_blade", ROTOR_BLADE_BLOCKSTATE),
                writeBlockState(writer, ns, "landing_skid", LANDING_SKID_BLOCKSTATE),

                writeModel(writer, blockModelId(ns, "thruster"), THRUSTER_BLOCK_MODEL),
                writeModel(writer, blockModelId(ns, "steering_wheel"), STEERING_WHEEL_BLOCK_MODEL),
                writeModel(writer, blockModelId(ns, "bug"), BUG_BLOCK_MODEL),
                writeModel(writer, blockModelId(ns, "airframe_panel"), AIRFRAME_PANEL_BLOCK_MODEL),
                writeModel(writer, blockModelId(ns, "fuselage_frame"), FUSELAGE_FRAME_BLOCK_MODEL),
                writeModel(writer, blockModelId(ns, "helicopter_engine"), HELICOPTER_ENGINE_BLOCK_MODEL),
                writeModel(writer, blockModelId(ns, "rotor_hub"), ROTOR_HUB_BLOCK_MODEL),
                writeModel(writer, blockModelId(ns, "rotor_blade"), ROTOR_BLADE_BLOCK_MODEL),
                writeModel(writer, blockModelId(ns, "landing_skid"), LANDING_SKID_BLOCK_MODEL),

                writeModel(writer, itemModelId(ns, "thruster"), THRUSTER_ITEM_MODEL),
                writeModel(writer, itemModelId(ns, "steering_wheel"), STEERING_WHEEL_ITEM_MODEL),
                writeModel(writer, itemModelId(ns, "bug"), BUG_ITEM_MODEL),
                writeModel(writer, itemModelId(ns, "airframe_panel"), AIRFRAME_PANEL_ITEM_MODEL),
                writeModel(writer, itemModelId(ns, "fuselage_frame"), FUSELAGE_FRAME_ITEM_MODEL),
                writeModel(writer, itemModelId(ns, "helicopter_engine"), HELICOPTER_ENGINE_ITEM_MODEL),
                writeModel(writer, itemModelId(ns, "rotor_hub"), ROTOR_HUB_ITEM_MODEL),
                writeModel(writer, itemModelId(ns, "rotor_blade"), ROTOR_BLADE_ITEM_MODEL),
                writeModel(writer, itemModelId(ns, "landing_skid"), LANDING_SKID_ITEM_MODEL),

                // AIR-040: crafting-intermediate items — item model only, no block/
                // blockstate (they are plain Items, never placed). Texture already
                // exists under textures/block/<id>.png from the AIR-032/AIR-040 asset
                // commit; referenced directly rather than duplicated under textures/item/.
                writeModel(writer, itemModelId(ns, "metal_sheet"), generatedItemModel("block/metal_sheet")),
                writeModel(writer, itemModelId(ns, "rotor_shaft"), generatedItemModel("block/rotor_shaft")),
                writeModel(writer, itemModelId(ns, "engine_core"), generatedItemModel("block/engine_core")),
                writeModel(writer, itemModelId(ns, "bearing_assembly"), generatedItemModel("block/bearing_assembly"))
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

    /**
     * AIR-040: standard vanilla flat-icon item model ({@code minecraft:item/generated}
     * parent, single {@code layer0} texture) for the crafting-intermediate items —
     * these have no block counterpart, so unlike thruster/steering_wheel/bug's item
     * models above they don't parent to a {@code sharkengine:block/...} block model.
     */
    private static String generatedItemModel(String texturePath) {
        return """
                {
                  "parent": "minecraft:item/generated",
                  "textures": {
                    "layer0": "sharkengine:%s"
                  }
                }
                """.formatted(texturePath);
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

    /**
     * AIR-040: {@code airframe_panel} (SKIN role, concept §4 "Blockstate" column:
     * {@code facing}, full six-direction). Default model orientation is
     * {@code facing=up} (a 2px-thick plate at the top of the block); every other
     * facing is reached by rotating that default model rigidly — the same
     * default-up + x/y-rotation-table idiom vanilla uses for {@code lightning_rod}
     * (a model whose "business end" points in one direction, reused for all six
     * facings via pure rotation, no per-direction model variants needed).
     * {@link dev.sharkengine.content.block.AirframePanelBlock}'s {@code VoxelShape}
     * table is rigid-transform-consistent with this exact rotation set — see that
     * class's javadoc.
     */
    private static final String AIRFRAME_PANEL_BLOCKSTATE = """
            {
              "variants": {
                "facing=up":    { "model": "sharkengine:block/airframe_panel" },
                "facing=down":  { "model": "sharkengine:block/airframe_panel", "x": 180 },
                "facing=north": { "model": "sharkengine:block/airframe_panel", "x": 90 },
                "facing=east":  { "model": "sharkengine:block/airframe_panel", "x": 90, "y": 90 },
                "facing=south": { "model": "sharkengine:block/airframe_panel", "x": 90, "y": 180 },
                "facing=west":  { "model": "sharkengine:block/airframe_panel", "x": 90, "y": 270 }
              }
            }
            """;

    /**
     * AIR-040: {@code fuselage_frame} (STRUCTURE role, concept §4 "Blockstate" column:
     * {@code axis}, "placed along X/Y/Z like a log/pillar"). Uses the exact same
     * default-{@code axis=y} + rotation-table idiom vanilla itself uses for pillar
     * blocks (e.g. {@code oak_log}'s blockstate: {@code axis=y} unrotated,
     * {@code axis=x} → x:90/y:90, {@code axis=z} → x:90) — reproduced here verbatim
     * rather than invented, since {@link net.minecraft.world.level.block.RotatedPillarBlock}
     * (the vanilla base class {@link dev.sharkengine.content.ModBlocks#FUSELAGE_FRAME}
     * is registered with) expects its {@code AXIS} property read the same way. The
     * block's texture is uniform across all six faces (see
     * {@link #FUSELAGE_FRAME_BLOCK_MODEL}), so the three variants are visually
     * identical today — kept for blockstate correctness and forward-consistency with
     * how every other pillar-shaped part in this mod will need to declare rotation.
     */
    private static final String FUSELAGE_FRAME_BLOCKSTATE = """
            {
              "variants": {
                "axis=y": { "model": "sharkengine:block/fuselage_frame" },
                "axis=x": { "model": "sharkengine:block/fuselage_frame", "x": 90, "y": 90 },
                "axis=z": { "model": "sharkengine:block/fuselage_frame", "x": 90 }
              }
            }
            """;

    /**
     * AIR-040: {@code helicopter_engine} (PROPULSION role, {@code liftMode=ROTOR}).
     * Concept §4 "Blockstate" column: {@code facing}, full six-direction — same
     * default-{@code facing=up} + x/y-rotation-table idiom as
     * {@link #AIRFRAME_PANEL_BLOCKSTATE} (see
     * {@link dev.sharkengine.content.block.HelicopterEngineBlock}'s javadoc for why
     * full six-direction rather than {@code bug}'s horizontal-only variant was
     * chosen). The single {@code helicopter_engine.png} texture is uniform across all
     * six faces (see {@link #HELICOPTER_ENGINE_BLOCK_MODEL}), so — exactly like
     * {@link #FUSELAGE_FRAME_BLOCKSTATE}'s three axis variants — every facing variant
     * is visually identical today; kept for blockstate correctness.
     */
    private static final String HELICOPTER_ENGINE_BLOCKSTATE = """
            {
              "variants": {
                "facing=up":    { "model": "sharkengine:block/helicopter_engine" },
                "facing=down":  { "model": "sharkengine:block/helicopter_engine", "x": 180 },
                "facing=north": { "model": "sharkengine:block/helicopter_engine", "x": 90 },
                "facing=east":  { "model": "sharkengine:block/helicopter_engine", "x": 90, "y": 90 },
                "facing=south": { "model": "sharkengine:block/helicopter_engine", "x": 90, "y": 180 },
                "facing=west":  { "model": "sharkengine:block/helicopter_engine", "x": 90, "y": 270 }
              }
            }
            """;

    /**
     * AIR-040: {@code rotor_hub} (ROTOR_HUB role, concept §4 "Blockstate" column:
     * {@code axis} — same "placed along X/Y/Z like a log/pillar" semantics as
     * {@link #FUSELAGE_FRAME_BLOCKSTATE}, reproduced verbatim from that idiom since
     * {@link dev.sharkengine.content.ModBlocks#ROTOR_HUB} is likewise registered with
     * vanilla's {@link net.minecraft.world.level.block.RotatedPillarBlock}). The
     * single {@code rotor_hub.png} texture is uniform across all six faces (see
     * {@link #ROTOR_HUB_BLOCK_MODEL}), so — exactly like
     * {@link #FUSELAGE_FRAME_BLOCKSTATE}'s three axis variants — every axis variant is
     * visually identical today; kept for blockstate correctness.
     */
    private static final String ROTOR_HUB_BLOCKSTATE = """
            {
              "variants": {
                "axis=y": { "model": "sharkengine:block/rotor_hub" },
                "axis=x": { "model": "sharkengine:block/rotor_hub", "x": 90, "y": 90 },
                "axis=z": { "model": "sharkengine:block/rotor_hub", "x": 90 }
              }
            }
            """;

    /**
     * AIR-040: {@code rotor_blade} (ROTOR_BLADE role, concept §4 "Blockstate" column:
     * {@code facing}, full six-direction). Default model orientation is
     * {@code facing=up}, reached by the exact same default-up + x/y-rotation-table
     * idiom as {@link #AIRFRAME_PANEL_BLOCKSTATE}/{@link #HELICOPTER_ENGINE_BLOCKSTATE}.
     * {@link dev.sharkengine.content.block.RotorBladeBlock}'s {@code VoxelShape} table
     * is rigid-transform-consistent with this exact rotation set — see that class's
     * javadoc.
     */
    private static final String ROTOR_BLADE_BLOCKSTATE = """
            {
              "variants": {
                "facing=up":    { "model": "sharkengine:block/rotor_blade" },
                "facing=down":  { "model": "sharkengine:block/rotor_blade", "x": 180 },
                "facing=north": { "model": "sharkengine:block/rotor_blade", "x": 90 },
                "facing=east":  { "model": "sharkengine:block/rotor_blade", "x": 90, "y": 90 },
                "facing=south": { "model": "sharkengine:block/rotor_blade", "x": 90, "y": 180 },
                "facing=west":  { "model": "sharkengine:block/rotor_blade", "x": 90, "y": 270 }
              }
            }
            """;

    /**
     * AIR-040: {@code landing_skid} (LANDING_GEAR role, concept §4 "Blockstate"
     * column: {@code facing}, full six-direction). Default model orientation is
     * {@code facing=up}, reached by the exact same default-up + x/y-rotation-table
     * idiom as {@link #AIRFRAME_PANEL_BLOCKSTATE}/{@link #HELICOPTER_ENGINE_BLOCKSTATE}/
     * {@link #ROTOR_BLADE_BLOCKSTATE}. {@link
     * dev.sharkengine.content.block.LandingSkidBlock}'s {@code VoxelShape} table is
     * rigid-transform-consistent with this exact rotation set — see that class's
     * javadoc.
     */
    private static final String LANDING_SKID_BLOCKSTATE = """
            {
              "variants": {
                "facing=up":    { "model": "sharkengine:block/landing_skid" },
                "facing=down":  { "model": "sharkengine:block/landing_skid", "x": 180 },
                "facing=north": { "model": "sharkengine:block/landing_skid", "x": 90 },
                "facing=east":  { "model": "sharkengine:block/landing_skid", "x": 90, "y": 90 },
                "facing=south": { "model": "sharkengine:block/landing_skid", "x": 90, "y": 180 },
                "facing=west":  { "model": "sharkengine:block/landing_skid", "x": 90, "y": 270 }
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

    private static final String AIRFRAME_PANEL_BLOCK_MODEL = """
            {
              "parent": "minecraft:block/block",
              "textures": {
                "all": "sharkengine:block/airframe_panel"
              },
              "elements": [
                {
                  "comment": "2px plate, default orientation facing=up",
                  "from": [0, 14, 0],
                  "to": [16, 16, 16],
                  "faces": {
                    "north": { "uv": [0, 0, 16, 2], "texture": "#all" },
                    "south": { "uv": [0, 0, 16, 2], "texture": "#all" },
                    "east":  { "uv": [0, 0, 16, 2], "texture": "#all" },
                    "west":  { "uv": [0, 0, 16, 2], "texture": "#all" },
                    "up":    { "uv": [0, 0, 16, 16], "texture": "#all" },
                    "down":  { "uv": [0, 0, 16, 16], "texture": "#all" }
                  }
                }
              ]
            }
            """;

    /**
     * AIR-040: {@code fuselage_frame} is a solid structural block (full cube), unlike
     * {@code airframe_panel}'s thin skin plate — its texture is a single uniform
     * 16×16 image applied to all six faces, so this reuses vanilla's
     * {@code minecraft:block/cube_all} parent directly instead of hand-writing an
     * {@code elements} array (this provider's own class javadoc explains why other
     * models here needed inline {@code elements}: no vanilla
     * {@code BlockModelGenerators} entry point is reachable from mod code — that
     * constraint is about the *generation API*, not about referencing an existing
     * vanilla model file by name in raw JSON, which is what this does).
     */
    private static final String FUSELAGE_FRAME_BLOCK_MODEL = """
            {
              "parent": "minecraft:block/cube_all",
              "textures": {
                "all": "sharkengine:block/fuselage_frame"
              }
            }
            """;

    /**
     * AIR-040: {@code helicopter_engine} is a solid engine block (full cube, no
     * custom {@code VoxelShape} — see the block class javadoc), with a single
     * uniform texture across all six faces — reuses {@code minecraft:block/cube_all}
     * directly, same as {@link #FUSELAGE_FRAME_BLOCK_MODEL}.
     */
    private static final String HELICOPTER_ENGINE_BLOCK_MODEL = """
            {
              "parent": "minecraft:block/cube_all",
              "textures": {
                "all": "sharkengine:block/helicopter_engine"
              }
            }
            """;

    /**
     * AIR-040: {@code rotor_hub} is a solid bearing/hub block (full cube, no custom
     * {@code VoxelShape} — {@link dev.sharkengine.content.ModBlocks#ROTOR_HUB} is
     * registered with vanilla's {@code RotatedPillarBlock}, whose default shape is a
     * full cube), with a single uniform texture across all six faces — reuses
     * {@code minecraft:block/cube_all} directly, same as {@link #FUSELAGE_FRAME_BLOCK_MODEL}
     * and {@link #HELICOPTER_ENGINE_BLOCK_MODEL}.
     */
    private static final String ROTOR_HUB_BLOCK_MODEL = """
            {
              "parent": "minecraft:block/cube_all",
              "textures": {
                "all": "sharkengine:block/rotor_hub"
              }
            }
            """;

    /**
     * AIR-040: {@code rotor_blade} is a thin blade plate (custom {@code elements}
     * array, 3px thick — see {@link dev.sharkengine.content.block.RotorBladeBlock}'s
     * javadoc), not a solid full cube — same reasoning as
     * {@link #AIRFRAME_PANEL_BLOCK_MODEL}'s own inline {@code elements}, just with a
     * thinner {@code from}/{@code to} extent (13→16 instead of 14→16).
     */
    private static final String ROTOR_BLADE_BLOCK_MODEL = """
            {
              "parent": "minecraft:block/block",
              "textures": {
                "all": "sharkengine:block/rotor_blade"
              },
              "elements": [
                {
                  "comment": "3px blade, default orientation facing=up",
                  "from": [0, 13, 0],
                  "to": [16, 16, 16],
                  "faces": {
                    "north": { "uv": [0, 0, 16, 3], "texture": "#all" },
                    "south": { "uv": [0, 0, 16, 3], "texture": "#all" },
                    "east":  { "uv": [0, 0, 16, 3], "texture": "#all" },
                    "west":  { "uv": [0, 0, 16, 3], "texture": "#all" },
                    "up":    { "uv": [0, 0, 16, 16], "texture": "#all" },
                    "down":  { "uv": [0, 0, 16, 16], "texture": "#all" }
                  }
                }
              ]
            }
            """;

    /**
     * AIR-040: {@code landing_skid} is a 4px skid rail (custom {@code elements}
     * array, thicker than {@link #ROTOR_BLADE_BLOCK_MODEL}'s 3px blade — see
     * {@link dev.sharkengine.content.block.LandingSkidBlock}'s javadoc for the
     * "Kufenprofil" rationale), not a solid full cube — same reasoning as
     * {@link #AIRFRAME_PANEL_BLOCK_MODEL}/{@link #ROTOR_BLADE_BLOCK_MODEL}'s own
     * inline {@code elements}, just with a thicker {@code from}/{@code to} extent
     * (12→16 instead of 13→16 or 14→16).
     */
    private static final String LANDING_SKID_BLOCK_MODEL = """
            {
              "parent": "minecraft:block/block",
              "textures": {
                "all": "sharkengine:block/landing_skid"
              },
              "elements": [
                {
                  "comment": "4px skid rail, default orientation facing=up",
                  "from": [0, 12, 0],
                  "to": [16, 16, 16],
                  "faces": {
                    "north": { "uv": [0, 0, 16, 4], "texture": "#all" },
                    "south": { "uv": [0, 0, 16, 4], "texture": "#all" },
                    "east":  { "uv": [0, 0, 16, 4], "texture": "#all" },
                    "west":  { "uv": [0, 0, 16, 4], "texture": "#all" },
                    "up":    { "uv": [0, 0, 16, 16], "texture": "#all" },
                    "down":  { "uv": [0, 0, 16, 16], "texture": "#all" }
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

    private static final String AIRFRAME_PANEL_ITEM_MODEL = """
            {
              "parent": "sharkengine:block/airframe_panel"
            }
            """;

    private static final String FUSELAGE_FRAME_ITEM_MODEL = """
            {
              "parent": "sharkengine:block/fuselage_frame"
            }
            """;

    private static final String HELICOPTER_ENGINE_ITEM_MODEL = """
            {
              "parent": "sharkengine:block/helicopter_engine"
            }
            """;

    private static final String ROTOR_HUB_ITEM_MODEL = """
            {
              "parent": "sharkengine:block/rotor_hub"
            }
            """;

    private static final String ROTOR_BLADE_ITEM_MODEL = """
            {
              "parent": "sharkengine:block/rotor_blade"
            }
            """;

    private static final String LANDING_SKID_ITEM_MODEL = """
            {
              "parent": "sharkengine:block/landing_skid"
            }
            """;
}
