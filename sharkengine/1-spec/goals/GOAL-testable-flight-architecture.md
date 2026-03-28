# GOAL-testable-flight-architecture: Testable Flight Architecture

**Description**: Extract flight logic from ShipEntity into a pure, decoupled HovercraftController class that can be tested deterministically without a running Minecraft instance. The controller takes typed input and state objects and returns movement output — no side effects, no Minecraft API dependencies.

**Status**: Approved

**Priority**: Must-have

**Source stakeholder**: [STK-mod-developer](../stakeholders.md)

## Success Criteria

- [ ] A `HovercraftController` class exists with pure input/output interface (`HovercraftInput`, `HovercraftState`, `HovercraftOutput`)
- [ ] `ShipEntity.tick()` delegates flight computation to `HovercraftController` rather than implementing it inline
- [ ] `HovercraftController` has no direct dependency on Minecraft or Fabric APIs
- [ ] Unit tests for the controller can run without a Minecraft instance (no Fabric mocking required)

## Related Artifacts

- User stories: [US-testable-controller](../user-stories/US-testable-controller.md)
- Requirements: [REQ-MNT-hovercraft-controller-class](../requirements/REQ-MNT-hovercraft-controller-class.md), [REQ-MNT-ship-entity-delegates](../requirements/REQ-MNT-ship-entity-delegates.md)
