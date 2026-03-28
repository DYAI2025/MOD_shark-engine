# DEC-diagonal-normalization: Normalize Diagonal Movement to Prevent Speed Boost

**Status**: Active

**Category**: Architecture

**Scope**: backend

**Source**: [REQ-F-strafe-movement](../1-spec/requirements/REQ-F-strafe-movement.md)

**Last updated**: 2026-03-28

## Context

When forward and strafe inputs are combined (e.g., both = 1.0), the resulting movement vector has magnitude sqrt(2) ≈ 1.414. Without normalization, diagonal movement would be ~41% faster than single-axis movement.

## Decision

When the horizontal movement vector magnitude exceeds 1.0, normalize it to magnitude 1.0. This ensures diagonal movement is not faster than single-axis movement.

## Enforcement

### Trigger conditions

- **Code phase**: when implementing movement vector computation in `HovercraftController`

### Required patterns

- After combining forward and strafe vectors, check `if magnitude > 1.0: normalize`
- Vertical axis is independent — not included in horizontal normalization

### Required checks

1. Test F (combination test): verify total speed is capped regardless of input combination
2. Verify no axis suppresses the other during normalization

### Prohibited patterns

- Clamping individual axes instead of normalizing the combined vector
- Including vertical axis in the horizontal normalization
