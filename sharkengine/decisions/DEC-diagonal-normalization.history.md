# DEC-diagonal-normalization: Trail

> Companion to `DEC-diagonal-normalization.md`.

## Alternatives considered

### Option A: Vector normalization when magnitude > 1.0 (chosen)
- Pros: Fair speed across all directions, standard game dev practice
- Cons: Diagonal input feels slightly less responsive than single-axis

### Option B: No normalization
- Pros: Simpler
- Cons: Diagonal movement is 41% faster — unfair and breaks test F

### Option C: Clamp each axis independently
- Pros: Simple per-axis logic
- Cons: Diagonal still faster, weird directional behavior at limits

## Reasoning

Vector normalization is the standard approach in game development for this exact problem. It satisfies the requirement that "no axis dominates" in combination tests.

## Human involvement

**Type**: ai-proposed/human-approved

**Notes**: Proposed during architecture design, approved as part of design review.

## Changelog

| Date | Change | Involvement |
|------|--------|-------------|
| 2026-03-28 | Initial decision | ai-proposed/human-approved |
