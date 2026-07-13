# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Shark Engine is a Minecraft 1.21.1 Fabric mod that lets players build, assemble, and pilot flying vehicles. Players place a Steering Wheel, attach blocks (including Thrusters), and launch controllable airships with physics-based flight. Currently at MVP stage (v0.0.1) with the AIR vehicle class functional.

The repo root also holds deployment and publishing tooling around the mod itself — see [Root-Level Tooling](#root-level-tooling).

## Build & Development Commands

All commands run from the `sharkengine/` subdirectory:

```bash
cd sharkengine

./gradlew build          # Compile main+client, remap, run tests — mirrors CI, use this to catch client-only compile breaks
./gradlew test           # Run JUnit 5 tests only — does NOT compile the client source set (see warning below)
./gradlew check          # Tests + static analysis — also does NOT compile the client source set
./gradlew runClient      # Launch Fabric dev client for in-game testing
./gradlew clean          # Clean build artifacts — ALSO deletes src/main/generated (see datagen gotcha below)
./gradlew runDatagen     # Regenerate src/main/generated (recipes/loot/models/blockstates/tags/lang for datagen-migrated blocks)

# Run a single test class or method
./gradlew test --tests "dev.sharkengine.ship.ShipPhysicsTest"
./gradlew test --tests "dev.sharkengine.ship.ShipPhysicsTest.methodName"
```

Requires **Java 21** — Fabric Loom enforces the toolchain and fails on wrong JDK.

**Client compile gotcha:** `splitEnvironmentSourceSets()` (below) means `test`/`check` never invoke `compileClientJava` — a client-only break can be fully green on `test`/`check` while the mod doesn't build. This happened for real (2026-03-17 to 2026-07-12, undetected): a package-private record used cross-package from client code. Always run `./gradlew build` before claiming client-side work (rendering, HUD, screens) is done; CI runs `build`, not `test`, specifically because of this.

**Datagen/`clean` gotcha:** `fabricApi.configureDataGeneration()` (added 2026-07-12, AIR-030) wires `src/main/generated` deletion into the `clean` task, but *regenerating* it is not part of the normal build graph — running `./gradlew clean build` alone leaves `src/main/generated` missing and `test`/`runGametest` red (missing recipe/loot/blockstate/model/tag/lang files for datagen-migrated blocks: currently `bug`, `steering_wheel`, `thruster`). After `clean`, always run `./gradlew runDatagen` before `build`/`test`/`runGametest` — or just `./gradlew clean runDatagen build`. This does **not** affect CI (`.github/workflows/ci.yml` never runs `clean`; the generated output is committed to git, so it's present immediately after checkout) or normal incremental `./gradlew build` without a preceding `clean`.

**GameTest registration gotcha:** `@GameTest`-annotated classes are **not** classpath-scanned — they only run if listed in `src/main/resources/fabric.mod.json`'s `"fabric-gametest"` array. Adding a new GameTest class/file without adding it there compiles and runs cleanly, just silently contributes zero tests (the total test count in `runGametest`'s output stays unchanged — that's the tell). Happened for real 2026-07-13 with `ShipEntityMountGameTest`. Always add new GameTest classes to that array and confirm the reported test count went up.

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

**Entity `interact()` on large-hitbox vehicles must PASS on a non-empty hand.** `ShipEntity`'s hitbox spans the whole assembled structure (up to the 32-block radius above), so any entity-level `interact()` override on it — or on any future large-hitbox vehicle entity — intercepts right-clicks *before* vanilla's normal block-placement path ever runs. Returning `CONSUME` unconditionally (e.g. for a generic "mount the pilot" fallback) silently defeats block placement anywhere on/near the vehicle, with zero exceptions logged — exactly what happened in `ShipEntity.interact()` until the 2026-07-13 fix (`ShipEntity.java`, see the fix's inline comment): every right-click holding a `ship_eligible` block near an already-launched ship got mounted-and-consumed instead of placing. Rule: only consume/handle when `player.getItemInHand(hand).isEmpty()`; otherwise return `InteractionResult.PASS` so vanilla gets a chance at normal item-use/placement (same pattern as right-clicking a vanilla boat while holding a block).

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
- `BuilderValidationTest` — Build-mode/assembly validation conditions
- `ResourceValidationTest` — Validates resource files (lang, tags) against expected content

Tests use `@DisplayName` tied to gameplay behavior. Mock Fabric abstractions where needed.

## Debugging "nothing happens" reports

When a player reports an interaction that silently does nothing (no crash, no log error) — e.g. "this item won't place" — check the server log first, but don't stop at "no exceptions, so the block/item registration must be fine." Isolate registration from interaction with `/setblock <pos> <block>` in a live client: if that succeeds, the block/blockstate/model is registered correctly and the bug is somewhere in the *interaction* path (an entity `interact()` override, a custom `useOn`, client-side input handling), not the block definition. This found the `ShipEntity.interact()` mount-hijack bug (2026-07-13) in a couple of commands, versus a much longer detour through blind GUI automation first (see also: `xdotool` synthetic mouse motion does not move the camera in this game — it uses GLFW raw mouse input, which ignores synthetic X11 pointer warps; use `/tp <pos> <yaw> <pitch>` or chat-driven `/data get`/`/setblock` commands for live diagnosis instead of trying to simulate mouse-look).

## CI

GitHub Actions:
- `.github/workflows/ci.yml` — two parallel jobs: `build` runs `./gradlew build` (compiles main+client, runs unit tests — see client compile gotcha above), `gametest` runs `./gradlew runGametest` (Fabric GameTest smoke suite). JDK 21 Temurin, on push/PR to `main`, `develop`, `feature/**`.
- `.github/workflows/deploy.yml` — On push to `main`: re-runs tests, then deploys to Railway (`railway up --service sharkengine-mc-mod --environment production`) using the root `Dockerfile`. Requires `RAILWAY_TOKEN` secret.

## Key Resources

- `src/main/resources/fabric.mod.json` — Mod metadata, entrypoints, dependencies
- `src/main/resources/data/sharkengine/tags/block/ship_eligible.json` — Blocks allowed in ship structures (note: singular `block`, not `blocks`)
- `src/main/resources/assets/sharkengine/lang/en_us.json` — All translatable strings
- `MSP-1.md` (repo root) — Current milestone plan (guided builder + flight loop)
- `docs/PRODUCTION_MVP_TASKS.md` (repo root) — Full production backlog and gap analysis; flags that flight is still 2D-ish (no true 6DoF), block-count-only weight model (no per-block mass), and no persistent vehicle-class buffs
- `sharkengine/specs/001-vertikale-bewegung/` — Feature spec for the AIR flight-physics system (acceleration phases, weight/speed categories, fuel, height penalty). Its own `state.yaml`/`README.md` status tracker is stale (marks itself pending/in-progress) — don't trust it over the actual code in `src/main/java/dev/sharkengine/ship/`, which has these systems implemented.

## Coding Conventions

- 4-space indent, braces on same line
- PascalCase for classes/enums, lowerCamelCase for methods/fields
- Resource IDs use `lowercase_with_underscores`
- Registry helpers go in `dev.sharkengine.content` (`ModBlocks`, `ModEntities`, etc.)
- Commits: imperative scope format (`Fix: ...`, `Feat: ...`)
- Read `sharkengine/specs/001-vertikale-bewegung/` before modifying flight mechanics

## Root-Level Tooling

The repo root contains mod-deployment and publishing tooling outside the Gradle build, exposed as project slash commands (`.claude/commands/`):

- **`/mc-bugtest`** — Full manual bug-test cycle: `gradlew build`, verify JAR contents, deploy to Prism Launcher, re-enable disabled companion mods, generate an in-game test checklist from the recent git diff.
- **`/mod-deploy`** — Builds the mod JAR and copies it into Prism Launcher's Flatpak mods folder, re-enabling `.disabled` companion mods (fabric-api, controlify, yacl).
- **`/test-server`** — Rebuilds and restarts the Dockerized Minecraft test server (container `sharkengine-server`, host port 25566), using the root `Dockerfile` and `server/server.properties`.

**Modrinth publishing**: `tools/modrinth-mcp-server/` is a Node/TypeScript MCP server (registered in root `.mcp.json` as `modrinth`) exposing search/version/publish tools against the Modrinth API — used to search Modrinth and publish mod releases. Requires `MODRINTH_TOKEN` env var for write operations (create version, modify project).

**Note on `sharkengine/CLAUDE.md`**: that file and `sharkengine/.claude/` are a vendored generic "Spec-Flow" agent-workflow scaffold (unrelated `/feature`, `/epic`, `/ship` slash-command SDLC tooling), not documentation of this mod. Don't confuse its instructions with this file's.
