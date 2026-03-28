# REQ-F-forward-by-player-yaw: Forward Movement Follows Player Yaw

**Type**: Functional

**Status**: Approved

**Priority**: Must-have

**Source**: [US-fly-forward-backward](../user-stories/US-fly-forward-backward.md)

**Source stakeholder**: [STK-player](../stakeholders.md)

## Description

When `moveForward > 0`, the vehicle must accelerate along the player's current horizontal look direction (derived from player yaw, projected onto the XZ plane). The lateral component (strafe axis) and vertical component must remain ≈ 0 when only forward input is active.

## Acceptance Criteria

- Given player yaw = 0° (south) and `moveForward = 1`, when 20 ticks are simulated, then the movement vector points south (positive Z); lateral and vertical components are < 0.01
- Given player yaw changes to 90° (west) before input, when `moveForward = 1` is processed, then the movement vector points west — not toward the previous BUG-block orientation
- Given `moveForward = 1` and `moveStrafe = 0` and `moveVertical = 0`, when the tick runs, then velocity has no strafe or vertical component above floating-point epsilon
