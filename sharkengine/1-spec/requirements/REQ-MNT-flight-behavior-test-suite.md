# REQ-MNT-flight-behavior-test-suite: Flight Behavior Test Suite (Tests A–J)

**Type**: Maintainability

**Status**: Approved

**Priority**: Must-have

**Source**: [US-flight-behavior-tests](../user-stories/US-flight-behavior-tests.md)

**Source stakeholder**: [STK-mod-developer](../stakeholders.md)

## Description

A JUnit 5 test suite must implement tests A–J from the analysis document against the real `HovercraftController`. Each test must be deterministic, require no Fabric or Minecraft environment, and cover the following cases:

- **A** — Neutral/no-drift: zero input over 40 ticks produces no movement
- **B** — Forward: `moveForward=1` produces movement along `playerForwardXZ`
- **C** — Backward: `moveForward=-1` produces movement opposite to `playerForwardXZ`
- **D** — Strafe left/right: `moveStrafe=±1` produces orthogonal movement, no forward component
- **E** — Vertical: `moveVertical=±1` changes only Y; X/Z remain ≈ 0
- **F** — Combination: simultaneous multi-axis input produces correct vector sum without axis dominance
- **G** — Input release: vehicle stops within 10 ticks after all inputs reach 0
- **H** — Controller deadzone: sub-threshold stick values produce input = 0.0
- **I** — Keyboard/controller parity: equal logical inputs from both sources produce identical velocity vectors (delta < 0.001)
- **J** — Look direction: same input with different player yaw moves the vehicle along the new yaw direction, not the BUG-block orientation

## Acceptance Criteria

- Given `./gradlew test`, when the suite runs, then all 10 test cases (A–J) pass
- Given any test in the suite, when its class file is inspected, then no `net.minecraft.*` or `net.fabricmc.*` imports are present
- Given tests that previously used `MockShipData` to simulate flight, when the suite is reviewed, then they are either replaced by controller tests or annotated with `// NOTE: logic-simulation test — does not verify real HovercraftController behavior`

## Related Constraints

- [CON-server-authoritative-physics](../constraints/CON-server-authoritative-physics.md) — controller independence from Fabric enables these tests to run in standard JUnit without a server
