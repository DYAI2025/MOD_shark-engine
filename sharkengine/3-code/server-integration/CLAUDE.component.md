# Server Integration

**Responsibility**: Bridges the pure HovercraftController to the Minecraft entity system. Receives C2S input payloads, modifies ShipEntity to delegate flight computation to the controller, decouples BUG-block from flight direction, and registers the new networking payload.

**Technology**: Java 21 + Fabric Server API

**Source location**: `src/main/java/dev/sharkengine/ship/ShipEntity.java` (modified), `src/main/java/dev/sharkengine/net/` (modified)

## Interfaces

- Receives C2S `HovercraftInputPayload` from **client-input** via Fabric networking
- Calls **hovercraft-controller** via class API: `HovercraftController.tick(input, state)`

## Requirements Addressed

| File | Type | Priority | Summary |
|------|------|----------|---------|
| [REQ-F-input-model](../../1-spec/requirements/REQ-F-input-model.md) | Functional | Must-have | Three-axis input model received and stored on ShipEntity |
| [REQ-F-bugblock-orientation-only](../../1-spec/requirements/REQ-F-bugblock-orientation-only.md) | Functional | Must-have | BUG-block determines model orientation only; flight direction from player yaw |
| [REQ-MNT-ship-entity-delegates](../../1-spec/requirements/REQ-MNT-ship-entity-delegates.md) | Maintainability | Must-have | ShipEntity.tick() delegates flight to HovercraftController |
| [REQ-REL-no-regression](../../1-spec/requirements/REQ-REL-no-regression.md) | Reliability | Must-have | All pre-existing tests pass; no new exceptions |
| [REQ-COMP-fabric-api-compatibility](../../1-spec/requirements/REQ-COMP-fabric-api-compatibility.md) | Compliance | Must-have | All APIs used must be in Fabric 1.21.1 surface |

## Relevant Decisions

| File | Title | Trigger |
|------|-------|---------|
| [DEC-breaking-protocol-change](../../decisions/DEC-breaking-protocol-change.md) | Replace HelmInputPayload with HovercraftInputPayload | When modifying ModNetworking registration and handlers |
