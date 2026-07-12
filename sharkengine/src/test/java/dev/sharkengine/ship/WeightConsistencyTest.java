package dev.sharkengine.ship;

import dev.sharkengine.ship.part.ShipPartAnalyzer;
import dev.sharkengine.ship.part.ShipStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AIR-023 consistency requirement: the category the client HUD shows
 * ({@code FuelHudOverlay}, via {@code ShipEntity.getWeightCategory()}) must be
 * exactly the category the server used to compute max speed
 * ({@code ShipEntity.applyBlueprintStats}/{@code updatePhysics}, via
 * {@code ShipPhysics.calculateMaxSpeed}).
 *
 * <p>Both paths are exercised here at the pure-logic level (no
 * {@code ShipEntity}/Minecraft bootstrap needed): {@link ShipPartAnalyzer} produces
 * the same {@link ShipStats#mass()} {@code ShipEntity.applyBlueprintStats} uses,
 * and both {@link WeightCategory#fromMass(int)} and
 * {@link ShipPhysics#calculateMaxSpeed(int)} are driven from that one mass value —
 * proving the two call sites cannot disagree because they are, in substance, the
 * same computation. Before AIR-023, the client recomputed
 * {@code WeightCategory.fromBlockCount(blockCount)} independently of the
 * mass-driven speed — for a mixed-mass ship this could show "flying fine" on the
 * HUD while the server actually enforced 0 blocks/sec (or vice versa).</p>
 */
@DisplayName("Weight Consistency Tests (AIR-023: HUD category == server category)")
class WeightConsistencyTest {

    private static final String HELICOPTER_ENGINE_ID = "sharkengine:helicopter_engine";
    private static final String AIRFRAME_PANEL_ID = "sharkengine:airframe_panel";

    @Test
    @DisplayName("mixed-mass ship: 16 helicopter_engine blocks (mass 6 each) -> mass 96, OVERLOADED everywhere")
    void fewHeavyEngineBlocksAreOverloadedByMassNotBlockCount() {
        // Deliberately FEW blocks (16, well under the old block-count
        // OVERLOADED threshold of 61) but each one heavy
        // (helicopter_engine mass=6, concept §4) -> mass 96.
        List<String> blockIds = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            blockIds.add(HELICOPTER_ENGINE_ID);
        }

        ShipStats stats = ShipPartAnalyzer.analyze(blockIds);
        assertEquals(16, blockIds.size());
        assertEquals(96, stats.mass(), "16 * mass6 helicopter_engine should sum to 96");

        // The category the HUD would show (ShipEntity.getWeightCategory() ->
        // WeightCategory.fromMass(getMass()), getMass() reading SYNC_MASS).
        WeightCategory hudCategory = WeightCategory.fromMass(stats.mass());

        // The speed the server actually enforces
        // (ShipEntity.updatePhysics() -> ShipPhysics.calculateMaxSpeed(mass)).
        float serverMaxSpeed = ShipPhysics.calculateMaxSpeed(stats.mass());

        assertEquals(WeightCategory.OVERLOADED, hudCategory,
                "96 mass must read OVERLOADED, not LIGHT — a block-count-only read (16 blocks) would wrongly say LIGHT");
        assertEquals(0.0f, serverMaxSpeed,
                "OVERLOADED must ground the ship (0 b/s), matching what the HUD category says");

        // The actual consistency invariant: whatever category any consumer
        // derives from the mass must report the same max speed the server
        // uses to fly the ship — they can't be "HUD overloaded but still
        // flying at 30 b/s" because both read the same WeightCategory.
        assertEquals(hudCategory.getMaxSpeed(), serverMaxSpeed,
                "HUD-displayed category's max speed must equal the server's actual max speed");
    }

    @Test
    @DisplayName("mixed-mass ship: many light airframe_panel blocks (mass 1 each) stay LIGHT despite a large block count")
    void manyLightPanelBlocksStayLightByMassNotBlockCount() {
        // The inverse mix: lots of blocks (25, over the old block-count
        // LIGHT threshold of 20) but each one is mass-1 -> mass 25, still
        // comfortably LIGHT under the new mass threshold (<=30).
        List<String> blockIds = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            blockIds.add(AIRFRAME_PANEL_ID);
        }

        ShipStats stats = ShipPartAnalyzer.analyze(blockIds);
        assertEquals(25, stats.mass());

        WeightCategory hudCategory = WeightCategory.fromMass(stats.mass());
        float serverMaxSpeed = ShipPhysics.calculateMaxSpeed(stats.mass());

        assertEquals(WeightCategory.LIGHT, hudCategory);
        assertEquals(30.0f, serverMaxSpeed);
        assertEquals(hudCategory.getMaxSpeed(), serverMaxSpeed);
    }

    @Test
    @DisplayName("for any mass, WeightCategory.fromMass(mass).getMaxSpeed() equals ShipPhysics.calculateMaxSpeed(mass)")
    void categoryAndPhysicsAgreeAcrossFullMassRange() {
        // Sweeps across and past every boundary; this is the general form of
        // the invariant the two scenario tests above are concrete instances
        // of — no mass value can make the two paths disagree, because
        // ShipPhysics.calculateMaxSpeed derives from WeightCategory instead
        // of hardcoding its own thresholds (AIR-023 "single authority").
        for (int mass = 0; mass <= 200; mass += 5) {
            float expected = mass <= 0 ? 0.0f : WeightCategory.fromMass(mass).getMaxSpeed();
            assertEquals(expected, ShipPhysics.calculateMaxSpeed(mass), "mass=" + mass);
        }
    }
}
