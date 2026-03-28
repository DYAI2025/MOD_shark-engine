# ASM-player-yaw-server-accessible: Player Yaw Is Accessible Server-Side in ShipEntity.tick()

**Category**: Technology

**Status**: Verified

**Risk if wrong**: High — the entire hovercraft movement model depends on deriving the movement vector from the player's current yaw. If player yaw is not reliably accessible server-side during `tick()`, the model cannot be implemented as specified.

## Statement

The player's current yaw (horizontal look direction) is accessible server-side within `ShipEntity.tick()` via `getFirstPassenger()` cast to a `PlayerEntity`, and the value is sufficiently up-to-date (synced within the same tick or the previous tick) to use as the movement direction basis.

## Rationale

Minecraft syncs player rotation to the server as part of the standard player movement packet. `ShipEntity` already reads passenger data in other contexts. The existing yaw+thrust model uses `entity.getYaw()` on the ship entity itself — the new model needs the pilot's yaw instead.

## Verification Plan

Verify during the Design phase by reading `ShipEntity.java` and checking passenger access patterns. Confirm that `((PlayerEntity) getFirstPassenger()).getYaw()` returns a current value during server-side `tick()` execution. Mark Verified after a successful in-code check or a passing test that reads pilot yaw.

## Related Artifacts

- [GOAL-hovercraft-flight-model](../goals/GOAL-hovercraft-flight-model.md), [REQ-F-forward-by-player-yaw](../requirements/REQ-F-forward-by-player-yaw.md)
