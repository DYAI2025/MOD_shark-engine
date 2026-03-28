# DEC-friction-multiplier: Trail

> Companion to `DEC-friction-multiplier.md`.

## Alternatives considered

### Option A: Fixed friction 0.7
- Pros: Simple, deterministic, achieves 10-tick stop from reasonable max speeds
- Cons: Not tunable per weight category

### Option B: Variable friction by weight category
- Pros: Heavier vehicles brake slower (more realistic)
- Cons: More complexity, harder to test, not required by any approved requirement

### Option C: Linear deceleration (subtract fixed amount per tick)
- Pros: Predictable braking distance
- Cons: Abrupt stop, less smooth feel

## Reasoning

Option A is the simplest design that satisfies REQ-F-controlled-deceleration. Variable friction (Option B) is YAGNI — no requirement demands weight-dependent braking. Linear deceleration (Option C) produces a less smooth deceleration curve.

## Human involvement

**Type**: ai-proposed/human-approved

**Notes**: Proposed during design phase, approved as part of architecture review.

## Changelog

| Date | Change | Involvement |
|------|--------|-------------|
| 2026-03-28 | Initial decision | ai-proposed/human-approved |
