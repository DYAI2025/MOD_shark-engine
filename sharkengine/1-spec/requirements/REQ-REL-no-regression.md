# REQ-REL-no-regression: No Regression in Existing Systems After Refactor

**Type**: Reliability

**Status**: Approved

**Priority**: Must-have

**Source**: [US-no-regression](../user-stories/US-no-regression.md)

**Source stakeholder**: [STK-server-operator](../stakeholders.md)

## Description

After introducing `HovercraftController` and refactoring `ShipEntity`, all pre-existing functionality must remain intact. Specifically: ship assembly (BFS scan, block validation), builder mode feedback, fuel consumption and display, mounting/dismounting, and the `ShipEntity` lifecycle must behave identically to the pre-refactor baseline.

## Acceptance Criteria

- Given the refactored codebase, when `./gradlew test` is run, then `ShipPhysicsTest`, `ShipAssemblyServiceTest`, and `FuelSystemTest` all pass without modification to test code
- Given a ship assembled and launched in-game, when the player mounts, receives fuel HUD, and dismounts, then no `NullPointerException`, `IllegalStateException`, or other runtime exceptions appear in server logs
- Given flight with `HovercraftController` active, when server tick time is measured over 100 ticks, then the mean tick time does not exceed the pre-refactor baseline by more than 1ms

## Related Constraints

- [CON-preserve-existing-systems](../constraints/CON-preserve-existing-systems.md) — this requirement directly enforces the preserve constraint
