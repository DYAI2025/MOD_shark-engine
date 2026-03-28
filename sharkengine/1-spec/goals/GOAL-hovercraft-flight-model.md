# GOAL-hovercraft-flight-model: Hovercraft Flight Model

**Description**: Replace the current yaw+thrust flight model with a true hovercraft translation model. The vehicle moves relative to the player's horizontal look direction on all six axes. The BUG-block is decoupled from flight direction and used for model/build orientation only.

**Status**: Approved

**Priority**: Must-have

**Source stakeholder**: [STK-player](../stakeholders.md)

## Success Criteria

- [ ] Forward input moves the vehicle along the player's horizontal look direction (XZ plane)
- [ ] Backward input moves the vehicle opposite to the player's horizontal look direction
- [ ] Strafe-left and strafe-right inputs move the vehicle orthogonally to the look direction without forward/backward component
- [ ] Vertical inputs (up/down) only affect the Y axis; XZ remains unchanged
- [ ] Zero input produces no movement — no self-drift, no unintended yaw rotation
- [ ] Releasing all inputs causes the vehicle to decelerate and stop within a defined tick count
- [ ] BUG-block orientation no longer determines flight direction at runtime

## Related Artifacts

- User stories: [US-fly-forward-backward](../user-stories/US-fly-forward-backward.md), [US-strafe-left-right](../user-stories/US-strafe-left-right.md), [US-vertical-movement](../user-stories/US-vertical-movement.md), [US-no-drift-at-rest](../user-stories/US-no-drift-at-rest.md)
- Requirements: [REQ-F-input-model](../requirements/REQ-F-input-model.md), [REQ-F-forward-by-player-yaw](../requirements/REQ-F-forward-by-player-yaw.md), [REQ-F-backward-movement](../requirements/REQ-F-backward-movement.md), [REQ-F-strafe-movement](../requirements/REQ-F-strafe-movement.md), [REQ-F-vertical-only](../requirements/REQ-F-vertical-only.md), [REQ-F-no-drift-neutral](../requirements/REQ-F-no-drift-neutral.md), [REQ-F-controlled-deceleration](../requirements/REQ-F-controlled-deceleration.md), [REQ-F-bugblock-orientation-only](../requirements/REQ-F-bugblock-orientation-only.md), [REQ-F-controller-deadzone](../requirements/REQ-F-controller-deadzone.md), [REQ-F-keyboard-controller-parity](../requirements/REQ-F-keyboard-controller-parity.md)
