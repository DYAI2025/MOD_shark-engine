# DEC-java-records-for-data-types: Trail

> Companion to `DEC-java-records-for-data-types.md`.

## Alternatives considered

### Option A: Java records
- Pros: Immutable by default, compact, value equality built-in, no boilerplate
- Cons: Cannot extend classes (not needed here)

### Option B: Plain classes with final fields
- Pros: More flexible (inheritance, custom constructors)
- Cons: More boilerplate, must manually implement equals/hashCode

## Reasoning

Records are the idiomatic Java 21 choice for immutable value objects. The project already requires Java 21 via Fabric Loom, so no additional dependency is introduced.

## Human involvement

**Type**: ai-proposed/human-approved

**Notes**: Proposed during data model design, approved as part of design review.

## Changelog

| Date | Change | Involvement |
|------|--------|-------------|
| 2026-03-28 | Initial decision | ai-proposed/human-approved |
