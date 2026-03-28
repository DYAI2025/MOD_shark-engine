# DEC-gamepad-triggers-for-vertical: Gamepad Triggers for Vertical Movement

**Status**: Active

**Category**: Convention

**Scope**: frontend

**Source**: [REQ-F-vertical-only](../1-spec/requirements/REQ-F-vertical-only.md)

**Last updated**: 2026-03-28

## Context

The gamepad has limited inputs. The left stick handles forward/strafe (2 axes). Vertical movement (up/down) needs a separate control. Options: right stick Y axis, triggers (LT/RT), or bumpers (LB/RB).

## Decision

Use **right trigger (RT) for ascend** and **left trigger (LT) for descend**. Triggers provide analog input (0.0–1.0) which allows smooth vertical control.

## Enforcement

### Trigger conditions

- **Code phase**: when implementing gamepad input mapping in `ControllerInput` or `HelmInputClient`

### Required patterns

- RT → `moveVertical = +trigger_value` (up, 0.0 to 1.0)
- LT → `moveVertical = -trigger_value` (down, 0.0 to -1.0)
- Simultaneous LT+RT: values cancel (net vertical = RT - LT)

### Required checks

1. Deadzone applies to trigger values as well
2. Keyboard/controller parity: Space/Shift produce ±1.0, triggers produce analog ±0.0..1.0 — both are valid within [-1..1] range

### Prohibited patterns

- Using bumpers (binary on/off) for vertical when analog control is available
- Using right stick for vertical (reserves right stick for potential future camera control)
