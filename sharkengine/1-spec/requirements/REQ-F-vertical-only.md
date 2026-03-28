# REQ-F-vertical-only: Vertical Input Affects Only Y Axis

**Type**: Functional

**Status**: Approved

**Priority**: Must-have

**Source**: [US-vertical-movement](../user-stories/US-vertical-movement.md)

**Source stakeholder**: [STK-player](../stakeholders.md)

## Description

When `moveVertical ≠ 0`, the vehicle must move only on the Y axis. Player pitch must not influence horizontal movement direction. The XZ components of velocity must remain ≈ 0 when only vertical input is active.

## Acceptance Criteria

- Given `moveVertical = 1` and `moveForward = 0` and `moveStrafe = 0`, when the tick runs, then only the Y velocity component increases; X and Z components are < 0.01
- Given `moveVertical = -1` and `moveForward = 0` and `moveStrafe = 0`, when the tick runs, then only the Y velocity component decreases; X and Z components are < 0.01
- Given player pitch = 45° (looking up) and `moveForward = 1`, when the tick runs, then no vertical velocity is introduced by the forward input
