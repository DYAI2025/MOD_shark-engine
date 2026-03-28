# DEC-breaking-protocol-change: Trail

> Companion to `DEC-breaking-protocol-change.md`.

## Alternatives considered

### Option A: Clean replacement (chosen)
- Pros: No dead code, simpler networking layer, no ambiguity
- Cons: Breaking change — requires coordinated update

### Option B: Dual support (old + new payloads)
- Pros: Backward compatible during transition
- Cons: Complexity, dead code, mod is pre-release so no users to break

## Reasoning

The mod is pre-release (v0.0.1). There are no production users. Maintaining backward compatibility would add complexity for zero benefit.

## Human involvement

**Type**: ai-proposed/human-approved

**Notes**: Proposed during API design, approved as part of design review.

## Changelog

| Date | Change | Involvement |
|------|--------|-------------|
| 2026-03-28 | Initial decision | ai-proposed/human-approved |
