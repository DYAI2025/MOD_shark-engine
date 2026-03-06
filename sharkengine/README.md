# Shark Engine (Fabric Mod for Minecraft 1.21.1)

Shark Engine is a **Fabric-based prototype mod** that turns block structures into controllable flying ships.
It focuses on a clean MVP gameplay loop:

1. Place a **Steering Wheel** block.
2. Build a valid structure around it.
3. Enter builder mode and assemble it.
4. Pilot the generated ship entity with thrust, turn, climb, and descent controls.

This README is written for **guests and first-time contributors** so you can quickly understand what the mod does, how it is structured, and how to run or validate it.

---

## Project Snapshot

- **Mod ID:** `sharkengine`
- **Minecraft:** `1.21.1`
- **Loader:** Fabric (`>= 0.16.5`)
- **Fabric API:** `0.114.0+1.21.1`
- **Java target/toolchain:** Java 21
- **Current version:** `0.0.1`

Core metadata is defined in `fabric.mod.json` and Gradle properties.

---

## What the Mod Currently Implements

### 1) Build & Assemble Flow
- A custom **Steering Wheel** block starts the tutorial and mode selection flow when placed/used.
- The server scans connected structure blocks using BFS (with hard limits for block count and radius).
- The scan validates:
  - eligible block tags,
  - no invalid attachments,
  - world contacts,
  - thruster presence,
  - minimum structural neighbors near core.
- If valid, the structure is converted to a `ShipBlueprint`, source blocks are removed from world, and a `ShipEntity` is spawned.

### 2) Ship Flight & Physics
- Ships have acceleration phases (`PHASE_1` to `PHASE_5`) and speed ramps.
- Max speed is weight-category dependent (block count).
- High altitude applies a speed penalty.
- Vertical movement is supported (`jump`/`shift`).
- Collision checks use the blueprint footprint (not only a single point).

### 3) Fuel & Engine-Out Behavior
- Fuel is represented as energy units with a max capacity.
- Consumption depends on current acceleration phase.
- Consumption is charged per-second (20 ticks), not per-tick.
- Engine-out behavior includes controlled falling / buoyancy over fluid.

### 4) Client UX
- Ship renderer and particles.
- Builder preview screen + world-space line overlays for valid/invalid blocks.
- Flight camera helper.
- HUD overlay for fuel, speed, and status warnings.
- Tutorial popup flow for onboarding.

### 5) Networking
- Clear split of C2S and S2C payloads:
  - helm input,
  - assemble request,
  - tutorial progression,
  - blueprint + preview + tutorial popup sync.

---

## Repository / Module Structure

All active mod code lives in the `sharkengine/` module.

```text
sharkengine/
├─ build.gradle, gradle.properties
├─ src/
│  ├─ main/
│  │  ├─ java/dev/sharkengine/
│  │  │  ├─ SharkEngineMod*.java         # Entry points / init
│  │  │  ├─ content/                     # Blocks, entities, tags, sounds
│  │  │  ├─ ship/                        # Physics, entity, assembly, fuel
│  │  │  ├─ net/                         # Payload definitions + handlers
│  │  │  └─ tutorial/                    # Guided onboarding logic
│  │  └─ resources/
│  │     ├─ fabric.mod.json
│  │     ├─ assets/sharkengine/          # lang, models, sounds, blockstates
│  │     └─ data/sharkengine/            # recipes, loot tables, block tags
│  ├─ client/java/dev/sharkengine/client/
│  │  ├─ render/                         # HUD + entity renderer
│  │  ├─ builder/                        # Builder preview + screen
│  │  └─ tutorial/                       # Tutorial popup UI
│  └─ test/java/dev/sharkengine/ship/
│     ├─ ShipPhysicsTest.java
│     ├─ FuelSystemTest.java
│     ├─ ShipAssemblyServiceTest.java
│     ├─ BuilderValidationTest.java
│     ├─ WeightCategoryTest.java
│     └─ AccelerationPhaseTest.java
```

---

## Controls (Current)

When mounted on a ship:

- **W**: forward acceleration
- **A / D**: yaw left/right
- **Space**: climb
- **Shift**: descend

Additional interactions:

- **Right click ship**: mount
- **Shift + right click ship**: anchor toggle / disassemble when anchored

---

## Build & Run (Recommended)

From `sharkengine/`:

```bash
./gradlew build
./gradlew test
./gradlew runClient
```

Important: Use **Java 21** for this project (configured in Gradle toolchain).

---

## Testing & Validation Status

### What is covered by tests
The test suite emphasizes deterministic gameplay logic:

- acceleration phase boundaries and monotonicity,
- weight categories and speed caps,
- height penalty and effective speed math,
- fuel conversion/time formatting/critical thresholds,
- assembly validation logic and edge-case regressions,
- lightweight collision helper behavior.

### Notes from current verification run
- I fixed an unresolved merge marker in `ShipEntity.java` that would break Java compilation.
- In this environment, full Gradle test execution is currently blocked by dependency resolution for `fabric-loom:1.7-SNAPSHOT` (plugin artifact not resolved from configured repositories).

If you run this locally, ensure:
1. Java 21 is active.
2. Fabric Loom snapshot dependency is resolvable in your network/repo setup.

---

## Guest-Friendly Functional Walkthrough

1. Start a dev client (`runClient`).
2. Place a steering wheel.
3. Follow popup instructions and choose AIR mode.
4. Build a compact structure with valid blocks and at least one thruster.
5. Open builder preview and inspect highlight feedback.
6. Assemble the ship.
7. Fly with W/A/D + Space/Shift and monitor fuel HUD.
8. Land, anchor, and disassemble if needed.

This gives a realistic tour through all major systems (tutorial, assembly, network sync, physics, HUD).

---

## Known Limitations / MVP Boundaries

- The project is intentionally MVP-scoped to flying mode (`VehicleClass.AIR`).
- Fuel refill gameplay loop appears partially implemented in core logic and may need additional interaction UX wiring.
- Plugin/tooling setup may require a stable Loom version or matching repo configuration depending on your environment.

---

## For Contributors

- Keep gameplay logic in `src/main/java/dev/sharkengine/...`.
- Keep client-only code under `src/client/java/dev/sharkengine/client/...`.
- Add/update tests under mirrored package names in `src/test/java`.
- Prefer small focused commits with imperative prefixes.

Suggested first tasks:
- add integration tests for builder-to-entity flow,
- complete in-game refuel interaction UX,
- harden collision/physics edge behavior at chunk boundaries,
- expand localization consistency across all player-facing messages.

---

## License

MIT (as declared in `fabric.mod.json`).
