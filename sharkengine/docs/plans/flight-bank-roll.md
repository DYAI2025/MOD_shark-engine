# Flight Feel: Bank/Roll Into Turns

**Status:** Done. FLR-001..004 implemented, live-verified 2026-07-13. Written 2026-07-13
in response to live playtest feedback ("wenn das Luftfahrzeug sich in den Kurven wie ein
Flugzeug zur Seite neigen würde, so dass es wirklich wie ein kleiner Flugsimulator wäre").

**FLR-003 axis/sign, as actually shipped:** `Axis.XN`, composed after the yaw `mulPose`.
Getting there took three live-`runClient` iterations, not one — recorded here since the
task text below (left as originally written) undersells how much empirical correction was
needed and this project has already been burned twice by assuming a rotation sign instead
of ground-truthing it:
1. `Axis.ZP` (first guess) — wrong axis entirely, produced pitch (nose/tail tilt) instead
   of roll.
2. `Axis.XP` — correct axis (left/right dip), wrong sign — a right turn dipped the left
   side.
3. `Axis.XN` — confirmed correct on both axis and sign by the user
   ("er neigt sich richtig jetzt nach links und rechts").

See `ShipEntityRenderer.java`'s inline comment above the `mulPose(Axis.XN...)` call for
the full derivation/rationale.

## 1. Goal

When a ship turns (A/D input), the rendered hull visually rolls/banks toward the turn
direction — like a real aircraft — instead of staying perfectly level through a flat
yaw-only turn. Purely a **feel** improvement: makes flying read as banking, not sliding.

## 2. Non-goals (explicitly out of scope for this plan)

- **No aerodynamic/physics effect.** Banking does not change turn rate, speed, lift, or
  add a stall/altitude-loss mechanic. `ShipEntity.tick()`'s movement math is untouched.
- **No camera roll.** The third-person camera (`FlightCameraHandler`) stays level; only
  the rendered hull tilts underneath/in front of it. This matches how most third-person
  vehicle games handle banking (camera stays a stable reference frame).
- **No pitch (nose up/down).** A separate, larger feature — vertical-input-driven pitch
  would need its own design (does it affect vertical speed? camera? collision probe
  shape?) and is not bundled into this plan.
- **No effect on `ShipPhysics.checkCollision`'s rotated offsets.** Those stay yaw-only.
  Rolling the rendered model while the collision volume stays flat is an accepted,
  deliberate simplification (see Risks).

## 3. Preconditions and known gaps

- `ShipEntityRenderer.render()` currently applies exactly one `PoseStack.mulPose()` — a
  yaw rotation via `ShipTransform.effectiveYaw(entityYaw, blueprint.assemblyYaw())`
  (AIR-011). Roll is a **second**, composed rotation; get the order/axis wrong and the
  hull rolls around the wrong pivot (world-space Z instead of the ship's own,
  already-yawed, forward axis) — see Risks.
- `inputTurn` (`ShipEntity.java`) is a **plain server-only field**, never synced to the
  client via `EntityData`. Rendering runs for every observer (including a third party
  watching someone else's ship), so the roll target needs a synced value — it cannot be
  read from the local player's own last-sent input the way `HelmInputClient` can.
- No existing roll/bank concept anywhere in this codebase. This is new, not an extension
  of something partially built.
- This session already hit the *exact* failure mode of getting a rotation sign backwards
  twice (steering inversion; render-vs-physics yaw mismatch) — both times the fix was to
  ground-truth against an already-proven convention rather than re-derive from scratch.
  Task FLR-003 below deliberately reuses the same proven turn-sign convention instead of
  re-deriving bank direction independently.

## 4. Task list

### FLR-001 · Sync current turn input to the client
- **REQ:** flight-feel-01 · **Depends:** —
- **Files:** `ship/ShipEntity.java` — new `EntityDataAccessor<Float> SYNC_TURN`, defined
  in `defineSynchedData` (default `0.0f`), set alongside the other `SYNC_*` writes in
  `tick()`'s server branch, read via a new `getSyncedTurn()` getter following the
  existing `getWeightCategory()`/`getFuelLevel()` client/server-branching pattern.
- **Tests:** none plain-unit-testable (EntityData needs a live entity) — covered by
  FLR-004's manual check plus (optionally) a GameTest asserting the synced value updates
  after `setInputs()`, mirroring `BlueprintPersistenceGameTest`'s style if a regression
  guard is wanted here.
- **Evidence:** `./gradlew build` green; no behavior change yet (nothing reads the new
  field until FLR-002/003).

### FLR-002 · Pure roll-angle math (testable)
- **REQ:** flight-feel-01 · **Depends:** FLR-001 (conceptually; not a compile dependency)
- **Files:** `ship/ShipTransform.java` — new pure function, e.g.
  `rollFromTurnInput(float turnInput, float maxBankDeg): float`, clamped
  `turnInput ∈ [-1, 1] → roll ∈ [-maxBankDeg, +maxBankDeg]`. Sign convention: **reuse**
  the already-proven relationship from the P0 steering fix
  (`ShipEntity.tick()`: `yaw = getYRot() - inputTurn*3f`, i.e. positive `inputTurn`
  turns left) — banking into a left turn should roll the same sign as a real aircraft's
  left bank; derive the constant's sign from that existing code, don't re-derive
  independently (see Preconditions).
- **Tests:** `ShipTransformTest` — new cases: `turnInput=0 → roll=0`;
  `turnInput=±1 → roll=±maxBankDeg` (boundary); a mid-value case;
  out-of-range `turnInput` (e.g. 1.5) stays clamped to `maxBankDeg` (defensive, even
  though callers should already clamp).
- **Evidence:** unit tests green.

### FLR-003 · Smoothed roll state + renderer rotation
- **REQ:** flight-feel-01 · **Depends:** FLR-001, FLR-002
- **Files:** `client/render/ShipEntityRenderer.java` — a per-renderer-instance (or
  per-entity, via a small `Map<UUID, Float>` if multiple ships must animate
  independently — confirm during implementation whether `EntityRenderer` instances are
  already effectively per-entity-type-shared and pick accordingly) smoothed roll float
  that lerps toward `ShipTransform.rollFromTurnInput(entity.getSyncedTurn(), MAX_BANK_DEG)`
  each frame; apply as a **second** `poseStack.mulPose(Axis.??.rotationDegrees(roll))`
  call, composed so it rotates around the ship's own forward axis **after** the existing
  yaw `mulPose` (i.e. roll is applied in the already-yaw-rotated local frame, not world
  space) — verify the exact axis (`Axis.ZP` vs `Axis.ZN`) empirically in `runClient`
  rather than assuming; a wrong axis here rolls the hull sideways-into-the-screen instead
  of banking.
- **Tests:** not unit-testable (rendering) — this project's established pattern for
  render-only tasks (e.g. AIR-011) is a manual verification checklist as the acceptance
  evidence, not a skipped test.
- **Evidence:** `./gradlew build` green (this touches `src/client/java` — full `build`,
  not `test`, per the client-compile gotcha); manual checklist in FLR-004.

### FLR-004 · Tuning constants + manual QA
- **REQ:** flight-feel-01 · **Depends:** FLR-003
- **Files:** `ship/part/VehicleBalance.java` — add `MAX_BANK_DEG` (suggest starting at
  20–25°, tune by feel) and a roll-smoothing lerp factor as named constants, matching
  this project's "single authority for numeric constants, locked by a table-driven test"
  convention already established there.
- **Manual checklist (`runClient`):**
  1. Turn left (A) — hull rolls the same direction a real aircraft banks left; camera
     stays level.
  2. Turn right (D) — mirrors correctly, not inverted (explicitly re-check this given
     this session's history with inverted-turn bugs).
  3. Release turn input — roll smoothly returns to level, no snap.
  4. Fly straight — no residual roll/jitter at rest.
  5. Collision/flight behavior unchanged (roll is purely visual) — fly through a tight
     turn near terrain, confirm no new phantom collisions from the tilted-looking hull.
  6. A second client observing the first player's ship sees the same roll (confirms the
     synced-field approach from FLR-001 actually reaches other observers, not just the
     pilot's own render).
- **Evidence:** all 6 checklist items confirmed by a human tester (this is explicitly a
  "pipeline output quality is a user gate" style task, per this project's precedent).

## 5. Risks and rollback

- **Wrong rotation axis/order** (roll composed before yaw, or around the wrong axis) —
  the most likely failure mode. Mitigation: FLR-003 explicitly calls for empirical
  `runClient` verification of the axis rather than assuming; rollback is trivial (revert
  the one added `mulPose` call, everything else is inert until consumed).
- **Sign inversion** (bank rolls opposite to the turn direction) — this exact class of
  bug has hit this session's steering work twice already. Mitigation: FLR-002 explicitly
  reuses the already-proven turn-sign convention instead of re-deriving it.
- **Visual-only roll disagreeing with collision** — a ship banked hard into a turn will
  render as if leaning into a wall it isn't actually colliding with yet (collision stays
  yaw-only/flat). Accepted for this plan's scope (see Non-goals); if it feels wrong in
  practice, a follow-up could shrink `MAX_BANK_DEG` rather than extend collision to be
  roll-aware (a much bigger change).
- **Rollback:** every task here is additive (new field, new pure function, new renderer
  call, new constants) — nothing modifies existing turn/collision/physics code. Reverting
  any subset of FLR-001..004 leaves the mod exactly as it behaves today, no partial-state
  risk.

## 6. Effort estimate

Small, self-contained feature — one new synced float, one pure/unit-tested math
function, one additional renderer rotation call, tuning constants. Comparable in size to
a single one of tonight's AIR-0XX render/physics tasks (e.g. AIR-011's renderer fix), not
a multi-slice epic like the aircraft-extension work. Rough estimate: **1–2 focused
implementation sessions**, most of the uncertainty concentrated in FLR-003's
axis/rotation-order empirical verification (the one part that can't be nailed down by
reasoning alone and needs `runClient` iteration).
