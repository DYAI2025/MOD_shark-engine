# US-no-drift-at-rest: Vehicle Stays Still With No Input

**As a** Minecraft player, **I want** the vehicle to remain stationary when I release all controls, **so that** it does not drift or fly off unintentionally.

**Status**: Approved

**Priority**: Must-have

**Source stakeholder**: [STK-player](../stakeholders.md)

**Related goal**: [GOAL-hovercraft-flight-model](../goals/GOAL-hovercraft-flight-model.md)

## Acceptance Criteria

- Given all inputs are zero and the vehicle is at rest, when 40 ticks are simulated, then horizontal velocity remains ≈ 0, vertical velocity remains ≈ 0, yaw does not change, and total position delta stays below a small epsilon
- Given the vehicle is moving and all inputs are released, when ticks are simulated, then the vehicle decelerates and reaches a full stop within 10 ticks without overshooting or oscillating
- Given a gamepad is connected with sticks below the deadzone threshold, when input is read, then the resulting input value is exactly 0 — no drift caused by stick noise

## Derived Requirements

- [REQ-F-no-drift-neutral](../requirements/REQ-F-no-drift-neutral.md)
- [REQ-F-controlled-deceleration](../requirements/REQ-F-controlled-deceleration.md)
- [REQ-F-controller-deadzone](../requirements/REQ-F-controller-deadzone.md)
- [REQ-F-keyboard-controller-parity](../requirements/REQ-F-keyboard-controller-parity.md)
