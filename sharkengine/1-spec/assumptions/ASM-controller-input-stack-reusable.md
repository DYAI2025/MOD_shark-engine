# ASM-controller-input-stack-reusable: Existing Input Stack Can Be Adapted to Three-Axis Model

**Category**: Technology

**Status**: Unverified

**Risk if wrong**: Medium — if the existing `ControllerInput` and `HelmInputClient` stack cannot be adapted to send `moveForward`, `moveStrafe`, `moveVertical` without a major rewrite, the client-side scope of the refactor grows significantly and may affect the MSP-1 timeline.

## Statement

The existing `HelmInputClient` and `ControllerInput` classes can be updated to produce the three-axis input model (`moveForward`, `moveStrafe`, `moveVertical`) by modifying key binding mappings and the C2S payload, without requiring a full rewrite of the client input architecture.

## Rationale

The analysis document identifies `HelmInputClient` as sending `forward`, `turn`, and `vertical` today. Replacing `turn` with `moveStrafe` and extending `forward` to `[-1..1]` is a bounded change. The C2S payload struct and the `ModNetworking` registration would need updating, but the pattern is already established.

## Verification Plan

Verify during the Design phase by reading `HelmInputClient.java` and `ControllerInput.java` in full. Confirm that adding a `moveStrafe` field and removing `turn` from the payload is achievable without restructuring the input pipeline. Mark Verified after the design document confirms the approach.

## Related Artifacts

- [REQ-F-input-model](../requirements/REQ-F-input-model.md), [GOAL-hovercraft-flight-model](../goals/GOAL-hovercraft-flight-model.md)
