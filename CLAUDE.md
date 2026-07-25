# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Shark Engine is a Minecraft 1.21.1 Fabric mod that lets players build, assemble, and pilot flying vehicles. Players place a Steering Wheel, attach blocks (Thrusters, a Pilot Seat, hull parts), and launch controllable airships with physics-based flight. The AIR vehicle class is functional; LAND and WATER exist only as `VehicleClass` enum constants and a "coming soon" server notice.

**Current state (2026-07-25):** `mod_version=0.1.0` (`sharkengine/gradle.properties`). AIR Release 1 (tasks T01–T24) is merged to `main` and published to Modrinth (project `xwlGFlcw`, version `bpmKXmLT`). The signed-off gate record is `docs/release/2026-07-30-release-evidence.md` — that filename carries the PRD's *target release date*, not its authoring date (created 2026-07-24, rows dated 2026-07-25).

The repo root also holds deployment and publishing tooling around the mod itself — see [Root-Level Tooling](#root-level-tooling).

## Build & Development Commands

All commands run from the `sharkengine/` subdirectory:

```bash
cd sharkengine

./gradlew build          # Compile main+client, remap, run tests — mirrors CI, use this to catch client-only compile breaks
./gradlew test           # Run JUnit 5 tests only — does NOT compile the client source set (see warning below)
./gradlew check          # Tests + static analysis — also does NOT compile the client source set
./gradlew runClient      # Launch Fabric dev client for in-game testing
./gradlew clean          # Clean build artifacts — ALSO deletes src/main/generated (see datagen gotcha below)
./gradlew runDatagen     # Regenerate src/main/generated (recipes/loot/models/blockstates/tags/lang for datagen-migrated blocks)
./gradlew runGametest    # Fabric GameTest suite (94 tests / 30 classes) — boots a real GameTestServer

# Run a single test class or method
./gradlew test --tests "dev.sharkengine.ship.ShipPhysicsTest"
./gradlew test --tests "dev.sharkengine.ship.ShipPhysicsTest.methodName"
```

Requires **Java 21** — Fabric Loom enforces the toolchain and fails on wrong JDK.

**Client compile gotcha:** `splitEnvironmentSourceSets()` (below) means `test`/`check` never invoke `compileClientJava` — a client-only break can be fully green on `test`/`check` while the mod doesn't build. This happened for real (2026-03-17 to 2026-07-12, undetected): a package-private record used cross-package from client code. Always run `./gradlew build` before claiming client-side work (rendering, HUD, screens) is done; CI runs `build`, not `test`, specifically because of this.

**Datagen/`clean` gotcha:** `fabricApi.configureDataGeneration()` (added 2026-07-12, AIR-030) wires `src/main/generated` deletion into the `clean` task, but *regenerating* it is not part of the normal build graph — running `./gradlew clean build` alone leaves `src/main/generated` missing and `test`/`runGametest` red (missing recipe/loot/blockstate/model/tag/lang files). After `clean`, always run `./gradlew runDatagen` before `build`/`test`/`runGametest`. Note `test` goes red *across dozens of assertions* rather than failing at a build step, because `ResourceValidationTest` asserts against `src/main/generated` on the **filesystem**, not the classpath. This does **not** affect CI (`.github/workflows/ci.yml` never runs `clean`; the generated output is committed to git, so it's present immediately after checkout) or normal incremental `./gradlew build` without a preceding `clean`.

**⚠️ Datagen ordering race (2026-07-24 incident, happened for real during T24 evidence collection):** `./gradlew clean runDatagen build` as ONE invocation is NOT safe — the exact same command produced a green run once and, minutes later, a build whose `build/resources/main` (and therefore the built JAR) silently lacked the ENTIRE generated datapack: no tags, recipes, loot tables, blockstates, models or lang. Observed facts: 64/92 GameTests failed with `assembly_fail_empty` (no `ship_eligible` tag on the classpath) while CI stayed green on the same commit (CI never runs `clean`/`runDatagen`); the broken state PERSISTED across plain re-runs (`processResources` reported UP-TO-DATE); `./gradlew processResources --rerun-tasks` fully repaired it (all 92 GameTests green again). Most consistent explanation (inference, not verified against Gradle internals): `processResources` snapshotted `src/main/generated` while `clean`/`runDatagen` were still manipulating it — the CLI task list imposes no execution-order constraint between independent tasks. Rules: (1) run the three steps as SEPARATE invocations — `./gradlew clean && ./gradlew runDatagen && ./gradlew build`; (2) before treating any built JAR as an artifact, verify it actually contains the datapack: `unzip -l build/libs/sharkengine-*.jar | grep -c 'data/sharkengine.*json'` must be > 0 — a racy build produces a structurally valid, silently gameplay-dead JAR.

**GameTest registration gotcha:** `@GameTest`-annotated classes are **not** classpath-scanned — they only run if listed in `src/main/resources/fabric.mod.json`'s `"fabric-gametest"` array. Adding a new GameTest class/file without adding it there compiles and runs cleanly, just silently contributes zero tests (the total test count in `runGametest`'s output stays unchanged — that's the tell). Happened for real 2026-07-13 with `ShipEntityMountGameTest`. Always add new GameTest classes to that array and confirm the reported test count went up. Baseline at HEAD: **30 registered classes, 94 `@GameTest` methods** (array and directory currently in exact sync). Adding a method to an *already-registered* class needs no array change.

## Architecture

### Source Set Split

Fabric Loom splits code into two compilation units:
- **`src/main/java`** — Server-side + shared logic (physics, assembly, networking, entities). Runs on both client and dedicated server.
- **`src/client/java`** — Client-only code (rendering, input, HUD, camera). Never loaded on a dedicated server.

This is enforced by Loom's `splitEnvironmentSourceSets()`. Putting client imports in `main` causes server crashes.

### Package Map

| Package | Purpose |
|---------|---------|
| `dev.sharkengine` | Mod entrypoints (`SharkEngineModEntrypoint`, `SharkEngineMod`) |
| `dev.sharkengine.content` | Block/item/entity/sound/tag/component/recipe-serializer registries (`ModBlocks`, `ModTags`, `ModComponents`, …) |
| `dev.sharkengine.content.block` | The 11 registered block classes + `SteeringWheelItem` |
| `dev.sharkengine.ship` | Core vehicle logic — entity, physics, assembly, fuel, weight, acceleration, edit-mode gates |
| `dev.sharkengine.ship.part` | Part/role model: `VehicleBalance` (all balance constants), `ShipPartAnalyzer`, `VehiclePartRegistry`, `AssemblyIssue`, `PartRole`, `ShipStats` |
| `dev.sharkengine.ship.session` | Pre-launch build-session authorization. **Zero `net.minecraft.*` imports** — pure JUnit-testable; `ship.BuildSessionGate` is its only Fabric adapter |
| `dev.sharkengine.net` | Client-server networking payloads and handlers (7 payloads) |
| `dev.sharkengine.datagen` | Data generators (`fabric-datagen` entrypoint) — see [Datagen](#datagen) |
| `dev.sharkengine.gametest` | Fabric GameTests. Lives in `src/main/java`, so it **ships inside the released jar** — any "production source" analysis must exclude it |
| `dev.sharkengine.tutorial` | Onboarding popup flow and stage management |
| `dev.sharkengine.client` | Client entrypoint, input handler, controller, camera, keybindings, blueprint sync |
| `dev.sharkengine.client.render` | Entity renderer (`ShipEntityRenderer`), HUD overlay (`FuelHudOverlay`), onboarding card |
| `dev.sharkengine.client.builder` | Builder mode UI with block highlights |
| `dev.sharkengine.client.tutorial` | Tutorial popup rendering |

### Core Systems

**Ship Assembly** (`ShipAssemblyService`): a stateless static service with three pipelines sharing one BFS (`scanStructure`) — pre-launch assembly (`tryAssemble`), builder preview (`openBuilderPreview`), and the Edit Mode round-trip (`openEditMode` → `materializeForEdit` → `commitEdit`). The BFS starts at the Steering Wheel and collects connected `ship_eligible`-tagged blocks. Produces a `ShipBlueprint`.

`StructureScan#canAssemble()` is a **10-term conjunction**, and `AssemblyIssue.Code` now has **12** codes (translation keys `assembly_issue.sharkengine.<id>`):

| Requirement | Issue code(s) |
|---|---|
| structure non-empty | `EMPTY_STRUCTURE` |
| zero invalid attachments | `INVALID_ATTACHMENTS` |
| `contactPoints == 0` | `TERRAIN_CONTACT` |
| ≥1 PROPULSION-role part | `NO_PROPULSION` |
| **exactly 1 PILOT_SEAT-role part** | `NO_PILOT_SEAT`, `MULTI_PILOT_SEAT` |
| all 4 horizontal wheel neighbours are ship blocks | `TOO_FEW_CORE_NEIGHBORS` |
| exactly 1 BUG, on the outer edge | `NO_BUG`, `MULTI_BUG`, `BUG_INSIDE` |
| **pilot seat at its one legal position** | `SEAT_ANCHOR_INVALID` |
| **cockpit visibility** | `COCKPIT_VISIBILITY_INSUFFICIENT` |

`MAX_BLOCKS=512` / `MAX_RADIUS=32` (Manhattan) are BFS scan caps, not validation failures — blocks beyond them are silently not part of the ship.

Assembly traps, all code-verified, all silent when violated:

- **A flat, one-block-thick ship can never assemble.** Cockpit visibility is `seatY + 1.62 >= tallestAdjacentHullTopY - 0.1` with `seatY` hardcoded 0 and a fixed `STANDARD_PLAYER_EYE_HEIGHT` (never the requesting player's real eye height, so assembly is player-independent). Over integers that resolves to: **some column orthogonally adjacent to the pilot-seat column must contain a block at `dy >= 1`.** The wheel itself always occupies one of those columns at `dy=0`, so a single-layer plate always fails with a message ("add more hull around the seat") that never states the actual rule. The canonical minimal ship only works because the thruster sits at `WHEEL_POS.above()`. `VISIBILITY_MARGIN` (0.1) and `STANDARD_PLAYER_EYE_HEIGHT` (1.62) in `CockpitVisibility` are the single source of truth for that geometry — changing either silently re-qualifies existing player builds.
- **"DOWN is exempt" applies only to `contactPoints`, not to `invalidAttachments`.** Any non-air block that is not `ship_eligible` and touches any ship block in *any of the 6 directions* — including the dirt/stone the ship is parked on, adjacent water, tall grass, torches, snow layers — is an invalid attachment and blocks assembly. This makes `countWorldContacts`' DOWN exemption ("ground-parked ships can always take off") practically unreachable over ordinary terrain.
- **The BFS does not stop at a floor.** `#minecraft:planks`, `logs`, `wooden_slabs`, `wooden_stairs` and `wool` are all in the `ship_eligible` tag, so building on a wooden platform absorbs that whole platform into the ship (up to the caps), silently inflating mass and possibly grounding the craft as OVERLOADED.
- **`MAX_BLOCKS` is a loop-top guard**, so a >512-block structure is silently *truncated*: contacts, attachments and stats are computed over the truncated set only, and the truncated ship can still pass `canAssemble()` and launch, permanently orphaning the unscanned blocks in the world.
- **`canAssemble()==true` does not guarantee `tryAssemble()` succeeds.** An 11th rejection — a spawn preflight intersecting the new structure's cells against nearby parked ships' rotated blueprint cells — has *no* `AssemblyIssue` code, so the builder preview can show an all-green structure that then refuses to launch (`assembly_fail_spawn_blocked`) with a message the preview never predicted.
- **Adding a new part id is a two-place change, each failure silent.** It must go into `VehicleBalance.PARTS` (`VehiclePartRegistry` deliberately has no map of its own) *and* into the `ship_eligible` tag via `SharkEngineTagProvider` + `runDatagen`. Miss the tag → the BFS classifies it as an invalid attachment that blocks assembly. Miss the `PARTS` row → it resolves to `FALLBACK` (`STRUCTURE`, mass 1, no role), so a would-be PROPULSION or PILOT_SEAT part is simply never seen by the gate, with no error anywhere.
- Everything up to and including the spawn preflight is **read-only**; the first `level.setBlock` is well after all validation. `tryAssemble`'s short-circuit order differs from `issues()`' emission order — that ordering decides which single chat message a player sees.

**Part/role model** (`ship.part`): `VehiclePartRegistry.resolve(id)` reads `VehicleBalance.PARTS` directly; every unregistered `ship_eligible` block (all the vanilla planks/logs/glass/wool) falls back to `STRUCTURE, mass 1`. `PartRole` has 12 constants but **assembly enforces only two** — `PROPULSION >= 1` and `PILOT_SEAT == 1`; `COPILOT_SEAT` only emits extra seat anchors, and every other role is inert at assembly time (in particular `ROTOR_HUB`'s javadoc claim that it "requires an adjacent PROPULSION part" is enforced nowhere). Of `ShipStats`' seven fields only `mass`, `propulsionCount` and `pilotSeatCount` have production readers — `lift`/`thrust`/`drag`/`fuelCapacity` and `VehiclePartDefinition.LiftMode` are currently declared-ahead-of-use and read only by GameTests. `VehicleBalance.PARTS` holds 16 rows but only 11 blocks are registered: `wing_root`, `wing_panel`, `wing_tip`, `tail_fin` and `fuel_tank` are balance-locked ahead of implementation, not shipped content.

> The `propulsion` **block tag** contains only `helicopter_engine` — `thruster` is deliberately absent. The PROPULSION requirement is role-based via `VehiclePartRegistry`, never tag-based. Using the `propulsion` tag as a gameplay input would silently exclude thrusters and break the core AIR ship.

**Entity `interact()` on large-hitbox vehicles must PASS on a non-empty hand.** `ShipEntity`'s hitbox spans the whole assembled structure (up to the 32-block radius above), so any entity-level `interact()` override on it — or on any future large-hitbox vehicle entity — intercepts right-clicks *before* vanilla's normal block-placement path ever runs. Returning `CONSUME` unconditionally (e.g. for a generic "mount the pilot" fallback) silently defeats block placement anywhere on/near the vehicle, with zero exceptions logged — exactly what happened in `ShipEntity.interact()` until the 2026-07-13 fix (`ShipEntity.java`, see the fix's inline comment): every right-click holding a `ship_eligible` block near an already-launched ship got mounted-and-consumed instead of placing. Rule: only consume/handle when `player.getItemInHand(hand).isEmpty()`; otherwise return `InteractionResult.PASS` so vanilla gets a chance at normal item-use/placement (same pattern as right-clicking a vanilla boat while holding a block). Note `interact()` returns `SUCCESS` on the client before any branch runs — every seat/edit/mount decision is server-side only, and the client-side result cannot be used to signal PASS-through.

**Seats & control authority.** The two seat kinds are structurally different:

- **Pilot seat — positional.** Exactly one `PILOT_SEAT`-role part must exist anywhere in the structure, *and* it must sit at one deterministic position: `wheelPos.offset(rotateOffset(0, 1, bugYawDeg))` — i.e. the wheel's horizontal neighbour, at the wheel's own Y, in the direction the single BUG block **faces**. **The anchor is wheel-relative, not bug-relative**; the BUG's own position is irrelevant, only its `FACING` (SOUTH=0°, WEST=90°, NORTH=180°, EAST=−90°). Several in-repo comments say "directly in front of the BUG", which reads as bug-relative and is wrong. There is exactly one world block-state read and **no fallback search** — a valid pilot seat elsewhere in the structure does not help. Because the anchor is always a cardinal wheel neighbour at `dy=0`, it doubles as one of the four required `coreNeighbors`.
- **Copilot seat — non-positional.** Any number of `copilot_seat` blocks anywhere each become their own seat anchor; there is **no count constraint and no position rule**, and assembly never rejects on copilot seats. Only one copilot can actually ride, because occupancy is a single `UUID copilot` field.

Control authority is **identity-based, not occupancy-based**. `pilot` and `copilot` are two independent UUID fields. Every privileged action — helm input, refuel, anchor/disassemble, both Edit-Mode entry gestures, `commitEdit`, and exemption from `EditModeBlockProtection` — checks `isPilot` only; the copilot is a genuine rider with zero authority. `pilot` is assigned **only** in `tryAssemble` and is never cleared on dismount, so: a returning pilot always reclaims control, a stranger can never become pilot even when the seat is mechanically empty, and a `ShipEntity` that never went through `tryAssemble` (`/summon`, a save missing the `Pilot` tag) is permanently uncontrollable. Both mount paths use `startRiding(this, true)`, which bypasses vanilla's single-passenger cap — the `copilot` UUID is the only thing enforcing "one extra passenger", which is why it is persisted.

Ordering constraint on both mount paths: **assign the role field before `startRiding`**. `addPassenger` resolves which seat was occupied via `isPilot`/`isCopilot` and returns silently for an untracked passenger, so reversing the order makes the check silently no-op rather than fail.

**Physics** (`ShipEntity` + `ShipPhysics`): Weight categories are MASS-based via `VehicleBalance` (per-part masses summed by `ShipPartAnalyzer`, thresholds raised 4x on 2026-07-13): LIGHT ≤120 → 30 blocks/sec, MEDIUM ≤240 → 20, HEAVY ≤360 → 10 + warning, OVERLOADED 361+ → 0. An OVERLOADED structure still assembles — it is only grounded at flight time; there is deliberately no assembly-time refusal. Five `AccelerationPhase` stages ramp speed 5→30 blocks/sec over 6 seconds. Height penalty reduces speed above Y=100 (×0.8/0.6/0.4 at Y≥100/150/200).

**Three load-bearing conventions in `ShipPhysics`, each the scar of a live-playtest P0 (2026-07-12):**

1. **Turn sign.** Increasing Minecraft yaw turns the ship RIGHT, and both client input sources send `+1` for a LEFT turn — so `calculateYawStep` **subtracts** the input, at the single point of consumption. Do not "simplify" the sign; `FlightControlAuthorityTest` locks the direction. (Original symptom: "Lenkung ist invertiert".)
2. **Collision solidity must be the block's actual collision shape, not `!isAir()`.** The old `!isAir()` test treated grass, flowers, torches, signs and carpets as solid, so a freshly-launched ship false-triggered a collision almost immediately and the response (`setDeltaMovement(Vec3.ZERO)`, no escape) left it permanently stuck.
3. **Collision offsets must be rotated by the ship's *live* effective yaw** (`ShipTransform.effectiveYaw(entityYaw, blueprint.assemblyYaw())`, the same one the renderer uses), not by its frozen build-time orientation — otherwise the probed volume diverges from the visible ship the moment it turns, which continuous A/D turning makes happen within seconds of launch.

`ShipPhysics.clampInput` is the **single sanitization point for every C2S helm-input float**: NaN is neutralized to `0` explicitly (plain `Math.max/min` propagates NaN and poisoned yaw → movement vector → entity position from one corrupt payload); ±Infinity needs no special case. `ShipEntity.clamp` is a thin delegate. This is one of the fixes lost in the 2026-07 recovery and reinstated for REQ-015/T16 — see [Recovery audit](#recovery-audit-lost-fix-check).

**Fuel** (`FuelSystem`): 100 energy max, 1 wood = 100 energy. Consumption is nominal 1-3 units/sec by phase × `VehicleBalance.FUEL_CONSUMPTION_RATE` (0.25) → effective 0.25-0.75/sec, accumulated fractionally in `fuelDebt`; burns only while thrusting. Critical at <20%.

**Networking**: seven payloads — 4 C2S (helm input, builder assemble, tutorial advance, tutorial mode selection) and 3 S2C (ship blueprint, builder preview, tutorial popup) — all registered in `ModNetworking.init()` from the **main** entrypoint, with client receivers in the client entrypoint. Every C2S receiver body is wrapped in `ctx.server().execute(...)` and every client one in `ctx.client().execute(...)`, so codecs decode off-thread while handlers run on the game thread.

**Continuous ship state does not travel by payload.** Fuel, speed, mass, block count, health, engine-out, turn and vertical ride `SynchedEntityData` accessors written in one block at the end of `ShipEntity.updatePhysics()`. Only the one-shot blueprint uses an S2C payload, sent from exactly one place (`ShipEntity.startSeenByPlayer`).

Networking constraints worth knowing before you touch it:

- **`writeEnum`/`readEnum` are ordinal-varint with no bounds check.** Reordering or mid-list insertion in `AssemblyIssue.Code` or `VehicleClass` is a **silent wire-format break** — no compile error, and the round-trip GameTest can't catch it because it encodes and decodes in the same JVM. **Append new constants at the end only.** (Within `TutorialPopupS2CPayload` the encodings are inconsistent: `stage` is a stable string id, `routes` are ordinals — so renaming a stage id breaks one field and reordering `VehicleClass` breaks the other.)
- **Never add a `StreamCodec`/`FriendlyByteBuf` field to any `ship.part` or `ship.session` class.** `src/test` genuinely cannot compile against `net.minecraft.network.*`, and a record's static initializer would throw `NoClassDefFoundError` the instant *any* plain unit test called e.g. `AssemblyIssue.of(...)`, including tests that never encode anything (same failure class AIR-015 hit with `ShipBlueprint`). Wire code for those types lives in the payload class (`BuilderPreviewS2CPayload.writeIssues`/`readIssues`), and its tests are GameTests, not unit tests.
- **`BuilderAssembleC2SPayload` with `sessionId == null` is legitimate**, not malformed — it is the Edit-Mode commit path, which resolves authority through `findEditModeShip` instead of a session token. The handler is dual-purpose and order-sensitive: it tries the edit commit first and only falls through to `BuildSessionGate.tryAssemble`. "Hardening" it by rejecting null session ids silently kills the whole edit-commit path.
- **All `SYNC_*` writes are stranded when the ship is anchored, in edit mode, or destroyed** — `tick()` returns before `updatePhysics()` in the first two cases, and the `entityData.set(...)` block is at its end. `addFuel` is the only path that syncs independently.
- Helm inputs are **never reset on dismount**: nothing zeroes `inputForward`/`inputTurn`/`inputVertical`, so a pilot who bails at full throttle leaves the ship flying and burning fuel until engine-out. (`inputThrottle` is written by both `setInputs` overloads and read by nothing — physics uses `inputVertical`.)

**Tutorial** (`TutorialService` + `TutorialPopupStage`): Sequential popup flow — WELCOME → MODE_SELECTION → BUILD_GUIDE → READY_TO_LAUNCH → FLIGHT_TIPS. All three `VehicleClass` buttons are clickable; the server answers a non-AIR pick with a "coming soon" notice (it is the sole authority).

**Build sessions vs. Edit Mode — two unrelated things both called a "session".**

*Pre-launch `VehicleBuildSession`* (`ship.session`, adapter `BuildSessionGate`) is a server-owned authorization token minted when a player picks AIR at a Steering Wheel. `VehicleBuildSessionValidator.validate` evaluates **all six axes unconditionally** (owner, dimension, distance, expiry, session id, consumed) — deliberately not an if-return chain, so a request violating one axis reports exactly one reason and no axis can mask another. `MAX_DISTANCE_BLOCKS = 8.0` (Euclidean) and `DEFAULT_TTL_MILLIS = 10 min` are documented engineering **assumptions**, not PRD numbers. The registry is keyed by `(dimensionId, wheelPos)` — not by player, not by session id — precisely so those axes stay independently testable; `create(...)` returns **null** rather than evicting another player's live session (session-theft fix, closed as one atomic `compute`). The session is consumed only on *structural* assembly success, so an authorized-but-invalid attempt leaves it usable for the retry. `sessionIdForOwner` is the single choke point stopping the bearer token leaking to a non-owner — needed because `openBuilderPreview` is reachable from a tutorial path with no ownership check of its own.

*Edit Mode* (`EditModeDistanceGate`, `EditModeBlockProtection`) is a single persisted `boolean editModeActive` on an **already-launched** ship: `openEditMode` materializes the blueprint back into real world blocks, ordinary vanilla place/break **is** the editing UI, and `commitEdit` re-runs the full BFS scan and either adopts a new blueprint or rolls back. Its atomicity is on the *blueprint*, not the world — a rejected commit clears the materialized footprint to air with **no drops**. `materializeForEdit` is all-or-nothing (read-only `isAir()` preflight over every target first). All seat/cockpit invariants are re-validated on commit.

> **Two different distance metrics coexist in this service.** `scanStructure` uses Manhattan (`distManhattan`) against `MAX_RADIUS`; `EditModeDistanceGate` uses genuine Euclidean 3D against `MAX_DISTANCE_BLOCKS = 5.0`, and the plan doc explicitly forbids reusing the Manhattan idiom for it. They provably disagree (offset (3,3,0): Euclidean ≈4.24 accepts, Manhattan 6 rejects). Copying the nearby one is the trap.

**Persistence.** Ship state lives in the entity's own NBT (15 tags) plus a nested `Blueprint` compound. Everything derived — mass, weight category, max speed, `hasThrusters` — is **recomputed on load**, never stored, so editing `VehicleBalance` silently re-classifies every existing saved ship (a LIGHT ship can reload OVERLOADED and grounded) with no migration step. The blueprint's NBT form **is also its wire form** (`ShipBlueprintS2CPayload` ships `toNbt()` verbatim), so any schema change breaks save compatibility and client/server protocol compatibility at once. `CURRENT_SCHEMA_VERSION = 4`, with migrations keyed on **tag presence, never on the version number** — a migrated blueprint keeps `schemaVersion = 1` in memory and on re-save, so branching on it is wrong.

- **Read order in `readAdditionalSaveData` is load-bearing:** `BugYaw`/`ThrustYaw` must be read *before* `Blueprint`, because a v1 blueprint (no `AssemblyYaw`) is patched with `withAssemblyYaw(bugYawDeg)` immediately after `fromNbt`. Reordering silently reintroduces the visual-snap bug for every non-SOUTH-facing legacy ship.
- Four load-time sanitizers guard corrupt/hand-edited saves: `Health` clamped to [0,100], `FuelLevel` to [0,100], `CurrentSpeed` rejected unless finite then clamped to [0,30], `FuelDebt` through `FuelSystem.sanitizeFuelDebt` (anything outside [0,1) → 0). `SeatAnchor`'s `Role` is the **outlier** — `SeatRole.valueOf` is unguarded, so a corrupt role string throws straight out of entity load.
- `fuelDebt` must round-trip exactly or every save/load quietly refunds up to one unit of burned fuel; the GameTest asserts exact float equality, so any future normalization of that field will fail it.

### Server Authority Model

All validation and physics run server-side. The client handles input capture (`HelmInputClient`), rendering (`ShipEntityRenderer`), and HUD display (`FuelHudOverlay`). Client sends input payloads; server computes movement and syncs state back.

`BuilderScreen` re-implements the assemble gate client-side to enable/disable its button, but only as a **strict subset** of the server's `canAssemble()` — it omits pilot-seat count, `bugOnEdge`, seat anchor and cockpit visibility. Expect the button to look enabled on structures the server will reject, and keep the server as the only authority.

### Client-Side Systems

`src/client/java` declares **zero mixins** (all behaviour is Fabric events) and has **zero tests** — `test`/`check` don't even compile it, which is exactly why `./gradlew build` is the pre-claim gate for any client work.

- **Input.** One `KeyMapping` only: `ShipKeyBindings.DESCEND` (default LEFT ALT). Everything else reuses vanilla options. Turn convention is **positive = LEFT**, consistent across keyboard, controller and server physics. `HelmInputC2SPayload` is sent **unconditionally every 2 client ticks (~10 Hz)**, not on change (`lastThrottle`/`lastTurn`/`lastForward` are write-only dead state from a removed send-on-change path). Adding a keybinding is a two-file change: register it in `ShipKeyBindings` (whose static initializer only runs because `SharkEngineClient` calls the no-op `ShipKeyBindings.init()` — delete that call and the binding silently never registers) **and** add `key.sharkengine.<name>` to `SharkEngineLangProvider`, then `runDatagen`.
- **Controller.** Raw `glfwGetJoystickAxes`/`Buttons` polling, *not* `glfwGetGamepadState`, with no `glfwJoystickIsGamepad` filter and a hardcoded Xbox axis layout; it latches onto the first present joystick slot and never re-scans. Any HID device in slot 1 is read as if it were an Xbox pad. Keyboard and controller merge by `maxAbs`. Of the polled buttons only B (dismount) is wired — `isAnchorPressed()`'s consumer is an empty `if` body and `isInteractPressed()` has no callers.
- **Renderer.** Exactly three rotations, in order: `Axis.YN(effectiveYaw)` → `Axis.XN(clientRoll)` → `Axis.ZN(clientPitch)`. Roll/pitch are **cosmetic only**, lerped client-side from the synced turn/vertical inputs (`MAX_BANK_DEG=25°`, `MAX_PITCH_DEG=18°`, both at 0.15/tick, all in `VehicleBalance`); they touch no physics or collision. **Never fix a bank/pitch visual regression by flipping the sign in `ShipTransform.rollFromTurnInput`/`pitchFromVerticalInput`** — those signs are anchored to the proven turn/climb physics convention. The axis *and* sign mapping lives solely in `ShipEntityRenderer` and was corrected twice against live `runClient` output.
- **HUD.** `FuelHudOverlay` is the only UI with hardcoded German strings (everything else is `Component.translatable`); bars are text glyphs, not textures. It reads `ship.getWeightCategory()` (server-synced mass) rather than recomputing from block count — `WeightCategory.fromBlockCount` was removed precisely so a mixed-mass ship can't read "light" on the HUD while being OVERLOADED in flight.

## Testing

Two disjoint worlds. Baseline at HEAD: **362 `@Test` methods across 22 unit-test classes**, **94 `@GameTest` methods across 30 registered classes**.

**Unit tests (`src/test/java`) run on a JVM with no Minecraft jar on the classpath at all** — `build.gradle` declares only JUnit. That, not a style preference, is why seven hand-written stubs live under `src/test/java/net/minecraft/` (`BlockPos`, `ParticleOptions`/`ParticleTypes`, `StringRepresentable`, `BlockGetter`, `BlockState`, `VoxelShape`); several exist purely so a class *verifies*, and are never called. Existing production code gets away with unstubbed types only because JVM resolution is lazy — e.g. `ShipPhysics` takes `Level` in three signatures with no stub, which works solely because no unit test calls those overloads. **Adding a `net.minecraft.*` type that a unit test actually exercises means adding a stub too**, and adding a key to `ShipParticles.resolve` breaks tests at *runtime* with `NoSuchFieldError` unless mirrored into the `ParticleTypes` stub.

Tests use `@DisplayName` tied to gameplay behavior (361 of 362 methods carry one). There are no `@ParameterizedTest`/`@Disabled`/`@Tag` and no `src/test/resources`.

**Five of the 22 unit-test classes are not behavioural tests — they are filesystem/source-scanning conformance gates**, which is why `./gradlew test` can go red for reasons that look nothing like a code bug:

- `ResourceValidationTest` reads `src/main/generated`, `src/main/resources` **and `../tools/asset-gen/palette.json`** — a path that escapes the Gradle project into the repo root. All gates use paths relative to `sharkengine/`; running tests from another working directory fails with an IOException that looks unrelated. It also asserts *negatives* (the MC 1.21 renames: `loot_table`/`recipe` singular; `data/sharkengine/recipes`, `loot_tables` and `assets/sharkengine/items` must **not** exist).
- `LoopingBacklogDocumentationTest` makes `sharkengine/docs/BACKLOG.md` a **compile-free build dependency** — deleting/renaming it, or dropping any of its pinned tokens, turns `test` red. A routine docs cleanup breaks the build.
- `AccelerationPhaseTest#noLoopRelatedPhaseIntroduced` reads `AccelerationPhase.java` **raw, without comment stripping**, and fails if the case-insensitive substring "loop" appears anywhere — including in a comment. Writing `// loop over the phases` turns `test` red.
- `VehicleCoreSeamCallSiteTest` and `ShipEntityConditionalGrowthTest` scan Java source (via the shared `JavaSourceStripper`, itself locked by its own test after a regex predecessor silently passed in both directions). The latter hard-fails on any `VehicleClass.LAND|WATER` / `switch (vehicleClass)` construct in `ShipEntity.java` — class-conditionals must go behind a seam — and also asserts the stripped source exceeds 10,000 chars, so a large legitimate extraction fails it with a misleading "stripping or read broken".

`ResourceValidationTest` keeps hand-written literal id lists because `ModBlocks` cannot be classloaded in the test JVM. `RegistrationClosureTests` regex-scans `ModBlocks.java`/`ModItems.java` **source text** to catch ids missing from those lists — but two fossil 3-element constants (`CRAFTABLE_BLOCK_IDS`, `MIGRATED_BLOCK_IDS`, both still `{steering_wheel, thruster, bug}`) are **not** closure-guarded and can rot indefinitely without any test going red.

**GameTests** live in `src/main/java/dev/sharkengine/gametest/` and only run if registered in `fabric.mod.json`'s `fabric-gametest` array (see gotcha above). The default template `EMPTY_STRUCTURE` is an inherited `FabricGameTest` interface field, not a project constant — a fixed **8×8×8 air pocket carved into a world that is not air outside it**, so every placed block *and every block whose neighbours the assembly BFS inspects* must stay within local 1..6 on all axes, or the outward lookup hits real terrain and trips "invalid attachments" instead of the condition under test. Two tests needing more room use the hand-committed 40×14×40 all-air template `sharkengine:gametest_large_empty`, which lives in `src/main/resources` and therefore survives `clean`. Naming has exactly one exception: every registered class ends in `GameTest` except `AssemblySmokeTest`, so a `*GameTest.java` glob silently misses it.

**Known pre-existing GameTest flakiness under sustained system load** (root-caused twice independently via git-stash A/B against an unmodified tree): the failure cluster is always `AssemblySmokeTest` + `BlueprintPersistenceGameTest` + whichever gametest is newest. A red `runGametest` under load is not automatically your change's fault — re-run on a quiet machine before debugging.

**GameTest gotchas (all paid for in real debug cycles, 2026-07-24):** (1) a mock player whose collision box overlaps the target position makes vanilla REJECT `BlockItem` placement — stand the player clear before `useOn`, reposition afterwards; (2) `build/gametest/logs/latest.log` rotates on the NEXT run — copy it away immediately after a failure or the evidence is gone; (3) when grepping filtered gametest output, always include `GameTestAssertException` in the pattern, or the actual assertion message never reaches you (only the pass/fail count does).

## Recovery audit (lost-fix check)

When a development line is recovered/reimplemented from an abandoned one (as happened 2026-07: `archive/local-fixline-2026-07` → the current line), diff the abandoned line's fix history (`git log --oneline --grep='Fix:' <old-line>`) against the new line and classify EVERY fix class as present/lost — never assume the recovery carried them. Recovery reliably carries feature work but drops small hardening fixes (they are inconspicuous and often untested on the new line). Three fixes were provably lost in that recovery: the NaN input clamp and the Health load clamp (rediscovered by accident during T16/T18), and the Dockerfile sources-jar exclusion (old `6ec531f`; the recovered Dockerfile's `sharkengine-*.jar` glob shipped the sources jar into `mods/` while its own comment claimed otherwise — rediscovered 2026-07-24 by exactly this audit while preparing the smoke server). A third suspect (helm input send rate: old line every-tick/20 Hz vs. current ~10 Hz) was checked 2026-07-24 and REFUTED — a worked example of how to audit: the old fix's commit message shows it addressed a payload-yaw COPY (`hcPlayerYaw` copied onto entity yaw every tick → hold-then-jump staircase at 10 Hz); the current line carries no continuous yaw in the payload and instead integrates the HELD turn input server-side every tick, so the staircase mechanism is structurally absent. Compare the old fix's MECHANISM against the new architecture, never just the symptom name. (Remaining nuance, not a defect: ~100 ms input-change latency at 10 Hz ≈ up to ~6° over-turn on key release; every-tick sending stays the cheap knob if flight-feel smoke testing flags turn responsiveness.)

## Debugging "nothing happens" reports

When a player reports an interaction that silently does nothing (no crash, no log error) — e.g. "this item won't place" — check the server log first, but don't stop at "no exceptions, so the block/item registration must be fine." Isolate registration from interaction with `/setblock <pos> <block>` in a live client: if that succeeds, the block/blockstate/model is registered correctly and the bug is somewhere in the *interaction* path (an entity `interact()` override, a custom `useOn`, client-side input handling), not the block definition. This found the `ShipEntity.interact()` mount-hijack bug (2026-07-13) in a couple of commands, versus a much longer detour through blind GUI automation first (see also: `xdotool` synthetic mouse motion does not move the camera in this game — it uses GLFW raw mouse input, which ignores synthetic X11 pointer warps; use `/tp <pos> <yaw> <pitch>` or chat-driven `/data get`/`/setblock` commands for live diagnosis instead of trying to simulate mouse-look).

## CI

GitHub Actions:
- `.github/workflows/ci.yml` — two parallel jobs (no `needs:` between them): `build` runs `./gradlew --no-daemon build` (compiles main+client, runs unit tests — see client compile gotcha above), `gametest` runs `./gradlew --no-daemon runGametest`. JDK 21 Temurin, on push **and** PR to `main`, `develop`, `feature/**`.
- `.github/workflows/deploy.yml` — On push to `main` (plus `workflow_dispatch`): runs `./gradlew --no-daemon test`, then deploys to Railway (`railway up --detach --service sharkengine-mc-mod --environment production`) using the root `Dockerfile`. Requires `RAILWAY_TOKEN` secret.

Three things the workflow badges do **not** tell you:

1. **A green "Deploy Test Server" run does not mean anything was deployed.** The deploy job probes `secrets.RAILWAY_TOKEN` and gates both the CLI install and `railway up` on it being present; with the secret unset it prints "RAILWAY_TOKEN is not configured, skipping deploy." and **exits SUCCESS**. Check the step log or the live server, never the badge.
2. **The deploy path never compiles the client and never runs GameTests.** `deploy.yml` gates on `test` (which skips `compileClientJava`), and the Dockerfile builder runs `build -x test`. Only `ci.yml` runs `build` and `runGametest` — and there is no `needs`/`workflow_run` link between the two files, so a red CI does not block a deploy.
3. **Neither workflow runs `clean` or `runDatagen`**, so both depend entirely on the committed `src/main/generated` (72 tracked files). If that tree were ever gitignored or dropped, CI's `gametest` job and the built server image would go silently gameplay-dead (no `ship_eligible` tag) rather than failing the build.

The Dockerfile is a two-stage build (temurin `21-jdk` builder → `21-jre` runtime) that `ADD`s the Fabric server *launcher* and Fabric API from the network, so the image is **not self-contained** — first boot needs outbound network, and a successful image build says nothing about whether the server can start. Fabric loader/API versions are duplicated between `gradle.properties` and hardcoded URLs in the Dockerfile with **nothing validating the pair** (and `fabric.mod.json` only declares `"fabric-api": "*"`, so a mismatch won't be caught by the mod's own dependency check). `server/server.properties`, baked into the image, sets `online-mode=false` — the deployed server accepts any username with no Mojang auth.

> **`.dockerignore` ordering trap:** line 9 is `!server/server.properties`, a re-include that is currently a no-op. If someone "fixes" the build-context bloat by appending `server/` at the *end* of the file, last-match-wins makes the exclusion beat the negation and `COPY server/server.properties .` fails the build. Any such exclusion must go **before** that line.

## Datagen

`src/main/generated` (72 files, all git-tracked, load-bearing for CI) is a real resource root that Loom auto-registers. **All 11 registered blocks are fully datagen-migrated** — blockstate, block model, item model, loot table, recipe, `ship_eligible` membership, `en_us`/`de_de` lang — plus 4 crafting-intermediate items (item model + recipe only). `src/main/resources` retains only textures, sounds, one GameTest `.nbt` template and `fabric.mod.json`.

> Four provider javadocs still describe the migration as covering only `thruster`/`steering_wheel`/`bug` while their code covers all 11. **Do not read those javadocs as a scope statement** — read the `run()`/`generate()`/`buildRecipes()`/`addTags()` bodies. (That stale text is almost certainly where the previous version of this file got its wrong "3 blocks" claim.)

Non-obvious provider constraints, each documented in-source with a decompiler-verified rationale:

- **`SharkEngineModelProvider` is a raw `DataProvider`, not a `FabricModelProvider`** — every blockstate and model is a verbatim Java text-block constant, because `BlockModelGenerators`' output consumers are package-private and vanilla's model DSL has no public entry point for arbitrary inline `elements` geometry. Adding a block model means adding a constant **and** a matching entry to `run()`'s hand-written list; there is no iteration over `ModBlocks`. Forget the entry and the file is simply never generated.
- **Vanilla tag references must use `addOptionalTag`, not `addTag`.** `TagsProvider.run()`'s completeness check consults only this run's builders plus an empty parent lookup, never the real registry — a required reference to `#minecraft:planks` fails the datagen **run** with "Couldn't define tag … missing following references". Block elements are also added as `ResourceKey<Block>` via `builtInRegistryHolder().key()`, not the `add(Block)` overload, because in this project's actual dependency bytecode `tag(TagKey)` returns the raw `TagAppender` — idiomatic Fabric snippets from the docs will not compile here.
- **Recipes bypass `ShapedRecipeBuilder`**, which unconditionally synthesizes an unlock advancement and throws without `unlockedBy`. Recipes are constructed directly and `accept`ed with a null `AdvancementHolder`, so the mod ships **zero recipe-unlock advancements**.
- **Lang is monolithic and generated**: 108 keys per language, covering tutorial screens, HUD onboarding, assembly failure messages and builder UI — not just blocks. Adding *any* translatable string anywhere means editing `SharkEngineLangProvider` and re-running `runDatagen`.

## Trail Colour Pipeline

Thruster colour crosses four representations with **no dedicated storage anywhere**: a craft-time `DyeColor` data component on the item stack → an `EnumProperty<TrailColor>` blockstate on the placed block → the verbatim `BlockState` inside `ShipBlueprint` (so NBT save/load, S2C sync and disassembly carry it for free) → an RGB tint on a vanilla `DustParticleOptions` in the renderer. Persistence is therefore free and there is deliberately no second colour store. The design explicitly forbids a per-colour item/block id, a block entity, per-colour models or textures, and per-dye recipe JSONs — **each of those is locked by a failing test**, so if a change makes one of them look necessary, the design decision is what needs revisiting, not the test. Colouring is craft-time only (one `CustomRecipe` matcher for all 16 dyes, so it never appears in the recipe book); breaking a coloured thruster drops the plain item.

- The `ShipEntity` `TrailConfig` NBT compound is a **decoy** — a reserved pass-through slot nothing ever populates. A persistence GameTest locks the pass-through, which makes it look load-bearing in test output.
- **Derive part ids with `BuiltInRegistries.BLOCK.getKey(state.getBlock())`, never `BlockState.toString()`.** The latter yields `sharkengine:thruster[trail_color=red]`, which resolves to `FALLBACK` — mass 1, **no PROPULSION role** — i.e. a silently thrust-less ship with no exception and no log line. Two tests exist specifically to force this conversation; if someone "fixes" the registry to accept decorated ids instead of fixing the call site, the unit test fails by design.

## Known Open Defects

Recorded here because a reader of this file alone would otherwise assume the transform/VFX work is finished. All code-verified; not all reproduced in-game.

- **Disassembly ignores rotation.** `ShipEntity.disassemble()` places blocks at raw `dx/dy/dz` offsets from the current position, using neither `assemblyYaw` nor the live yaw, while rendering, collision and Edit-Mode materialization all use `ShipTransform.effectiveYaw`. Any ship that turned in flight drops its structure in the pre-turn orientation. Blocked target positions are counted but **never dropped as items**, and `discard()` runs unconditionally — those blocks are permanently lost; replaceable blocks (grass, water, snow) are silently overwritten without drops. `materializeForEdit` does this correctly — copy that path, don't re-derive.
- **Client thruster VFX are permanently stuck on `PHASE_1`.** `ShipEntity.phase` is a plain field written only inside `updatePhysics()`, which never runs client-side, and it is not in `SynchedEntityData` (unlike speed/mass/fuel/turn/vertical/health). The renderer therefore always sees 10 campfire-smoke particles at intensity 0.2 and never escalates to flame. Tuning trail visuals requires adding a `SYNC_PHASE` accessor first — touching the renderer alone has no visible effect.
- **Particle/sound emission is per-frame, not per-tick** (`spawnThrusterParticles` is called from `render()`), so it scales with FPS despite the constant being named `MAX_PARTICLES_PER_TICK`. Each frame also re-walks the entire blueprint and allocates a fresh list plus new `DustParticleOptions`. Moving it to a tick handler is the fix; raising the cap is not.
- **Edit-mode commit likely cannot reach the server once a ship has moved.** The server sends `ship.blockPosition()` as the preview's `wheelPos`, but the client stores `blueprint.origin()` (the *original* assembly position) and echoes that back, while `findEditModeShip` requires exact `blockPosition()` equality. The two coincide only immediately after assembly. Pre-launch assembly is unaffected.
- **A ship saved anchored reloads with a lying HUD.** Only `ANCHORED` and the blueprint-derived block count/mass are restored on load; fuel, health, engine-out and speed are re-synced only inside `updatePhysics()`, which `tick()` skips while anchored or in edit mode. The client shows `defineSynchedData` defaults (fuel 100, health 100) until the ship un-anchors and ticks once.
- **`editModeActive` is persisted**, so a world saved mid-edit reloads with it still true: disassembly is refused and re-entry is rejected as a conflict before the client is ever handed the `wheelPos` a commit would need.
- **Blueprint changes are never re-pushed** to already-tracking clients — `ShipBlueprintS2CPayload` is sent only from `startSeenByPlayer`, so after an edit commit the HUD shows the new stats while the rendered geometry stays pre-edit until the entity re-enters tracking range.
- **A pre-REQ-006 ship reloads with `seatAnchors = []` permanently** — migration never re-derives them. `hasCopilotSeat()` then returns false forever and visibility compliance passes vacuously, both silently. Re-assembly is the only fix.
- **Silent no-op:** a non-pilot right-clicking (empty hand) a ship with no copilot seat goes through `mountCopilot`, which returns false — but `interact` discards the return value and returns `CONSUME`. Nothing happens, nothing is logged, nothing is shown. Same shape as the historical `interact()` mount-hijack bug.
- `sharkengine/docs/plans/aircraft-extension-implementation.md` additionally records AIR-012 as explicitly "PARTIALLY DONE — do not mark DONE yet" (the collision probe tests the *current* position, not current + delta), plus open bug-ledger entries B2, B5 (entity bounds fixed 2.5×1.5 for up to 512 blocks), B10 and B11.

## Key Resources

- `src/main/resources/fabric.mod.json` — Mod metadata, entrypoints, dependencies, and the `fabric-gametest` registration array
- `src/main/java/dev/sharkengine/ship/part/VehicleBalance.java` — **single authority for every numeric balance constant** (masses, weight thresholds, fuel rate, bank/pitch). Change numbers here and in the table-driven `VehicleBalanceTest`, nowhere else.
- `src/main/generated/data/sharkengine/tags/block/ship_eligible.json` — Blocks allowed in ship structures; generated by `SharkEngineTagProvider` (edit the provider, run `./gradlew runDatagen`, commit the output — never hand-edit generated files)
- `src/main/generated/assets/sharkengine/lang/en_us.json` / `de_de.json` — All translatable strings; generated by `SharkEngineLangProvider` (same rule)
- `docs/release/2026-07-30-release-evidence.md` — the **live, signed-off gate record** and the only doc that tracks actual completion. Its rule: all 9 evidence rows must cite the same commit SHA; any new commit invalidates all of them.
- `docs/plans/2026-07-18-shark-engine-air-release-1.md` — the AIR Release 1 plan (T01–T24, plus an unnumbered REQ-025 Day-0 gate; 25 REQs / 24 tasks). **It contains no completion tracking** — its status line still reads `ready-for-phase-2` from 2026-07-19. Completion lives in the release-evidence doc and in git history, not here. PRD/vision/canvas/`traceability.md` sit alongside it.
- `sharkengine/docs/AIRCRAFT_CONCEPT_V2.md` — the concept doc `VehicleBalance`/`PartRole`/`WeightCategory`/`ShipTransform` javadoc cites as source of truth. **Only partially so**: `pilot_seat` and `copilot_seat` postdate it, and `VehicleBalance` is authoritative for them.
- `sharkengine/docs/BACKLOG.md` — live, and **test-locked** (see [Testing](#testing)); don't clean it up casually.

### Doc traps

- **Two `docs/` roots share the same relative prefix.** Javadoc in `ship/` and `ship/part/` (`docs/AIRCRAFT_CONCEPT_V2.md`, `docs/plans/flight-*.md`) resolves under **`sharkengine/docs/`**; javadoc in `gametest/` (`docs/plans/2026-07-18-…-test-plan.md`) resolves under **repo-root `docs/`**. Grepping a cited path from the wrong root looks like a broken reference when it isn't.
- **Stale status headers that will cost you real work if believed:**
  - `sharkengine/docs/plans/flight-pitch.md` says "Status: Draft, not started" — pitch **is implemented** (`ShipTransform.pitchFromVerticalInput`, shipped 2026-07-13). Every `file:line` reference in the `sharkengine/docs` plan docs is pinned to commit `33c3deb` and several are already known-stale.
  - `MSP-1.md` (repo root) is **superseded, not current** — untouched since 2026-02-27, and all four of its deliverables are implemented.
  - `sharkengine/docs/PRODUCTION_MVP_TASKS.md` — older backlog/gap analysis, untouched since 2026-03-06; its "block-count-only weight model" flag is resolved (mass-based since AIR-023) and its "no real 3D steering" claim is stale.
  - `sharkengine/specs/001-vertikale-bewegung/` — feature spec for the AIR flight-physics system. Its `state.yaml`/`README.md` tracker is stale (marks itself pending/in-progress) — and worse, `state.yaml`'s `phases:` mapping contains the key `tasks:` **twice**, so any YAML loader silently keeps the second (`pending`) block and discards the first (`complete`, 32 tasks). Don't trust it over the code.
  - `sharkengine/README.md`'s "Verified status" block cites 328 unit tests / 81 GameTests at `3c9ede5`; HEAD is 362 / 94. Any expected test count taken from it is wrong. Its *controls* documentation is accurate and useful.
  - `docs/intake/SHA256SUMS.txt` is a dead seal — it mixes path bases so `sha256sum -c` can never resolve all rows from any one cwd, and 5 of 12 entries fail because the files were edited after sealing. `docs/intake/VALIDATION_REPORT.md` predates the council that added REQ-025 and reports 24 where `traceability.md` says 25.
- **`AGENTS.md` (repo root) is superseded, not a peer source.** Untouched since 2026-02-25, it directs contributors to read `specs/001-vertikale-bewegung/plan.md` + `state.yaml` — exactly the tracker this file flags as untrustworthy — and misstates where the registries live (`ModBlocks` is in `dev.sharkengine.content`, not `dev.sharkengine.ship.*`).

## Coding Conventions

- 4-space indent, braces on same line
- PascalCase for classes/enums, lowerCamelCase for methods/fields
- Resource IDs use `lowercase_with_underscores`
- Registry helpers go in `dev.sharkengine.content` (`ModBlocks`, `ModEntities`, etc.)
- Commits: imperative scope format (`Fix: ...`, `Feat: ...`)
- Before modifying flight mechanics, read `sharkengine/specs/001-vertikale-bewegung/` for the *intended* design and `sharkengine/docs/AIRCRAFT_CONCEPT_V2.md` for the balance rationale — but treat the code in `src/main/java/dev/sharkengine/ship/` and the constants in `VehicleBalance` as the truth (see [Doc traps](#doc-traps))

## Root-Level Tooling

The repo root contains mod-deployment and publishing tooling outside the Gradle build, exposed as project slash commands (`.claude/commands/`):

- **`/mc-bugtest`** — Full manual bug-test cycle: `gradlew build`, verify JAR contents, deploy to Prism Launcher, re-enable disabled companion mods, generate an in-game test checklist from the recent git diff.
- **`/mod-deploy`** — Builds the mod JAR and copies it into Prism Launcher's Flatpak mods folder, re-enabling `.disabled` companion mods (fabric-api, controlify, yacl).
- **`/test-server`** — Rebuilds and restarts the Dockerized Minecraft test server (container `sharkengine-server`, host port 25566), using the root `Dockerfile` and `server/server.properties`.

> **`sharkengine/build/libs/` accumulates jars across version bumps and its filenames do not track content freshness.** It currently holds both `0.0.1` and `0.1.0` jars (plus their sources jars), and *neither* matches HEAD — one predates the gametest entrypoint entirely, and the local `0.1.0` jar is a third of the size of the released one. Any `sharkengine-*.jar` glob or highest-version heuristic — which is exactly what `/mod-deploy` and `/mc-bugtest` do when copying into Prism — can silently deploy a weeks-old artifact or a sources jar (duplicate mod-id crash). The Docker path is immune only because `.dockerignore` excludes `sharkengine/build`. Before treating any jar as *the* artifact, check its mtime and verify it contains the datapack (see the datagen ordering-race gotcha).

**Modrinth publishing**: `tools/modrinth-mcp-server/` is a Node ESM/TypeScript MCP server (registered in root `.mcp.json` as `modrinth`) exposing 9 `modrinth_*` search/version/publish tools against the Modrinth API v2. Write tools require PAT scopes `VERSION_CREATE` / `PROJECT_WRITE`. **As committed it cannot start**: `dist/` and `node_modules/` are gitignored (run `npm install && npm run build` in that directory first), and `.mcp.json` — which *is* tracked — hardcodes an absolute path into a different, unbuilt checkout and sets `"MODRINTH_TOKEN": ""`, so even on a machine that exports the token the server process receives an empty one and every write tool returns an error string instead of publishing.

**`tools/asset-gen/`**: the deterministic Python/Pillow texture pipeline the concept doc prescribes (`palette.json`, `generate.py`, 11 per-part scripts). It really exists, at **repo root** — while the concept doc describing it lives under `sharkengine/docs/`. Note `ResourceValidationTest` reads `../tools/asset-gen/palette.json`, so reorganizing this directory breaks unit tests.

**Leftover vendored scaffold — not documentation of this mod**: `sharkengine/.spec-flow/` (its `CLAUDE.md` and `.claude/` were removed 2026-07), plus `sharkengine/GEMINI.md`, `SPEC_FLOW_README.md`, `SPEC_FLOW_VERSTAENDNIS.md` and `SPCK__save_2026-02-14_*.md`. `GEMINI.md` in particular is a generic Spec-Flow orchestrator brief referencing files that don't exist here.
