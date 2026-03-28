# REQ-MNT-hovercraft-controller-class: Pure HovercraftController Class

**Type**: Maintainability

**Status**: Approved

**Priority**: Must-have

**Source**: [US-testable-controller](../user-stories/US-testable-controller.md)

**Source stakeholder**: [STK-mod-developer](../stakeholders.md)

## Description

A `HovercraftController` class must be created with a pure, typed interface: it takes a `HovercraftInput` and a `HovercraftState` and returns a `HovercraftOutput`. It must have no direct dependency on Minecraft or Fabric APIs, making it runnable in a standard JUnit 5 test without any Fabric mocking.

## Acceptance Criteria

- Given the `HovercraftController` class, when inspected for imports, then no `net.minecraft.*` or `net.fabricmc.*` imports are present
- Given a JUnit 5 test that instantiates `HovercraftController` directly, when the test is run with `./gradlew test`, then it passes without requiring a Fabric environment
- Given a known `HovercraftInput` and `HovercraftState`, when `HovercraftController.tick(input, state)` is called, then it returns a deterministic `HovercraftOutput` with the expected velocity vector

## Related Constraints

- [CON-server-authoritative-physics](../constraints/CON-server-authoritative-physics.md) — controller runs server-side; independence from Fabric API enables testing without a running server
