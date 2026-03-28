# US-strafe-left-right: Strafe Left and Right Without Rotating

**As a** Minecraft player, **I want** to move the vehicle sideways (left/right) without rotating it, **so that** I can maneuver precisely without changing my facing direction.

**Status**: Approved

**Priority**: Must-have

**Source stakeholder**: [STK-player](../stakeholders.md)

**Related goal**: [GOAL-hovercraft-flight-model](../goals/GOAL-hovercraft-flight-model.md)

## Acceptance Criteria

- Given I press the strafe-left key, when the input is processed, then the vehicle moves orthogonally left relative to my look direction with no forward/backward or vertical component
- Given I press the strafe-right key, when the input is processed, then the vehicle moves orthogonally right relative to my look direction with no forward/backward or vertical component
- Given I press strafe while also pressing forward/vertical, when the input is processed, then both axes contribute to the movement vector without either axis suppressing the other

## Derived Requirements

- [REQ-F-strafe-movement](../requirements/REQ-F-strafe-movement.md)
