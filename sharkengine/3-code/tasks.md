# Tasks

## Status Legend

| Symbol | Status |
|--------|--------|
| `Todo` | Not started |
| `In Progress` | Currently being worked on |
| `Blocked` | Waiting on a dependency or decision (reason **must** be noted in the Notes column) |
| `Done` | Completed |
| `Cancelled` | No longer needed (reason **must** be noted in the Notes column) |

## Priority Legend

| Priority | Meaning |
|----------|---------|
| `P0` | Infrastructure / cross-cutting — required before feature work |
| `P1` | Implements a Must-have goal |
| `P2` | Implements a Should-have goal |
| `P3` | Implements a Could-have goal |

---

## Task Table

### Hovercraft Controller

| ID | Task | Priority | Status | Req | Dependencies | Updated | Notes |
|----|------|----------|--------|-----|--------------|---------|-------|
| TASK-create-data-records | Create HovercraftInput, HovercraftState, HovercraftOutput as Java records | P1 | Done | [REQ-MNT-hovercraft-controller-class](../1-spec/requirements/REQ-MNT-hovercraft-controller-class.md) | - | 2026-03-28 | |
| TASK-implement-movement-vector | Implement movement vector computation from playerYaw + forward/strafe axes with diagonal normalization | P1 | Done | [REQ-F-forward-by-player-yaw](../1-spec/requirements/REQ-F-forward-by-player-yaw.md), [REQ-F-strafe-movement](../1-spec/requirements/REQ-F-strafe-movement.md) | TASK-create-data-records | 2026-03-28 | DEC-diagonal-normalization applies |
| TASK-implement-vertical-axis | Implement vertical axis (Y-only, pitch-independent) | P1 | Done | [REQ-F-vertical-only](../1-spec/requirements/REQ-F-vertical-only.md) | TASK-create-data-records | 2026-03-28 | Implemented within HovercraftController.tick() |
| TASK-implement-backward | Implement backward movement (moveForward accepts [-1..1]) | P1 | Done | [REQ-F-backward-movement](../1-spec/requirements/REQ-F-backward-movement.md) | TASK-implement-movement-vector | 2026-03-28 | moveForward accepts full [-1..1] range |
| TASK-implement-deceleration | Implement friction deceleration (multiplier 0.7, snap below epsilon, 10-tick stop) | P1 | Done | [REQ-F-controlled-deceleration](../1-spec/requirements/REQ-F-controlled-deceleration.md) | TASK-create-data-records | 2026-03-28 | DEC-friction-multiplier applied: 0.7f + epsilon snap |
| TASK-implement-no-drift | Ensure zero input produces zero acceleration and no velocity change | P1 | Done | [REQ-F-no-drift-neutral](../1-spec/requirements/REQ-F-no-drift-neutral.md) | TASK-implement-deceleration | 2026-03-28 | isZero() triggers friction path only |
| TASK-implement-speed-cap | Implement speed capping by WeightCategory from HovercraftState | P2 | Done | [REQ-PERF-controller-tick-budget](../1-spec/requirements/REQ-PERF-controller-tick-budget.md) | TASK-implement-movement-vector | 2026-03-28 | maxSpeed from WeightCategory / 20 TPS |
| TASK-test-neutral-input | Test A: zero input over 40 ticks → no movement, no yaw change | P1 | Done | [REQ-MNT-flight-behavior-test-suite](../1-spec/requirements/REQ-MNT-flight-behavior-test-suite.md) | TASK-implement-no-drift | 2026-03-28 | |
| TASK-test-forward | Test B: moveForward=1 → movement along playerForwardXZ | P1 | Done | [REQ-MNT-flight-behavior-test-suite](../1-spec/requirements/REQ-MNT-flight-behavior-test-suite.md) | TASK-implement-movement-vector | 2026-03-28 | |
| TASK-test-backward | Test C: moveForward=-1 → movement opposite to playerForwardXZ | P1 | Done | [REQ-MNT-flight-behavior-test-suite](../1-spec/requirements/REQ-MNT-flight-behavior-test-suite.md) | TASK-implement-backward | 2026-03-28 | |
| TASK-test-strafe | Test D: moveStrafe=±1 → orthogonal movement | P1 | Done | [REQ-MNT-flight-behavior-test-suite](../1-spec/requirements/REQ-MNT-flight-behavior-test-suite.md) | TASK-implement-movement-vector | 2026-03-28 | |
| TASK-test-vertical | Test E: moveVertical=±1 → only Y changes | P1 | Done | [REQ-MNT-flight-behavior-test-suite](../1-spec/requirements/REQ-MNT-flight-behavior-test-suite.md) | TASK-implement-vertical-axis | 2026-03-28 | |
| TASK-test-combination | Test F: simultaneous multi-axis → correct vector sum, no axis dominance | P1 | Done | [REQ-MNT-flight-behavior-test-suite](../1-spec/requirements/REQ-MNT-flight-behavior-test-suite.md) | TASK-implement-movement-vector, TASK-implement-vertical-axis | 2026-03-28 | |
| TASK-test-deceleration | Test G: input release → stop within 10 ticks, monotonic decrease | P1 | Done | [REQ-MNT-flight-behavior-test-suite](../1-spec/requirements/REQ-MNT-flight-behavior-test-suite.md) | TASK-implement-deceleration | 2026-03-28 | Friction revised from 0.7 to 0.4 |
| TASK-test-look-direction | Test J: same input, different yaw → movement follows yaw | P1 | Done | [REQ-MNT-flight-behavior-test-suite](../1-spec/requirements/REQ-MNT-flight-behavior-test-suite.md) | TASK-implement-movement-vector | 2026-03-28 | |

### Server Integration

| ID | Task | Priority | Status | Req | Dependencies | Updated | Notes |
|----|------|----------|--------|-----|--------------|---------|-------|
| TASK-create-input-payload | Create HovercraftInputPayload with 4 floats and Fabric codec | P1 | Done | [REQ-F-input-model](../1-spec/requirements/REQ-F-input-model.md) | TASK-create-data-records | 2026-03-28 | HovercraftInputC2SPayload created |
| TASK-register-payload | Replace HelmInputPayload registration with HovercraftInputPayload in ModNetworking | P1 | Done | [REQ-F-input-model](../1-spec/requirements/REQ-F-input-model.md) | TASK-create-input-payload | 2026-03-28 | Both payloads registered; old removed in Phase 3 |
| TASK-server-handler | Implement server handler: validate, clamp, store inputs on ShipEntity | P1 | Done | [REQ-F-input-model](../1-spec/requirements/REQ-F-input-model.md) | TASK-register-payload | 2026-03-28 | Clamps + pilot validation |
| TASK-ship-entity-set-inputs | Modify ShipEntity.setInputs() to accept moveForward, moveStrafe, moveVertical, playerYaw; remove forward clamp | P1 | Done | [REQ-MNT-ship-entity-delegates](../1-spec/requirements/REQ-MNT-ship-entity-delegates.md) | TASK-server-handler | 2026-03-28 | setHovercraftInputs() added |
| TASK-ship-entity-delegate-tick | Modify ShipEntity.tick() to build HovercraftInput/State, call controller, apply HovercraftOutput | P1 | Done | [REQ-MNT-ship-entity-delegates](../1-spec/requirements/REQ-MNT-ship-entity-delegates.md) | TASK-ship-entity-set-inputs, TASK-implement-movement-vector | 2026-03-28 | Conditional branch: hovercraft vs legacy |
| TASK-decouple-bugblock | Decouple BUG-block facing from flight direction; use pilot yaw from ServerPlayer lookup | P1 | Done | [REQ-F-bugblock-orientation-only](../1-spec/requirements/REQ-F-bugblock-orientation-only.md) | TASK-ship-entity-delegate-tick | 2026-03-28 | Hovercraft path uses playerYaw from payload |
| TASK-verify-existing-tests | Run existing ShipPhysicsTest, ShipAssemblyServiceTest, FuelSystemTest — all must pass | P1 | Done | [REQ-REL-no-regression](../1-spec/requirements/REQ-REL-no-regression.md) | TASK-decouple-bugblock | 2026-03-28 | All existing + new tests pass |
| TASK-verify-build | Run ./gradlew build — no compilation errors | P1 | Done | [REQ-COMP-fabric-api-compatibility](../1-spec/requirements/REQ-COMP-fabric-api-compatibility.md) | TASK-verify-existing-tests | 2026-03-28 | BUILD SUCCESSFUL |

### Client Input

| ID | Task | Priority | Status | Req | Dependencies | Updated | Notes |
|----|------|----------|--------|-----|--------------|---------|-------|
| TASK-keyboard-mapping | Modify HelmInputClient: map W/S→moveForward, A/D→moveStrafe, Space/Shift→moveVertical; send playerYaw | P1 | Todo | [REQ-F-input-model](../1-spec/requirements/REQ-F-input-model.md) | TASK-register-payload | 2026-03-28 | |
| TASK-gamepad-strafe | Modify ControllerInput: left stick X→moveStrafe, left stick Y→moveForward | P1 | Todo | [REQ-F-input-model](../1-spec/requirements/REQ-F-input-model.md) | TASK-keyboard-mapping | 2026-03-28 | |
| TASK-gamepad-triggers | Map RT→moveVertical(+), LT→moveVertical(-) for gamepad triggers | P1 | Todo | [REQ-F-input-model](../1-spec/requirements/REQ-F-input-model.md) | TASK-gamepad-strafe | 2026-03-28 | DEC-gamepad-triggers-for-vertical applies |
| TASK-deadzone-filter | Apply deadzone filtering: sub-threshold stick values → exactly 0.0 | P1 | Todo | [REQ-F-controller-deadzone](../1-spec/requirements/REQ-F-controller-deadzone.md) | TASK-gamepad-strafe | 2026-03-28 | |
| TASK-input-normalization | Normalize keyboard and gamepad input to identical [-1..1] range before payload | P1 | Todo | [REQ-F-keyboard-controller-parity](../1-spec/requirements/REQ-F-keyboard-controller-parity.md) | TASK-deadzone-filter | 2026-03-28 | |
| TASK-remove-old-payload | Remove HelmInputPayload class and all references | P1 | Todo | [REQ-F-input-model](../1-spec/requirements/REQ-F-input-model.md) | TASK-input-normalization | 2026-03-28 | DEC-breaking-protocol-change applies |
| TASK-test-deadzone | Test H: sub-threshold stick values produce input = 0 | P1 | Todo | [REQ-MNT-flight-behavior-test-suite](../1-spec/requirements/REQ-MNT-flight-behavior-test-suite.md) | TASK-deadzone-filter | 2026-03-28 | |
| TASK-test-parity | Test I: keyboard and gamepad inputs produce identical controller movement | P1 | Todo | [REQ-MNT-flight-behavior-test-suite](../1-spec/requirements/REQ-MNT-flight-behavior-test-suite.md) | TASK-input-normalization | 2026-03-28 | |
| TASK-final-build-verify | Run ./gradlew build + ./gradlew test — full green | P1 | Todo | [REQ-REL-no-regression](../1-spec/requirements/REQ-REL-no-regression.md) | TASK-remove-old-payload | 2026-03-28 | |

### Deploy & Operations

| ID | Task | Priority | Status | Req | Dependencies | Updated | Notes |
|----|------|----------|--------|-----|--------------|---------|-------|
| TASK-phase-1-manual-testing | Verify all controller tests pass with ./gradlew test; document test commands | P1 | Done | - | TASK-test-look-direction | 2026-03-28 | 23 tests pass: 9 records + 14 controller |
| TASK-phase-2-manual-testing | Update runbook with server-side integration verification steps | P1 | Done | - | TASK-verify-build | 2026-03-28 | ./gradlew build green, all tests pass |
| TASK-phase-3-manual-testing | Create end-to-end runbook: launch client, build ship, fly with keyboard + gamepad, verify all axes | P1 | Todo | - | TASK-final-build-verify | 2026-03-28 | |

---

## Execution Plan

Defines the order in which tasks should be executed. Tasks are grouped into phases; complete all tasks in a phase before moving to the next. Within a phase, execute tasks in the listed order. Each phase ends with a deployable or testable system.

### Phase 1: Pure Flight Controller + Tests

**Capabilities delivered:**
- HovercraftController computes correct movement vectors from player yaw + 3 axes (GOAL-hovercraft-flight-model partial)
- Deceleration stops vehicle in 10 ticks (GOAL-hovercraft-flight-model partial)
- Pure class with no MC dependencies exists (GOAL-testable-flight-architecture complete)
- Tests A–G and J pass against real controller (GOAL-reliable-flight-tests partial)

**Tasks:**
1. TASK-create-data-records
2. TASK-implement-movement-vector
3. TASK-implement-vertical-axis
4. TASK-implement-backward
5. TASK-implement-deceleration
6. TASK-implement-no-drift
7. TASK-implement-speed-cap
8. TASK-test-neutral-input
9. TASK-test-forward
10. TASK-test-backward
11. TASK-test-strafe
12. TASK-test-vertical
13. TASK-test-combination
14. TASK-test-deceleration
15. TASK-test-look-direction
16. TASK-phase-1-manual-testing

### Phase 2: Server Integration

**Capabilities delivered:**
- ShipEntity delegates flight computation to HovercraftController (GOAL-testable-flight-architecture reinforced)
- BUG-block determines model orientation only, not flight direction (GOAL-hovercraft-flight-model partial)
- All existing tests pass (GOAL-no-regression-existing-systems partial)
- Build compiles without errors (REQ-COMP-fabric-api-compatibility)

**Tasks:**
1. TASK-create-input-payload
2. TASK-register-payload
3. TASK-server-handler
4. TASK-ship-entity-set-inputs
5. TASK-ship-entity-delegate-tick
6. TASK-decouple-bugblock
7. TASK-verify-existing-tests
8. TASK-verify-build
9. TASK-phase-2-manual-testing

### Phase 3: Client Input + End-to-End

**Capabilities delivered:**
- Keyboard WASD/Space/Shift sends 3-axis input (GOAL-hovercraft-flight-model complete)
- Gamepad sticks + triggers for strafe/vertical (GOAL-hovercraft-flight-model complete)
- Deadzone filters stick drift (REQ-F-controller-deadzone)
- Keyboard/controller parity (REQ-F-keyboard-controller-parity)
- Full end-to-end hovercraft flight functional
- No regression in existing systems (GOAL-no-regression-existing-systems complete)

**Tasks:**
1. TASK-keyboard-mapping
2. TASK-gamepad-strafe
3. TASK-gamepad-triggers
4. TASK-deadzone-filter
5. TASK-input-normalization
6. TASK-remove-old-payload
7. TASK-test-deadzone
8. TASK-test-parity
9. TASK-final-build-verify
10. TASK-phase-3-manual-testing
