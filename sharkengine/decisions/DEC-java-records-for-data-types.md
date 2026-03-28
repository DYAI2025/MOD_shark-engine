# DEC-java-records-for-data-types: Java Records for Flight Data Types

**Status**: Active

**Category**: Convention

**Scope**: backend

**Source**: [REQ-MNT-hovercraft-controller-class](../1-spec/requirements/REQ-MNT-hovercraft-controller-class.md)

**Last updated**: 2026-03-28

## Context

HovercraftInput, HovercraftState, and HovercraftOutput are immutable value objects with no behavior beyond field access. Java 21 (required by Fabric Loom) supports records natively.

## Decision

Use Java **records** for `HovercraftInput`, `HovercraftState`, and `HovercraftOutput`. Records provide immutability, value equality, and compact syntax.

## Enforcement

### Trigger conditions

- **Code phase**: when creating or modifying HovercraftInput, HovercraftState, or HovercraftOutput

### Required patterns

- `public record HovercraftInput(float moveForward, float moveStrafe, float moveVertical, float playerYawDeg) {}`
- Helper methods (e.g., `isZero()`, `speed()`) added as instance methods on the record

### Required checks

1. Verify no mutable fields in any of the three types
2. Verify Java 21 is the toolchain target (already enforced by Fabric Loom)

### Prohibited patterns

- Mutable classes with getters/setters for these data types
- Builder pattern for simple value objects with 3-5 fields
