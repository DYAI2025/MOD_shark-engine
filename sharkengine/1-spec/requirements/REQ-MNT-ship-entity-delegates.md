# REQ-MNT-ship-entity-delegates: ShipEntity Delegates Flight to HovercraftController

**Type**: Maintainability

**Status**: Approved

**Priority**: Must-have

**Source**: [US-testable-controller](../user-stories/US-testable-controller.md)

**Source stakeholder**: [STK-mod-developer](../stakeholders.md)

## Description

`ShipEntity.tick()` must delegate all flight computation to `HovercraftController` rather than implementing movement logic inline. The tick method prepares the input and state objects, calls the controller, and applies the output to the entity. No flight physics logic should remain directly inside `tick()`.

## Acceptance Criteria

- Given `ShipEntity.tick()`, when inspected, then it contains no inline velocity calculation for the hovercraft axes — all such logic lives in `HovercraftController`
- Given a change to flight physics, when it is implemented, then only `HovercraftController` needs to be modified — `ShipEntity` does not require changes for pure physics adjustments
- Given the existing assembly, fuel, and mounting logic in `ShipEntity`, when the refactor is applied, then those systems remain unaffected

## Related Constraints

- [CON-preserve-existing-systems](../constraints/CON-preserve-existing-systems.md) — refactor must not break assembly, fuel, mounting, or entity structure
