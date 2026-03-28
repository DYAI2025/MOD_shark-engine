# US-testable-controller: Test Flight Controller Without Minecraft Instance

**As a** mod developer, **I want** to test the flight controller logic without a running Minecraft instance, **so that** I can reliably detect regressions and verify behavior changes in CI.

**Status**: Approved

**Priority**: Must-have

**Source stakeholder**: [STK-mod-developer](../stakeholders.md)

**Related goal**: [GOAL-testable-flight-architecture](../goals/GOAL-testable-flight-architecture.md)

## Acceptance Criteria

- Given a `HovercraftController` class with typed input/state/output interfaces, when I write a JUnit 5 test against it, then the test runs without any Fabric or Minecraft mocking
- Given the controller is invoked with a known input and state, when the output is inspected, then the movement vector is deterministic and matches the expected value
- Given the existing `MockShipData`-based integration tests, when reviewing them, then they are either replaced by real controller tests or clearly annotated as logic-simulation tests that do not verify real flight behavior

## Derived Requirements

- [REQ-MNT-hovercraft-controller-class](../requirements/REQ-MNT-hovercraft-controller-class.md)
- [REQ-MNT-ship-entity-delegates](../requirements/REQ-MNT-ship-entity-delegates.md)
