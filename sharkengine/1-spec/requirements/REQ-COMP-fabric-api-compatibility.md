# REQ-COMP-fabric-api-compatibility: All APIs Must Be Available in Fabric 1.21.1

**Type**: Compliance

**Status**: Approved

**Priority**: Must-have

**Source**: [CON-fabric-minecraft-1-21-1](../constraints/CON-fabric-minecraft-1-21-1.md)

**Source stakeholder**: [STK-mod-developer](../stakeholders.md)

## Description

All classes, methods, and APIs used in the mod — including any new classes introduced for the HovercraftController refactor — must be available in the Fabric API and Minecraft 1.21.1 vanilla surface as resolved by Fabric Loom. The build must complete without unresolved dependency errors.

## Acceptance Criteria

- Given the refactored codebase, when `./gradlew build` is run, then it completes without compilation errors or unresolved class references
- Given any new class introduced in the refactor, when its imports are inspected, then all referenced APIs are present in the Fabric 1.21.1 dependency set defined in `build.gradle`

## Related Constraints

- [CON-fabric-minecraft-1-21-1](../constraints/CON-fabric-minecraft-1-21-1.md) — this requirement directly enforces the platform constraint
