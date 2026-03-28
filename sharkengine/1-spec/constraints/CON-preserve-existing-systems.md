# CON-preserve-existing-systems: Preserve Assembly, Builder, Fuel, and Mounting Systems

**Category**: Business

**Status**: Active

**Source stakeholder**: [STK-mod-developer](../stakeholders.md)

## Description

The following existing systems must remain functional and must not be broken by changes to the flight controller: ship assembly (BFS scan, block validation), builder mode (real-time feedback, block highlights), fuel system (capacity, consumption, critical threshold), mounting/dismounting (entity ride mechanics), and the core ShipEntity structure.

## Rationale

These systems are already functional and represent completed work. The analysis document ("die_aktuelle_steuerung_ist_kein_hovercraft_system") explicitly identifies them as what "can be kept". Rewriting them would introduce regression risk without delivering new player value.

## Impact

The new HovercraftController must be designed as a replaceable component within ShipEntity, not a full entity rewrite. The BUG-block retains its role for build/model orientation but must be decoupled from flight direction. All existing tests for assembly, fuel, and physics unrelated to input handling remain valid.
