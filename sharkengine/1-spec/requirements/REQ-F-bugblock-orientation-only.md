# REQ-F-bugblock-orientation-only: BUG-Block Determines Orientation Only

**Type**: Functional

**Status**: Approved

**Priority**: Must-have

**Source**: [US-fly-forward-backward](../user-stories/US-fly-forward-backward.md)

**Source stakeholder**: [STK-mod-developer](../stakeholders.md)

## Description

The BUG-block's facing direction must be used only for build/model orientation (e.g., visual alignment of the ship model). It must not determine the vehicle's flight direction at runtime. Flight direction must be derived exclusively from the player's current yaw at the time of input processing.

## Acceptance Criteria

- Given a ship assembled with BUG-block facing north, when the player looks east and presses forward, then the vehicle moves east — not north
- Given `ShipAssemblyService`, when a blueprint is produced, then the BUG-block facing is stored for model orientation but is not used to initialize a flight yaw that overrides player yaw during flight
- Given `ShipEntity.tick()`, when movement is computed, then the movement direction is derived from the current player entity yaw, not from a stored BUG-block yaw

## Related Constraints

- [CON-preserve-existing-systems](../constraints/CON-preserve-existing-systems.md) — BUG-block remains relevant for builder/assembly; only its role as flight direction source is removed
