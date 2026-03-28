# DEC-breaking-protocol-change: Replace HelmInputPayload with HovercraftInputPayload

**Status**: Active

**Category**: Architecture

**Scope**: system-wide

**Source**: [REQ-F-input-model](../1-spec/requirements/REQ-F-input-model.md)

**Last updated**: 2026-03-28

## Context

The existing HelmInputPayload carries `throttle`, `turn`, and `forward` fields. The new hovercraft input model requires `moveForward`, `moveStrafe`, `moveVertical`, and `playerYaw`. These are fundamentally different — there is no meaningful mapping from the old to the new format.

## Decision

Replace `HelmInputPayload` entirely with `HovercraftInputPayload`. No backward compatibility layer. Client and server must both run the same mod version.

## Enforcement

### Trigger conditions

- **Code phase**: when modifying networking payloads or ModNetworking registration
- **Deploy phase**: when releasing or distributing the mod

### Required patterns

- Remove all references to `HelmInputPayload` from `ModNetworking`
- Register `HovercraftInputPayload` with identifier `sharkengine:hovercraft_input`
- Server handler validates and clamps all incoming values

### Required checks

1. No references to `HelmInputPayload` remain in codebase after migration
2. `./gradlew build` succeeds without the old payload class

### Prohibited patterns

- Dual registration of old and new payloads
- Compatibility shims that translate old format to new
