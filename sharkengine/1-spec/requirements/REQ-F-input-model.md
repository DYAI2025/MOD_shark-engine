# REQ-F-input-model: Hovercraft Input Model with Three Axes

**Type**: Functional

**Status**: Approved

**Priority**: Must-have

**Source**: [US-fly-forward-backward](../user-stories/US-fly-forward-backward.md)

**Source stakeholder**: [STK-player](../stakeholders.md)

## Description

The flight input model must expose three independent axes: `moveForward` in [-1..1], `moveStrafe` in [-1..1], and `moveVertical` in [-1..1]. The existing `turn` channel must be removed from the flight input payload. An optional `desiredFacingYaw` field may be added for model visual orientation only.

## Acceptance Criteria

- Given the client captures player input, when the C2S payload is built, then it contains `moveForward`, `moveStrafe`, and `moveVertical` fields; no `turn` field is present in the flight input
- Given `moveForward = 1.0`, when the controller processes it, then the forward axis drives movement along the player yaw direction
- Given `moveStrafe = 1.0`, when the controller processes it, then the strafe axis drives movement orthogonal to the player yaw direction
- Given `moveVertical = 1.0`, when the controller processes it, then the vertical axis drives movement on the Y axis only

## Related Constraints

- [CON-server-authoritative-physics](../constraints/CON-server-authoritative-physics.md) — payload is transmitted C2S; server computes movement from the three axes
