# Client Input

**Responsibility**: Client-side input capture from keyboard and gamepad, deadzone filtering, normalization to [-1..1], and transmission of `HovercraftInputPayload` to server.

**Technology**: Java 21 + Fabric Client API (Loom client source set)

**Source location**: `src/client/java/dev/sharkengine/client/` — modified classes: `HelmInputClient`, `ControllerInput`

## Interfaces

- C2S networking with **server-integration**: sends `HovercraftInputPayload(moveForward, moveStrafe, moveVertical, playerYaw)` each tick while piloting

## Requirements Addressed

| File | Type | Priority | Summary |
|------|------|----------|---------|
| [REQ-F-input-model](../../1-spec/requirements/REQ-F-input-model.md) | Functional | Must-have | Three-axis input model: moveForward, moveStrafe, moveVertical; no turn channel |
| [REQ-F-controller-deadzone](../../1-spec/requirements/REQ-F-controller-deadzone.md) | Functional | Must-have | Stick values below deadzone threshold produce exactly 0 input |
| [REQ-F-keyboard-controller-parity](../../1-spec/requirements/REQ-F-keyboard-controller-parity.md) | Functional | Must-have | Equal logical inputs from keyboard and gamepad produce identical movement |

## Relevant Decisions

| File | Title | Trigger |
|------|-------|---------|
| [DEC-gamepad-triggers-for-vertical](../../decisions/DEC-gamepad-triggers-for-vertical.md) | Gamepad Triggers for Vertical Movement | When implementing gamepad input mapping |
| [DEC-breaking-protocol-change](../../decisions/DEC-breaking-protocol-change.md) | Replace HelmInputPayload with HovercraftInputPayload | When modifying networking payload construction |
