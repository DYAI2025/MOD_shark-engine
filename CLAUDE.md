# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Shark Engine is a Minecraft 1.21.1 Fabric mod that lets players build, assemble, and pilot flying vehicles. Players place a Steering Wheel, attach blocks (including Thrusters), and launch controllable airships with physics-based flight. Currently at MVP stage (v0.0.1) with the AIR vehicle class functional.

## Build & Development Commands

All commands run from the `sharkengine/` subdirectory:

```bash
cd sharkengine

./gradlew build          # Compile, remap, run tests
./gradlew test           # Run JUnit 5 tests only
./gradlew check          # Tests + static analysis (mirrors CI)
./gradlew runClient      # Launch Fabric dev client for in-game testing
./gradlew clean          # Clean build artifacts
```

Requires **Java 21** — Fabric Loom enforces the toolchain and fails on wrong JDK.

## Architecture

### Source Set Split

Fabric Loom splits code into two compilation units:
- **`src/main/java`** — Server-side + shared logic (physics, assembly, networking, entities). Runs on both client and dedicated server.
- **`src/client/java`** — Client-only code (rendering, input, HUD, camera). Never loaded on a dedicated server.

This is enforced by Loom's `splitEnvironmentSourceSets()`. Putting client imports in `main` causes server crashes.

### Package Map

| Package | Purpose |
|---------|---------|
| `dev.sharkengine` | Mod entrypoints (`SharkEngineModEntrypoint`, `SharkEngineMod`) |
| `dev.sharkengine.content` | Block/item/entity/sound/tag registries (`ModBlocks`, `ModEntities`, etc.) |
| `dev.sharkengine.content.block` | Block & item classes (`SteeringWheelBlock`, `SteeringWheelItem`) |
| `dev.sharkengine.ship` | Core vehicle logic — entity, physics, assembly, fuel, weight, acceleration |
| `dev.sharkengine.net` | Client-server networking payloads and handlers |
| `dev.sharkengine.tutorial` | Onboarding popup flow and stage management |
| `dev.sharkengine.client` | Client entrypoint, input handler, camera, blueprint sync |
| `dev.sharkengine.client.render` | Entity renderer (`ShipEntityRenderer`) and HUD overlay (`FuelHudOverlay`) |
| `dev.sharkengine.client.builder` | Builder mode UI with block highlights |
| `dev.sharkengine.client.tutorial` | Tutorial popup rendering |

### Core Systems

**Ship Assembly** (`ShipAssemblyService`): BFS scan from Steering Wheel finds connected `ship_eligible`-tagged blocks. Constraints: min 4 blocks adjacent to wheel, at least 1 Thruster, no terrain contact, max 512 blocks, max 32-block radius. Produces a `ShipBlueprint`.

**Physics** (`ShipEntity` + `ShipPhysics`): Weight categories (LIGHT 1-20, MEDIUM 21-40, HEAVY 41-60, OVERLOADED 61+) determine max speed. Five `AccelerationPhase` stages ramp speed from 5 to 30 blocks/sec over 6 seconds. Height penalty reduces performance above Y=100.

**Fuel** (`FuelSystem`): 100 energy max, 1 wood = 100 energy, consumption 1-3 units/sec by phase. Critical at <20%.

**Networking**: C2S payloads carry helm input (throttle/turn/forward), assembly requests, and tutorial actions. S2C payloads sync blueprints, builder previews, and tutorial popups. All registered in `ModNetworking`.

**Tutorial** (`TutorialService` + `TutorialPopupStage`): Sequential popup flow — WELCOME → MODE_SELECTION → BUILD_GUIDE → READY_TO_LAUNCH → FLIGHT_TIPS.

### Server Authority Model

All validation and physics run server-side. The client handles input capture (`HelmInputClient`), rendering (`ShipEntityRenderer`), and HUD display (`FuelHudOverlay`). Client sends input payloads; server computes movement and syncs state back.

## Testing

JUnit 5 tests in `src/test/java/dev/sharkengine/ship/`:
- `ShipPhysicsTest` — Speed calculation, height penalty, acceleration phases
- `ShipAssemblyServiceTest` — BFS assembly, validation, contact detection
- `FuelSystemTest` — Fuel conversions, flight time, display formatting

Tests use `@DisplayName` tied to gameplay behavior. Mock Fabric abstractions where needed.

## CI

GitHub Actions (`.github/workflows/ci.yml`): Runs `./gradlew test` with JDK 21 Temurin on push/PR to `main`, `develop`, `feature/**`.

## Key Resources

- `src/main/resources/fabric.mod.json` — Mod metadata, entrypoints, dependencies
- `src/main/resources/data/sharkengine/tags/blocks/ship_eligible.json` — Blocks allowed in ship structures
- `src/main/resources/assets/sharkengine/lang/en_us.json` — All translatable strings
- `MSP-1.md` — Current milestone plan (guided builder + flight loop)
- `docs/PRODUCTION_MVP_TASKS.md` — Full production backlog and gap analysis

## Coding Conventions

- 4-space indent, braces on same line
- PascalCase for classes/enums, lowerCamelCase for methods/fields
- Resource IDs use `lowercase_with_underscores`
- Registry helpers go in `dev.sharkengine.content` (`ModBlocks`, `ModEntities`, etc.)
- Commits: imperative scope format (`Fix: ...`, `Feat: ...`)
- Read `specs/001-vertikale-bewegung/` before modifying flight mechanics
