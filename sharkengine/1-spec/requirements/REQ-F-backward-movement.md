# REQ-F-backward-movement: Backward Movement Opposite to Player Yaw

**Type**: Functional

**Status**: Approved

**Priority**: Must-have

**Source**: [US-fly-forward-backward](../user-stories/US-fly-forward-backward.md)

**Source stakeholder**: [STK-player](../stakeholders.md)

## Description

When `moveForward < 0`, the vehicle must accelerate in the direction exactly opposite to the player's horizontal look direction. The `forward` field in the current implementation is clamped to [0..1] — this clamp must be removed to allow negative values.

## Acceptance Criteria

- Given `moveForward = -1`, when 20 ticks are simulated, then the movement vector points opposite to `playerForwardXZ`
- Given the `ShipEntity.setInputs(...)` method, when `moveForward = -1` is passed, then the value is not clamped to 0
- Given `moveForward = -1` and `moveStrafe = 0`, when the tick runs, then the lateral component of velocity is < 0.01
