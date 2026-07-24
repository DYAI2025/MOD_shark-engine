# Shark Engine AIR Release 1 — Acceptance/E2E Test Plan (Phase 1 TDD & QA Setup)

**Feature Slug:** `shark-engine-air-release-1`
**Author role:** Tester/QA (Plumbline AgileTeam Phase 1) — derived independently, black-box, from the
locked spec set below, BEFORE any coder starts. This document is a contract, not a suggestion.
**Date:** 2026-07-18
**Status:** `draft-for-planner-and-coder`

**Locked source documents read for this plan (not modified):**
- `docs/canvas/shark-engine-air-release-1.canvas.md`
- `docs/vision/shark-engine-air-release-1.vision.md`
- `docs/prd/shark-engine-air-release-1.prd.md` (25 REQs, user-confirmed Canvas/Vision; PRD itself
  `finalized-pending-user-confirmation`)
- `docs/traceability.md` (25/25 REQs linked)
- `docs/intake/PHASE-0.16-COUNCIL-CHALLENGE.md` (3-role council transcript)
- `docs/intake/ULTRATHINK_REVIEW.md` (craftsmanship review, failure modes FM1–FM3)
- `docs/intake/MISSING_BLOCKER_LEDGER.md` (LED-001..007)
- `sharkengine/src/test/java/dev/sharkengine/...` (existing test layout/conventions)
- `sharkengine/src/main/resources/fabric.mod.json` (`fabric-gametest` registration array)

## How to read this document

For each REQ-NNN: a **thesis** (what AC-NNN already asserts), a **counter-thesis** (the strongest way
an implementation could look done — pass a shallow version of the AC — without actually serving
CAN-001's problem or CAN-004's value promise), and a **sharpened test** that specifically kills that
false positive, not just the happy path. Then concrete test type/location, and explicit AC-NNN/EV-NNN
mapping so nothing drifts from the confirmed PRD.

**⚠ GameTest registration reminder (repo-specific gotcha, see root `CLAUDE.md`):**
`@GameTest`-annotated classes are **not** classpath-scanned. A new GameTest class that is not added to
`sharkengine/src/main/resources/fabric.mod.json`'s `"fabric-gametest"` array compiles and runs cleanly
but silently contributes **zero** tests — the total GameTest count in `runGametest`'s output stays
unchanged, which is the only tell. **Every GameTest class named in this plan is flagged `[NEEDS
fabric.mod.json REGISTRATION]` and is re-listed in the consolidated checklist near the end.** The coder
must add each one and confirm the reported test count increased by exactly that many.

**⚠ Test-source-set classpath constraint (observed directly in this repo):** `src/test/java` has no
Minecraft/Fabric dependency (see `build.gradle`'s `testImplementation` block and
`ResourceValidationTest`'s own documented rationale). Only classes built purely from shimmed types
under `src/test/java/net/minecraft/...` (currently: `BlockPos`, `BlockState`, `VoxelShape`,
`BlockGetter`, `ParticleOptions`/`ParticleTypes`) or classes with zero Minecraft imports (e.g.
`ShipTransform`, `VehiclePartRegistry`, `ShipPartAnalyzer`) can run as plain JUnit tests. Classes that
touch `ModBlocks`, `ModItems`, `Item`, `BuiltInRegistries`, or a new `DyeColor` type **cannot** be
directly unit-tested unless the coder adds a matching shim (a new `DyeColor` shim is explicitly
recommended below for REQ-018/019/020) — otherwise that coverage must move to a GameTest instead.

---

## REQ-001 — Vehicle route popup (P0)

**Maps to:** AC-001, EV-001 (TRC-001) · Risks: RISK-001

**Thesis (AC-001):** Placing or interacting with a Steering Wheel opens a popup showing AIR, LAND, and
WATER as three recognizable routes.

**Counter-thesis:** A popup that renders three static labels satisfies "drei erkennbare Routen"
literally while never actually being wired to the real trigger — e.g. it only opens from a debug
command, or only fires on block *placement* and not on *interacting with an already-placed* wheel (the
REQ text requires both: "platziert ODER benutzt"). A screenshot of the UI proves nothing about which
server event opened it.

**Sharpened test:** Exercise both trigger paths server-side and assert the resulting state
programmatically (not visually): (1) placing a Steering Wheel block triggers the popup open path, (2)
right-clicking an already-placed, previously-untouched Steering Wheel also triggers it. Inspect the
S2C payload/session state produced, asserting it carries exactly 3 route identifiers: AIR, LAND, WATER.

**Test type + location:** Fabric GameTest — `dev.sharkengine.gametest.VehicleRoutePopupGameTest`
`[NEEDS fabric.mod.json REGISTRATION]`. Two `@GameTest` methods: `popupOpensOnPlacement`,
`popupOpensOnInteractWithExistingWheel`.

---

## REQ-002 — Release route availability (P0)

**Maps to:** AC-002, EV-002 (TRC-002) · Risks: RISK-005

**Thesis (AC-002):** Only AIR creates an active build session; LAND/WATER show a non-misleading future
status and don't mutate the world.

**Counter-thesis:** "No world mutation" is trivially satisfiable by making LAND/WATER **unclickable**
(greyed out) — but CAN-006 explicitly requires them "sichtbar" as routes, not disabled buttons; a
disabled button is not "shown as a future route," it looks broken. Equally false-positive: LAND/WATER
silently swallow the click with **zero** feedback — technically "no world mutation," but indistinguishable
from a bug to the player, violating "nicht irreführenden Zukunftsstatus." A test that only asserts
"world unchanged" cannot tell a designed non-goal from a silently broken button.

**Sharpened test:** For LAND and WATER independently: (a) selection remains interactable (not
disabled/greyed), (b) selecting it produces an explicit, distinguishable "coming soon / not available in
Release 1" feedback state (not silence), (c) a full block-state + entity-count diff of the interaction
area is empty before/after selection. For AIR: selecting it results in exactly one server-side
`VehicleBuildSession` (or equivalent) existing, tied to that player + wheel position.

**Test type + location:** Fabric GameTest — `dev.sharkengine.gametest.VehicleRouteAvailabilityGameTest`
`[NEEDS fabric.mod.json REGISTRATION]`. Methods: `landSelectionIsInteractableButNonMutating`,
`waterSelectionIsInteractableButNonMutating`, `airSelectionCreatesExactlyOneSession`.

---

## REQ-003 — Server-owned build session (P0)

**Maps to:** AC-003, EV-003 (TRC-003) · Risks: RISK-004

**Thesis (AC-003):** The server rejects selection/assembly requests with invalid player, dimension,
position, distance, session status, or expiry, without mutating the world.

**Counter-thesis:** A shallow test that only exercises **one** invalid axis (e.g. an expired session)
would pass an implementation that validates that one field carefully while silently trusting
client-supplied dimension or position — exactly the kind of gap a desynced or forged client exploits.
Worse: an implementation could perform partial world mutation (e.g. clear blocks) *before* the
rejecting check runs, and a test that only inspects final "was assembly created: no" would miss the
transient mutation. A session keyed globally instead of per-player/per-wheel would also let two
players' sessions cross-contaminate — invisible unless tested concurrently.

**Sharpened test:** A parametrized C2S authorization matrix covering six *independent* invalid axes —
non-owner player, wrong dimension, out-of-range distance, expired session, wrong/absent session id,
duplicate/replayed request — each individually producing rejection with a full block-diff + entity-count
diff of **zero** (not just "no ship spawned"). Include one positive control (all fields valid → session
created) to prove the matrix isn't vacuously "always rejects." Add a two-player session-isolation test:
concurrent sessions on two different wheels don't leak into each other.

**Test type + location:**
- Pure JUnit (recommended architecture note: implement session/authorization validation as a pure
  function set, analogous to `ShipPartAnalyzer`, so it's unit-testable without Fabric bootstrap) —
  `dev.sharkengine.ship.session.VehicleBuildSessionValidationTest` (new package
  `dev.sharkengine.ship.session`, mirroring the existing `ship.part` pattern).
- Fabric GameTest for the full server-authoritative flow with a real `ServerPlayer`/dimension —
  `dev.sharkengine.gametest.BuildSessionAuthorizationGameTest` `[NEEDS fabric.mod.json REGISTRATION]`.

---

## REQ-004 — Complete craft/resource closure (P0)

**Maps to:** AC-004, EV-004 (TRC-004) · Risks: RISK-001

**Thesis (AC-004):** Every Release-1 AIR component is registered, craftable (or documented otherwise),
placeable, localized, and datagen-complete.

**Counter-thesis:** `ResourceValidationTest`'s own resource-contract tests (`ALL_BLOCK_IDS`,
`CRAFTABLE_IDS`, etc.) are **hand-maintained literal arrays** — the file's own doc comment admits this
("must therefore be kept in sync by hand"). A coder can add a new pilot-seat block, copilot-seat block,
or the REQ-018 colored-thruster item to `ModBlocks`/`ModItems` without adding it to that literal list,
and every existing resource test keeps passing while the new component is silently uncraftable/
unlocalized in the shipped jar. This is exactly the class of regression the file already warns about,
just not yet guarded against.

**Sharpened test:** (a) extend the resource-contract literal ID lists to include every REQ-005–020
component. (b) Add a companion **source-text-scanning** meta-test (same regex-over-`.java`-source idiom
`ResourceValidationTest` already uses for JSON — text-based, not classloading, since `ModBlocks`/
`ModItems` cannot be referenced from the `test` source set) that parses `ModBlocks.java`/`ModItems.java`'s
registration call sites and asserts every registered id string literal also appears in the
resource-contract's literal list — so a newly-registered-but-untracked block/item fails CI loudly
instead of silently falling through a stale array.

**Test type + location:** JUnit unit tests, `dev.sharkengine.ship.ResourceValidationTest` (extend
existing file) + new nested class `RegistrationClosureTests` in the same file (or a sibling
`dev.sharkengine.ship.RegistryClosureTest`) for the source-scanning meta-test.

---

## REQ-005 — Generic pilot seat (P0)

**Maps to:** AC-005, EV-005 (TRC-005) · Risks: RISK-003, RISK-005

**Thesis (AC-005):** Assembly requires exactly one valid pilot seat; zero or more than one fails with a
clear message and leaves the world unchanged.

**Counter-thesis:** The sharpest false positive is an off-by-one counting bug: a scan loop that does
`if (found) break;` on the first pilot-seat block, never verifying there isn't a *second* one elsewhere
in the (up to 512-block, 32-radius) structure. This passes a "seat count ≥ 1" happy-path test while
silently violating "genau einen" — the exact ambiguity REQ-006's "no silent fallback" rule exists to
close for the anchor case, but here for the *count* case.

**Sharpened test:** Three-way partition GameTest, not just the happy path: (1) zero pilot seats → fails
explicitly; (2) exactly one → succeeds; (3) **two** pilot seats, deliberately placed far apart within the
structure (not adjacent — to defeat a scan that only checks immediate duplicates) → fails explicitly,
world unchanged. All three share the same otherwise-valid base structure.

**Test type + location:** Fabric GameTest — `dev.sharkengine.gametest.PilotSeatCountGameTest`
`[NEEDS fabric.mod.json REGISTRATION]`. Methods: `zeroSeatsRejected`, `oneSeatAccepted`,
`twoSeatsRejectedEvenWhenFarApart`.

---

## REQ-006 — Pilot seat anchor (P0)

**Maps to:** AC-006, EV-006 (TRC-006) · Risks: RISK-003

**Thesis (AC-006):** The seat anchor is deterministically the single block directly in front of the
wheel's facing; if occupied/invalid, assembly fails explicitly — no silent fallback to another position.

**Counter-thesis:** The riskiest false positive here is invisible from the happy path alone: an
implementation with a hidden fallback branch that searches for *any* nearby valid seat position when the
front slot is occupied would still pass a naive "seat anchor got set to *some* position" test whenever
the front slot happens to be free — the fallback code path is simply never exercised by that test. Only
a test that deliberately occupies the front-of-wheel slot can distinguish "deterministic front-slot
resolution" from "fallback search that happens to land there."

**Sharpened test:** Two-part: (1) occupy the exact front-of-wheel-facing position with a non-seat/invalid
block → assembly fails with an explicit, distinguishable error, **and** the resulting blueprint has
**zero** SeatAnchor entries (not one at a silently-chosen alternate position); (2) front slot free and
valid, tested across all 4 wheel-facing rotations (N/E/S/W) → SeatAnchor offset in the blueprint equals
the exact front-of-wheel offset for that facing, and survives a round-trip through
`ShipTransform.rotateOffset()` (AIR-010's existing single rotation authority) plus a save/load cycle
(per AC-006's explicit "assembliert, gedreht, gespeichert und geladen").

**Test type + location:**
- JUnit unit test for the rotation/round-trip math — extend `dev.sharkengine.ship.ShipTransformTest`
  (existing file, pure math, no Fabric bootstrap) with seat-anchor-offset cases.
- Fabric GameTest for the occupied-position failure + full assembly flow —
  `dev.sharkengine.gametest.PilotSeatAnchorGameTest` `[NEEDS fabric.mod.json REGISTRATION]`.

---

## REQ-007 — Cockpit visibility (P0)

**Maps to:** AC-007, EV-007 (TRC-007) · Risks: RISK-003

**Thesis (AC-007):** Visibility is governed purely by a server-side eye-height check at the seat anchor,
independent of armor/skin/third-person camera — no bounding-box special cases.

**Counter-thesis:** The false positive here is a client-only cosmetic fix — e.g. hiding the player model
via a local render-layer trick that makes the pilot *look* tucked in on the fixing developer's own
screen. That satisfies "looks right in my test" while the actual mandated mechanism (a server-side,
synced eye-height computation) is never implemented, and would fail immediately for a second observer
client, or the moment any armor/skin-conditional branch sneaks in (which AC-007 explicitly forbids).

**Sharpened test:** (a) A pure server-side unit test computing "is fully exposed above hull" purely from
`SeatAnchor` Y-offset + eye-height, whose function signature structurally cannot accept an armor or skin
parameter — proven by asserting two simulated players with different armor/skin values at the *same* eye
height produce identical results. (b) A GameTest asserting the seat's Y-position places the eye-height
point below the top face of the tallest adjacent hull block by a fixed margin, exercised for a seat
against a tall (2+ block) hull wall.

**Test type + location:**
- JUnit unit test, pure logic — `dev.sharkengine.ship.CockpitVisibilityTest` (new file, no Minecraft
  imports needed if eye-height math is expressed in plain doubles/offsets).
- Fabric GameTest — `dev.sharkengine.gametest.CockpitVisibilityGameTest`
  `[NEEDS fabric.mod.json REGISTRATION]`.

---

## REQ-008 — Pilot control authority (P0)

**Maps to:** AC-008, EV-008 (TRC-008) · Risks: RISK-001

**Thesis (AC-008):** Only the server-assigned pilot's control commands take effect; non-pilot/copilot
attempts leave state unchanged and are rate-limited-logged.

**Counter-thesis:** The load-bearing false positive: an authorization check that tests "is this player
riding the ship at all" instead of "is this player *specifically* the pilot, not the copilot" — since the
copilot **is** a rider. A test suite that only checks a *non-rider* sending control input (easy, obvious
case) would never catch this, because the copilot case is the one where "is a rider" and "is the pilot"
diverge. This is precisely the confusion REQ-010 exists to prevent, and the two REQs must be tested as
distinct roles sharing the same rider status, not conflated into one "unauthorized user" test.

**Sharpened test:** Three-way C2S authorization matrix against the helm-input payload path: (a)
non-rider sends control input → rejected, no state change; (b) **copilot** (occupying the copilot
SeatAnchor, genuinely riding) sends the *same* payload → rejected, velocity/yaw/fuel/anchor/edit-state
byte-identical before/after — this is the discriminating case; (c) pilot sends it → accepted, state
changes as expected (positive control).

**Test type + location:** Fabric GameTest — `dev.sharkengine.gametest.PilotControlAuthorityGameTest`
`[NEEDS fabric.mod.json REGISTRATION]`. Methods: `nonRiderInputRejected`,
`copilotInputRejectedDespiteBeingARider`, `pilotInputAccepted`.

---

## REQ-009 — Craftable copilot seat (P0)

**Maps to:** AC-009, EV-009 (TRC-009) · Risks: RISK-003

**Thesis (AC-009):** A copilot seat accepts exactly one additional passenger, correctly shown on all
clients.

**Counter-thesis:** A false positive: the occupancy check overwrites the seat's occupant reference
instead of rejecting a second interact — so player B interacting with an already-occupied copilot seat
silently **replaces** player A (A is displaced with no dismount event, desyncing A's client) rather than
being refused. A shallow test that only checks "B ends up riding" cannot distinguish this from a
correctly-additive two-seat design (there's only one copilot seat in Release 1, so this bug manifests as
silent eviction, not a second seat).

**Sharpened test:** Occupy the copilot seat with player A, assert A riding; have player B interact with
the **same already-occupied** seat → B's mount is rejected **and** A remains mounted (not silently
displaced); server-tracked occupancy count stays exactly 1 throughout. Separately, assert the
server-authoritative occupant id matches what a second (observer) client connection's tracked passenger
list reports (cross-client sync half of RISK-003).

**Test type + location:** Fabric GameTest — `dev.sharkengine.gametest.CopilotSeatOccupancyGameTest`
`[NEEDS fabric.mod.json REGISTRATION]`. Methods: `secondPlayerCannotDisplaceFirstCopilot`,
`occupancyIsObserverConsistent`. **Flag:** true dual-real-client rendering desync is not GameTest-testable
(single server instance); the observer-consistency check here is limited to server-side tracked state,
not actual rendered client frames — full cross-client visual proof remains part of EV-009's manual
"Zwei-Client-Smoke," noted under Risk Coverage below.

---

## REQ-010 — Passive copilot behavior (P0)

**Maps to:** AC-010, EV-010 (TRC-010) · Risks: RISK-003

**Thesis (AC-010):** Copilot control inputs are no-ops during flight; normal dismount doesn't corrupt
role or vehicle state.

**Counter-thesis:** Input-rejection is already the discriminating case tested under REQ-008(b). The
distinct failure mode specific to REQ-010 is **dismount-triggered corruption**: a copilot exiting
mid-flight, through a poorly-scoped dismount handler, could accidentally clear the *pilot's* seat
reference too (e.g. a shared "occupants" list cleared on any dismount), or leave the copilot seat's
internal state dangling so it appears occupied forever afterward — silently breaking REQ-011's re-entry
guarantee even though nobody is in the seat. A test that only checks "copilot input has no effect" never
exercises the dismount code path at all.

**Sharpened test:** Mid-flight (non-zero velocity, active fuel burn) copilot dismount GameTest asserting:
(a) pilot remains mounted, retains full control authority, unaffected; (b) fuel/velocity/vehicle NBT
immediately before/after dismount are identical except the copilot's own occupant slot; (c) the vacated
copilot seat is immediately re-mountable by a different player within the same test run (this directly
feeds REQ-011's re-entry test as a regression guard).

**Test type + location:** Fabric GameTest — `dev.sharkengine.gametest.CopilotDismountIntegrityGameTest`
`[NEEDS fabric.mod.json REGISTRATION]`. Methods: `midFlightDismountLeavesPilotUnaffected`,
`vacatedSeatIsImmediatelyRemountable`.

---

## REQ-011 — Vehicle re-entry (P0)

**Maps to:** AC-011, EV-011 (TRC-011) · Risks: RISK-003

**Thesis (AC-011):** After dismount, an authorized player can remount a free seat without the vehicle
being rebuilt, and receives exactly that seat's role.

**Counter-thesis:** The sharpest false positive: "remount" implemented by silently re-running full
assembly validation (since the structure already physically exists, an interact-driven re-scan could
trigger REQ-005/006's seat-count/anchor checks again) — which could either wrongly reject the remount, or
worse, silently **respawn** the `ShipEntity` instead of just toggling occupancy on the existing one. A
test asserting only "the player is riding again, with the right role" cannot distinguish a legitimate
in-place remount from a silent reassembly that destroys and recreates the entity — exactly what AC-011's
explicit "ohne das Fahrzeug neu zu bauen" clause forbids, and what would silently reset fuel/damage/trail
state.

**Sharpened test:** Capture the `ShipEntity`'s unique entity UUID, fuel level, and
damage/trail-configuration state before dismount. After remount, assert the **same** entity UUID is
ridden (no despawn/respawn detected) and that fuel/damage/trail are byte-identical to their pre-dismount
values — the only reliable way to falsify a "remount via silent reassembly" implementation, since
role-correctness alone doesn't prove no rebuild happened. Also assert the received role matches exactly
the seat interacted with (pilot seat → pilot role, copilot seat → copilot role), not a default-to-whatever
free role.

**Test type + location:** Fabric GameTest — `dev.sharkengine.gametest.VehicleReentryGameTest`
`[NEEDS fabric.mod.json REGISTRATION]`. Methods: `remountPreservesEntityIdentityAndState`,
`remountGrantsExactlyTheInteractedSeatsRole`.

---

## REQ-012 — Safe edit-mode gate (P0)

**Maps to:** AC-012, EV-012 (TRC-012) · Risks: RISK-004, RISK-007 (resolved via OQ-001)

**Thesis (AC-012):** Edit mode opens only when the vehicle is stationary/safe/conflict-free **and** the
player is within Euclidean 3D distance ≤5 blocks of the Control Anchor in every direction; otherwise no
state change.

**Counter-thesis (the sharpest one in the whole plan, grounded in an actual codebase idiom):** OQ-001
explicitly resolved the metric as **Euclidean** 3D distance. But `ShipAssemblyService.scanStructure()`
— the existing, shipped BFS scan — already uses **Manhattan** distance for its own radius check
(`current.distManhattan(wheelPos) > MAX_RADIUS`, `ShipAssemblyService.java` line ~239). Manhattan
distance is the codebase's *existing idiom* for "how far is this block" checks. A coder reusing that
idiom for REQ-012 under time pressure would silently ship a Manhattan (or Chebyshev) gate instead of
Euclidean — and a loose test using only axis-aligned or near-diagonal points (e.g. "5 blocks north = ok,
6 blocks north = blocked") would never notice, because axis-aligned distances are identical under all
three metrics. Only genuinely diagonal offsets discriminate.

**Sharpened test:** Parametrized boundary test using specific offsets where Euclidean and
Manhattan/Chebyshev verdicts **disagree**: offset (3,3,0) → Euclidean ≈4.24 (≤5, must ACCEPT) vs.
Manhattan=6 (would wrongly REJECT if the existing idiom were reused) — this single case falsifies a
Manhattan-based implementation outright. Also: offset (3,4,0) → Euclidean = 5.0 exactly (boundary, must
ACCEPT per "≤5"); offset (3,4,1) → Euclidean ≈5.10 (must REJECT). Combine with the stationary/
safe/conflict-free precondition checks (damaged/moving vehicle → rejected regardless of distance).

**Test type + location:**
- JUnit unit test for the pure distance-metric function — `dev.sharkengine.ship.EditModeDistanceTest`
  (new file, plain doubles, no Minecraft imports needed if the metric function takes raw coordinates).
- Fabric GameTest for the full gate (distance + stationary + safe + conflict-free, real world state) —
  `dev.sharkengine.gametest.EditModeDistanceGameTest` `[NEEDS fabric.mod.json REGISTRATION]`.

---

## REQ-013 — Builder reopen (P0)

**Maps to:** AC-013, EV-013 (TRC-013) · Risks: RISK-004

**Thesis (AC-013):** Successfully entering edit mode opens the builder menu showing the existing
structure and permits valid extension.

**Counter-thesis:** A false positive: the builder "opens" (session state transitions correctly, passing
a shallow state-machine test) but loads a **stale** or **generic/blank** blueprint instead of the
vehicle's actual current structure — e.g. a cached blueprint snapshot from before the vehicle's last
committed edit, rather than a live re-scan. The player would see an empty or outdated grid, defeating
"zeigt die bestehende Fahrzeugstruktur," while a test that only checks "session state == BUILDING" would
never notice the payload's content is wrong or old.

**Sharpened test:** Modify the vehicle's structure via one prior edit-session commit (block count N →
N+1), then reopen edit mode in a later, separate interaction and assert the builder preview payload's
block list exactly matches the **current** (N+1) blueprint — compared directly against a live
`ShipAssemblyService.scanStructure()`-equivalent scan of the entity's current blueprint, not a cached
snapshot from session start.

**Test type + location:** Fabric GameTest — `dev.sharkengine.gametest.BuilderReopenGameTest`
`[NEEDS fabric.mod.json REGISTRATION]`.

---

## REQ-014 — Atomic edit/reassembly (P0)

**Maps to:** AC-014, EV-014 (TRC-014) · Risks: RISK-004

**Thesis (AC-014):** Exiting edit mode validates the changed structure against AIR policy and commits
atomically or rolls back completely.

**Counter-thesis:** "Atomic" can be faked by an apply-then-maybe-undo implementation: apply each block
change to the live world incrementally as the player edits, then on final invalid state, replay inverse
operations to "undo." This looks atomic from a black-box "final state matches old-valid" test — but if
the undo sequence itself is interrupted (chunk unload, a position becoming blocked in the interim), you
get a state that matches neither old-valid nor new-invalid. A test checking only the *final* state after
a successful undo can't distinguish "never touched the world" (true preflight atomicity) from "touched it
and successfully undid it" (fragile, TOCTOU-prone).

**Sharpened test:** Assert the edit-mode commit path uses **preflight validation before any world
mutation**, not apply-then-rollback: for a deliberately-invalid final edit, assert **zero** block-state
changes occur anywhere in the structure's bounding box at any point during the attempt (not "net zero
after undo") — via a before/during/after block-state snapshot diff, checked mid-attempt if the
implementation exposes a hook, or at minimum a full snapshot immediately after the rejected commit
returns. Companion positive-path test: a valid edit commits fully and the block count changes as
expected.

**Test type + location:** Fabric GameTest — `dev.sharkengine.gametest.AtomicEditReassemblyGameTest`
`[NEEDS fabric.mod.json REGISTRATION]`. Methods: `invalidEditNeverTouchesWorld`,
`validEditCommitsFully`.

---

## REQ-015 — AIR flight controls (P0)

**Maps to:** AC-015, EV-015 (TRC-015) · Risks: RISK-001

**Thesis (AC-015):** A fueled pilot in the cockpit can control speed, climb, descent, bank, and turn;
the vehicle responds sensibly with no NaN/Infinity/authority errors.

**Counter-thesis:** Speed/height-penalty/phase math is already covered by the existing `ShipPhysicsTest`
/`AccelerationPhaseTest`. Release 1's genuinely new claim is bank/turn ("Neigung und Kurvenflug"). A
false positive: bank/turn implemented as **purely cosmetic** client-side visual roll (a render-only tilt
animation) that never actually changes the ship's yaw or velocity vector — the ship visibly "banks" in a
smoke check while its heading never turns, which is strictly weaker than "supports... Kurvenflug" (actual
turning flight, not decorative tilt).

**Sharpened test:** Server-side unit test (extending the existing no-Fabric-bootstrap `ShipPhysicsTest`
pattern) asserting a sustained turn-input payload changes the ship's **yaw** over N simulated ticks by a
nonzero, input-direction-consistent amount — not merely a cosmetic roll/pitch render field. Also a
fuzz-style boundary sweep: extreme/edge input combinations (min/max throttle × min/max turn, zero fuel)
across a multi-tick loop must never produce NaN/Infinity in position, velocity, or yaw — a swept range,
not one nominal sample.

**Test type + location:** JUnit unit test — extend `dev.sharkengine.ship.ShipPhysicsTest`, or new
`dev.sharkengine.ship.FlightControlAuthorityTest` (pure logic, no Fabric bootstrap, following the
existing pattern).

---

## REQ-016 — Fuel and speed loop (P0)

**Maps to:** AC-016, EV-016 (TRC-016) · Risks: RISK-001

**Thesis (AC-016):** Fuel is craftable/refillable, consumed during powered flight per defined logic,
visible in HUD, and consistent across save/load; speed reacts to input traceably.

**Counter-thesis:** Pure fuel-conversion math is already covered by `FuelSystemTest`. The
Release-1-specific false positive is a **HUD/server desync**: the server-authoritative fuel value
correctly decrements (passing a server-only unit test), while the client HUD overlay only syncs on
mount/dismount events (or a slow interval) rather than continuously — so a player watching the HUD
mid-flight sees a number that visibly lags the true value used for save/load. This narrowly satisfies
"Fuel muss... im HUD sichtbar" (some number shown) while violating AC-016's explicit "HUD/gespeicherter
Wert stimmen überein" during the desync window.

**Sharpened test:** Since HUD rendering itself is a client concern outside JUnit's reach, the sharpened
server-side proxy is: assert a fuel-sync S2C payload is transmitted on **every** fuel-changing tick (not
just mount/dismount), and that its value matches the entity's authoritative fuel NBT at that exact tick —
testing sync *cadence*, not merely that a sync happens once. Pair with a `FuelSystemTest`-style save/load
round-trip: serialize a mid-flight (non-round-number) fuel value, deserialize, assert exact match with no
rounding/truncation drift.

**Test type + location:**
- Fabric GameTest for sync cadence — `dev.sharkengine.gametest.FuelSyncCadenceGameTest`
  `[NEEDS fabric.mod.json REGISTRATION]`.
- JUnit extension of existing `dev.sharkengine.ship.FuelSystemTest` for the save/load precision
  round-trip.

---

## REQ-017 — Persistence and restart (P0)

**Maps to:** AC-017, EV-017 (TRC-017) · Risks: RISK-001

**Thesis (AC-017):** VehicleClass, blueprint, pilot/copilot seats, occupancy eligibility, fuel, damage,
trail configuration, and edit state are all consistently serialized and restored after a dedicated-server
restart.

**Counter-thesis:** A false positive is **partial** persistence that passes a shallow "the ship still
exists after restart" smoke test while silently dropping exactly the fields that are *new* this release
and therefore have no pre-existing persistence code path: seat-occupancy-eligibility metadata, trail
DyeColor config, in-progress edit-mode state. Pre-existing fields (blueprint, fuel) already have
persistence coverage per this repo's schema-versioning convention (NFR-004); the field that silently
regresses is invisible to a test that only checks "vehicle count > 0 after restart."

**Sharpened test:** Assemble a ship with a colored copilot thruster, both seats populated, non-round-
number fuel spent, then persist and reload, asserting each of the 7 named fields **individually** —
VehicleClass, blueprint, pilot seat, copilot seat, fuel, damage, trail DyeColor — not just aggregate
vehicle presence.

**Test type + location:** Fabric GameTest — `dev.sharkengine.gametest.VehiclePersistenceRestartGameTest`
`[NEEDS fabric.mod.json REGISTRATION]`, using a full `ServerLevel` save-to-NBT-and-reload-in-place cycle.

**Flag (explicit coverage gap, per instructions):** a true OS-level dedicated-server *process* restart
is not achievable inside a single Fabric GameTest run — GameTest executes inside one live server
instance, which EV-017's own wording ("Dedicated-Server-Restart-Test") implicitly concedes. The
GameTest above is the closest pre-implementation-testable proxy (in-place save/reload); genuine
process-restart coverage remains a manual/orchestrator-level release-evidence step folded into REQ-024's
end-of-release gate, not silently assumed covered here.

---

## REQ-018 — Single Thruster item with craft-time DyeColor component (P1)

**Maps to:** AC-018, EV-018 (TRC-018) · Risks: RISK-006, RISK-008 (resolved via OQ-002 + council
amendment)

**Thesis (AC-018):** Crafting with any supported dye yields exactly one Thruster item type carrying a
craft-time DyeColor data component; no second color-specific item id; no post-placement recoloring; one
generic recipe-book entry for all 16 dyes.

**Counter-thesis:** The council amendment that resolved RISK-008 specifically **rejected** a 16-item
design in favor of a single item + data component — which means the single sharpest false positive is a
coder reverting to the rejected design under 12-day deadline pressure (hacking 16 recipes is mechanically
simpler than a data-component-driven single recipe). A second, subtler false positive: the component is
built correctly at craft time, but a leftover/debug interaction path (dye-on-block, anvil-style rename,
loom-adjacent right-click) lets the component be changed **after** placement — satisfying "craft-time
component exists" while violating "kein nachträgliches Färben."

**Sharpened test:** (a) Registry-enumeration test asserting **exactly one** thruster item/block id exists
across `ModItems`/`ModBlocks` — fails loudly if a second `thruster_<color>`-style id appears, directly
falsifying a reverted-to-16-items regression. (b) Recipe-count test asserting exactly one recipe JSON
file governs the colored-thruster craft path (not 16). (c) Interaction-path test: right-clicking an
already-**placed** colored thruster with any dye item is a no-op — component value unchanged before/
after — since (a) and (b) only prove craft-time correctness, not post-placement immutability.

**Test type + location:**
- JUnit unit tests (resource-contract pattern) — extend `dev.sharkengine.ship.ResourceValidationTest`
  with a `ThrusterDyeComponentResourceTests` nested class (exactly-one-item-id + exactly-one-recipe
  checks).
- Fabric GameTest for the post-placement no-op — `dev.sharkengine.gametest.ThrusterRecolorRejectionGameTest`
  `[NEEDS fabric.mod.json REGISTRATION]`.

**Implementation note for the coder:** a `DyeColor` type has no existing shim under
`src/test/java/net/minecraft/...`. If a pure-unit test of the component-assignment logic is wanted
(recommended), either add a minimal `DyeColor` shim (it's a plain 16-value enum, cheap to shim like the
existing `BlockState`/`VoxelShape` shims) or design the component-resolution function to accept a raw
`String`/`int` rather than the real Minecraft `DyeColor` type.

---

## REQ-019 — Persistent colored trail via single render path (P1)

**Maps to:** AC-019, EV-019 (TRC-019) · Risks: RISK-006, RISK-008

**Thesis (AC-019):** A componented thruster's trail persists its DyeColor via **one** tinted-texture/
color-provider render path across restart; an uncomponented thruster uses the existing default trail —
one render implementation, not 16 models/textures.

**Counter-thesis:** A purely visual/behavioral test cannot tell a tinted single-texture implementation
apart from a texture-swap implementation that bakes 16 separate particle textures and `switch`es between
them by color — both *look* correct in a screenshot or a "does the right color render" check, but the
latter violates "genau eine Renderimplementierung... keine 16 separaten Modelle/Texturen." Only a
resource-inventory check can distinguish them.

**Sharpened test:** Resource-contract test (same pattern as `ResourceValidationTest`'s texture-palette
checks) asserting **exactly one** trail/thruster particle texture file exists under the relevant assets
path — directly falsifying a texture-swap implementation regardless of how correct it looks visually.
Pair with a pure-function unit test of the tint provider (if implemented as `DyeColor → RGB`, no Fabric
dependency): the **same** function call, exercised across all 16 dye values plus the "no component"
default, proving one code path handles 17 cases rather than 17 separate paths.

**Test type + location:**
- JUnit unit test (resource-contract pattern) — extend `dev.sharkengine.ship.ResourceValidationTest`
  with a `TrailTextureResourceTests` nested class.
- JUnit unit test for the pure tint-provider function — `dev.sharkengine.ship.TintProviderTest` (new
  file; needs the same `DyeColor` shim note as REQ-018, or an `int`/`String`-keyed provider signature).

---

## REQ-020 — Trail isolation and bounded rendering (P1)

**Maps to:** AC-020, EV-020 (TRC-020) · Risks: RISK-006

**Thesis (AC-020):** DyeColor trail color is purely cosmetic — thrust, fuel consumption, mass, and
control authority are identical regardless of color; particle emission is qualitatively bounded per
vehicle, with **no hard numeric performance gate for Release 1** (explicit user decision, OQ-005).

**Counter-thesis:** "Identical gameplay values" tends to get verified only for the most obvious stat
(thrust). The easily-missed side channel: `VehiclePartRegistry.resolve()` (or an equivalent lookup) keys
on the full `ItemStack`-with-components identity instead of the base block/item id — a classic bug class
that data components introduce which plain block-id lookups never had. If so, a colored thruster could
silently resolve to a *different* (or missing/fallback) `VehiclePartDefinition` than an uncolored one,
quietly changing mass/lift/fuelCapacity too, not just a cosmetic value — invisible unless mass/lift/drag
are checked, not just thrust.

**Sharpened test:** Extend the existing pure-logic `VehiclePartRegistryTest`/`ShipPartAnalyzerTest`
pattern: assert `VehiclePartRegistry.resolve()` returns the **identical** `VehiclePartDefinition` for the
base thruster id regardless of any DyeColor component being present — i.e., resolution must key on
block/item identity, not full-stack NBT. Add a `ShipStats`-level assembly comparison: two otherwise-
identical ships (one all-default thrusters, one all-colored) must produce bit-identical `ShipStats`
(mass/lift/thrust/drag/fuelCapacity), not just equal thrust.

**Test type + location:** JUnit unit test — extend `dev.sharkengine.ship.part.VehiclePartRegistryTest`
and/or `dev.sharkengine.ship.part.ShipPartAnalyzerTest`.

**Flag (explicit, per OQ-005/NFR-006):** the *performance* half of RISK-006 (particle emission cost) is
**not** given a numeric gate for Release 1 by explicit user decision — this is not an oversight, it's a
deliberate deferral to real playtest data. QA proposes only a qualitative smoke check (a stress GameTest
with N simultaneous colored vehicles doesn't throw or hang) — `dev.sharkengine.gametest.
TrailParticleStressGameTest` `[NEEDS fabric.mod.json REGISTRATION, qualitative-only]` — and explicitly
flags the absence of a real performance regression test as a deliberate, user-approved coverage gap, not
silently treated as "tested."

---

## REQ-021 — Transactional world mutation (P0)

**Maps to:** AC-021, EV-021 (TRC-021) · Risks: RISK-004

**Thesis (AC-021):** Assembly, disassembly, and edit-reassembly all use preflight validation + rollback,
so an aborted operation never duplicates or loses blocks.

**Counter-thesis:** This generalizes REQ-014's atomicity concern to assembly/disassembly. The distinct
new failure mode: **disassembly** iterating the blueprint and calling `setBlock` per position could
partially fail if the entity despawns/unloads mid-loop (chunk boundary, area not loaded) — a classic
"loop that assumes the world stays loaded" bug. The existing `AssemblySmokeTest`'s 8×8×8
`EMPTY_STRUCTURE` template is always fully loaded, so a test built the same way would never exercise this
path, giving false confidence.

**Sharpened test:** (a) Reuse REQ-014's preflight-before-mutation pattern for assembly failure paths
(spawn position blocked by another entity, or the structure becomes invalid between preview and commit —
a TOCTOU race) — assert zero mutation. (b) Disassembly-specific: assert the restored world block count
**exactly** equals the blueprint's block count (not "≥1 restored"), tested at both a small near-origin
structure and a structure near `ShipAssemblyService.MAX_BLOCKS`/`MAX_RADIUS` (512 blocks / 32 radius) to
surface large-structure partial-restore bugs a small smoke structure can't reveal.

**Test type + location:** Fabric GameTest — `dev.sharkengine.gametest.AssemblyDisassemblyRollbackGameTest`
`[NEEDS fabric.mod.json REGISTRATION]`. Methods: `assemblyFailurePathNeverMutatesWorld`,
`disassemblyRestoresExactBlockCountAtSmallScale`, `disassemblyRestoresExactBlockCountAtMaxScale`.

**Flag:** true chunk-unload-mid-disassembly is not exercisable inside a single-chunk GameTest template —
flagged as a coverage gap requiring either a larger multi-chunk GameTest template or manual
dedicated-server testing; not silently assumed covered by the max-scale test above (which stresses size,
not chunk boundaries).

---

## REQ-022 — Minimal Vehicle Core, no premature generalization (P0)

**Maps to:** AC-022, EV-022 (TRC-022) · Risks: RISK-005 (mitigated by this REQ's demotion, Phase 0.16
council, 3/3 round-1 convergence)

**Thesis (AC-022):** Only seams AIR's shipped code actually calls exist; a seam interface needs ≥1
non-trivial call-site or it fails; no duplicated entity/assembly/persistence/networking pipeline for
LAND/WATER.

**Counter-thesis:** AC-022's own operational rule is **one-sided** — it only checks for
too-much-abstraction ("≥1 call-site or it fails"). A coder under deadline pressure could satisfy it
trivially by extracting **zero** seams at all (everything inlined into `ShipEntity`) — there are no
interfaces to have 0 call-sites, so the literal rule is vacuously satisfied — while this simultaneously
violates NFR-003 ("keine wachsenden AIR/LAND/WATER-Bedingungsblöcke in `ShipEntity.tick()`") and defeats
CAN-011/GOAL-003's actual purpose (the minimal *reusable* seams that let LAND/WATER exist later without
duplication). AC-022's literal test is a trap that only catches over-abstraction, not the equally-real
under-abstraction failure the council was also implicitly worried about ("vorzeitige Generalisierung...
kann das AIR-MVP verzögern" cuts both ways: over-engineering delays AIR, but zero extraction reproduces
RISK-005's sibling problem for LAND/WATER later).

**Sharpened test:** Needs a **two-sided** architecture check: (a) for every interface/abstract class
under any newly-introduced seam package (e.g. a `dev.sharkengine.ship.core`-style package), a
source-text-scanning test (same regex-over-`.java` idiom as REQ-004, no classloading) counts real
call-sites in `src/main/java` and `src/client/java` and fails if any such interface has **zero** — AC-022's
literal rule. (b) A companion NFR-003 regression guard asserting `ShipEntity.tick()` (and its direct
callees) contains **no new** `if (vehicleClass == VehicleClass.LAND)`/`WATER`-shaped conditional branches
— closing the opposite direction (everything inlined, a growing switch statement instead of any
extraction).

**Test type + location:**
- JUnit unit test, source-scanning — `dev.sharkengine.ship.VehicleCoreSeamCallSiteTest` (new file,
  text-based regex scan of `.java` source files, following `ResourceValidationTest`'s established idiom).
- JUnit unit test, source-scanning — `dev.sharkengine.ship.ShipEntityConditionalGrowthTest` (asserts no
  new AIR/LAND/WATER conditional branches in `ShipEntity.tick()`'s source text vs. a documented baseline).

**Flag:** (a) is regex/text-based static analysis and is necessarily best-effort — it cannot perfectly
distinguish a "non-trivial" call-site from a trivial pass-through wrapper (AC-022's own caveat).
Genuinely ambiguous extraction decisions should be flagged for human architecture review at PR time, not
silently auto-passed by this test.

---

## REQ-023 — Looping backlog only (P2)

**Maps to:** AC-023, EV-023 (TRC-023) · Risks: RISK-005

**Thesis (AC-023):** Looping is documented as a post-release feature with confirmed design notes and
blocks no Release-1 test.

**Counter-thesis:** Since this REQ is documentation-only, the false positive isn't a runtime bug — it's
**scope creep**: a coder working on REQ-015's bank/turn physics could "helpfully" start wiring
partial looping support (an unused `LOOP_ENTRY`-style acceleration phase, or a pitch-past-90° branch)
because it feels adjacent, silently pulling P2 scope into P0 flight-control code and potentially
perturbing REQ-015's math (e.g. a dormant loop-detection branch that misfires on a hard turn) — violating
CAN-013's non-goal even though "looping" per se was never finished or announced.

**Sharpened test:** (a) Documentation-only check: a resource/doc-existence test asserting a backlog entry
for looping exists with the confirmed design notes referenced by REQ-023. (b) **Negative** regression
test against the physics code introduced for REQ-015: assert no new `AccelerationPhase` enum value or
loop-related field exists in the shipped physics code — a direct, falsifiable guard against accidental
scope creep into P0 physics, using the existing test-referenceable `AccelerationPhase` type (already
directly testable per the existing `AccelerationPhaseTest`, no shim needed).

**Test type + location:**
- JUnit unit test, doc-existence — `dev.sharkengine.ship.LoopingBacklogDocumentationTest` (new file,
  resource-contract pattern).
- JUnit unit test, negative regression — extend `dev.sharkengine.ship.AccelerationPhaseTest` (existing
  file) with a `noLoopRelatedPhaseIntroduced` assertion over `AccelerationPhase.values()`.

---

## REQ-024 — Release evidence gate (P0)

**Maps to:** AC-024, EV-024 (TRC-024) · Risks: RISK-001, RISK-002 (residual, per traceability.md's own
`canvas-risk-status` note on TRC-024)

**Thesis (AC-024):** A release artifact ships only when the exact commit has passed build, unit/resource
tests, Fabric GameTests, client smoke, dedicated-server smoke, two-player smoke, and restart proof.

**Counter-thesis:** The traceability matrix itself already flags the load-bearing risk for this row: "a
single roll of the dice at day ~12" carrying RISK-002's unresolved during-window integration-error
exposure, because the user explicitly declined the council's incremental per-REQ gating alternative
(Round 2, Critic proposal, not adopted). The concrete false positive: evidence collected **piecemeal
across different commits** during the 12-day window (client smoke on commit A three days ago, server
smoke on commit B yesterday, unit tests on commit C today) gets declared "the release is evidenced" by
union of ever-passing runs, rather than requiring every gate to pass on the **same** exact commit SHA. A
checklist that tracks only "has each gate ever passed" (boolean per gate, no commit-hash column) silently
accepts this drift.

**Sharpened test:** This is fundamentally a process/orchestrator-level gate, not a JUnit-testable unit.
QA's contract-level specification: every release-evidence entry (build, unit/resource tests, GameTest,
client smoke, dedicated-server smoke, two-player smoke, restart proof) must be tagged with the **same**
git commit SHA, and sign-off must be refused if any entry's SHA differs from the others.

**Test type + location:** **Flagged blocker, not a Java test class.** Cannot be verified pre-
implementation by this repo's automated test suite. QA flags this as a **process blocker** for the
orchestrator/Product Owner: enforce a single-commit-SHA release-evidence checklist manually at release
time (see Risk Coverage Matrix below, RISK-002 row).

---

## REQ-025 — Day-0 build/runtime verification gate (P0, blocking) — SPECIAL HANDLING

**Maps to:** AC-025, EV-025 (TRC-025) · Risks: RISK-001, RISK-002 (partial)

**As instructed, this REQ is flagged specially rather than assigned a Java test class.**

`./gradlew build` and `./gradlew runGametest` on the current `main` are a Day-0 sequencing gate — before
any other P0 task branch for this feature opens — not a release-end gate (that's REQ-024). This is a
CI/process gate that the orchestrator executes directly on `main`, not something a per-task unit test can
meaningfully wrap: by the time a coder would write a JUnit test *for* REQ-025, the gate it verifies
(build/runtime health of `main` itself) would already need to have passed for that coder's branch to
exist.

**Verification status:** **pending orchestrator report.** This test plan does not claim REQ-025 has been
executed, nor does it simulate that claim. QA's role here is limited to: (a) confirming AC-025's binary
pass/fail contract is unambiguous (`./gradlew build` exit code 0 AND `./gradlew runGametest` exit code 0,
both on `main` HEAD, both logged with output/commit SHA per EV-025), and (b) flagging that no P0 task
branch under this feature should exist until that orchestrator report is filed and green. If this
plan is being read by a coder who has not seen a green Day-0 report, that is itself a blocker — stop and
escalate rather than proceeding to any other REQ's implementation.

**Test type + location:** None (process gate). Do not create a Java test class for REQ-025.

---

## Risk & Failure-Mode Coverage Matrix

Every failure-mode named anywhere in the source material below is disposed of as either a named test
above, or an explicit flagged blocker. Nothing is left as silently-agreed-with prose.

| ID | Failure mode (verbatim source) | Disposition |
|---|---|---|
| RISK-001 | Build/runtime status unverified; feature work on a broken base compounds damage. | **Test: REQ-025** (Day-0 gate, process-verified, "pending orchestrator report" — see REQ-025 section). Residually touched by nearly every GameTest above, which will simply fail to run at all if the base is broken. |
| RISK-002 | Deadline pressure → parallel changes across entity/networking/persistence/rendering → more integration errors. PRD's own text: REQ-025 only closes the *Day-0* sub-case; the *during-window* (~12 days of parallel P0 work) exposure is explicitly **not** resolved by REQ-025. | **Partial test, partial flagged blocker.** During-window integration errors are covered indirectly by ordinary per-branch CI (`./gradlew build`/`check` on every PR) and by REQ-024's end-of-release full evidence gate (see REQ-024 section) — no dedicated new test added, since this is a process/scheduling risk, not a code behavior. **Flagged blocker:** QA recommends the orchestrator track a same-commit-SHA release-evidence checklist (detailed under REQ-024) as the only concrete artifact-level mitigation available; the underlying "too much parallel change" risk itself is not testable pre-implementation. |
| RISK-003 | Cockpit/passenger transforms can desync between clients or collide with vanilla riding/camera. | **Tests: REQ-005, 006, 007, 008, 009, 010, 011, 017** (seat anchor rotation/round-trip, eye-height check, occupancy non-displacement, dismount integrity, remount identity-preservation, cross-restart seat persistence). **Flagged blocker (partial):** true multi-client rendering/camera desync cannot be verified inside a single-server-instance GameTest; the deepest pre-implementation-testable proxy is server-authoritative state consistency (implemented above). Genuine cross-client visual proof remains the manual "Zwei-Client-Smoke" named in EV-007/EV-009/EV-010/EV-011 — release evidence, not a developer-facing automated test, and must not be silently treated as covered by the GameTests above. |
| RISK-004 | Non-atomic edit-mode/reassembly can duplicate or lose blocks. | **Tests: REQ-003, 012, 013, 014, 021** (preflight-before-mutation pattern, exact block-count restoration at multiple scales). |
| RISK-005 | Premature Vehicle Core generalization can delay the AIR MVP. | **Test: REQ-022** (two-sided call-site + anti-conditional-growth check). Directly mitigated at the requirement level by the Phase 0.16 council's REQ-022 demotion (3/3 round-1 convergence per `docs/intake/PHASE-0.16-COUNCIL-CHALLENGE.md`); QA's addition is the two-sided test since AC-022's own literal rule is one-sided (see REQ-022 counter-thesis). |
| RISK-006 | Colored particles can hurt client performance. | **Test (invariant half): REQ-020** (color never changes mass/lift/thrust/drag). **Flagged blocker (performance half):** explicitly not numerically gated for Release 1 by user decision (OQ-005/NFR-006) — QA adds only a qualitative non-crash smoke test (`TrailParticleStressGameTest`) and flags the absence of a real performance regression test as a deliberate, user-approved coverage gap, not silently "tested." |
| RISK-007 | *(RESOLVED, OQ-001)* Five-block metric was unclear, risking unusable/exploitable Edit Mode. | **Test: REQ-012** (Euclidean-vs-Manhattan-discriminating boundary test) — the residual risk after resolution is "was the resolved metric actually implemented correctly," which is exactly what the sharpened REQ-012 test targets. |
| RISK-008 | *(RESOLVED, OQ-002 + council amendment)* Craft-only dye workflow could multiply into ≥16 item variants with recipe/model/texture/lang/datagen sprawl. | **Tests: REQ-018, 019** (exactly-one-item-id, exactly-one-recipe, exactly-one-texture registry checks) — directly falsify a reversion to the rejected 16-item design. |
| ULTRATHINK FM1 | "Unklare Fünf-Block-Regel → falsche Distanzlogik → Edit Mode unbenutzbar oder exploitable → Release-Blocker." | Same as RISK-007 — **covered by REQ-012.** |
| ULTRATHINK FM2 | "Vehicle Core zu allgemein → zusätzlicher Refactor → AIR-Integration verspätet → Termin verfehlt." | Same as RISK-005 — **covered by REQ-022.** |
| ULTRATHINK FM3 | "Nutzerbestätigung simuliert → AgileTeam plant auf falscher Vision → Scope Drift." | **Not code-testable — flagged CLOSED-PROCESS, not carried forward as a test.** This is a process-integrity failure mode about the *confirmation gate itself*, already closed per `MISSING_BLOCKER_LEDGER.md` LED-007 (user supplied the exact verbatim confirmation phrase in-session, 2026-07-18) and the Canvas/Vision documents' own `user-confirmed` status headers. QA has no runtime artifact to test against a simulated-confirmation failure; noting it here satisfies the hard rule against silently agreeing with it in prose, but it is explicitly not a code test. |
| Council Round 1, Challenger | "Attacking CAN-001... silently fuses two separable problems... into one P0 set due 2026-07-30" (deadline conflates missing-gameplay work with runtime-verification-maturity work). | **Test: REQ-024/REQ-025 together** — REQ-025 tests the runtime-verification-maturity half at Day 0; REQ-024 tests it again at release end. The "two separable problems" critique itself is a scope-framing critique of the PRD, not a testable runtime claim — no further action beyond REQ-024/025's existing tests, which the council itself converged on as the mitigation (see Convergence summary in the council transcript). |
| Council Round 1, Challenger | "CAN-002/003 are asserted, not evidenced... only in the Product Owner's own architecture preference." | **Not testable pre-implementation, and explicitly out of this council's/QA's mandate** — Critic's Round 2 rebuttal ("re-litigates VIS-008/ASM-004, already user-confirmed vision — out of this council's mandate") applies equally to QA: target-user validity is a product-research question, not a black-box acceptance-test question. Flagged here as a noted product-research gap, not a release blocker, per the hard rule against silent agreement. |
| Council Round 1/2, Critic | "24 P0-heavy requirements... too much surface for 12 days on an unverified base." | Same underlying risk as RISK-002 — **flagged blocker**, see RISK-002 row above; Critic's own incremental-per-REQ-gating counter-proposal was presented to the user and explicitly **not adopted** (council transcript convergence summary), so QA does not build a test plan around it. |
| ULTRATHINK "Bias-Risiken" | Architecture Bias, Deadline Bias, Confirmation Bias, Sunk-Cost Bias (re: the PRD-writing process itself). | **Out of scope for a black-box acceptance-test plan** — these are meta-risks about how the specification documents were produced, not runtime behaviors of the shipped mod. Noted here, not silently dropped, but no test is proposed because there is no reality-touching artifact to test against a documentation-drafting bias. |

---

## Consolidated: New GameTest Classes Requiring `fabric.mod.json` Registration

**18 new GameTest classes.** Every one of these MUST be added to
`sharkengine/src/main/resources/fabric.mod.json`'s `"fabric-gametest"` array, or it silently
contributes zero tests (see warning banner at top of this document). After registration, confirm
`./gradlew runGametest`'s reported total test count increased by exactly 18 (plus however many
`@GameTest`-annotated methods each class contains beyond one).

1. `dev.sharkengine.gametest.VehicleRoutePopupGameTest` (REQ-001)
2. `dev.sharkengine.gametest.VehicleRouteAvailabilityGameTest` (REQ-002)
3. `dev.sharkengine.gametest.BuildSessionAuthorizationGameTest` (REQ-003)
4. `dev.sharkengine.gametest.PilotSeatCountGameTest` (REQ-005)
5. `dev.sharkengine.gametest.PilotSeatAnchorGameTest` (REQ-006)
6. `dev.sharkengine.gametest.CockpitVisibilityGameTest` (REQ-007)
7. `dev.sharkengine.gametest.PilotControlAuthorityGameTest` (REQ-008)
8. `dev.sharkengine.gametest.CopilotSeatOccupancyGameTest` (REQ-009)
9. `dev.sharkengine.gametest.CopilotDismountIntegrityGameTest` (REQ-010)
10. `dev.sharkengine.gametest.VehicleReentryGameTest` (REQ-011)
11. `dev.sharkengine.gametest.EditModeDistanceGameTest` (REQ-012)
12. `dev.sharkengine.gametest.BuilderReopenGameTest` (REQ-013)
13. `dev.sharkengine.gametest.AtomicEditReassemblyGameTest` (REQ-014)
14. `dev.sharkengine.gametest.FuelSyncCadenceGameTest` (REQ-016)
15. `dev.sharkengine.gametest.VehiclePersistenceRestartGameTest` (REQ-017)
16. `dev.sharkengine.gametest.ThrusterRecolorRejectionGameTest` (REQ-018)
17. `dev.sharkengine.gametest.TrailParticleStressGameTest` (REQ-020, qualitative-only, non-gating)
18. `dev.sharkengine.gametest.AssemblyDisassemblyRollbackGameTest` (REQ-021)

## Consolidated: New Pure-JUnit Test Classes (no Fabric bootstrap)

1. `dev.sharkengine.ship.session.VehicleBuildSessionValidationTest` (REQ-003) — recommends a new
   `dev.sharkengine.ship.session` package for pure session-validation logic, mirroring `ship.part`.
2. `dev.sharkengine.ship.ResourceValidationTest` — extended, not new (REQ-004, REQ-018, REQ-019).
3. `dev.sharkengine.ship.RegistryClosureTest` (REQ-004, alternative location if not nested in #2).
4. `dev.sharkengine.ship.ShipTransformTest` — extended, not new (REQ-006).
5. `dev.sharkengine.ship.CockpitVisibilityTest` (REQ-007).
6. `dev.sharkengine.ship.EditModeDistanceTest` (REQ-012).
7. `dev.sharkengine.ship.ShipPhysicsTest` — extended, or new `FlightControlAuthorityTest` (REQ-015).
8. `dev.sharkengine.ship.FuelSystemTest` — extended, not new (REQ-016).
9. `dev.sharkengine.ship.TintProviderTest` (REQ-019).
10. `dev.sharkengine.ship.part.VehiclePartRegistryTest` / `ShipPartAnalyzerTest` — extended (REQ-020).
11. `dev.sharkengine.ship.VehicleCoreSeamCallSiteTest` (REQ-022).
12. `dev.sharkengine.ship.ShipEntityConditionalGrowthTest` (REQ-022).
13. `dev.sharkengine.ship.LoopingBacklogDocumentationTest` (REQ-023).
14. `dev.sharkengine.ship.AccelerationPhaseTest` — extended, not new (REQ-023).

**Implementation note flagged twice above, restated once for visibility:** REQ-018/REQ-019's pure-unit
coverage needs either a new `DyeColor` shim under `src/test/java/net/minecraft/world/item/DyeColor.java`
(cheap — 16-value enum, same pattern as the existing `BlockState`/`VoxelShape` shims) or a
component-resolution/tint-provider API designed around a primitive (`int`/`String`) key instead of the
real Minecraft type. Flagging this now so the coder doesn't discover the classpath constraint mid-task.

---

## Summary

- **25/25 REQs (REQ-001–025)** received the full thesis / counter-thesis / sharpened-test 3-beat
  treatment. None were skipped as "too simple" — REQ-002 (a visible-but-inert LAND/WATER button),
  REQ-013 (builder reopen), and REQ-023 (a documentation-only P2 backlog note) looked the most trivial
  on the surface and turned out to have the least-obvious counter-theses (silent stale-cache reopen,
  cosmetic-only bank/turn scope creep into P0 physics, respectively).
- **REQ-025 received special handling** as instructed: no Java test class, verification status
  **pending orchestrator report**, with an explicit escalation instruction if a coder encounters this
  plan without a filed green Day-0 report.
- **8 named risks (RISK-001–008)** + **3 ULTRATHINK failure modes (FM1–FM3)** + **3 distinct council
  critique threads** (deadline/date-before-baseline, target-user evidence, 24-req scope) = **14 total
  named failure-modes/risks**, every one individually disposed of in the Risk & Failure-Mode Coverage
  Matrix: **9 became named falsifying tests** (RISK-003 partial, RISK-004, RISK-005, RISK-006 partial,
  RISK-007, RISK-008, FM1, FM2, plus RISK-001 via REQ-025's process gate), and **5 became explicit
  flagged blockers** that this plan states cannot be pre-implementation-tested (RISK-002's
  during-window exposure, RISK-003's true cross-client desync, RISK-006's performance-gate absence,
  FM3's process-confirmation closure, and the council's target-user-evidence critique) — none left as
  silently-agreed-with prose.
- **18 new Fabric GameTest classes** proposed, every one flagged `[NEEDS fabric.mod.json
  REGISTRATION]` and consolidated into a single checklist to prevent the exact "silently contributes
  zero tests" failure this repo has already hit once (`ShipEntityMountGameTest`, 2026-07-13).
- **14 new or extended pure-JUnit test classes** proposed, with an explicit `DyeColor`-shim
  implementation note flagged for the coder ahead of time.
- **File written:** `docs/plans/2026-07-18-shark-engine-air-release-1-test-plan.md`
- **Not touched:** Canvas, Vision, PRD, `traceability.md` — read-only per instructions. No new
  requirements invented; every test traces to an existing REQ-NNN/AC-NNN/EV-NNN triple already present
  in the locked PRD and traceability matrix.
