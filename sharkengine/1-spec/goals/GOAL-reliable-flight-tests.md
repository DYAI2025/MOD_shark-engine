# GOAL-reliable-flight-tests: Reliable Flight Behavior Tests

**Description**: Replace or clearly mark the existing mock-based integration tests that do not verify real flight behavior. Implement deterministic unit tests (tests A–J as defined in the analysis document) that prove the HovercraftController behaves correctly for all input combinations.

**Status**: Approved

**Priority**: Must-have

**Source stakeholder**: [STK-mod-developer](../stakeholders.md)

## Success Criteria

- [ ] Tests A–J from the analysis document are implemented and passing against the real `HovercraftController` logic (not a mock)
- [ ] Neutral-input test: zero input over 40 ticks produces position delta below epsilon and no yaw change
- [ ] Directional tests: forward, backward, strafe-left, strafe-right, and vertical each produce movement only on the expected axis
- [ ] Combination test: simultaneous multi-axis input produces the correct vector sum without any axis dominating
- [ ] Input-release test: releasing all inputs causes the vehicle to stop within a defined tick count
- [ ] Controller deadzone test: stick values below threshold produce zero input
- [ ] Keyboard/controller parity test: equivalent logical inputs produce identical movement responses
- [ ] Look-direction test: same input with different player yaw produces movement in the correct yaw-relative direction
- [ ] Existing tests that use `MockShipData` are either replaced or annotated to clarify they do not verify real flight behavior

## Related Artifacts

- User stories: [US-flight-behavior-tests](../user-stories/US-flight-behavior-tests.md)
- Requirements: [REQ-MNT-flight-behavior-test-suite](../requirements/REQ-MNT-flight-behavior-test-suite.md)
