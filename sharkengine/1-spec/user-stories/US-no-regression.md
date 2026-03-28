# US-no-regression: Existing Systems Unaffected by Refactor

**As a** server operator, **I want** the HovercraftController refactor to leave all existing systems (assembly, fuel, mounting, builder) fully functional, **so that** I can upgrade the mod without encountering crashes, behavior changes, or performance degradation.

**Status**: Approved

**Priority**: Must-have

**Source stakeholder**: [STK-server-operator](../stakeholders.md)

**Related goal**: [GOAL-no-regression-existing-systems](../goals/GOAL-no-regression-existing-systems.md)

## Acceptance Criteria

- Given the refactored mod, when `./gradlew test` is run, then all pre-existing tests for assembly, fuel, and physics pass without modification
- Given a server running the refactored mod, when a player places a Steering Wheel, builds a ship, mounts, flies, and dismounts, then no new exceptions are thrown and behavior matches the pre-refactor baseline
- Given server tick profiling before and after the refactor, when `HovercraftController` is active during flight, then tick time increase is not measurable above normal variance

## Derived Requirements

- [REQ-REL-no-regression](../requirements/REQ-REL-no-regression.md)
