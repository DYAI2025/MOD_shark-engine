# CON-fabric-minecraft-1-21-1: Fabric Mod Loader for Minecraft 1.21.1

**Category**: Technical

**Status**: Active

**Source stakeholder**: [STK-mod-developer](../stakeholders.md)

## Description

The mod must target Minecraft 1.21.1 using the Fabric mod loader. No other mod loaders (Forge, NeoForge) or Minecraft versions are supported.

## Rationale

The project was initiated on Fabric 1.21.1. Changing the loader or target version would require a full rewrite of mixins, registry access, and networking layers.

## Impact

All APIs used must be available in Fabric's 1.21.1 API surface. Physics and input handling must conform to Fabric's entity and networking model. Test utilities must mock or stub Fabric abstractions.

## Related Artifacts

- [REQ-COMP-fabric-api-compatibility](../requirements/REQ-COMP-fabric-api-compatibility.md) — derived requirement enforcing this constraint
