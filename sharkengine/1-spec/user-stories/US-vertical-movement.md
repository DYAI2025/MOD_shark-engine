# US-vertical-movement: Control Altitude Independently of Look Direction

**As a** Minecraft player, **I want** to control altitude independently of my look direction, **so that** looking up or down does not cause unintended horizontal movement.

**Status**: Approved

**Priority**: Must-have

**Source stakeholder**: [STK-player](../stakeholders.md)

**Related goal**: [GOAL-hovercraft-flight-model](../goals/GOAL-hovercraft-flight-model.md)

## Acceptance Criteria

- Given I press the ascend key (Space), when the input is processed, then only the Y component of velocity increases; X and Z remain ≈ 0
- Given I press the descend key (Shift), when the input is processed, then only the Y component of velocity decreases; X and Z remain ≈ 0
- Given I am looking upward (pitch > 0) and press forward, when the input is processed, then the vehicle moves horizontally only — pitch does not contribute to the movement vector

## Derived Requirements

- [REQ-F-vertical-only](../requirements/REQ-F-vertical-only.md)
