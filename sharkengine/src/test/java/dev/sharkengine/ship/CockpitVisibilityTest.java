package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REQ-007/AC-007 (T08) falsifying-test contract (test-plan
 * {@code docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md}, "REQ-007 — Cockpit
 * visibility", sharpened test (a)): {@link CockpitVisibility#isFullyExposedAboveHull} computes
 * visibility PURELY from a seat's Y-offset and a plain eye-height double — OQ-003 (resolved
 * 2026-07-18) deliberately rejects any per-armor/per-skin bounding-box special case.
 *
 * <p>The counter-thesis this class exists to close: a client-only cosmetic fix that merely
 * *looks* right on one developer's screen, or an implementation that quietly special-cases armor
 * or skin "as an enhancement", would still pass a test that only samples one player configuration.
 * This class proves invariance by CONSTRUCTION rather than by one coincidental sample: {@link
 * StructuralProof#signatureHasNoRoomForArmorOrSkinParameters} asserts via reflection that the
 * production method's entire parameter list is primitive doubles — there is no parameter an
 * armor/skin value could ever be threaded through, even by a future careless edit — and {@link
 * ArmorSkinInvariance} then demonstrates the resulting behavior: two "simulated players" that
 * differ in every armor/skin/cosmetic field but share the same eye height always get the
 * byte-identical verdict, in both the concealed and the exposed case.</p>
 *
 * <p>Pure JUnit, plain doubles only — no Minecraft/Fabric imports, matching the
 * {@code dev.sharkengine.ship.part} pattern of running with zero game bootstrap.</p>
 */
@DisplayName("CockpitVisibility Tests (REQ-007/T08)")
class CockpitVisibilityTest {

    /**
     * A stand-in for "a real player's cosmetic/loadout state" — deliberately NOT accepted by
     * {@link CockpitVisibility}. Only {@link #eyeHeight()} ever reaches the production method;
     * every other field exists purely so the tests below have something concretely different to
     * vary while proving it has zero effect.
     */
    private record SimulatedPlayer(String skinId, int helmetArmorValue, int chestplateArmorValue,
                                    int bootsArmorValue, boolean hasElytraEquipped, double eyeHeight) {
    }

    @Nested
    @DisplayName("armor/skin invariance (OQ-003)")
    class ArmorSkinInvariance {

        @Test
        @DisplayName("two players with completely different armor/skin at the same eye height are both concealed identically")
        void differentArmorAndSkinAtSameEyeHeightProduceIdenticalConcealedResults() {
            double sharedEyeHeight = 1.62; // vanilla standing eye height
            SimulatedPlayer nakedDefaultSkin =
                    new SimulatedPlayer("default", 0, 0, 0, false, sharedEyeHeight);
            SimulatedPlayer fullNetheriteCustomSkinWithElytra =
                    new SimulatedPlayer("custom_slim_arms_cape", 15, 15, 15, true, sharedEyeHeight);

            double seatAnchorY = 0.0;
            double tallHullTopY = 3.0; // 3-high hull wall -> should conceal both regardless of loadout

            boolean resultNaked = CockpitVisibility.isFullyExposedAboveHull(
                    seatAnchorY, nakedDefaultSkin.eyeHeight(), tallHullTopY);
            boolean resultGeared = CockpitVisibility.isFullyExposedAboveHull(
                    seatAnchorY, fullNetheriteCustomSkinWithElytra.eyeHeight(), tallHullTopY);

            assertEquals(resultNaked, resultGeared,
                    "armor/skin must never influence the visibility result -- both players share "
                            + "the same eye height and must therefore get an identical verdict");
            assertFalse(resultNaked, "expected both to be concealed against a tall hull wall");
        }

        @Test
        @DisplayName("two players with completely different armor/skin at the same eye height are both exposed identically")
        void differentArmorAndSkinAtSameEyeHeightProduceIdenticalExposedResults() {
            double sharedEyeHeight = 1.62;
            SimulatedPlayer diamondArmorPlayer =
                    new SimulatedPlayer("skin_a", 10, 10, 10, false, sharedEyeHeight);
            SimulatedPlayer bareSkinPlayer =
                    new SimulatedPlayer("skin_b", 0, 0, 0, false, sharedEyeHeight);

            double seatAnchorY = 0.0;
            double noHullTopY = 1.0; // only the seat block's own level, nothing tall around it

            boolean resultDiamond = CockpitVisibility.isFullyExposedAboveHull(
                    seatAnchorY, diamondArmorPlayer.eyeHeight(), noHullTopY);
            boolean resultBare = CockpitVisibility.isFullyExposedAboveHull(
                    seatAnchorY, bareSkinPlayer.eyeHeight(), noHullTopY);

            assertEquals(resultDiamond, resultBare,
                    "armor/skin must not affect the exposed-case result either");
            assertTrue(resultDiamond, "expected exposure with no meaningful hull around the seat");
        }
    }

    @Nested
    @DisplayName("signature structurally cannot accept armor/skin data")
    class StructuralProof {

        @Test
        @DisplayName("every declared public method takes only primitive double parameters")
        void signatureHasNoRoomForArmorOrSkinParameters() {
            Method[] methods = CockpitVisibility.class.getDeclaredMethods();
            boolean foundAtLeastOnePublicMethod = false;
            for (Method method : methods) {
                if (!Modifier.isPublic(method.getModifiers())) {
                    continue;
                }
                foundAtLeastOnePublicMethod = true;
                for (Class<?> paramType : method.getParameterTypes()) {
                    assertEquals(double.class, paramType,
                            "public method " + method.getName() + " declares a non-double "
                                    + "parameter (" + paramType + ") -- this is exactly the kind "
                                    + "of parameter an armor/skin value could be smuggled "
                                    + "through; OQ-003 requires every input to be a plain double");
                }
            }
            assertTrue(foundAtLeastOnePublicMethod,
                    "expected at least one public method on CockpitVisibility to inspect");
        }

        @Test
        @DisplayName("no constructor is reachable -- the class cannot be instantiated to carry armor/skin state as fields")
        void classIsNotInstantiable() {
            assertTrue(Modifier.isFinal(CockpitVisibility.class.getModifiers()),
                    "expected CockpitVisibility to be a final utility class");
            assertEquals(1, CockpitVisibility.class.getDeclaredConstructors().length);
            assertTrue(Modifier.isPrivate(CockpitVisibility.class.getDeclaredConstructors()[0].getModifiers()),
                    "expected the sole constructor to be private -- no instance exists that could "
                            + "ever be given armor/skin fields to carry");
        }
    }

    @Nested
    @DisplayName("hull-relative visibility computation")
    class VisibilityComputation {

        @Test
        @DisplayName("eye point well below a tall hull top (beyond the margin) is concealed")
        void concealedBelowTallHull() {
            assertFalse(CockpitVisibility.isFullyExposedAboveHull(0.0, 1.62, 3.0));
        }

        @Test
        @DisplayName("eye point at/above a short hull top is exposed")
        void exposedAboveShortHull() {
            assertTrue(CockpitVisibility.isFullyExposedAboveHull(0.0, 1.62, 1.0));
        }

        @Test
        @DisplayName("eye point exactly at the margin boundary counts as exposed")
        void marginBoundaryCountsAsExposed() {
            double seatAnchorY = 0.0;
            double eyeHeight = 1.5;
            double eyeWorldY = seatAnchorY + eyeHeight; // 1.5
            double hullTopY = eyeWorldY + CockpitVisibility.VISIBILITY_MARGIN; // exactly margin above eye
            assertTrue(CockpitVisibility.isFullyExposedAboveHull(seatAnchorY, eyeHeight, hullTopY),
                    "eyeWorldY >= hullTopY - margin holds exactly at the boundary (>=, not >)");
        }

        @Test
        @DisplayName("eye point just inside the margin is concealed")
        void justInsideMarginIsConcealed() {
            double seatAnchorY = 0.0;
            double eyeHeight = 1.5;
            double eyeWorldY = seatAnchorY + eyeHeight;
            double hullTopY = eyeWorldY + CockpitVisibility.VISIBILITY_MARGIN + 0.01;
            assertFalse(CockpitVisibility.isFullyExposedAboveHull(seatAnchorY, eyeHeight, hullTopY));
        }

        @Test
        @DisplayName("a raised seat anchor (dy > 0) shifts the eye point up accordingly")
        void raisedSeatAnchorShiftsEyePoint() {
            // Same hullTopY (2.0) for both: at dy=0 the eye point (1.62) sits below it and is
            // concealed; raising the seat to dy=1 pushes the eye point (2.62) above it, exposing
            // the same player at the same eye height purely because the seat itself moved.
            assertFalse(CockpitVisibility.isFullyExposedAboveHull(0.0, 1.62, 2.0));
            assertTrue(CockpitVisibility.isFullyExposedAboveHull(1.0, 1.62, 2.0));
        }
    }
}
