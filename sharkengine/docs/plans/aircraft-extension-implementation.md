# Aircraft Extension — Implementation Plan (TDD, iterative slices)

**Design source of truth:** `docs/AIRCRAFT_CONCEPT_V2.md` (REQ register in §11, bug ledger B1–B13 in §2, balance numbers in §4, palette in §5).
**Verified against:** repo commit `33c3deb`, 2026-07-12, branch `feature/aircraft-extension` (rebased onto recovered `main` — see concept doc's "Wichtiger Nachtrag"). All file:line references were re-verified against current source, not the audit snapshot.
**Task IDs:** self-contained register. Inherited from the external audit backlog (not committed to this repo): AIR-000, 010–014, 020–022, 030, 031, 040–042, 050–053, 060, 070–072. New in this plan: AIR-002 (resource repair, replaces audit AIR-001), AIR-003 (gametest infra), AIR-015 (blueprint v2), AIR-016 (dismount), AIR-023 (balance constants), AIR-032 (asset pipeline), AIR-054 (helicopter lift rule, pulled forward from audit P6). No ID is reused with a different meaning.

**Recovered-history note (2026-07-12):** `main` was restored from a lost force-push (see concept
doc). This closed several bugs the original plan assumed were fully open: B3 (fuel rate) and B7–B9
(dead resource paths) are now FIXED — AIR-014's fuel fix and AIR-002's resource fix were re-verified
against the recovered code and remain correct/still needed (the recovery didn't touch fuel timing or
thruster/steering_wheel paths; it added a THIRD instance of the same B7/B9 bug class for a new `bug`
block, fixed alongside AIR-002 — see AIR-002 task below). B1 (renderer yaw) is now PARTIALLY fixed by
an existing BUG-block-driven yaw system — AIR-011/AIR-015 below are rewritten to extend that system
rather than build a parallel one. B2/B5/B10 remain fully open as originally planned.

## Goal

Ship the extension in seven playable slices: repair the currently-dead resources and fuel bug, fix the yaw/transform foundation, introduce a property-based part system, add 11 craftable aircraft parts with own 16×16 copper-steel textures via datagen + a deterministic asset-gen pipeline, animate rotors, add flight rules, and harden multiplayer. Every task is TDD: failing test first, then implementation, then evidence.

## Non-goals

- No 6DoF/pitch/roll flight model (yaw-only rotation, matching current physics).
- No block-entity state inside blueprints (schema v2 reserves room; no BE serialization).
- No ground-mode rotor animation (placed blades stay static).
- No aerodynamics simulation (linear, table-driven stat model only).
- No 1.21.2+ port; target is exactly MC 1.21.1 / Fabric Loader 0.17.0 / Fabric API 0.114.0+1.21.1 / Loom 1.7 / Gradle 8.8 / Java 21.
- No Modrinth publishing until Slice 6 passes (`real-boundary-smoke`).

## Preconditions and known gaps

- Work happens on a feature branch (`feature/aircraft-extension`), not `main`. One branch per slice or one long-lived branch with slice-tagged merges — decide at Slice 0; default: long-lived branch, PR per slice.
- All commands run from `sharkengine/`. `./gradlew test` currently green locally; CI must be confirmed green first (AIR-000).
- **Gap (verify during AIR-003/AIR-030):** exact Fabric API syntax for GameTest run config and `fabricApi { configureDataGeneration() }` on Loom 1.7 / FAPI 0.114.0+1.21.1 — including datagen-with-split-sources (`client = true`) and the `fabric-gametest` / `fabric-datagen` entrypoint names. The patterns below are the well-known Fabric idioms; confirm against Fabric docs for 1.21.1 before wiring (Context7/Fabric wiki). Do not trust this plan's snippet syntax blindly.
- **Gap:** no numeric performance budget exists for 512-block ships; AIR-013 defines a measurement, not a pass/fail number, until a baseline exists.
- Existing saves: v1 blueprints in test worlds must survive (REQ-M1). Back up `run/` worlds before Slice 1.
- Manual visual gates need a human: texture/optics review and two-client smoke are user-facing checkpoints — flag them in PR descriptions.
- **Gap noted 2026-07-12 (ultrathink-craftsmanship):** `build.gradle` pins `id "fabric-loom" version "1.7-SNAPSHOT"` — a floating Maven coordinate, not a fixed release. "Exactly ... Loom 1.7" in Non-goals is aspirational, not actually reproducible: the same `./gradlew` invocation can silently resolve a different Loom build on a future day (no lockfile exists in this repo). Not fixed here — pinning to an exact non-SNAPSHOT Loom version is a real option but wasn't validated in this pass; flag if a build ever behaves differently across machines/CI runs than expected.
- **Open planning question 2026-07-12 (ultrathink-craftsmanship, unresolved — needs a decision, not just a doc fix):** AIR-040 (ships the concept doc's own first-playable-helicopter target) hard-`Depends` on AIR-030 (datagen) and AIR-031 (resource contract expansion, which itself depends on AIR-030) — but the risk table (below) claims these infra tasks are "isolated ... with no gameplay coupling" and that "hand-written resources from AIR-002 remain valid; datagen can land later without blocking Slice 3 asset work." AIR-002 already proved exactly that hand-write-then-migrate pattern works for 3 blocks; nothing in this doc explains why it wasn't extended to the 7 new helicopter parts. AIR-032 (the texture generator) is a *different* kind of dependency — new parts genuinely need new art that doesn't exist yet, so that one holds. Recommendation (not yet applied): drop AIR-030/AIR-031 from AIR-040's hard `Depends`, let AIR-040 hand-write its recipe/loot/model/blockstate/lang resources against the AIR-002 pattern, and let AIR-030 retarget them non-destructively whenever it lands. This is a sequencing decision, not a correction — apply only after confirming it doesn't conflict with intent for Slice 3.

## Slice 0 — Repair & Rails (playable gate: parts craftable in survival, fuel lasts documented time)

### AIR-000 · CI baseline green
- **REQ:** REQ-G1 · **Depends:** —
- **Files:** `.github/workflows/ci.yml` (read-only check), `sharkengine/gradle.properties`
- **Do:** Run `./gradlew clean check` locally; push branch; confirm CI green. Record version matrix (MC/Loader/FAPI/Loom/Gradle/Java) in PR description.
- **Tests:** existing suite (must stay green).
- **Evidence:** CI run URL green on the feature branch.

### AIR-002 · Resource repair: dead paths, stale formats, missing recipe/loot (B7, B8, B9)
- **REQ:** REQ-A4 · **Depends:** AIR-000
- **Files:** `src/main/resources/data/sharkengine/recipes/**` → `recipe/`, `loot_tables/blocks/**` → `loot_table/blocks/`, delete `assets/sharkengine/items/steering_wheel.json`, new `recipe/steering_wheel.json`, `loot_table/blocks/steering_wheel.json`; `src/test/java/dev/sharkengine/ship/ResourceValidationTest.java`
- **TDD:**
  1. RED: extend `ResourceValidationTest` — assert singular `data/sharkengine/recipe/` and `loot_table/` exist, plural dirs do NOT exist (mirror the existing tag guard, lines 51–68), every registered block id has recipe + loot table, recipe JSONs use `result.id` (not `result.item`), and no `assets/sharkengine/items/` dir exists. Write path assertions against a single resolvable resource-root constant — AIR-030 later retargets that root to the generated dir without rewriting each assertion.
  2. GREEN: move/rewrite the two thruster files (fix `result.item` → `result.id`), add steering_wheel recipe (`.P./PCP/.P.`, P=planks tag, C=copper_ingot) and loot table, delete the 1.21.2-format items file.
- **Evidence:** tests green + `runClient`: craft thruster and steering wheel from recipe book, break both, items drop. (Manual checklist via `/mc-bugtest`.)
- **STATUS: DONE** (2026-07-12, commits `9720b11`/`33c3deb`). Executed as planned; one scope addition discovered mid-task: the recovered `main` history (see top-of-doc recovered-history note) independently introduced a THIRD instance of the same bug class for the new `bug` block (`recipe/bug.json` had `result.item` not `result.id`; `assets/items/bug.json` was the same stale 1.21.2+ path as steering_wheel's). Fixed alongside in the same rebase pass; `CRAFTABLE_BLOCK_IDS` in `ResourceValidationTest` now covers `bug` too. 152/152 tests green.

### AIR-014 · Fuel rate fix (B3) — pulled forward from P1 because it is small and immediately playable
- **REQ:** REQ-F3 · **Depends:** AIR-000
- **Files:** `src/main/java/dev/sharkengine/ship/ShipEntity.java:421-423`, `ShipPhysics.java:102-121` (javadoc), `FuelSystem.java:65-71`; `src/test/java/dev/sharkengine/ship/FuelSystemTest.java`, `ShipPhysicsTest.java`
- **TDD:**
  1. RED: test "a ship consuming in PHASE_1 pays exactly 1 fuel after 20 *consuming* ticks, 0 before" — pure helper `FuelTicker` (throttle-conditioned counter), unit-testable without Fabric.
  2. GREEN: **accumulator, NOT tick-modulo** — a counter on ShipEntity that increments only while consumption conditions hold (`!engineOut && inputForward > 0`); at 20 it resets and subtracts `calculateFuelConsumption(phase)`. Rationale: `tickCount % 20` is entity-lifetime-aligned, not throttle-aligned — it fails the RED test when throttle starts mid-window and is exploitable (pulsing throttle around modulo boundaries flies nearly fuel-free). Persist the counter in NBT.
  3. Assert HUD remaining-time matches actual drain (both derive from the same per-second constant).
- **Evidence:** unit tests lock the 20-consuming-ticks rate; `runClient`: full tank at held full throttle lasts ≈ 37 s (phases advance at ticks 40/80/100/120 per `AccelerationPhase`, so most flight time runs at phase-5 rate 3/s — the exact expectation is locked in the unit test, the manual check validates order of magnitude).

### AIR-003 · Fabric GameTest infrastructure
- **REQ:** REQ-T1 · **Depends:** AIR-000
- **Files:** `build.gradle` (gametest run config + `fabric-gametest-api-v1` dependency if not in FAPI bundle), `fabric.mod.json` (`fabric-gametest` entrypoint), new `src/gametest/` or `src/main` test class `dev.sharkengine.gametest.AssemblySmokeTest`, `.github/workflows/ci.yml` (gametest job)
- **Do:** VERIFY exact 1.21.1 wiring against Fabric docs first (see Preconditions gap). Then: one smoke gametest — structure template with steering wheel + 4 planks + thruster, invoke `ShipAssemblyService.scanStructure`, assert `canAssemble`.
- **TDD:** the smoke gametest IS the red/green cycle for the infra.
- **Evidence:** `./gradlew runGametest` (or equivalent task) green locally and in CI.
- **STATUS: DONE** (2026-07-12, commit `c659f54`). Gap resolved by ground-truthing against the actual dependency bytecode (fabric-gametest-api-v1 2.0.5 jar, mapped minecraft-common jar), not just docs — docs across Fabric API versions disagreed on whether server-side gametests need a `fabric.mod.json` entrypoint. They do (`"fabric-gametest": ["dev.sharkengine.gametest.AssemblySmokeTest"]`); reproduced "No test functions were given!" without it first, confirming this empirically rather than trusting either doc source blindly. `fabric-gametest-api-v1` is transitively bundled via the umbrella `fabric-api` dependency — no separate Gradle dependency line needed. `loom { runs { gametest { inherit server; vmArg "-Dfabric-api.gametest"; ... } } }` boots a headless dedicated server; it requires EULA acceptance in its run dir, automated via an `acceptGametestEula` Gradle task (`runGametest` dependency) so this works from a clean checkout / CI without manual setup. `AssemblySmokeTest` uses `GameTestHelper.setBlock` against `FabricGameTest.EMPTY_STRUCTURE` — no NBT structure file needed for this simple case. Test class lives in `src/main/java` (not a separate `src/gametest` source set — deferred, not needed for a single smoke test; revisit if the gametest suite grows large enough to want isolation from the shipped mod jar). Updated to include an edge-placed BUG block per the now-current assembly requirements (`bugCount==1 && bugOnEdge`, unknown when this plan was first written — see concept doc §2 BUG-block section).
  **Correction 2026-07-12 (ultrathink-craftsmanship):** "verified bidirectionally" originally described an ad-hoc manual check during development (temporarily deleted the BUG-block placement, ran the test, saw it fail with `bugCount=0`, then reverted the edit) — no negative-path test was ever committed, so nothing would have caught a regression in the `bugCount==1` requirement. Fixed: `AssemblySmokeTest` now has a second, permanent `@GameTest` method, `rejectsAssemblyWithoutBug`, asserting `canAssemble()==false` and `bugCount()==0` with the BUG block omitted. Both methods verified green together (`All 2 required tests passed`).

## Slice 1 — Foundation (playable gate: asymmetric L-ship consistent at 0/90/180/270° in render, collision, disassembly)

### AIR-010 · `ShipTransform` — single rotation authority
- **REQ:** REQ-F1, REQ-F2 · **Depends:** AIR-000
- **Files:** new `src/main/java/dev/sharkengine/ship/ShipTransform.java`; new `src/test/java/dev/sharkengine/ship/ShipTransformTest.java`
- **TDD:** RED: pure-math tests — `rotateOffset` roundtrips for 0/90/180/270 and arbitrary angles (float tolerance), `snapToCardinal` boundaries (44.9°→0, 45.1°→90, negative angles), `effectiveYaw` wrap-around (−180/+180), `worldBlock` dedupe behavior on rounding collisions. **Plus one sign-convention cross-consistency test:** pin `rotateOffset(θ=+90°)` to the exact same `(dx,dz)` mapping as the `Rotation` enum used in disassembly (document the world truth: MC yaw is clockwise-positive from above, 0 = south; e.g. yaw +90° ⇔ `Rotation.CLOCKWISE_90`). Without this, renderer, collision, and disassembly can each be internally consistent yet mutually mirrored. GREEN: implement (no Minecraft classes in core math; `Rotation`/`BlockPos` only in thin adapters so tests run with existing stub pattern).
- **Evidence:** unit tests; grep gate: no other `Math.sin/cos` on blueprint offsets anywhere else (add an ArchUnit-style grep test or a documented review check).
- **STATUS: DONE** (2026-07-12, commit `933a15b`). Sign convention ground-truthed by decompiling the actual `net.minecraft.core.BlockPos.rotate()` bytecode (not assumed): `rotateOffset(+90)` confirmed identical to `Rotation.CLOCKWISE_90` (`(dx,dz)->(-dz,dx)`), `+180`/`+270` confirmed against `CLOCKWISE_180`/`COUNTERCLOCKWISE_90` the same way. TDD caught a real off-by-one in `wrapDegrees`'s `(-180,180]` boundary (used `>=`/`<` instead of `>`/`<=`, which excluded the valid value 180 and included the invalid value -180) — fixed before GREEN. Grep gate result: two other `Math.sin/cos` call sites exist (`ShipEntity.java:734-735` movement vector — not a blueprint offset, out of scope; `:783-784` dismount offset — this *is* B10, correctly still unmigrated until AIR-016). 167/167 tests green.
  **Caveats added 2026-07-12 (ultrathink-craftsmanship):** (1) the decompilation itself is NOT preserved anywhere in the repo — this doc's claim is reproducible only by redoing the same `javap` inspection against the same pinned dependency jar; if that becomes load-bearing (e.g. a Loom/Minecraft version bump), redo it, don't trust the claim from memory. (2) **`ShipTransform` is not wired into any production code yet** — `grep -rn ShipTransform` matches only `ShipTransform.java` and its own test. B1/B2 are exactly as broken in the live game today as before this task; what changed is that a correct, tested, but currently-unused utility now exists for AIR-011/012/016 to consume. Disassembly does not use `Rotation` yet either (that's AIR-012) — the cross-consistency check above is against vanilla's `BlockPos.rotate()` directly, not against this mod's own disassembly code.

### AIR-015 · Blueprint v2: `SchemaVersion` + `AssemblyYaw` sourced from existing `bugYawDeg` (B6 partial, B1 fix)
- **REQ:** REQ-M1 · **Depends:** AIR-010
- **Files:** `src/main/java/dev/sharkengine/ship/ShipBlueprint.java` (toNbt/fromNbt, new fields), `ShipAssemblyService.java:246` (local `bugYawDeg` already computed here — pass it into the blueprint constructor instead of only into `shipEntity.setBugYawDeg`/`setYawDeg` at lines 137-138), `ShipEntity.java` (legacy-save fallback in `readAdditionalSaveData`); new `ShipBlueprintTest.java`
- **REVISED (2026-07-12, recovered-history finding):** this is NOT a greenfield concept — `bugYawDeg` already exists, is already computed at scan time from the BUG block's `FACING`, and is already persisted in entity NBT (`ShipEntity.java:354,378`, key `BugYaw`). The only gap: it lives on the *entity*, not the *blueprint*, so `ShipEntityRenderer` and `ShipPhysics` — which only see the blueprint via `ShipBlueprintS2CPayload` — have no way to read it without a second, redundant sync path. **Do:** add an `assemblyYaw` field to `ShipBlueprint`/`ShipBlock` container, populate it from the existing `bugYawDeg` local variable in `ShipAssemblyService.tryAssemble()`, serialize in `toNbt`/`fromNbt`. Keep `ShipEntity.bugYawDeg` as-is for backward compat (existing saves) but make the blueprint field authoritative going forward.
- **Design constraint (client delivery):** `assemblyYaw` is a **ShipBlueprint v2 field written by `toNbt`** — never entity-only NBT. The blueprint's sole client path is `ShipBlueprintS2CPayload` (carries `blueprint.toNbt()`, sent in `startSeenByPlayer`); an entity-NBT-only field would never reach `ShipEntityRenderer` and AIR-011 would rotate from a wrong reference (this is the CURRENT bug — see AIR-011). The legacy-save fallback (`assemblyYaw := entity's persisted BugYaw`) runs **server-side in `readAdditionalSaveData` immediately after `fromNbt`**, before tracking starts — every S2C payload thus already carries v2 NBT; a client-side v1 path never exists.
- **TDD:** RED: roundtrip test v2 (SchemaVersion=2, AssemblyYaw preserved, matches the `bugYawDeg` that would have been computed for that BUG facing — reuse `directionToYaw()`'s SOUTH=0/WEST=90/NORTH=180/EAST=-90 convention in the test); legacy-save fallback as pure math with explicit yaw parameter; the "no visual jump for SOUTH-facing BUG, but a jump for others is exactly the current bug" property is asserted at entity level in a GameTest (assemble with each of the 4 BUG facings, assert render/collision agree with blueprint before AIR-011/AIR-012 land — this GameTest should FAIL before those tasks and PASS after, proving the fix); corrupt-BlockCount case (fromNbt trusts stored BlockCount over list size, `ShipBlueprint.java:100` — fix to derive from list). GREEN: implement.
- **Evidence:** unit tests; load a pre-change test world save → ship renders unchanged (SOUTH-facing BUG ships already look correct today, must stay correct).
- **STATUS: DONE** (2026-07-12, commit pending). Implemented largely as revised above, with two adjustments discovered during execution:
  1. **Infra discovery: `ShipBlueprint` cannot be unit-tested at all**, not just its NBT methods. `toNbt()`/`fromNbt()` reference real `CompoundTag`/`NbtUtils` types with no test-classpath stub (unlike `BlockPos`/`BlockState`); the moment ANY test touches `ShipBlueprint` — even just its constructor — the JVM's class-verification step needs those types resolvable and throws `NoClassDefFoundError`, before any NBT method is actually invoked. `ShipBlueprintTest.java` was deleted; all AIR-015 coverage (roundtrip, legacy-v1 default, corrupt-BlockCount fix, WEST-facing-BUG plumbing through the full assembly flow) is in a new `BlueprintPersistenceGameTest` instead, which runs against the real classpath. 6/6 GameTests green (2 from AIR-003 + 4 new).
  2. **The "assert render/collision agree with blueprint... should FAIL before AIR-011/012 land" GameTest was NOT written** — it would have committed a permanently-red test to CI until those tasks land, which we don't do. `assemblyPlumbsWestFacingBugYawIntoBlueprint` covers the DATA side only (assembling with a non-SOUTH BUG correctly sets `blueprint.assemblyYaw()`) — it's green today because it doesn't touch rendering/collision at all. The full "no visual jump" property becomes AIR-011's own acceptance gate once the renderer actually subtracts `assemblyYaw`.

  `ShipAssemblyService.StructureScan.toBlueprint()` and `openBuilderPreview()` both now call `.withAssemblyYaw(scan.bugYawDeg())`. `ShipEntity.readAdditionalSaveData` reads `BugYaw`/`ThrustYaw` before the blueprint (order now matters — previously the blueprint was read first) and patches `assemblyYaw` via `withAssemblyYaw(bugYawDeg)` whenever the loaded `Blueprint` NBT has no `AssemblyYaw` key (i.e. v1). Corrupt-BlockCount fix: `fromNbt` now always derives `blockCount` from `blocks.size()`, never trusts the stored NBT value (still written, for information only). Full build (client+main+test) and 167/167 unit tests green throughout.

### AIR-011 · Renderer subtracts assemblyYaw — fixes the non-SOUTH-BUG rotation snap (B1)
- **REQ:** REQ-F1 · **Depends:** AIR-010, AIR-015
- **Files:** `src/client/java/dev/sharkengine/client/render/ShipEntityRenderer.java:61-62`
- **REVISED (2026-07-12):** the renderer already rotates — `float smoothYaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot()); poseStack.mulPose(Axis.YN.rotationDegrees(smoothYaw));` — this is NOT new work. The bug is that `smoothYaw` uses raw entity yaw with no `assemblyYaw` subtraction, and blueprint offsets are captured in raw world-space (`ShipAssemblyService.java:202-207`), so a ship assembled with its BUG facing WEST/NORTH/EAST visibly snap-rotates by `bugYawDeg` the instant it launches (SOUTH, `bugYawDeg=0`, happens to look correct today — this is why nobody caught it: most test ships were probably built facing the default direction).
- **Do:** change `smoothYaw` computation to `Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - blueprint.assemblyYaw()` (from AIR-015). Everything else in the render method (the `mulPose` call site, per-block translate loop) is unchanged — this is a one-line fix once AIR-015 lands, not a rewrite.
- **Tests:** not unit-testable directly, but AIR-015's GameTest (assemble with all 4 BUG facings) is the real regression gate — write it to fail before this change and pass after. Manual checklist: assemble facing WEST/NORTH/EAST/SOUTH, confirm hull does NOT visibly rotate at the moment of launch in any case (previously: only SOUTH was silent); turning in flight still visibly rotates hull; parked (anchored) ship unchanged; **render orientation matches disassembled block orientation for the L-ship at 90° from a non-SOUTH BUG facing** (guards against renderer and disassembly — AIR-012 — rotating in mirrored directions).
- **Evidence:** `runClient` screenshots at all 4 BUG facings × 0/90/180/270 in-flight yaw attached to PR; checklist signed off; AIR-015's GameTest green.
- **STATUS: DONE** (2026-07-12, commit `5cfe331`, folded into the P0 live-playtest hotfix below). Implemented as the one-line fix described above, via `ShipTransform.effectiveYaw` (not a bare subtraction, per AIR-010's own rationale) rather than as a standalone task — the live bug report ("ruckelt/zieht", "kurz nach Start nicht weiterfliegen") made it clear this and AIR-012's collision-rotation half had to land together or the renderer and collision volume would visibly disagree the moment a ship turned. `runClient` screenshot verification not yet done by a human; GameTest regression coverage for the multi-facing property described above was NOT added (deferred — see AIR-012 note on the same gap). 167/167 unit tests + 6/6 GameTests green.

### AIR-012 · Collision + disassembly use transformed offsets (B2, B12, B13)
- **REQ:** REQ-F2 · **Depends:** AIR-010, AIR-015, AIR-003
- **Files:** `src/main/java/dev/sharkengine/ship/ShipPhysics.java:171-181` (`collectOffsets` takes effective yaw; probe NEXT position = current + deltaMovement), `ShipEntity.java:464-500` (`disassemble()`: target offset built at line 473 — snap to cardinal, `BlockState.rotate(Rotation)`, **and fix B13**: blocked target positions currently vanish silently — drop them as ItemStacks via `Block.popResource` instead), `ShipEntity.java:720,754,763` (the collision call site — `ShipPhysics.checkCollision(level(), blockPosition(), blueprint)` at 754, runs between the two `this.move(MoverType.SELF, …)` calls at 720/763 — pass yaw + next-pos probe here). *(Corrected 2026-07-12 via ultrathink-craftsmanship — the previous citations, 352-361 and 499-504, pointed at NBT-read and `this.discard()` code respectively, not disassembly/collision. Re-derive line numbers from the live file before editing; do not trust either version blindly.)*
- **TDD:** RED: unit tests for rotated offset collision sets (L-shape at 4 cardinals, dedupe after rounding); gametests: assemble L-ship, rotate 90°, fly toward wall → collision happens at rotated footprint; disassemble at 90° → blocks placed rotated, FACING states rotated; **with terrain deliberately occupying part of the rotated footprint: placed + dropped items == blueprint.blocks().size()** (no silent loss). GREEN: implement.
- **Evidence:** unit + gametest green; manual L-ship flight check.
- **PARTIALLY DONE (2026-07-12, commit `5cfe331`)** — the *collision* half of this task shipped early, ahead of schedule, as part of the same P0 live-playtest hotfix as AIR-011 above (the two cannot correctly land separately: collision and rendering must agree on the same rotated footprint or a ship that visibly clears an obstacle can still "collide" against its stale unrotated hitbox, which is exactly what live playtesting reported as "es ruckelt/zieht"). Specifically done: `ShipPhysics.checkCollision` gained a 4th `effectiveYawDeg` parameter, `collectOffsets` was renamed `collectRotatedOffsets` and now calls `ShipTransform.rotateOffset` per block before rounding to int; the solidity predicate was also fixed from `!isAir()` to `!getCollisionShape(level,pos).isEmpty()` (this second bug wasn't in this task's original scope — see B2's live-report writeup — but was inseparable from it in practice, since fixing rotation alone would still leave phantom-terrain false positives). **Still outstanding, NOT done:** the "next position" probe (this task specified probing `current + deltaMovement`, i.e. pre-empting a collision one tick early; the shipped fix probes the *current* position only, matching the pre-existing call pattern), disassembly rotation (`BlockState.rotate(Rotation)` at the disassemble() call site), and B13 (blocked-target items vanishing instead of dropping). Also still missing: the unit tests for rotated offset collision sets and the gametests listed above — the hotfix was verified via the existing (now-passing) test suite plus the 6 pre-existing GameTests, not new coverage for this specific rotation behavior. Re-open this task for the disassembly + B13 + next-pos-probe + new-test remainder; do not mark DONE yet.

### AIR-013 · Blueprint-derived render/culling bounds (B5) — culling ONLY, hitbox stays small
- **REQ:** REQ-F4 · **Depends:** AIR-015
- **Files:** `src/main/java/dev/sharkengine/ship/ShipEntity.java` (override `getBoundingBoxForCulling` and/or `shouldRenderAtSqrDistance` from blueprint min/max, refreshed in `setBlueprint` — runs on both sides via `ShipBlueprintHandler`), `ShipPartAnalyzer` later supplies bounds (interim: compute min/max from blueprint)
- **Do NOT enlarge `getDimensions`/EntityDimensions.** `ShipEntity.tick()` calls `this.move(MoverType.SELF, …)` (`ShipEntity.java:720,763`), and vanilla `Entity.move()` collides the dimensions-AABB against terrain — a hull-extent cube would phantom-collide with terrain inside the cube where no hull block exists, breaking low flight and landing. `isPickable()==true` (`ShipEntity.java:789`) would additionally turn the whole cube into a click/raytrace target. World collision stays with `ShipPhysics.checkCollision` (AIR-012); the entity hitbox keeps its small size. *(Line numbers corrected 2026-07-12 — see AIR-012 note.)*
- **TDD:** RED: unit test bounds computation from blueprint (asymmetric shape → correct min/max; rotation-safe extent for the culling box). Gametest: culling AABB covers rotated hull; dimensions-AABB unchanged (2.5×1.5). GREEN: implement; measure (not gate) frame cost on a 512-block ship, record number in PR.
- **Evidence:** tests green; long ship no longer disappears at screen edge (manual check); low flight over uneven terrain shows no phantom stops.

### AIR-016 · Safe dismount (B10)
- **REQ:** REQ-F5 · **Depends:** AIR-010, AIR-013, AIR-003
- **Files:** `src/main/java/dev/sharkengine/ship/ShipEntity.java:508-514`
- **TDD:** RED gametest: dismount from large hull → player lands in air/on ground outside hull blocks, never inside a blueprint block. GREEN: search transformed perimeter positions (ShipTransform) for a 2-block air column.
- **Evidence:** gametest green.

## Slice 2 — Semantic part system (playable gate: thruster runs via definition; builder shows structured errors)

### AIR-020 · `PartRole`, `VehiclePartDefinition`, `VehiclePartRegistry` (B4)
- **REQ:** REQ-S1 · **Depends:** AIR-000
- **Files:** new `src/main/java/dev/sharkengine/ship/part/{PartRole,VehiclePartDefinition,VehiclePartRegistry}.java`; register legacy `thruster` (PROPULSION, `liftMode=DIRECT`, thrust 20, mass 2), `steering_wheel` (CONTROL, mass 2), and **`bug`** (CONTROL, mass 1 — the recovered BUG-Frontsystem block registers a third ModBlocks entry AIR-020 didn't originally know about; AIR-031's resource contract requires a `VehiclePartDefinition` for every registered block, so this is mandatory, not optional); fallback definition (STRUCTURE, mass 1); new `VehiclePartRegistryTest.java`
- **Design constraints:** `VehiclePartDefinition` carries `liftMode` (`DIRECT` = engine lifts by itself, `ROTOR` = drives rotors only) — this is what lets AIR-054 exempt thrust-only ships without ID comparison. Registration lives in the registry's static initializer or `SharkEngineMod.init()` (**common entrypoint, both sides**) — never in server- or client-only paths, because `ShipEntityRenderer` (client) must resolve ROTOR_BLADE parts from the same registry (split source sets: client compiles against main, so `ship.part` is visible).
- **TDD:** RED: every registered block id → exactly one definition; unknown block → fallback; thruster resolves as PROPULSION/DIRECT without string comparison; definitions resolve **without any Fabric bootstrap** (plain unit test proves common availability). GREEN: implement.
- **Evidence:** unit tests.
- **STATUS: DONE** (2026-07-12, commit `43ec73b`). Implemented as planned. Deviation: the concept doc's §3.3 illustrative sketch included a `renderGroup` field with no defined type/semantics anywhere; followed this task's own design-constraints bullet (`liftMode`, explicitly required) instead and omitted `renderGroup` — flagged for AIR-051 (rotor render pass) to sanity-check role-based filtering is sufficient. 10/10 new tests, full `./gradlew clean build` (not just `test`) green — 17 test classes total.

### AIR-021 · `ShipPartAnalyzer` + `ShipStats` replace `ThrusterRequirements`
- **REQ:** REQ-S2 · **Depends:** AIR-020
- **Files:** new `ship/part/ShipPartAnalyzer.java`, `ShipStats.java`; delete `ThrusterRequirements.java`; touch `ShipAssemblyService.java:51,75,162-170`, `ShipEntity.java:555`; port `ShipAssemblyServiceTest`
- **TDD:** RED: stats aggregation deterministic for mixed part sets (mass/lift/thrust/fuelCap sums); assembly still requires ≥1 PROPULSION; unknown parts count as STRUCTURE mass 1. GREEN: implement. `ThrusterRequirements` must be gone (compile-time proof).
- **Evidence:** unit tests; gametest from AIR-003 still green.
- **STATUS: DONE** (2026-07-12, commit `51e5900`). Implemented as planned; `ThrusterRequirements` confirmed deleted (compile-time). Two deviations: (1) `BuilderValidationTest.java` — not in this task's file list — also called `ThrusterRequirements` directly and needed porting once that class was deleted; (2) the plan's cited line numbers were stale (AIR-011/012/015 P0 hotfixes added ~400 lines to `ShipEntity.java` since the plan was written) — edited by intent instead. 185/185 tests, full build green, `runGametest` 6/6 green (AIR-003 suite unaffected).

### AIR-022 · Structured assembly validation codes
- **REQ:** REQ-S3 · **Depends:** AIR-021
- **Files:** new `ship/part/AssemblyIssue.java` (code enum + optional BlockPos + args), `ShipAssemblyService`, `net/BuilderPreviewS2CPayload` (carry codes), client `builder/BuilderScreen`/`PreviewState` (render list), `lang/{en_us,de_de}.json`
- **TDD:** RED: unit tests per issue code (NO_PROPULSION, TERRAIN_CONTACT, TOO_FEW_CORE_NEIGHBORS, …); payload roundtrip test. GREEN: implement; keep old message behavior as translation of codes.
- **Evidence:** unit tests; `runClient`: builder preview lists all blockers.
- **STATUS: DONE** (2026-07-12, commit `2c83ea7`). Implemented as planned, all listed files touched. Gap found: this repo's `test` source set cannot compile against `net.minecraft.network.*` at all (same classpath-gap class as AIR-015's `ShipBlueprint` issue) — `AssemblyIssue` itself stayed network-free (plain-unit-testable, 15/15 green); the payload roundtrip test therefore became a GameTest (`BuilderPreviewPayloadGameTest`, new, registered in `fabric.mod.json`) rather than a plain unit test, mirroring AIR-015's precedent. That GameTest's meaningfulness was adversarially verified (temporarily broke `readIssues()` to always return empty, confirmed the exact expected GameTest failure, reverted). 210/210 unit tests, full build green (this task touches `src/client/java`), `runGametest` 8/8 green (6 pre-existing + 2 new).

### AIR-023 · `VehicleBalance` constants + complete block-count→mass switch
- **REQ:** REQ-S4 · **Depends:** AIR-020, AIR-021 (mass comes from `ShipStats` — no second aggregation)
- **Files:** new `ship/part/VehicleBalance.java` (the full table from concept §4 + rotor ω/spool + weight thresholds on mass); `WeightCategory.java` (fromBlockCount → fromMass); **`ShipPhysics.java:52-62`** (`calculateMaxSpeed` independently hardcodes the 20/40/60 block thresholds — derive it from `WeightCategory` instead, one authority); **`ShipEntity.java:408, 552-554, getWeightCategory:156`** (call sites switch to stats mass); **synced mass**: new `SYNC_MASS` EntityData (or sync the server-computed category), because `FuelHudOverlay.java:153` currently recomputes the category client-side from the synced block count — without a synced mass the HUD would show a different category than the server enforces; `FuelHudOverlay.java` reads the new value. New `VehicleBalanceTest.java`, adjust `ShipPhysicsTest`.
- **TDD:** RED: lock every number from concept §4 in one table-driven test; weight category boundaries on mass (30/60/90); **consistency test: category shown to HUD == category used for max speed for mixed-mass ships** (e.g. 15 blocks of engines+tanks ≈ mass 95 → OVERLOADED everywhere, not "HUD overloaded but flying at 30 b/s"); legacy plain-block ship speeds unchanged within tolerance. GREEN: implement.
- **Evidence:** unit tests.
- **STATUS: DONE** (2026-07-12, commit `91a7b00`). Implemented as planned; `VehicleBalance` covers all 14 named concept-table parts (including Slice 3-5 parts with no block registered yet) plus the §6 rotor ω/spool constants (declared ahead of AIR-051/052, not yet consumed). Confirmed real bug fixed in passing: `FuelHudOverlay` was independently recomputing `WeightCategory` from synced block count instead of reading the server's actual category — exactly the HUD/server desync this task's own note predicted; now reads the synced value. Plan's cited line numbers were stale (post-AIR-021 shift); real call sites located via grep. Flagged, not a defect: the new mass thresholds (30/60/90) are not a linear rescale of the old block-count ones (20/40/60) — e.g. mass=60 was HEAVY/10 b/s under the old rule, is MEDIUM/20 b/s under the new one, per the concept doc's literal (non-rescaled) table. 220/220 tests, full build green (exercises the `FuelHudOverlay` client-side change).

## Slice 3 — Datagen + helicopter asset set (playable gate: 7 parts craftable, placeable, textured, assemblable)

### AIR-030 · Datagen entrypoint + providers
- **REQ:** REQ-A3 · **Depends:** AIR-002
- **Files:** `build.gradle` (`fabricApi { configureDataGeneration(...) }` — VERIFY split-sources/client flag per Preconditions gap), `fabric.mod.json` (`fabric-datagen` entrypoint), new `src/.../datagen/{SharkEngineDataGenerator, ModelProvider, LootProvider, RecipeProvider, TagProvider, LangProvider}.java`
- **Do:** first migrate existing thruster/steering_wheel resources into providers; generated output must be **semantically equal** to the AIR-002 hand-fixed files (same parsed JSON content — byte-identity is not required, datagen's formatting is its own). Then delete the hand-written ones from `src/main/resources`, keep the generated dir as canonical (Fabric convention: `src/main/generated`), and **retarget the AIR-002 resource-root constant in `ResourceValidationTest` to the generated root** (the AIR-002 assertions must keep running against the new location, not be deleted).
- **TDD:** RED: `ResourceValidationTest` asserts generated dir contains recipe+loot+model+blockstate+lang for every registered block. GREEN: providers. Diff-stability: run datagen twice → no diff (test via CI step or gradle task assertion).
- **Evidence:** `./gradlew runDatagen` (verify task name) idempotent; CI green; `runClient` recipes still work.
- **STATUS: DONE** (2026-07-12, commit `ca4799e`). Implemented as planned: `fabricApi.configureDataGeneration()`, `fabric-datagen` entrypoint, 6 provider classes, thruster/steering_wheel/bug fully migrated. API shape was ground-truthed by decompiling this project's own pinned dependency jars rather than trusted from general Fabric docs (resolved the plan's own flagged "VERIFY" gap): no `client` flag exists on `DataGenerationSettings` in this Loom 1.7-SNAPSHOT (block/item model generation lives in the merged common jar, not client-only); `FabricTagProvider.tag()` returns the raw `TagsProvider.TagAppender` in this project's resolved `fabric-data-generation-api-v1` version, not the covariant builder some doc sources implied; `ShapedRecipeBuilder.save()` always synthesizes an unlock advancement even with none requested, worked around by building `ShapedRecipe` directly; referencing vanilla block tags needs `addOptionalTag` (a single-mod datagen run can't see vanilla's own tag completeness). Safety: generated output was semantically diffed (structural JSON comparison) against the hand-written originals *before* deleting them — exact match on blockstates/models/lang, only benign codec-default field additions on recipes/loot/tags (documented in the commit). Diff-stability confirmed (two `runDatagen` runs, zero diff, including after `clean`). **New project-wide gotcha this task introduces** (added to CLAUDE.md): `configureDataGeneration()` wires `src/main/generated` deletion into the `clean` task, but generating it is *not* part of the normal build graph — `./gradlew clean build` alone now leaves the generated resources missing and `test`/`runGametest` red; local dev must run `./gradlew clean runDatagen build`. CI is unaffected (no `clean` step, generated output is committed and present after checkout) — confirmed by deliberately reproducing the failure mode against a plain `clean build` before writing the CLAUDE.md note, not just theorizing about it. 225/225 unit tests, adversarial RED check (deleted `src/main/generated`, confirmed 16/27 resource tests failed with the expected missing-file errors, restored), `runGametest` 8/8 green (with `runDatagen` run first).

### AIR-032 · Deterministic texture pipeline (`tools/asset-gen/`)
- **REQ:** REQ-A2 · **Depends:** — (parallel to AIR-030)
- **Files:** new `tools/asset-gen/{palette.json,generate.py,parts/*.py,README.md}` (repo root, beside `tools/modrinth-mcp-server/`); outputs to `sharkengine/src/main/resources/assets/sharkengine/textures/{block,item}/`
- **Do:** palette.json with the four families/hex values from concept §5.1; generator rules (rivet grid every 4 px, 1-px seams, light leading edge); one drawing function per texture; CLI `python3 generate.py [part…]`.
- **TDD:** RED: `ResourceValidationTest` — every texture referenced by any model exists, is 16×16, uses only palette colors. GREEN: generate first texture (`airframe_panel`) + wire test.
- **Evidence:** tests green; regeneration produces zero diff; human optics review in `runClient` (explicit PR checklist item — pipeline output quality is a user gate, not self-certified).

### AIR-031 · Resource contract expansion
- **REQ:** REQ-A3, REQ-A4 · **Depends:** AIR-020, AIR-030, AIR-032 (the per-block `VehiclePartDefinition` assertion needs the registry from AIR-020 — the Depends graph, not slice order, is the execution contract)
- **Files:** `ResourceValidationTest.java`
- **Do/TDD:** assert for EVERY entry in `ModBlocks`: blockstate, block model, item model, loot, recipe (if craftable), en+de translation, `VehiclePartDefinition`, ship_eligible tag membership, resolving texture refs, lowercase filenames. Deleting any one generated file must fail the suite with a useful message (spot-check this failure mode once, document in PR).
- **Evidence:** deliberately delete one file → red with clear message → restore → green.
- **STATUS: DONE** (2026-07-12, commit `e904f75`). New `PerBlockResourceContractTests` nested class iterating all 3 current `ModBlocks` entries (bug, steering_wheel, thruster), asserting the full contract listed above including `VehiclePartRegistry` resolution (AIR-020) and texture-reference resolution. Required failure-mode proof done as specified.

### AIR-040 · Core helicopter parts (registry → datagen → PartDefs → textures → tests)
- **REQ:** REQ-A1, REQ-A2 · **Depends:** AIR-021, AIR-023, AIR-030, AIR-031, AIR-032
- **Files:** `content/ModBlocks.java`, `content/ModTags.java` (+5 new tags), block classes under `content/block/` (facing/axis states, VoxelShapes), datagen providers, `tools/asset-gen/parts/*.py`, `VehiclePartRegistry` entries, `lang` via provider
- **Order (one PR per part or small groups, each fully green):** **intermediates FIRST** — `metal_sheet`, `rotor_shaft`, `engine_core`, `bearing_assembly` (every part recipe references them; registering a part recipe before its ingredients exist produces a datapack load error) — then `airframe_panel` → `fuselage_frame` → `helicopter_engine` → `rotor_hub` → `rotor_blade` → `landing_skid`.
- **TDD per part:** RED: resource-contract test fails for the new id the moment it is registered → GREEN: add all resources/definition; gametest: place+break drops item; assembly including the part yields expected `ShipStats` delta (table-driven from `VehicleBalance`).
- **Evidence:** per-part: unit+resource+gametest green; slice end: `runClient` — craft and place all 6 parts, assemble a helicopter shape (the strict rotor/lift validity rules arrive in Slice 4 via AIR-054; until then assembly uses the plain PROPULSION check from AIR-021, so `helicopter_engine` qualifies transitionally).

### AIR-042 · `fuel_tank` + capacity contribution
- **REQ:** REQ-A1 · **Depends:** AIR-040
- **Files:** part files as above; `ShipEntity` fuel init/refill path (`ShipEntity.java:201-202` MAX_FUEL cap → `100 + stats.fuelCapacity()`), `FuelHudOverlay` scale
- **TDD:** RED: unit — stats.fuelCapacity from blueprint with N tanks; refill caps at extended max; persistence roundtrip keeps extended fuel. GREEN: implement. Gametest: save/load keeps capacity.
- **Evidence:** tests green; `runClient` HUD shows extended tank.

## Slice 4 — Rotor MVP (playable gate: helicopter flies with visibly spinning rotor; lift rule enforced)

### AIR-050 · Rotor topology detection + validation
- **REQ:** REQ-R1 · **Depends:** AIR-040
- **Files:** new `ship/part/RotorAssembly.java`, analyzer extension; `AssemblyIssue` codes (ROTOR_NO_ENGINE, ROTOR_BLADE_GAP, ROTOR_MIXED_PLANE, ROTOR_BAD_BLADE_COUNT)
- **TDD:** RED first (this is the highest-unit-test-value task): valid 2-blade, valid 4-blade, gap in chain, one blade only, mixed plane, hub without adjacent engine, two rotors, main-vs-tail via hub axis. GREEN: implement detection during assembly scan; store assemblies in blueprint v2 (or derive deterministically — decide: derive, keeps NBT lean; document decision).
- **Evidence:** unit suite; gametest: invalid rotor blocks assembly with specific code.

### AIR-051 · Rotor render pass
- **REQ:** REQ-R2 · **Depends:** AIR-011, AIR-050
- **Files:** `ShipEntityRenderer.java` — filter ROTOR_BLADE blocks from static pass; per RotorAssembly: pivot to hub, rotate `ω·(level.getGameTime()+partialTick)` around hub axis (**game time, NOT entity `tickCount`** — `tickCount` is client-local and resets per tracking session, so two clients would permanently disagree on blade angle and re-tracking would visibly jump; `getGameTime()` is server-synced), render blades relative. **Cache the static-vs-rotor block partition once in `setBlueprint`** — no per-frame registry lookups across up to 512 blocks.
- **Tests:** manual checklist defined first: blades rotate around hub (not world origin), main+tail rotors spin on correct axes, static pass shows no duplicate blades, anchored ship idles slow, angle continuous after flying away and back (re-track).
- **Evidence:** `runClient` video/GIF in PR; checklist signed.

### AIR-052 · RPM/engine-state sync
- **REQ:** REQ-R3 · **Depends:** AIR-051
- **Files:** `ShipEntity` EntityData (target ω, engine state, `SPOOL_CHANGE_GAME_TIME` — the spool anchor is a **synced game-time timestamp**, never a client-local tick delta), spool logic in `VehicleBalance` constants (9→36°/tick over 40 ticks)
- **TDD:** RED: unit — spool interpolation deterministic from (state, gameTime − spoolChangeTime); the synced values change **only on state transitions** (assert the value sequence over a simulated flight: constant while cruising, one change per throttle/fuel event). Do NOT test via accessor-call counting — vanilla `SynchedEntityData.set()` already dedupes identical values (per-tick `set` calls with unchanged values generate zero network traffic, see existing pattern at `ShipEntity.java:678-682`), so call counts prove nothing about packets. GREEN: implement. Gametest: engine off (fuel out) → target ω hits idle→0.
- **Evidence:** unit + gametest; two-client visual check deferred to AIR-071 (note in PR).

### AIR-053 · Effects at part positions (B11)
- **REQ:** REQ-R4 · **Depends:** AIR-050, AIR-010
- **Files:** `ShipEntityRenderer.java:106-122` (particles at transformed PROPULSION/ROTOR_HUB positions), sound emitter position
- **TDD:** unit: transformed emitter positions for rotated ship (pure ShipTransform math). Manual: particles visually track engine when turning.
- **Evidence:** unit green + `runClient` check.

### AIR-054 · Helicopter lift rule (moved up from audit P6 — the rotor slice must gate flight, or rotors are decoration)
- **REQ:** REQ-S4, REQ-R1 · **Depends:** AIR-050, AIR-021
- **Files:** `ShipAssemblyService`/analyzer. Rule (role-based, NO block-ID comparison — REQ-S1): a craft may lift off iff **(a)** `Σ thrust(liftMode=DIRECT) > 0` (legacy thruster path — flies exactly as today) **or** **(b)** valid rotor topology AND `Σ blade lift ≥ mass` (AssemblyIssue INSUFFICIENT_LIFT). `helicopter_engine` is `liftMode=ROTOR` (from AIR-020), so it alone does not enable flight.
- **TDD:** RED: 2-blade rotor + mass 17 → rejected; mass 16 → accepted; thruster-only ship → accepted via DIRECT path; **helicopter_engine without any rotor → rejected** (ROTOR_NO_ENGINE/INSUFFICIENT_LIFT code); mixed thruster+rotor craft → accepted via DIRECT even with invalid rotor (document precedence). GREEN: implement.
- **Evidence:** unit + gametest.

## Slice 5 — Fixed-wing (playable gate: airplane with wing rules flies; invalid builds rejected with reasons)

### AIR-041 · Wing/tail asset set
- **REQ:** REQ-A1, REQ-A2 · **Depends:** AIR-040 pattern, AIR-031
- **Files:** as AIR-040 for `wing_root`, `wing_panel`, `wing_tip`, `tail_fin` (+ `reinforced_fabric` intermediate)
- **TDD:** same per-part contract cycle as AIR-040.
- **Evidence:** per-part green; parts orientable and contribute stats.

### AIR-060 · Fixed-wing flight rules
- **REQ:** REQ-W1 · **Depends:** AIR-041, AIR-054
- **Files:** analyzer/physics: lift counts at phase ≥3; tail_fin required for wing-lift crafts; left/right lift asymmetry ≤25% (AssemblyIssue codes ASYM_WINGS, NO_TAIL_FIN, INSUFFICIENT_LIFT_FIXED_WING)
- **TDD:** RED: table-driven validity matrix (symmetric ok, 30% asym rejected, no tail rejected, mixed rotor+wing craft → rotor rules win if valid rotor present — document precedence). GREEN: implement.
- **Evidence:** unit + gametest; `runClient` flight of a small plane.

## Slice 6 — Multiplayer release path

### AIR-070 · Protocol/schema versioning (B6 completion)
- **REQ:** REQ-M2, REQ-M1 · **Depends:** AIR-052
- **Files:** `net/ModNetworking.java` + new version-check payload or login-phase check; `blueprintSchemaVersion` already in NBT (AIR-015); clear disconnect message on mismatch
- **TDD:** RED: unit — version compare logic; payload roundtrip. GREEN: implement. Manual: old-client-jar vs new-server → clean rejection message (document how tested).
- **Evidence:** unit green + documented mismatch test.

### AIR-071 · Two-client smoke suite (`real-boundary-smoke`)
- **REQ:** REQ-M3 · **Depends:** AIR-070
- **Do:** Docker test server (`/test-server`, port 25566) + two real clients (Prism instance ×2, `/mod-deploy`). Scripted checklist: both see same craft/rotor animation/disassembly; join mid-flight; untrack/retrack (fly away+back); chunk reload; server restart persistence.
- **Evidence:** filled checklist + screenshots/GIF from both clients in PR. This is a human-in-the-loop gate.

### AIR-072 · Packaging
- **REQ:** REQ-G1 · **Depends:** AIR-071
- **Do:** version bump (`gradle.properties` mod_version 0.1.0), changelog, build server+client jar (identical), optional Modrinth draft via `tools/modrinth-mcp-server` (requires MODRINTH_TOKEN; ask user before publishing — outward-facing action).
- **Evidence:** built jar checksums recorded; publish only on explicit user go.

## Execution rules (apply to every task)

1. **RED first** — commit or at least run the failing test before implementation. GameTest counts as a test; a manual checklist written *before* coding counts as RED for render-only tasks.
2. **One task, one PR-sized change**; slice gates are user checkpoints (playable build + evidence summary).
3. `./gradlew check` + gametest task green before any "done" claim; never claim done from static JSON inspection or screenshots alone (concept §10).
4. Commit style: `Feat:`/`Fix:` imperative (repo convention); use feature branch, no work on `main`.
5. When a "VERIFY" note exists (Fabric datagen/gametest syntax), resolve it against current Fabric 1.21.1 docs before writing code, and record the verified syntax in the PR.

## Risks and rollback

| Risk | Mitigation | Rollback |
|---|---|---|
| Blueprint v2 breaks existing saves | v1-fallback path + dedicated legacy roundtrip tests (AIR-015); back up `run/` worlds before Slice 1 | fromNbt keeps reading v1 forever; revert renderer commit independently (AIR-011 is isolated) |
| Yaw rotation reveals collision edge cases (rounding dedupe, corner clipping) | next-pos probe + unit-tested offset sets (AIR-012); L-ship gate at 4 cardinals | feature-flag: effective yaw can be forced to 0 via a single constant in `ShipTransform` — instant behavioral revert without code removal |
| Fabric datagen/gametest wiring differs from assumed syntax on Loom 1.7/FAPI 0.114 | explicit VERIFY step against docs; infra tasks isolated (AIR-003/030) with no gameplay coupling | hand-written resources from AIR-002 remain valid; datagen can land later without blocking Slice 3 asset work (fallback: hand-write JSONs against the same contract tests) |
| Texture quality subjectively poor despite palette conformance | human optics gate per asset PR; pipeline makes iteration cheap | regenerate from adjusted generator scripts; palette.json is the single tuning point |
| Balance numbers feel wrong in play | all constants in `VehicleBalance`, table-locked tests updated in one place | change table + tests in a single commit; no scattered magic numbers |
| WeightCategory switch (blocks→mass) changes legacy thruster-ship speeds | mass≈1–2× block count by table; thresholds scaled 1.5× to compensate; legacy ships covered by explicit test in AIR-023 | keep `fromBlockCount` as deprecated shim if regression reported |
| 512-block render/tick cost after rotation+rotor passes | measure in AIR-013 and AIR-051, record numbers; MAX_BLOCKS=512 already caps worst case | reduce MAX_BLOCKS or add render distance cutoff (one constant) |
| Two-client smoke reveals desync late | rotor design avoids per-tick packets by construction (deterministic formula); EntityData-only sync tested in AIR-052 | resync on tracking-start already exists via blueprint S2C; worst case: force re-track |
