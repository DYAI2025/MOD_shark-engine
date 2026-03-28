# REQ-F-controlled-deceleration: Controlled Stop After Input Release

**Type**: Functional

**Status**: Approved

**Priority**: Must-have

**Source**: [US-no-drift-at-rest](../user-stories/US-no-drift-at-rest.md)

**Source stakeholder**: [STK-player](../stakeholders.md)

## Description

After all inputs are released, the vehicle must decelerate and reach a full stop within **10 ticks** (0.5 seconds at 20 TPS). The vehicle must not overshoot, oscillate, or drift indefinitely.

## Acceptance Criteria

- Given the vehicle is moving at maximum speed and all inputs are released, when 10 ticks are simulated, then speed drops to < 0.001 blocks/tick
- Given deceleration is in progress, when ticks are simulated, then speed decreases monotonically — no oscillation or reversal of direction
