# Shark Engine AIR Release 1 — Atomic Task Breakdown (Phase 1 TDD & QA Setup, Planner)

**Feature Slug:** `shark-engine-air-release-1`
**Author role:** Planner (Plumbline AgileTeam Phase 1, second half) — consumes the locked Canvas/
Vision/PRD/Traceability set and the tester's independently-derived acceptance/E2E test plan; does
**not** re-derive test strategy, only sequences the work the coder/reviewer loop will execute.
**Date:** 2026-07-19
**Status:** `ready-for-phase-2`

**Total atomic tasks (M): 24** — this is the `M` value the orchestrator's iteration-counter must
report against. It covers the 24 REQs *other than* REQ-025 (see Task #0 below, which is not part
of this count: it is executed directly by the orchestrator, not by the Phase 2 coder/reviewer
loop, and per the tester's plan is explicitly "not a Java test class" / "process gate").

**Governing loop limits for Phase 2 execution (already set for this run, restated here for the
executing loop, not re-derived):**
- `MAX_DEVREVIEW_LOOPS=4` — a single task may cycle through coder→reviewer at most 4 times before
  it must be escalated instead of silently retried a 5th time.
- `MAX_QA_RETURNS=3` — QA may bounce a task back to the coder at most 3 times before escalation.

**Locked source documents read for this plan (not modified):**
- `docs/canvas/shark-engine-air-release-1.canvas.md`
- `docs/vision/shark-engine-air-release-1.vision.md`
- `docs/prd/shark-engine-air-release-1.prd.md` (25 REQs, user-confirmed Canvas/Vision)
- `docs/traceability.md` (25/25 REQs linked)
- `docs/intake/PHASE-0.16-COUNCIL-CHALLENGE.md`
- `docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md` (tester's falsifying-test
  contract per REQ — this plan sequences work around that contract, it does not restate or
  re-derive it; each task below cites the tester's already-named test classes by name)
- `sharkengine/src/main/java/dev/sharkengine/` (package layout skim)
- Root `CLAUDE.md` (source-set split, datagen, GameTest-registration gotchas)

---

## Goal

Turn the 25 user-confirmed REQs into an atomic, dependency-ordered task sequence that a Phase 2
coder/reviewer loop can execute one task at a time, each task producing exactly the falsifying
tests the tester already specified for its REQ(s), without re-deriving test strategy and without
violating this repo's known structural gotchas (client/main source-set split, datagen-after-clean,
GameTest classpath-scanning).

## Non-Goals

- Not re-litigating any Canvas/Vision/PRD content, priority (P0/P1/P2), or the Phase 0.16 council's
  adopted amendments (REQ-022 demotion, REQ-025 Day-0 gate, REQ-018/019/020 single-item design).
- Not re-deriving or second-guessing the tester's test plan — this plan's "Tests to add/run" column
  is a pointer into that document, not a rewrite of it.
- Not producing the actual code, tests, or PR for any task — that is Phase 2's job.
- Not scheduling REQ-024 (release evidence gate) as anything other than the final task — it is a
  process/checklist gate over the exact release commit, not a feature to build early.

---

## Preconditions and known gaps

1. **REQ-025 (Task #0) must report GREEN before any task below opens a branch.** Per the PRD
   (REQ-025) and the tester's plan ("if this plan is being read by a coder who has not seen a
   green Day-0 report, that is itself a blocker — stop and escalate"), no task in this breakdown
   may start until the orchestrator's direct `./gradlew build` + `./gradlew runGametest` run on
   `main` is reported green. See Task #0 below for current status.
2. **GameTest registration gotcha applies to 15 of the 24 tasks below** (every task that names a
   new `@GameTest` class per the tester's consolidated list). Each such task's Definition of Done
   explicitly includes adding the class to `sharkengine/src/main/resources/fabric.mod.json`'s
   `"fabric-gametest"` array and confirming `./gradlew runGametest`'s reported total test count
   increased by the expected amount — this is *inside* the task's DoD, not a follow-up task, per
   this repo's own documented 2026-07-13 `ShipEntityMountGameTest` incident.
3. **Client/main source-set split applies wherever a task touches rendering, HUD, or input**
   (REQ-007 visibility interacts with `ShipEntityRenderer`/camera only cosmetically — the
   mandated mechanism is server-side per OQ-003; REQ-016's HUD sync touches
   `client/render/FuelHudOverlay.java`; REQ-019's render path touches `client/render/`). Any such
   task's DoD requires `./gradlew build` (not just `test`/`check`) to catch client-only compile
   breaks, per this repo's documented 2026-03-17→2026-07-12 undetected-break incident.
4. **`DyeColor` has no existing test shim.** The tester's plan flags this explicitly for REQ-018/
   019: either add a minimal `DyeColor` shim under `src/test/java/net/minecraft/world/item/
   DyeColor.java` (16-value enum, same pattern as the existing `BlockState`/`VoxelShape` shims) or
   design the component-resolution/tint-provider APIs around a primitive key. This decision is
   folded into Task T18 (REQ-018) since it is the first task in the chain that needs it.
5. **Datagen gotcha applies to every task that adds a new craftable block/item** (REQ-005, 009,
   018 at minimum): after registering a new block/item + recipe/loot/model/tag/lang entries, run
   `./gradlew runDatagen` and commit the regenerated `src/main/generated` output — it is not
   automatically regenerated by `clean` or plain `build`.
6. **`ShipAssemblyService.scanStructure()`'s existing radius check uses Manhattan distance**
   (`distManhattan`, line ~239) — this is the *wrong* idiom for REQ-012's Euclidean 5-block gate,
   per the tester's sharpened counter-thesis. T12's DoD explicitly forbids reusing that idiom and
   requires the diagonal-offset discriminating test cases the tester specified.
7. **REQ-017's persistence scope nominally includes "Trail-Konfiguration," but the DyeColor
   component (REQ-018/019) that trail config depends on is P1 and lands after REQ-017 (P0) in this
   sequence.** T17's DoD is scoped to the fields REQ-017 can fully test at that point
   (VehicleClass, blueprint, pilot/copilot seats, occupancy eligibility, fuel, damage, edit state)
   with a generic/optional trail-config NBT slot reserved but not yet DyeColor-populated; T19
   (REQ-019, P1) explicitly extends and closes the DyeColor-specific persistence round-trip once
   the component exists. This is called out so it is not silently dropped between the two tasks.
8. **REQ-022's architecture-conformance check is deliberately sequenced last within the P0 block**
   (T22), after every other P0 functional task (T01–T17, T21), per the run instructions: building
   the seam-extraction/call-site-scanning task before the concrete AIR features exist would
   contradict REQ-022's own amended text ("nur die Seams... die AIR... heute wirklich aufruft").

---

## Task #0 — REQ-025 Day-0 build/runtime verification gate (blocking, orchestrator-executed)

**Status:** Per `docs/prd/shark-engine-air-release-1.prd.md`'s RISK-001 entry, this gate has
already been executed by the orchestrator directly on `feature/shark-engine-air-release-1`
(source tree verified identical to `main` via `git diff main --stat` before execution — docs-only
diff, no code changes) and reported: `./gradlew build` → BUILD SUCCESSFUL in 26s (includes
`compileClientJava`); `./gradlew runGametest` → BUILD SUCCESSFUL in 18s, "All 18 required tests
passed :)" — see EV-025 in the PRD for the full record, including the two benign pre-existing log
lines observed and dismissed as non-test-relevant.

**This planner does not re-run or re-verify that gate** — it only records, per the run
instructions, that: (a) REQ-025 is task #0 and is already in progress/reported by the orchestrator
directly, not scheduled as one of the 24 numbered tasks below; (b) every task T01–T24 below is
blocked from opening a branch until that report is green. Per EV-025's own text this has already
happened for the current baseline — but this plan does not itself claim to have re-verified it on
today's exact HEAD, and any coder picking up T01 should confirm the green EV-025 report is still
current for the commit they are branching from before proceeding, not assume it silently carries
forward across arbitrary elapsed time.

**No Java test class** (per tester's plan: "Do not create a Java test class for REQ-025").

---

## Task Sequence Overview (dependency graph, left→right = build order)

```
Foundation:        T01(001) → T02(003) → T03(002) → T04(004,infra)
                                                        │
Seats/Cockpit:      T05(005) → T06(006) → T07(009) → T08(007) → T09(008) → T10(010) → T11(011)
                                                                       │
Edit Mode:                                                    T12(012) → T13(013) → T14(014) → T15(021)
                                                                       │
Flight/Persistence:                                          T16(015) → T17(016) → T18(017)
                                                                       │
Architecture Gate:                                                    T19(022)
                                                                       │
P1 Trail chain:                                                       T20(018) → T21(019) → T22(020)
                                                                       │
P2 Backlog:                                                           T23(023)
                                                                       │
Release Gate:                                                         T24(024)
```

Task IDs below (T01–T24) are stable identifiers for Phase 2. Each task maps to exactly one PRD
REQ except where noted.

---

## T01 — REQ-001: Vehicle route popup

**Depends on:** Task #0 (green)
**Priority:** P0

**Definition of Done (AC-001, EV-001):** Placing a Steering Wheel *and* right-clicking an
already-placed, previously-untouched Steering Wheel both trigger the same route-selection popup
path server-side; the resulting S2C payload/session state carries exactly 3 route identifiers
(AIR, LAND, WATER) — verified programmatically, not by screenshot (per tester's counter-thesis
that a screenshot proves nothing about which server event opened it).

**Files/packages likely affected:**
- `dev.sharkengine.content.block.SteeringWheelBlock` / `SteeringWheelItem` (server-side trigger on
  placement and interact — both paths, not just one)
- `dev.sharkengine.net.ModNetworking` + a new S2C payload class in `dev.sharkengine.net` carrying
  the 3 route identifiers (existing payload pattern: `ShipBlueprintS2CPayload`,
  `TutorialPopupS2CPayload`)
- New client-side popup screen under `dev.sharkengine.client` (client source set — mirrors the
  existing `dev.sharkengine.client.tutorial.TutorialPopupScreen` pattern; do not put this in
  `src/main/java`)

**Tests to add/run (per tester's plan, not re-derived here):**
- `dev.sharkengine.gametest.VehicleRoutePopupGameTest` `[NEEDS fabric.mod.json REGISTRATION]` —
  methods `popupOpensOnPlacement`, `popupOpensOnInteractWithExistingWheel`.
- Register in `sharkengine/src/main/resources/fabric.mod.json`'s `"fabric-gametest"` array; confirm
  `./gradlew runGametest` total test count increases by 2 (both `@GameTest` methods).
- `./gradlew build` (client screen compiles).

**Acceptance evidence:** GameTest output showing both trigger paths pass; `fabric.mod.json` diff
showing the new class registered; test-count delta confirmed in the run log.

---

## T02 — REQ-003: Server-owned build session

**Depends on:** T01
**Priority:** P0

**Definition of Done (AC-003, EV-003):** A parametrized C2S authorization matrix rejects each of 6
independent invalid axes (non-owner player, wrong dimension, out-of-range distance, expired
session, wrong/absent session id, duplicate/replayed request) with a full block-diff + entity-count
diff of zero, plus one positive control (all valid → session created) and a two-player
session-isolation check (concurrent sessions on two wheels don't cross-contaminate).

**Files/packages likely affected:**
- New package `dev.sharkengine.ship.session` (mirrors the existing `dev.sharkengine.ship.part`
  pattern per the tester's explicit recommendation) — `VehicleBuildSession` record/class + pure
  validation functions (player, dimension, position, distance, status, expiry), Fabric-free so it
  is unit-testable.
- `dev.sharkengine.net.ModNetworking` — wire the session lookup into the selection/assembly C2S
  handlers.

**Tests to add/run:**
- `dev.sharkengine.ship.session.VehicleBuildSessionValidationTest` — pure JUnit, no Fabric
  bootstrap.
- `dev.sharkengine.gametest.BuildSessionAuthorizationGameTest` `[NEEDS fabric.mod.json
  REGISTRATION]` — full server-authoritative flow with real `ServerPlayer`/dimension.
- Register in `fabric.mod.json`; confirm test-count delta.

**Acceptance evidence:** Matrix test output showing all 6 axes independently reject with zero
diff; positive control passes; isolation test passes.

---

## T03 — REQ-002: Release route availability

**Depends on:** T01, T02 (needs the popup trigger from T01 and the session mechanism from T02 to
know what "AIR creates a session" concretely means)
**Priority:** P0

**Definition of Done (AC-002, EV-002):** LAND and WATER remain interactable (not
disabled/greyed-out) and produce an explicit, distinguishable "coming soon / not available in
Release 1" feedback state with a verified-empty block-state + entity-count diff; AIR selection
results in exactly one `VehicleBuildSession` (from T02) tied to that player + wheel position.

**Files/packages likely affected:**
- `dev.sharkengine.content.block.SteeringWheelBlock` (route dispatch)
- `dev.sharkengine.ship.session.VehicleBuildSession` (from T02 — AIR path creates one, LAND/WATER
  paths explicitly don't)
- Client popup screen from T01 (LAND/WATER feedback state, non-disabled)
- `sharkengine/src/main/resources/assets/sharkengine/lang/en_us.json` (+ DE) for the "coming soon"
  string (NFR-007 EN/DE parity)

**Tests to add/run:**
- `dev.sharkengine.gametest.VehicleRouteAvailabilityGameTest` `[NEEDS fabric.mod.json
  REGISTRATION]` — `landSelectionIsInteractableButNonMutating`,
  `waterSelectionIsInteractableButNonMutating`, `airSelectionCreatesExactlyOneSession`.
- Register in `fabric.mod.json`; confirm test-count delta (+3).

**Acceptance evidence:** Three-method GameTest output; lang-key diff showing EN/DE parity for the
new feedback string.

---

## T04 — REQ-004: Complete craft/resource closure (registration-closure guard, built as
infrastructure)

**Depends on:** T01–T03 (established so the guard is live before REQ-005 onward starts registering
new blocks/items — this ordering is the real technical dependency: building the guard *before* the
first new-block task, T05, means every subsequent registration is caught by CI immediately instead
of drifting silently, closing the exact hand-maintained-array gap the tester's counter-thesis
describes)
**Priority:** P0

**Definition of Done (AC-004, EV-004):** The existing hand-maintained resource-contract literal ID
lists in `ResourceValidationTest` are extended to include all currently-registered components, AND
a new source-text-scanning meta-test parses `ModBlocks.java`/`ModItems.java`'s registration
call-site string literals and asserts every one also appears in the resource-contract lists — so
any block/item registered by a later task (T05, T07, T20, …) without updating the resource-contract
list fails CI loudly instead of silently shipping unlocalized/uncraftable. This task's DoD is the
existence and green state of the guard itself, not "all 25 REQs' components already closed" (they
don't exist yet) — full closure is what every subsequent block/item-adding task's own DoD is now
required to maintain against this guard.

**Files/packages likely affected:**
- `dev.sharkengine.ship.ResourceValidationTest` (extend existing file — `ALL_BLOCK_IDS`,
  `CRAFTABLE_IDS`, etc.)
- New nested class `RegistrationClosureTests` in the same file, or sibling
  `dev.sharkengine.ship.RegistryClosureTest`, for the source-scanning meta-test
- `dev.sharkengine.content.ModBlocks` / `ModItems` (read-only scan target, text-based per the
  `test` source set's no-Minecraft-dependency constraint — see tester's plan classpath note)

**Tests to add/run:**
- Extended `ResourceValidationTest` + new `RegistrationClosureTests`/`RegistryClosureTest` (pure
  JUnit, source-text-scanning idiom, no classloading).

**Acceptance evidence:** Meta-test green against current (pre-REQ-005) registrations; every
subsequent T05/T07/T20 task's DoD explicitly references "still green against T04's guard."

---

## T05 — REQ-005: Generic pilot seat

**Depends on:** T04 (registration-closure guard must be live before this task registers its first
new block)
**Priority:** P0

**Definition of Done (AC-005, EV-005):** A three-way partition GameTest proves the *count*
invariant, not just the happy path: zero pilot seats → assembly fails explicitly, world unchanged;
exactly one → succeeds; two pilot seats deliberately placed far apart in the structure (not
adjacent, to defeat a scan that only checks immediate duplicates) → fails explicitly, world
unchanged. Seat block/item is generic (no AIR-specific naming baked into the seat's own data model)
per REQ-005's "architektonisch auch für spätere LAND/WATER-Profile wiederverwendbar" requirement.

**Files/packages likely affected:**
- New `dev.sharkengine.content.block.PilotSeatBlock` (+ item) under `content/block`
- `dev.sharkengine.content.ModBlocks` / `ModItems` (registration — must also update T04's
  resource-contract lists in the same commit)
- `dev.sharkengine.ship.ShipAssemblyService` — seat-count validation, generalizing the existing
  role-based `hasThruster()`/`thrusterCount()` pattern (see `ship.part.PartRole`) rather than
  reintroducing ID-comparison counting
- `sharkengine/src/main/resources/data/sharkengine/tags/block/ship_eligible.json` (add the new
  block)
- Datagen: run `./gradlew runDatagen` after adding recipe/loot/model/tag/lang for the seat, commit
  `src/main/generated`

**Tests to add/run:**
- `dev.sharkengine.gametest.PilotSeatCountGameTest` `[NEEDS fabric.mod.json REGISTRATION]` —
  `zeroSeatsRejected`, `oneSeatAccepted`, `twoSeatsRejectedEvenWhenFarApart`.
- Register in `fabric.mod.json`; confirm test-count delta (+3).
- T04's `ResourceValidationTest`/`RegistryClosureTest` still green with the new block/item added to
  the literal lists.

**Acceptance evidence:** Three-method GameTest output; datagen diff committed;
`ResourceValidationTest` green.

---

## T06 — REQ-006: Pilot seat anchor

**Depends on:** T05 (explicit PRD/instruction dependency: the anchor resolution rule operates on
the pilot seat block T05 introduces)
**Priority:** P0

**Definition of Done (AC-006, EV-006):** Occupying the exact front-of-wheel-facing position with a
non-seat/invalid block makes assembly fail with an explicit error and the resulting blueprint has
**zero** SeatAnchor entries (not a silently-chosen alternate position). With the front slot free and
valid, tested across all 4 wheel-facing rotations (N/E/S/W), the SeatAnchor offset in the blueprint
equals the exact front-of-wheel offset for that facing and survives a round-trip through
`ShipTransform.rotateOffset()` plus a save/load cycle.

**Files/packages likely affected:**
- `dev.sharkengine.ship.ShipAssemblyService` — front-of-wheel offset resolution, reusing the
  `BugBlock`-facing pattern already established for ship direction (`directionToYaw()`)
- `dev.sharkengine.ship.ShipBlueprint` — add `SeatAnchor` field(s) to the record, NBT
  serialization (`toNbt`/schema version, per NFR-004)
- `dev.sharkengine.ship.ShipTransform` — reuse existing `rotateOffset()` as the single rotation
  authority (AIR-010) for the seat anchor too, do not introduce a second rotation path

**Tests to add/run:**
- `dev.sharkengine.ship.ShipTransformTest` — extend existing file with seat-anchor-offset cases
  (pure math, no Fabric bootstrap).
- `dev.sharkengine.gametest.PilotSeatAnchorGameTest` `[NEEDS fabric.mod.json REGISTRATION]` —
  occupied-position failure + full assembly flow across 4 facings.
- Register in `fabric.mod.json`; confirm test-count delta.

**Acceptance evidence:** `ShipTransformTest` green with new cases; GameTest showing zero
SeatAnchor entries on the failure path and correct offsets across all 4 rotations plus save/load.

---

## T07 — REQ-009: Craftable copilot seat

**Depends on:** T05 (reuses the generic SeatAnchor/seat-role representation REQ-005 establishes;
does not depend on T06's pilot-specific anchor-resolution logic, since the copilot seat's position
is not wheel-facing-derived)
**Priority:** P0

**Definition of Done (AC-009, EV-009):** A copilot seat already occupied by player A rejects
player B's interact attempt outright — A remains mounted, is not silently displaced (no dismount
event) — and the server-tracked occupancy count stays exactly 1 throughout. Server-authoritative
occupant id matches what an observer connection's tracked passenger list reports.

**Files/packages likely affected:**
- New `dev.sharkengine.content.block.CopilotSeatBlock` (+ item) under `content/block`
- `dev.sharkengine.content.ModBlocks` / `ModItems` (registration — update T04's resource-contract
  lists in the same commit)
- `dev.sharkengine.ship.ShipBlueprint` — extend `SeatAnchor` representation from T06 with a
  role field (PILOT/COPILOT) rather than a second parallel data structure
- `dev.sharkengine.ship.ShipEntity` — passenger-mount interaction, occupancy-rejection logic (must
  not overwrite an existing occupant reference)
- Datagen for the new block (recipe/loot/model/tag/lang), `./gradlew runDatagen`

**Tests to add/run:**
- `dev.sharkengine.gametest.CopilotSeatOccupancyGameTest` `[NEEDS fabric.mod.json REGISTRATION]` —
  `secondPlayerCannotDisplaceFirstCopilot`, `occupancyIsObserverConsistent`. Note the tester's
  explicit flag: true dual-real-client rendering desync is not GameTest-testable (single server
  instance) — this covers server-tracked state only; full cross-client visual proof remains a
  manual smoke test (EV-009), not silently claimed as covered here.
- Register in `fabric.mod.json`; confirm test-count delta (+2).
- T04's resource-contract guard still green.

**Acceptance evidence:** Two-method GameTest output; resource-contract green; explicit note in the
PR that cross-client visual desync remains a manual EV-009 smoke item.

---

## T08 — REQ-007: Cockpit visibility

**Depends on:** T06 (needs the resolved SeatAnchor Y-position to compute eye-height against)
**Priority:** P0

**Definition of Done (AC-007, EV-007):** A pure server-side function computing "is fully exposed
above hull" takes only `SeatAnchor` Y-offset + eye-height as inputs — its signature structurally
cannot accept an armor or skin parameter — proven by asserting two simulated players with different
armor/skin at the *same* eye height produce identical results. A GameTest confirms the seat's
Y-position places the eye-height point below the top face of the tallest adjacent hull block by a
fixed margin, exercised against a tall (2+ block) hull wall.

**Files/packages likely affected:**
- New `dev.sharkengine.ship.CockpitVisibilityTest`-adjacent production class, e.g.
  `dev.sharkengine.ship.CockpitVisibility` (pure logic, plain doubles/offsets, no armor/skin
  parameter in its signature by construction)
- `dev.sharkengine.ship.ShipEntity` — wire the eye-height check into seat occupancy/tick logic
  (server-side only; do **not** implement this as a client-only render-layer trick per the
  tester's counter-thesis)

**Tests to add/run:**
- `dev.sharkengine.ship.CockpitVisibilityTest` — new file, pure JUnit, no Minecraft imports needed.
- `dev.sharkengine.gametest.CockpitVisibilityGameTest` `[NEEDS fabric.mod.json REGISTRATION]`.
- Register in `fabric.mod.json`; confirm test-count delta.

**Acceptance evidence:** Unit test proving armor/skin-invariance by construction (not just by one
sampled case); GameTest against a tall hull wall.

---

## T09 — REQ-008: Pilot control authority

**Depends on:** T06 (pilot seat/anchor established), T07 (copilot seat must exist for the
discriminating "copilot is a rider but not the pilot" test case — per the tester's counter-thesis,
a test suite that only checks a non-rider case would never catch a check that conflates "is riding"
with "is the pilot")
**Priority:** P0

**Definition of Done (AC-008, EV-008):** Three-way C2S authorization matrix against the helm-input
payload path: non-rider input → rejected, no state change; copilot input (genuinely riding, via
T07's seat) → rejected, velocity/yaw/fuel/anchor/edit-state byte-identical before/after (the
discriminating case); pilot input → accepted, state changes as expected (positive control).

**Files/packages likely affected:**
- `dev.sharkengine.net.HelmInputC2SPayload` + its handler (likely in `ShipEntity` or a
  `ModNetworking` dispatch method) — add pilot-specific (not merely rider-specific) authorization
- `dev.sharkengine.ship.ShipEntity` — role lookup against the occupied SeatAnchor

**Tests to add/run:**
- `dev.sharkengine.gametest.PilotControlAuthorityGameTest` `[NEEDS fabric.mod.json REGISTRATION]`
  — `nonRiderInputRejected`, `copilotInputRejectedDespiteBeingARider`, `pilotInputAccepted`.
- Register in `fabric.mod.json`; confirm test-count delta (+3).

**Acceptance evidence:** Three-method GameTest output, with the copilot-discriminating case
explicitly passing (not just the easy non-rider case).

---

## T10 — REQ-010: Passive copilot behavior

**Depends on:** T09 (input-rejection is already covered as T09's discriminating case; T10 adds the
distinct dismount-corruption failure mode) and T07 (copilot seat)
**Priority:** P0

**Definition of Done (AC-010, EV-010):** Mid-flight (non-zero velocity, active fuel burn) copilot
dismount: pilot remains mounted, retains full control authority, unaffected; fuel/velocity/vehicle
NBT immediately before/after dismount are identical except the copilot's own occupant slot; the
vacated copilot seat is immediately re-mountable by a different player within the same test run
(this directly feeds T11/REQ-011's re-entry test as a regression guard, per the tester's plan).

**Files/packages likely affected:**
- `dev.sharkengine.ship.ShipEntity` — dismount handler; must not use a shared "occupants" list
  cleared on any dismount (the specific bug class the tester's counter-thesis names)

**Tests to add/run:**
- `dev.sharkengine.gametest.CopilotDismountIntegrityGameTest` `[NEEDS fabric.mod.json
  REGISTRATION]` — `midFlightDismountLeavesPilotUnaffected`, `vacatedSeatIsImmediatelyRemountable`.
- Register in `fabric.mod.json`; confirm test-count delta (+2).

**Acceptance evidence:** Two-method GameTest output; explicit NBT-diff assertion showing only the
copilot slot changed.

---

## T11 — REQ-011: Vehicle re-entry

**Depends on:** T10 (re-entry's remount path is exercised as a direct continuation of T10's
vacated-seat dismount test per the tester's plan — sequencing T11 right after T10 lets it reuse
that scenario as its starting state rather than re-deriving it)
**Priority:** P0

**Definition of Done (AC-011, EV-011):** Capture the `ShipEntity`'s unique entity UUID, fuel level,
and damage/trail-configuration state before dismount. After remount, assert the **same** entity
UUID is ridden (no despawn/respawn detected) and fuel/damage/trail are byte-identical to
pre-dismount values. The received role matches exactly the seat interacted with (pilot seat →
pilot role, copilot seat → copilot role), not a default-to-whatever-free-role.

**Files/packages likely affected:**
- `dev.sharkengine.ship.ShipEntity` — remount/interact logic; must toggle occupancy on the
  *existing* entity, not trigger a re-scan/reassembly path (the specific bug class the tester's
  counter-thesis names — re-running REQ-005/006's seat-count/anchor validation on every remount
  interact would either wrongly reject or silently respawn the entity)

**Tests to add/run:**
- `dev.sharkengine.gametest.VehicleReentryGameTest` `[NEEDS fabric.mod.json REGISTRATION]` —
  `remountPreservesEntityIdentityAndState`, `remountGrantsExactlyTheInteractedSeatsRole`.
- Register in `fabric.mod.json`; confirm test-count delta (+2).

**Acceptance evidence:** Two-method GameTest output; explicit UUID-identity assertion (not just
role correctness, which alone cannot distinguish a legitimate remount from a silent rebuild).

---

## T12 — REQ-012: Safe edit-mode gate

**Depends on:** T02 (reuses the session/control-anchor concept established for build sessions) —
independent of the seat/cockpit chain (T05–T11); may in principle run in parallel with it, but is
sequenced after T02 here since it is the next foundational gate before the builder-reopen chain
(T13) can exist
**Priority:** P0

**Definition of Done (AC-012, EV-012):** Parametrized boundary test using offsets where Euclidean
and Manhattan/Chebyshev verdicts **disagree**: offset (3,3,0) → Euclidean ≈4.24 (≤5, must ACCEPT)
vs. Manhattan=6 (would wrongly REJECT if the existing `ShipAssemblyService.scanStructure()`
Manhattan idiom were reused) — this case alone falsifies a Manhattan-based implementation. Offset
(3,4,0) → Euclidean = 5.0 exactly (boundary, must ACCEPT). Offset (3,4,1) → Euclidean ≈5.10 (must
REJECT). Combined with stationary/safe/conflict-free precondition checks (damaged/moving vehicle →
rejected regardless of distance).

**Files/packages likely affected:**
- New pure-logic distance function — do **not** reuse `ShipAssemblyService.scanStructure()`'s
  `distManhattan` idiom (existing codebase pattern flagged as the wrong metric for this REQ per
  the tester's sharpest counter-thesis); implement Euclidean distance from raw coordinates.
- `dev.sharkengine.ship.ShipEntity` or a new edit-mode gate class — wires the distance check +
  stationary/safe/conflict-free preconditions together server-side.

**Tests to add/run:**
- `dev.sharkengine.ship.EditModeDistanceTest` — new file, plain doubles, no Minecraft imports.
- `dev.sharkengine.gametest.EditModeDistanceGameTest` `[NEEDS fabric.mod.json REGISTRATION]` — full
  gate with real world state.
- Register in `fabric.mod.json`; confirm test-count delta.

**Acceptance evidence:** Unit test explicitly showing the (3,3,0) case passes Euclidean and would
fail Manhattan; GameTest showing the stationary/safe/conflict-free preconditions independently
gate.

---

## T13 — REQ-013: Builder reopen

**Depends on:** T12 (edit mode must open before the builder can reopen inside it)
**Priority:** P0

**Definition of Done (AC-013, EV-013):** Modify the vehicle's structure via one prior edit-session
commit (block count N → N+1), then reopen edit mode in a later, separate interaction and assert the
builder preview payload's block list exactly matches the **current** (N+1) blueprint — compared
against a live re-scan, not a cached snapshot from session start.

**Files/packages likely affected:**
- `dev.sharkengine.client.builder.BuilderScreen` / `BuilderModeClient` / `PreviewState` (client
  source set)
- `dev.sharkengine.net.BuilderPreviewS2CPayload` — must be re-derived from a live scan on reopen,
  not served from a stale cached snapshot

**Tests to add/run:**
- `dev.sharkengine.gametest.BuilderReopenGameTest` `[NEEDS fabric.mod.json REGISTRATION]`.
- Register in `fabric.mod.json`; confirm test-count delta.
- `./gradlew build` (client-side `BuilderScreen`/`PreviewState` changes — do not rely on `test`/
  `check` alone, per the client compile gotcha).

**Acceptance evidence:** GameTest showing the reopened builder payload matches the live N+1 scan,
not a stale N-block snapshot.

---

## T14 — REQ-014: Atomic edit/reassembly

**Depends on:** T13 (builder must be open/editable before its exit-commit path can be tested)
**Priority:** P0

**Definition of Done (AC-014, EV-014):** For a deliberately-invalid final edit, assert **zero**
block-state changes occur anywhere in the structure's bounding box at any point during the attempt
(not "net zero after undo") — preflight validation before any world mutation, not
apply-then-rollback. Companion positive-path test: a valid edit commits fully and the block count
changes as expected.

**Files/packages likely affected:**
- `dev.sharkengine.ship.ShipAssemblyService` — edit-commit path: validate against AIR policy
  *before* touching the world, not apply-then-undo
- `dev.sharkengine.net.BuilderAssembleC2SPayload` handler

**Tests to add/run:**
- `dev.sharkengine.gametest.AtomicEditReassemblyGameTest` `[NEEDS fabric.mod.json REGISTRATION]` —
  `invalidEditNeverTouchesWorld`, `validEditCommitsFully`.
- Register in `fabric.mod.json`; confirm test-count delta (+2).

**Acceptance evidence:** GameTest showing zero block-state diff during a rejected commit attempt
(preflight, not rollback-after-mutation) and full commit on the valid path.

---

## T15 — REQ-021: Transactional world mutation (assembly + disassembly)

**Depends on:** T14 (per the tester's plan, this task explicitly **reuses REQ-014's
preflight-before-mutation pattern** for the assembly failure path — T14 establishes that pattern
first, T15 extends it to assembly/disassembly rather than inventing a second mechanism)
**Priority:** P0

**Definition of Done (AC-021, EV-021):** (a) Assembly failure paths (spawn position blocked by
another entity, or structure becomes invalid between preview and commit — a TOCTOU race) → assert
zero mutation, reusing T14's preflight pattern. (b) Disassembly restores the world block count
**exactly** equal to the blueprint's block count (not "≥1 restored"), tested at both a small
near-origin structure and a structure near `ShipAssemblyService.MAX_BLOCKS`/`MAX_RADIUS` (512
blocks / 32 radius) to surface large-structure partial-restore bugs a small smoke structure can't
reveal.

**Files/packages likely affected:**
- `dev.sharkengine.ship.ShipAssemblyService` — disassembly loop (per-position `setBlock`), applying
  the same preflight-before-mutation discipline established in T14

**Tests to add/run:**
- `dev.sharkengine.gametest.AssemblyDisassemblyRollbackGameTest` `[NEEDS fabric.mod.json
  REGISTRATION]` — `assemblyFailurePathNeverMutatesWorld`,
  `disassemblyRestoresExactBlockCountAtSmallScale`, `disassemblyRestoresExactBlockCountAtMaxScale`.
- Register in `fabric.mod.json`; confirm test-count delta (+3).

**Acceptance evidence:** Three-method GameTest output including the max-scale (512-block/32-radius)
disassembly case. Note the tester's explicit flag: true chunk-unload-mid-disassembly is not
exercisable inside a single-chunk GameTest template — this remains a documented coverage gap, not
silently claimed as covered by the max-scale test (which stresses size, not chunk boundaries).

---

## T16 — REQ-015: AIR flight controls

**Depends on:** T09 (needs pilot control authority established so flight-control input has a
defined authorized sender)
**Priority:** P0

**Definition of Done (AC-015, EV-015):** A sustained turn-input payload changes the ship's **yaw**
over N simulated ticks by a nonzero, input-direction-consistent amount — not merely a cosmetic
roll/pitch render field (the tester's counter-thesis: bank/turn implemented as purely cosmetic
client-side visual roll would visibly "bank" while heading never turns). A fuzz-style boundary
sweep (min/max throttle × min/max turn, zero fuel) across a multi-tick loop never produces
NaN/Infinity in position, velocity, or yaw.

**Files/packages likely affected:**
- `dev.sharkengine.ship.ShipPhysics` — bank/turn must change yaw, not just a render-only tilt field
- `dev.sharkengine.ship.AccelerationPhase` (existing — speed/height-penalty/phase math already
  covered by `ShipPhysicsTest`/`AccelerationPhaseTest`; this task's genuinely new surface is
  bank/turn only)
- `dev.sharkengine.ship.ShipEntity` (tick-level wiring)

**Tests to add/run:**
- Extend `dev.sharkengine.ship.ShipPhysicsTest`, or new
  `dev.sharkengine.ship.FlightControlAuthorityTest` (pure logic, no Fabric bootstrap).

**Acceptance evidence:** Unit test showing yaw changes by a nonzero, direction-consistent amount
under sustained turn input; fuzz sweep producing no NaN/Infinity across the full input range.

---

## T17 — REQ-016: Fuel and speed loop

**Depends on:** T16 (fuel consumption "during powered flight" needs the flight-control loop to
exist)
**Priority:** P0

**Definition of Done (AC-016, EV-016):** A fuel-sync S2C payload is transmitted on **every**
fuel-changing tick (not just mount/dismount), matching the entity's authoritative fuel NBT at that
exact tick — testing sync *cadence*, not merely that a sync happens once. A `FuelSystemTest`-style
save/load round-trip: serialize a mid-flight (non-round-number) fuel value, deserialize, assert
exact match with no rounding/truncation drift.

**Files/packages likely affected:**
- `dev.sharkengine.ship.FuelSystem` (existing — conversion math already covered by
  `FuelSystemTest`; this task's new surface is sync cadence + precision round-trip)
- `dev.sharkengine.ship.ShipEntity` — per-tick fuel-sync payload dispatch
- `dev.sharkengine.client.render.FuelHudOverlay` (client source set — HUD must reflect the same
  cadence, not a slower interval)

**Tests to add/run:**
- `dev.sharkengine.gametest.FuelSyncCadenceGameTest` `[NEEDS fabric.mod.json REGISTRATION]`.
- Extend `dev.sharkengine.ship.FuelSystemTest` with the save/load precision round-trip.
- Register in `fabric.mod.json`; confirm test-count delta.
- `./gradlew build` (HUD overlay is client-only).

**Acceptance evidence:** GameTest showing per-tick sync cadence matches authoritative NBT; unit
test showing zero drift on a non-round-number fuel value round-trip.

---

## T18 — REQ-017: Persistence and restart

**Depends on:** T06 (pilot seat), T07 (copilot seat), T14 (edit state), T17 (fuel) — needs all of
these fields to exist before their persistence can be tested
**Priority:** P0

**Definition of Done (AC-017, EV-017):** Assemble a ship with both seats populated, non-round-number
fuel spent, then persist and reload (in-place `ServerLevel` save-to-NBT-and-reload cycle — the
closest pre-implementation-testable proxy to a true OS-level dedicated-server restart, per the
tester's explicit flag that full process-restart coverage is not GameTest-achievable and remains a
manual REQ-024 release-evidence step), asserting each of these fields **individually**:
VehicleClass, blueprint, pilot seat, copilot seat, fuel, damage, edit state. **Trail-DyeColor field
is explicitly scoped OUT of this task** (see Preconditions §7) — a generic/optional NBT slot for
trail config is reserved here but its DyeColor-specific round-trip is closed by T21 (REQ-019) once
the component exists.

**Files/packages likely affected:**
- `dev.sharkengine.ship.ShipEntity` — NBT read/write, schema version (NFR-004)
- `dev.sharkengine.ship.ShipBlueprint` — `toNbt`/`fromNbt`, seat/role serialization

**Tests to add/run:**
- `dev.sharkengine.gametest.VehiclePersistenceRestartGameTest` `[NEEDS fabric.mod.json
  REGISTRATION]` — asserts VehicleClass, blueprint, pilot seat, copilot seat, fuel, damage, edit
  state individually (not aggregate "vehicle count > 0").
- Register in `fabric.mod.json`; confirm test-count delta.

**Acceptance evidence:** GameTest output with 6 individually-asserted field checks (7th,
trail-DyeColor, explicitly deferred to T21 per Preconditions §7 — not silently dropped).

---

## T19 — REQ-022: Minimal Vehicle Core, no premature generalization (architecture-conformance
gate, last P0 functional task)

**Depends on:** T01–T18 (deliberately last in the P0 functional block — per the run instructions,
this task inspects what AIR's *actual, already-implemented* code calls; scheduling it before T01–
T18 exist would contradict REQ-022's own amended text, which is scoped to "was AIR heute wirklich
aufruft")
**Priority:** P0

**Definition of Done (AC-022, EV-022):** Two-sided architecture check, not just the PRD's own
one-sided literal rule (per the tester's counter-thesis — a zero-extraction implementation would
vacuously satisfy "≥1 call-site or fail" while defeating NFR-003 and CAN-011's actual purpose):
(a) for every interface/abstract class under any newly-introduced seam package, a source-scanning
test counts real call-sites in `src/main/java`/`src/client/java` and fails if any such interface
has zero (AC-022's literal rule). (b) A companion NFR-003 regression guard asserts `ShipEntity.
tick()` (and direct callees) contains no new `if (vehicleClass == VehicleClass.LAND)`/`WATER`-shaped
conditional branches — closing the opposite failure (everything inlined, a growing switch instead
of any extraction).

**Files/packages likely affected:**
- Whatever seam package(s) actually emerged from T01–T18's implementation (e.g. a
  `dev.sharkengine.ship.core`-style package, if one was introduced — this task audits the real
  outcome, it does not itself introduce new abstraction)
- `dev.sharkengine.ship.ShipEntity` (the `tick()` conditional-growth guard target)

**Tests to add/run:**
- `dev.sharkengine.ship.VehicleCoreSeamCallSiteTest` — new file, text-based regex scan of `.java`
  source, following `ResourceValidationTest`'s established idiom.
- `dev.sharkengine.ship.ShipEntityConditionalGrowthTest` — asserts no new AIR/LAND/WATER
  conditional branches in `ShipEntity.tick()`'s source text vs. a documented baseline.

**Acceptance evidence:** Both source-scanning tests green. Per the tester's flag, this is
best-effort regex-based static analysis that cannot perfectly distinguish a non-trivial call-site
from a trivial pass-through wrapper — genuinely ambiguous extraction decisions must be flagged for
human architecture review at PR time, not silently auto-passed.

---

## T20 — REQ-018: Single Thruster item with craft-time DyeColor component (P1, chain start)

**Depends on:** T19 (P0 completion boundary — per the run instructions, P1 is ordered after P0
completion, not interleaved, and no real technical dependency forces REQ-018 earlier: it needs
`ModItems`/datagen infra from T04 but not any P0 gameplay REQ specifically)
**Priority:** P1

**Definition of Done (AC-018, EV-018):** Registry-enumeration test asserts **exactly one** thruster
item/block id exists across `ModItems`/`ModBlocks` — fails loudly if a second
`thruster_<color>`-style id appears (directly falsifying a reversion to the rejected 16-item
design under deadline pressure, the tester's sharpest named risk for this REQ). Recipe-count test
asserts exactly one recipe JSON file governs the colored-thruster craft path. Interaction-path test:
right-clicking an already-**placed** colored thruster with any dye item is a no-op — component
value unchanged before/after.

**Files/packages likely affected:**
- `dev.sharkengine.content.ModItems` — the single Thruster item definition with a craft-time
  DyeColor data component (not 16 separate item ids)
- `dev.sharkengine.datagen.SharkEngineRecipeProvider` — exactly one recipe, appearing as one
  generic recipe-book entry for all 16 dyes (per REQ-018's explicit recipe-book-visibility clause)
- New test shim `src/test/java/net/minecraft/world/item/DyeColor.java` (16-value enum, per
  Preconditions §4) **or** a component-resolution API designed around a primitive key — this
  task's DoD includes making that explicit choice, since it is the first task in the chain that
  needs it
- `./gradlew runDatagen` after adding the recipe/model/lang entries; commit `src/main/generated`

**Tests to add/run:**
- Extend `dev.sharkengine.ship.ResourceValidationTest` with a `ThrusterDyeComponentResourceTests`
  nested class (exactly-one-item-id + exactly-one-recipe checks).
- `dev.sharkengine.gametest.ThrusterRecolorRejectionGameTest` `[NEEDS fabric.mod.json
  REGISTRATION]` — post-placement dye-no-op.
- Register in `fabric.mod.json`; confirm test-count delta.
- T04's resource-contract guard still green with the new single item added.

**Acceptance evidence:** Enumeration test showing exactly 1 item id, exactly 1 recipe file;
post-placement no-op GameTest.

---

## T21 — REQ-019: Persistent colored trail via single render path

**Depends on:** T20 (needs the DyeColor component to exist), T18/REQ-017 (extends and closes the
trail-DyeColor persistence round-trip explicitly deferred there per Preconditions §7)
**Priority:** P1

**Definition of Done (AC-019, EV-019):** Resource-contract test asserts **exactly one**
trail/thruster particle texture file exists under the relevant assets path — directly falsifying a
texture-swap implementation (16 baked particle textures switched by color) regardless of how
correct it looks visually. Pure-function unit test of the tint provider (`DyeColor → RGB`, no
Fabric dependency): the **same** function call, exercised across all 16 dye values plus the
"no component" default, proving one code path handles 17 cases rather than 17 separate paths. Also
closes T18's deferred persistence slot: a componented thruster's DyeColor value round-trips through
save/reload identically to T18's other persisted fields.

**Files/packages likely affected:**
- `dev.sharkengine.ship.ShipParticles` (main source set — particle emission is server-triggered)
- `dev.sharkengine.client.render.ShipEntityRenderer` (client source set — tinted-texture/
  color-provider render path, one implementation for all colors)
- `dev.sharkengine.ship.ShipEntity` — extend T18's persistence NBT path to round-trip the DyeColor
  value

**Tests to add/run:**
- Extend `dev.sharkengine.ship.ResourceValidationTest` with a `TrailTextureResourceTests` nested
  class.
- `dev.sharkengine.ship.TintProviderTest` — new file, pure function, uses the `DyeColor`
  shim/primitive-key decision from T20.
- Extend `dev.sharkengine.gametest.VehiclePersistenceRestartGameTest` (from T18) or add a
  dedicated trail-persistence assertion to close the deferred 7th field.
- `./gradlew build` (renderer changes are client-only).

**Acceptance evidence:** Texture-count test showing exactly 1 file; tint-provider unit test
covering all 17 cases (16 dyes + default) through one function; persistence round-trip closing
T18's deferred field.

---

## T22 — REQ-020: Trail isolation and bounded rendering

**Depends on:** T21 (needs the render path and component resolution to exist before checking they
don't leak into gameplay values)
**Priority:** P1

**Definition of Done (AC-020, EV-020):** `VehiclePartRegistry.resolve()` returns the **identical**
`VehiclePartDefinition` for the base thruster id regardless of any DyeColor component being present
— resolution must key on block/item identity, not full-stack NBT (the tester's named risk: a
component-aware registry lookup could silently resolve a colored thruster to a different/missing
part definition, quietly changing mass/lift/fuelCapacity, not just a cosmetic value). Two
otherwise-identical ships (one all-default thrusters, one all-colored) produce bit-identical
`ShipStats` (mass/lift/thrust/drag/fuelCapacity). Per OQ-005/NFR-006, no hard numeric performance
gate — only a qualitative stress smoke test.

**Files/packages likely affected:**
- `dev.sharkengine.ship.part.VehiclePartRegistry` — `resolve()` must key on base item/block
  identity, not `ItemStack`-with-components identity
- `dev.sharkengine.ship.part.ShipPartAnalyzer` — `ShipStats` aggregation

**Tests to add/run:**
- Extend `dev.sharkengine.ship.part.VehiclePartRegistryTest` and/or
  `dev.sharkengine.ship.part.ShipPartAnalyzerTest`.
- `dev.sharkengine.gametest.TrailParticleStressGameTest` `[NEEDS fabric.mod.json REGISTRATION,
  qualitative-only]` — N simultaneous colored vehicles don't throw or hang; explicitly not a
  performance regression gate (deliberate, user-approved coverage gap per OQ-005, not silently
  "tested").
- Register in `fabric.mod.json`; confirm test-count delta.

**Acceptance evidence:** Unit test showing identical `VehiclePartDefinition`/`ShipStats` regardless
of color; stress GameTest completing without throw/hang, explicitly labeled qualitative-only in the
PR description (not claimed as a performance gate).

---

## T23 — REQ-023: Looping backlog only (P2)

**Depends on:** T16/REQ-015 (needs `AccelerationPhase` to exist so the negative-regression
assertion has something to check against), and sequenced after the full P1 chain (T20–T22) per the
run instructions (P2 ordered after P0/P1, no technical dependency forces it earlier)
**Priority:** P2

**Definition of Done (AC-023, EV-023):** A backlog entry for looping exists with the confirmed
design notes referenced by REQ-023. A **negative** regression test against T16's physics code
asserts no new `AccelerationPhase` enum value or loop-related field exists in the shipped physics
code — a direct, falsifiable guard against the tester's named scope-creep risk (a coder working on
REQ-015's bank/turn "helpfully" wiring partial looping support).

**Files/packages likely affected:**
- A backlog/docs file (e.g. `sharkengine/docs/BACKLOG.md` or equivalent — location to be confirmed
  against whatever backlog doc convention the repo already uses, if any; create one if none exists,
  scoped only to this looping entry)
- `dev.sharkengine.ship.AccelerationPhase` (read-only regression target, no changes expected)

**Tests to add/run:**
- `dev.sharkengine.ship.LoopingBacklogDocumentationTest` — new file, resource-contract pattern,
  doc-existence check.
- Extend `dev.sharkengine.ship.AccelerationPhaseTest` (existing file) with a
  `noLoopRelatedPhaseIntroduced` assertion over `AccelerationPhase.values()`.

**Acceptance evidence:** Doc-existence test green; negative regression test green (proving T16
introduced no dormant loop-related surface).

---

## T24 — REQ-024: Release evidence gate (final task, process-level)

**Depends on:** T01–T23 (all of them — this is the release-artifact gate over the exact final
commit, not a feature to build early; per the traceability matrix's own `canvas-risk-status` note,
it is deliberately a single un-decomposed checkpoint since the user explicitly declined the
council's incremental per-REQ gating alternative in Phase 0.16 Round 2)
**Priority:** P0

**Definition of Done (AC-024, EV-024):** Every release-evidence entry — build, unit/resource tests,
Fabric GameTests, client smoke, dedicated-server smoke, two-player smoke, restart proof — is tagged
with the **same** git commit SHA; sign-off is refused if any entry's SHA differs from the others
(the tester's named false-positive: evidence collected piecemeal across different commits during
the ~12-day window, declared "evidenced" by union of ever-passing runs rather than one exact
commit).

**Files/packages likely affected:** None (process/checklist artifact, not a code change). Suggested
location: a release-evidence checklist doc (e.g. `docs/release/2026-07-30-release-evidence.md` or
equivalent), referencing `.github/workflows/ci.yml`/`deploy.yml` run ids for that exact commit SHA.

**Tests to add/run:** **None — flagged blocker, not a Java test class**, per the tester's explicit
instruction ("Cannot be verified pre-implementation by this repo's automated test suite"). This is
enforced by the orchestrator/Product Owner at release time via the single-commit-SHA checklist
above, not by CI.

**Acceptance evidence:** A single checklist document with one commit SHA column, all 7 evidence
rows populated against that SHA, reviewed and signed off by the Product Owner before the release
artifact is published.

---

## Risks and rollback notes

- **Loop-cap exhaustion:** if any task (most likely T06, T09, T14, T15, or T19 — the ones with the
  sharpest, most specific counter-theses in the tester's plan) exhausts `MAX_DEVREVIEW_LOOPS=4`
  without a green reviewer pass, escalate rather than silently extending the loop; do not relax
  the cap mid-run.
- **QA bounce exhaustion:** if a task is returned by QA 3 times (`MAX_QA_RETURNS=3`) and a 4th
  return would be needed, escalate to human review rather than auto-retrying — this usually
  signals the task's Definition of Done was under-specified, not that the coder needs one more
  attempt.
- **GameTest silent-zero regression:** every task above that registers a new GameTest class states
  an explicit expected test-count delta. If `./gradlew runGametest`'s reported total does not
  increase by exactly that amount after a task claims completion, treat it as a failed task, not a
  passed one with a documentation gap — this is the exact repo-documented failure mode from
  2026-07-13.
- **Client-only compile break:** any task touching `src/client/java` (T01, T13, T17, T21) must run
  `./gradlew build`, not just `test`/`check`, before being marked done — per the repo's documented
  2026-03-17→2026-07-12 undetected-break incident.
- **Datagen drift after `clean`:** if any task's branch runs `./gradlew clean` for any reason, it
  must run `./gradlew runDatagen` before `build`/`test`/`runGametest`, or every datagen-migrated
  block's tests go red for a reason unrelated to the task's actual change.
- **REQ-017/REQ-019 split-field risk:** T18 deliberately ships without the trail-DyeColor
  persistence field fully proven (Preconditions §7). If T20/T21 (the P1 chain) are ever descoped,
  deprioritized, or slip past the release date, T18's reserved-but-unpopulated NBT slot must be
  explicitly re-flagged as an open gap in T24's release-evidence checklist — not silently treated
  as "persistence is done" on the strength of T18 alone.
- **REQ-022 timing risk:** because T19 is deliberately last in the P0 functional block, if any
  P0 task after T19 were ever inserted retroactively, T19's call-site/conditional-growth checks
  would need to be re-run against the final state before T24 — this sequence assumes T19 is truly
  last among P0 functional tasks (T20 onward is P1/P2/release-gate only, which per REQ-022's own
  scope ("was AIR heute wirklich aufruft") should not introduce new LAND/WATER-shaped duplication
  for T19 to have missed).

---

## Traceability quick-reference (Task ID → REQ ID → AC/EV ID)

| Task | REQ | AC/EV | Priority | Depends on |
|---|---|---|---|---|
| #0 | REQ-025 | AC-025/EV-025 | P0 (Day-0 gate) | — (orchestrator-executed, already reported per PRD) |
| T01 | REQ-001 | AC-001/EV-001 | P0 | #0 |
| T02 | REQ-003 | AC-003/EV-003 | P0 | T01 |
| T03 | REQ-002 | AC-002/EV-002 | P0 | T01, T02 |
| T04 | REQ-004 | AC-004/EV-004 | P0 | T01–T03 |
| T05 | REQ-005 | AC-005/EV-005 | P0 | T04 |
| T06 | REQ-006 | AC-006/EV-006 | P0 | T05 |
| T07 | REQ-009 | AC-009/EV-009 | P0 | T05 |
| T08 | REQ-007 | AC-007/EV-007 | P0 | T06 |
| T09 | REQ-008 | AC-008/EV-008 | P0 | T06, T07 |
| T10 | REQ-010 | AC-010/EV-010 | P0 | T07, T09 |
| T11 | REQ-011 | AC-011/EV-011 | P0 | T10 |
| T12 | REQ-012 | AC-012/EV-012 | P0 | T02 |
| T13 | REQ-013 | AC-013/EV-013 | P0 | T12 |
| T14 | REQ-014 | AC-014/EV-014 | P0 | T13 |
| T15 | REQ-021 | AC-021/EV-021 | P0 | T14 |
| T16 | REQ-015 | AC-015/EV-015 | P0 | T09 |
| T17 | REQ-016 | AC-016/EV-016 | P0 | T16 |
| T18 | REQ-017 | AC-017/EV-017 | P0 | T06, T07, T14, T17 |
| T19 | REQ-022 | AC-022/EV-022 | P0 | T01–T18 |
| T20 | REQ-018 | AC-018/EV-018 | P1 | T19 |
| T21 | REQ-019 | AC-019/EV-019 | P1 | T18, T20 |
| T22 | REQ-020 | AC-020/EV-020 | P1 | T21 |
| T23 | REQ-023 | AC-023/EV-023 | P2 | T16, T22 |
| T24 | REQ-024 | AC-024/EV-024 | P0 (release gate) | T01–T23 |

**Coverage check:** 25/25 REQs accounted for — 24 as numbered Phase 2 tasks (T01–T24, M=24) + 1 as
Task #0 (REQ-025, already executed by the orchestrator, not part of the Phase 2 loop's M count).

---

## Not touched by this plan

Per the run instructions: Canvas, Vision, PRD, `traceability.md`, and the tester's test plan file
were read but not modified. No new requirements, acceptance criteria, or test strategies were
invented — every task above cites an existing REQ-NNN/AC-NNN/EV-NNN triple and the tester's
already-named test class(es) for it.
