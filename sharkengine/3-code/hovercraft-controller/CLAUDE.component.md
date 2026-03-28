# Hovercraft Controller

**Responsibility**: Pure flight logic — movement vector computation from player yaw + 3-axis input, deceleration via friction, speed capping by weight category. No Minecraft or Fabric dependencies.

**Technology**: Java 21 (standard library only). JUnit 5 for testing without Fabric environment.

**Source location**: `src/main/java/dev/sharkengine/ship/` — classes: `HovercraftController`, `HovercraftInput`, `HovercraftState`, `HovercraftOutput`

## Interfaces

- Class API consumed by **server-integration**: `HovercraftOutput tick(HovercraftInput input, HovercraftState state)`

## Requirements Addressed

| File | Type | Priority | Summary |
|------|------|----------|---------|
| [REQ-F-forward-by-player-yaw](../../1-spec/requirements/REQ-F-forward-by-player-yaw.md) | Functional | Must-have | Forward input moves along player horizontal yaw direction |
| [REQ-F-backward-movement](../../1-spec/requirements/REQ-F-backward-movement.md) | Functional | Must-have | Backward input (moveForward < 0) moves opposite to look direction |
| [REQ-F-strafe-movement](../../1-spec/requirements/REQ-F-strafe-movement.md) | Functional | Must-have | Strafe input moves orthogonally to look direction |
| [REQ-F-vertical-only](../../1-spec/requirements/REQ-F-vertical-only.md) | Functional | Must-have | Vertical input affects only Y axis |
| [REQ-F-no-drift-neutral](../../1-spec/requirements/REQ-F-no-drift-neutral.md) | Functional | Must-have | Zero input produces no movement |
| [REQ-F-controlled-deceleration](../../1-spec/requirements/REQ-F-controlled-deceleration.md) | Functional | Must-have | Vehicle stops within 10 ticks after input release |
| [REQ-MNT-hovercraft-controller-class](../../1-spec/requirements/REQ-MNT-hovercraft-controller-class.md) | Maintainability | Must-have | Pure class with no Fabric/MC dependencies |
| [REQ-MNT-flight-behavior-test-suite](../../1-spec/requirements/REQ-MNT-flight-behavior-test-suite.md) | Maintainability | Must-have | Tests A–J against real controller |
| [REQ-PERF-controller-tick-budget](../../1-spec/requirements/REQ-PERF-controller-tick-budget.md) | Performance | Should-have | tick() completes in < 0.5 ms |

## Relevant Decisions

| File | Title | Trigger |
|------|-------|---------|
| [DEC-friction-multiplier](../../decisions/DEC-friction-multiplier.md) | Friction Multiplier 0.7 for Deceleration | When implementing deceleration logic |
| [DEC-java-records-for-data-types](../../decisions/DEC-java-records-for-data-types.md) | Java Records for Flight Data Types | When creating HovercraftInput, HovercraftState, HovercraftOutput |
| [DEC-diagonal-normalization](../../decisions/DEC-diagonal-normalization.md) | Normalize Diagonal Movement | When implementing movement vector computation |
