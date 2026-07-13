# Flight Feel: Pitch Into Climb/Descend

**Status:** Draft, not started. Written 2026-07-13, same live playtest feedback session as
`docs/plans/flight-bank-roll.md` (FLR-001..004, done): "Du hattest ja auch [eine] Neigung
nach vorne und hinten vorhin drin. Mach die auch mal wieder rein und lass ihn nach unten
mit der Spitze neigen, wenn man per Shift nach unten an Höhe verliert, und für den
Steigflug über Space neigt sich die Spitze nach oben."

This is the pitch feature FLR's own Non-goals section explicitly deferred ("A separate,
larger feature... not bundled into this plan"). Same feel goal as FLR, different input
axis: vertical (Space/Shift) instead of turn (A/D).

## 1. Goal

When a ship climbs (Space) or descends (Shift), the rendered hull visually pitches nose-up
or nose-down — like a real aircraft — instead of moving vertically while staying flat.
Purely a **feel** improvement, same category as FLR's roll: makes flying read as pitching,
not elevator-style straight-up/down motion.

**Sign convention (from the user's own words):** Space (climb, `inputVertical=+1`) → nose
tips **up**. Shift (descend, `inputVertical=-1`) → nose tips **down**.

## 2. Non-goals (explicitly out of scope for this plan)

- **No change to actual vertical speed/physics.** `inputVertical` already directly drives
  vertical motion today (`ShipEntity.java:915`, `double verticalMotion = inputVertical *
  0.3;`) — this plan only adds a *rendered* tilt on top, it must not double-apply, scale,
  or otherwise interact with that existing physics term.
- **No camera pitch.** Camera stays level, matching FLR's own precedent — only the
  rendered hull tilts.
- **No effect on `ShipPhysics.checkCollision`'s offsets.** Collision stays flat, exact same
  accepted simplification as FLR (see that plan's Risks — a ship pitched hard into a climb
  renders as if nose-high into something it isn't actually colliding with yet).
- **Not touching FLR's existing roll code beyond adding one more composed rotation call.**
  `ShipEntityRenderer.render()` already has two `PoseStack.mulPose()` calls (yaw, then
  roll) — this adds a third, it doesn't restructure the first two.

## 3. Preconditions and known gaps

- **`inputVertical` is already synced client→server** — unlike `inputTurn` before FLR-001,
  no new C2S payload is needed. `HelmInputClient.java` already sends `vertical` (Space=+1,
  Shift=-1, keyboard/controller merged) as the `throttle` field of `HelmInputC2SPayload`;
  `ShipEntity.setInputs()` receives it and calls `setInputVertical(...)`, which sets
  `inputVertical` (already consumed by the physics term above). What's missing is the same
  gap FLR-001 filled for turn: **no S2C sync of the current value to clients**, so a third
  party watching someone else's ship can't render their pitch. FLP-001 is the direct
  analog of FLR-001, just for this field instead of `inputTurn`.
- **The render axis is very likely already known — a side effect of today's FLR-003 bug
  hunt, not a fresh guess.** FLR-003's *first* (wrong) attempt used `Axis.ZP` intending
  roll, and live-tested as visible PITCH (nose/tail tilt) instead — see
  `ShipEntityRenderer.java`'s inline comment on the `Axis.XN` roll call, and
  `flight-bank-roll.md`'s "as actually shipped" note. That means **Z is the empirically-
  confirmed pitch axis** in this exact rendering setup (block placement at
  `poseStack.translate(dx-0.5, dy, dz-0.5)`, same as roll) — only the **sign** (`Axis.ZP`
  vs `Axis.ZN`) needs live verification against the stated convention (climb → nose up),
  not the axis itself. Treat this as ground truth over re-deriving from scratch, same
  discipline FLR-002's javadoc already establishes for roll's sign.
- **Composition order with roll is a new open question FLR never had to answer** (FLR only
  had yaw+roll). Three composed rotations (yaw, roll, pitch) — the order pitch is inserted
  relative to roll (before or after) may visibly matter once a ship is both turning and
  climbing/descending at once. No existing convention to reuse here; FLP-003 must verify
  this specific case (turn + climb simultaneously) empirically, not just isolated pitch.
- This session has now hit "assumed rotation axis/sign was wrong" three times total across
  FLR-003's two corrections — treat every claim in this section as a strong prior, not a
  substitute for the FLP-004 manual checklist.

## 4. Task list

### FLP-001 · Sync current vertical input to the client
- **REQ:** flight-feel-02 · **Depends:** —
- **Files:** `ship/ShipEntity.java` — new `EntityDataAccessor<Float> SYNC_VERTICAL`,
  defined in `defineSynchedData` (default `0.0f`, alongside the existing `SYNC_TURN`),
  set in `tick()`'s server sync block from `inputVertical` (not `inputThrottle` — same
  value today, but `inputVertical` is the one the physics term and this feature both
  read; `inputThrottle` is otherwise near-vestigial), read via a new `getSyncedVertical()`
  getter mirroring `getSyncedTurn()`'s client/server branch exactly.
- **Tests:** none plain-unit-testable (EntityData needs a live entity), same as FLR-001 —
  covered by FLP-004's manual check.
- **Evidence:** `./gradlew build` green; no behavior change yet (nothing reads the new
  field until FLP-002/003).

### FLP-002 · Pure pitch-angle math (testable)
- **REQ:** flight-feel-02 · **Depends:** FLP-001 (conceptually; not a compile dependency)
- **Files:** `ship/ShipTransform.java` — new pure function
  `pitchFromVerticalInput(float verticalInput, float maxPitchDeg): float`, clamped
  `verticalInput ∈ [-1, 1] → pitch ∈ [-maxPitchDeg, +maxPitchDeg]`, structurally identical
  to `rollFromTurnInput`. Sign convention **from the user's own stated mapping** (see
  Goal): positive `verticalInput` (Space/climb) → positive return = nose up. Document this
  contract in the javadoc exactly like `rollFromTurnInput` does, including that this
  function does not decide the renderer's axis/sign mapping.
- **Tests:** `ShipTransformTest` — new nested class mirroring `RollFromTurnInputTests`:
  `verticalInput=0 → pitch=0`; `verticalInput=±1 → pitch=±maxPitchDeg`; a mid-value case;
  out-of-range input stays clamped.
- **Evidence:** unit tests green.

### FLP-003 · Smoothed pitch state + renderer rotation
- **REQ:** flight-feel-02 · **Depends:** FLP-001, FLP-002
- **Files:**
  - `ship/ShipEntity.java` — new client-only `clientPitch` field + `getClientPitch()`
    getter, updated in `tick()`'s client branch the same way `clientRoll` is: lerp toward
    `ShipTransform.pitchFromVerticalInput(getSyncedVertical(), VehicleBalance.MAX_PITCH_DEG)`
    using `VehicleBalance.PITCH_SMOOTHING_FACTOR`.
  - `client/render/ShipEntityRenderer.java` — a **third** `poseStack.mulPose(...)` call,
    composed after the existing yaw and roll calls. Start from `Axis.ZP`/`Axis.ZN` per the
    Preconditions note above and confirm sign live; also confirm composition order against
    roll using the combined-input case called out there.
- **Tests:** not unit-testable (rendering) — manual checklist in FLP-004, same pattern as
  FLR-003.
- **Evidence:** `./gradlew build` green (touches `src/client/java` — full `build`, not
  `test`); manual checklist in FLP-004.

### FLP-004 · Tuning constants + manual QA
- **REQ:** flight-feel-02 · **Depends:** FLP-003
- **Files:** `ship/part/VehicleBalance.java` — add `MAX_PITCH_DEG` (suggest starting
  15–20°, tune by feel — likely smaller than `MAX_BANK_DEG`'s 25°, since pitch reads as a
  more extreme attitude change at the same angle) and `PITCH_SMOOTHING_FACTOR` (start
  equal to `BANK_SMOOTHING_FACTOR=0.15f`, tune separately if it feels wrong), locked by a
  `VehicleBalanceTest` addition mirroring `bankRollConstantsAreSane()`.
- **Manual checklist (`runClient`):**
  1. Climb (Space) — nose tips up, matching the user's stated convention.
  2. Descend (Shift) — nose tips down, not inverted (this session has inverted a sign
     twice already — re-check explicitly).
  3. Release vertical input — pitch smoothly returns to level, no snap.
  4. Hover level (no vertical input) — no residual pitch/jitter.
  5. Turn AND climb/descend simultaneously — confirm pitch and roll compose sensibly
     together, not fighting or producing a nonsensical combined tilt (the open question
     from Preconditions).
  6. A second client observing the first player's ship sees the same pitch (confirms the
     synced-field approach reaches other observers, not just the pilot).
  7. Vertical speed/collision behavior unchanged (pitch is purely visual) — climb/descend
     near terrain, confirm no new phantom collisions from the tilted-looking hull.
- **Evidence:** all 7 checklist items confirmed by a human tester.

## 5. Risks and rollback

- **Wrong axis/sign** — mitigated more than FLR-003 was: the axis is a known side effect
  of today's bug hunt (see Preconditions), so this risk is narrowed to sign only. Rollback
  is trivial either way: revert the one added `mulPose` call, everything else is inert
  until consumed.
- **Composition order with roll** — new risk FLR didn't have (only one rotation before
  this). Mitigated by FLP-004 checklist item 5 explicitly testing the combined case.
  Rollback: reorder the two `mulPose` calls, no other change needed.
- **Sign inversion** — same class of bug this session has hit three times now (steering,
  FLR-003 twice). Mitigated by FLP-002 explicitly stating the sign convention in the
  user's own words rather than re-deriving it, and FLP-004's explicit re-check.
- **Visual-only pitch disagreeing with collision** — same accepted simplification as FLR
  (see that plan's Risks); if it feels wrong in practice, shrink `MAX_PITCH_DEG` rather
  than make collision pitch-aware.
- **Rollback:** every task here is additive (new field, new pure function, new renderer
  call, new constants), same as FLR. Reverting any subset leaves the mod exactly as it
  behaves today.

## 6. Effort estimate

Same size class as FLR (1–2 focused implementation sessions), likely at the **lower** end
of that range: the render axis is already known from today's own bug hunt instead of
needing to be discovered from three guesses, and the C2S input plumbing (`inputVertical`)
already exists — only the S2C sync is new. The one genuinely new risk (composition order
with roll, since this is the first *second* composed local-frame rotation) is the main
source of remaining uncertainty, not axis/sign discovery.
