# US-flight-behavior-tests: Deterministic Unit Tests for All Hovercraft Input Combinations

**As a** mod developer, **I want** deterministic unit tests for all hovercraft input combinations (tests A–J), **so that** I can verify and protect correct flight behavior in CI without a running Minecraft instance.

**Status**: Approved

**Priority**: Must-have

**Source stakeholder**: [STK-mod-developer](../stakeholders.md)

**Related goal**: [GOAL-reliable-flight-tests](../goals/GOAL-reliable-flight-tests.md)

## Acceptance Criteria

- Given the `HovercraftController`, when tests A–J are run, then all pass against the real controller logic — not a mock or simplified simulation
- Given test A (neutral): all inputs = 0 over 40 ticks → position delta < 0.01, speed < 0.001, yaw unchanged
- Given test B (forward): `moveForward=1` over 20 ticks → movement vector aligns with `playerForwardXZ`, lateral < 0.01
- Given test C (backward): `moveForward=-1` → movement vector points opposite to `playerForwardXZ`
- Given test D (strafe): `moveStrafe=±1` → movement orthogonal to look direction, forward component < 0.01
- Given test E (vertical): `moveVertical=±1` → only Y changes, X/Z < 0.01
- Given test F (combination): `forward + strafe + vertical` simultaneously → result is correct vector sum, no axis dominates
- Given test G (input release): 20 ticks movement, then inputs = 0 → vehicle stops within 10 ticks
- Given test H (deadzone): stick values below threshold → resulting input = 0.0
- Given test I (parity): equal logical inputs from keyboard and gamepad → velocity vectors differ by < 0.001
- Given test J (look direction): same input, different player yaw → movement follows yaw, not BUG-block
- Given `MockShipData`-based integration tests, when reviewed, then they are replaced or annotated as logic-simulation tests that do not prove real flight behavior

## Derived Requirements

- [REQ-MNT-flight-behavior-test-suite](../requirements/REQ-MNT-flight-behavior-test-suite.md)
