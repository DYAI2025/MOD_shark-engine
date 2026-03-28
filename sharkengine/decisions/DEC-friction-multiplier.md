# DEC-friction-multiplier: Friction Multiplier 0.7 for Deceleration

**Status**: Active

**Category**: Architecture

**Scope**: backend

**Source**: [REQ-F-controlled-deceleration](../1-spec/requirements/REQ-F-controlled-deceleration.md)

**Last updated**: 2026-03-28

## Context

The hovercraft must stop within 10 ticks after all inputs are released. A friction multiplier applied per tick to the velocity vector achieves this with a simple, deterministic formula: `newVel = currentVel * friction`. The multiplier must be tuned so that max speed reaches < 0.001 within 10 ticks.

## Decision

Use a per-tick friction multiplier of **0.7** applied to all velocity components when input is zero. Velocities below 0.001 are snapped to zero.

## Enforcement

### Trigger conditions

- **Design phase**: when defining or modifying the deceleration model
- **Code phase**: when implementing or modifying `HovercraftController` deceleration logic

### Required patterns

- `FRICTION_MULTIPLIER = 0.7f` as a named constant in `HovercraftController`
- Applied as `vel *= FRICTION_MULTIPLIER` per axis per tick when all inputs are zero
- Below `VELOCITY_EPSILON = 0.001f`, snap to zero

### Required checks

1. Verify that max speed reaches < 0.001 within 10 ticks with friction 0.7
2. Verify monotonic decrease — no oscillation

### Prohibited patterns

- Hardcoded friction values outside the named constant
- Different friction values per axis (uniform friction on all axes)
