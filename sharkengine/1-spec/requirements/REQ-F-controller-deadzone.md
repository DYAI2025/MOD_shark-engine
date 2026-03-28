# REQ-F-controller-deadzone: Gamepad Stick Deadzone Produces Zero Input

**Type**: Functional

**Status**: Approved

**Priority**: Must-have

**Source**: [US-no-drift-at-rest](../user-stories/US-no-drift-at-rest.md)

**Source stakeholder**: [STK-player](../stakeholders.md)

## Description

Gamepad stick values that fall below the configured deadzone threshold must be mapped to exactly 0 before being passed to the flight controller. This prevents stick noise and physical drift from causing unintended vehicle movement.

## Acceptance Criteria

- Given a stick value below the deadzone threshold (e.g., raw value = 0.05, threshold = 0.1), when input is read from `ControllerInput`, then the resulting `moveForward`/`moveStrafe`/`moveVertical` value is exactly 0.0
- Given a stick value above the deadzone threshold, when input is read, then the value is passed through (rescaled or raw, per design decision)
- Given the deadzone test (Test H from analysis document), when simulated with sub-threshold stick input, then no unintended rotation or movement occurs
