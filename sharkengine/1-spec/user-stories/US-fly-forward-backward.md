# US-fly-forward-backward: Fly Forward and Backward Relative to Look Direction

**As a** Minecraft player, **I want** to fly forward and backward relative to my look direction, **so that** the vehicle responds intuitively to where I am facing.

**Status**: Approved

**Priority**: Must-have

**Source stakeholder**: [STK-player](../stakeholders.md)

**Related goal**: [GOAL-hovercraft-flight-model](../goals/GOAL-hovercraft-flight-model.md)

## Acceptance Criteria

- Given I am piloting the vehicle and press the forward key, when the input is processed, then the vehicle moves along my horizontal look direction (XZ plane) with no lateral or vertical component
- Given I am piloting the vehicle and press the backward key, when the input is processed, then the vehicle moves in the exact opposite direction of my horizontal look direction
- Given my player yaw changes before I press forward, when the input is processed, then the vehicle moves in the new look direction — not the old BUG-block orientation

## Derived Requirements

- [REQ-F-input-model](../requirements/REQ-F-input-model.md)
- [REQ-F-forward-by-player-yaw](../requirements/REQ-F-forward-by-player-yaw.md)
- [REQ-F-backward-movement](../requirements/REQ-F-backward-movement.md)
- [REQ-F-bugblock-orientation-only](../requirements/REQ-F-bugblock-orientation-only.md)
