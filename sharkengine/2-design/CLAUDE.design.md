Phase-specific instructions for the **Design** phase. Extends [../CLAUDE.md](../CLAUDE.md).

## Purpose

This phase defines **how** we're building the system. Focus on architecture, data models, APIs, and key technical decisions.

## Files in This Phase

| File | Purpose |
|------|---------|
| [`architecture.md`](architecture.md) | System architecture overview and diagrams |
| [`data-model.md`](data-model.md) | Data structures, schemas, and relationships |
| [`api-design.md`](api-design.md) | API specifications and contracts |

---

## Decisions Relevant to This Phase

| File | Title | Trigger |
|------|-------|---------|
| [DEC-friction-multiplier](../decisions/DEC-friction-multiplier.md) | Friction Multiplier 0.7 for Deceleration | When modifying deceleration model or HovercraftController deceleration logic |
| [DEC-java-records-for-data-types](../decisions/DEC-java-records-for-data-types.md) | Java Records for Flight Data Types | When creating or modifying HovercraftInput, HovercraftState, HovercraftOutput |
| [DEC-breaking-protocol-change](../decisions/DEC-breaking-protocol-change.md) | Replace HelmInputPayload with HovercraftInputPayload | When modifying networking payloads or releasing the mod |
| [DEC-diagonal-normalization](../decisions/DEC-diagonal-normalization.md) | Normalize Diagonal Movement | When implementing movement vector computation |
| [DEC-gamepad-triggers-for-vertical](../decisions/DEC-gamepad-triggers-for-vertical.md) | Gamepad Triggers for Vertical Movement | When implementing gamepad input mapping |

---

## Linking to Other Phases

- Reference requirements from `1-spec/` to justify design choices
- Design documents guide implementation in `3-code/`
- Infrastructure design informs deployment in `4-deploy/`
