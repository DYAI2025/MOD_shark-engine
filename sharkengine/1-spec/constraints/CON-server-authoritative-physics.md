# CON-server-authoritative-physics: Server-Authoritative Physics Model

**Category**: Technical

**Status**: Active

**Source stakeholder**: [STK-mod-developer](../stakeholders.md)

## Description

All physics computation and movement validation must run on the server side. The client only captures player input and sends it as C2S payloads. The server computes the resulting movement and syncs state back to the client.

## Rationale

Minecraft's architecture requires server authority over world state to prevent cheating and ensure consistency across all clients on a multiplayer server.

## Impact

The flight controller must be a server-side class. Input payloads (moveForward, moveStrafe, moveVertical) are transmitted via the existing networking layer. Client-side prediction is not in scope. Deterministic unit tests for the flight controller can run without a Minecraft instance by testing the pure controller logic.
