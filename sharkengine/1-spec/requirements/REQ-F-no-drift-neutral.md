# REQ-F-no-drift-neutral: No Self-Movement with Zero Input

**Type**: Functional

**Status**: Approved

**Priority**: Must-have

**Source**: [US-no-drift-at-rest](../user-stories/US-no-drift-at-rest.md)

**Source stakeholder**: [STK-player](../stakeholders.md)

## Description

With all inputs set to zero and the vehicle at rest, simulating 40 ticks must produce no measurable movement. Horizontal velocity, vertical velocity, and yaw must all remain within a small epsilon of zero. This prevents self-flight and unintended drift.

## Acceptance Criteria

- Given all inputs = 0 and vehicle at rest, when 40 ticks are simulated against the real `HovercraftController`, then: horizontal speed < 0.001 blocks/tick, vertical speed < 0.001 blocks/tick, yaw delta = 0, total position delta < 0.01 blocks
