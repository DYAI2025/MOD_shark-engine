# REQ-F-strafe-movement: Strafe Movement Orthogonal to Look Direction

**Type**: Functional

**Status**: Approved

**Priority**: Must-have

**Source**: [US-strafe-left-right](../user-stories/US-strafe-left-right.md)

**Source stakeholder**: [STK-player](../stakeholders.md)

## Description

When `moveStrafe ≠ 0`, the vehicle must move orthogonally to the player's horizontal look direction on the XZ plane. Positive strafe = right, negative strafe = left (relative to look direction). The forward/backward and vertical components must remain ≈ 0 when only strafe input is active.

## Acceptance Criteria

- Given `moveStrafe = 1` and `moveForward = 0`, when the tick runs, then the movement vector is perpendicular to `playerForwardXZ`; forward component < 0.01
- Given `moveStrafe = -1` and `moveForward = 0`, when the tick runs, then the movement vector is perpendicular and opposite; forward component < 0.01
- Given `moveStrafe = 1` and `moveForward = 1` simultaneously, when the tick runs, then both axes contribute to the velocity vector and neither suppresses the other
