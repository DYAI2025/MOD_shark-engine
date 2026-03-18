# Shark Engine – Project Context

## Project Overview

**Shark Engine** is a Fabric-based Minecraft mod (version 1.21.1) that transforms block structures into controllable flying ships. The mod focuses on a clean MVP gameplay loop:

1. Place a **Steering Wheel** block
2. Build a valid structure with thrusters
3. Assemble via guided tutorial
4. Pilot the ship with physics-based flight controls

### Key Technologies

| Component | Version |
|-----------|---------|
| Minecraft | 1.21.1 |
| Fabric Loader | ≥0.16.5 (using 0.17.0) |
| Fabric API | 0.114.0+1.21.1 |
| Java | 21 (enforced via toolchain) |
| Build Tool | Gradle 8.x with Fabric Loom 1.7-SNAPSHOT |

### Current Status

- **Version:** 0.0.1 (MVP)
- **Vehicle Class:** AIR (flying ships only)
- **License:** MIT

---

## Building and Running

All commands run from the `sharkengine/` subdirectory:

```bash
cd sharkengine

./gradlew build          # Compile, remap, run tests
./gradlew test           # Run JUnit 5 tests only
./gradlew check          # Tests + static analysis (CI mirror)
./gradlew runClient      # Launch Fabric dev client
./gradlew clean          # Clean build artifacts
```

### Docker Deployment

A multi-stage Dockerfile is provided for server deployment:

```bash
docker build -t shark-engine .
docker run -p 25565:25565 shark-engine
```

### Requirements

- **Java 21** required – Fabric Loom enforces the toolchain
- Stable internet for Fabric Loom snapshot resolution

---

## Architecture

### Module Structure

```
sharkengine/
├── build.gradle, gradle.properties, settings.gradle
├── src/
│   ├── main/                    # Server-side + shared logic
│   │   ├── java/dev/sharkengine/
│   │   │   ├── SharkEngineMod*.java    # Entrypoints
│   │   │   ├── content/                # Blocks, entities, tags, sounds
│   │   │   ├── ship/                   # Physics, entity, assembly, fuel
│   │   │   ├── net/                    # Networking payloads + handlers
│   │   │   └── tutorial/               # Guided onboarding logic
│   │   └── resources/
│   │       ├── fabric.mod.json         # Mod metadata
│   │       ├── assets/sharkengine/     # Models, textures, sounds, lang
│   │       └── data/sharkengine/       # Recipes, loot tables, tags
│   │
│   ├── client/                  # Client-only code (never on dedicated server)
│   │   └── java/dev/sharkengine/client/
│   │       ├── render/           # HUD overlay, entity renderer
│   │       ├── builder/          # Builder preview UI
│   │       ├── tutorial/         # Tutorial popup rendering
│   │       └── HelmInputClient.java, FlightCameraHandler.java
│   │
│   └── test/                    # JUnit 5 tests
│       └── java/dev/sharkengine/ship/
│           ├── ShipPhysicsTest.java
│           ├── FuelSystemTest.java
│           ├── ShipAssemblyServiceTest.java
│           └── ...
```

### Core Systems

| System | Package | Description |
|--------|---------|-------------|
| **Assembly** | `ship.ShipAssemblyService` | BFS scan from Steering Wheel, validates structure (min 4 blocks, 1+ thruster, no terrain contact, max 512 blocks) |
| **Physics** | `ship.ShipEntity`, `ShipPhysics` | 5 acceleration phases, weight categories (LIGHT/MEDIUM/HEAVY/OVERLOADED), height penalty above Y=100 |
| **Fuel** | `ship.FuelSystem` | 100 energy max, 1 wood = 100 energy, consumption 1-3 units/sec by phase, critical threshold <20% |
| **Networking** | `net.*` | C2S: helm input, assembly requests, tutorial actions. S2C: blueprint sync, preview data, tutorial popups |
| **Tutorial** | `tutorial.*` | Sequential popup flow: WELCOME → MODE_SELECTION → BUILD_GUIDE → READY_TO_LAUNCH → FLIGHT_TIPS |

### Server Authority Model

All validation and physics run **server-side**. The client handles:
- Input capture (`HelmInputClient`)
- Rendering (`ShipEntityRenderer`)
- HUD display (`FuelHudOverlay`)
- Camera helpers (`FlightCameraHandler`)

Client sends input payloads → server computes movement → syncs state back.

---

## Gameplay Mechanics

### Controls (When Mounted)

| Key | Action |
|-----|--------|
| **W** | Forward acceleration |
| **A / D** | Yaw left/right |
| **Space** | Climb |
| **Shift** | Descend |
| **Right-click ship** | Mount |
| **Shift + right-click** | Anchor toggle / disassemble |

### Assembly Requirements

- Minimum **4 blocks** adjacent to Steering Wheel (horizontal neighbors)
- At least **1 Thruster** block
- **No terrain contact** (structure must not touch world blocks)
- Maximum **512 blocks** total
- Maximum **32-block radius** from Steering Wheel

### Physics Details

- **Acceleration Phases:** PHASE_1 → PHASE_5 (ramps from 5 to 30 blocks/sec over ~6 seconds)
- **Weight Categories:**
  - LIGHT: 1-20 blocks
  - MEDIUM: 21-40 blocks
  - HEAVY: 41-60 blocks
  - OVERLOADED: 61+ blocks
- **Height Penalty:** Speed reduction above Y=100

---

## Development Conventions

### Code Style

- 4-space indentation, braces on same line
- PascalCase for classes/enums (`ShipBlueprint`, `VehicleClass`)
- lowerCamelCase for methods/fields
- Resource IDs: `lowercase_with_underscores`
- Registry helpers in `dev.sharkengine.content` (`ModBlocks`, `ModEntities`)

### Commit Messages

Use imperative scope format:
```
Fix: resolve null pointer in fuel calculation
Feat: add thruster particle effects
Spec-Flow Feature 001: implement vertical movement
```

### Testing Practices

- Tests mirror package structure in `src/test/java`
- Use `@DisplayName` tied to gameplay behavior
- Mock Fabric abstractions for unit isolation
- Run `./gradlew test` before all PRs

### Documentation

- Read `specs/001-vertikale-bewegung/` before modifying flight mechanics
- Update specs when mechanics change
- High-level discussions in `docs/` or root-level Markdown

---

## CI/CD

GitHub Actions workflow (`.github/workflows/ci.yml`):

- Triggers: push/PR to `main`, `develop`, `feature/**`
- Runs: `./gradlew test` with JDK 21 Temurin
- Timeout: 30 minutes
- Caches: Gradle wrapper and dependencies

---

## Key Files Reference

| File | Purpose |
|------|---------|
| `sharkengine/build.gradle` | Build configuration, Fabric Loom setup |
| `sharkengine/gradle.properties` | Version pins (MC, Fabric, mod version) |
| `sharkengine/src/main/resources/fabric.mod.json` | Mod metadata, entrypoints |
| `sharkengine/src/main/resources/data/sharkengine/tags/blocks/ship_eligible.json` | Allowed structure blocks |
| `sharkengine/src/main/resources/assets/sharkengine/lang/en_us.json` | All translatable strings |
| `MSP-1.md` | Current milestone plan (guided builder + flight loop) |
| `CLAUDE.md` | Development guidelines |
| `AGENTS.md` | Repository contribution guidelines |

---

## Known Limitations (MVP Scope)

- Only **AIR** vehicle class implemented (no land/sea vehicles)
- Fuel refill UX partially implemented, needs interaction wiring
- Fabric Loom 1.7-SNAPSHOT may require specific repo configuration

---

## First-Time Contributor Tasks

Suggested starting points:

1. Add integration tests for builder-to-entity flow
2. Complete in-game refuel interaction UX
3. Harden collision/physics at chunk boundaries
4. Expand localization consistency
5. Review `specs/001-vertikale-bewegung/` for pending features

---

## Troubleshooting

### Build Fails with Loom Error

Ensure Java 21 is active:
```bash
java -version  # Should show 21.x.x
```

### Dependency Resolution Issues

Fabric Loom snapshots may require additional repositories. Check `build.gradle` for:
```gradle
repositories {
    mavenCentral()
    // May need: maven { url = "https://maven.fabricmc.net/" }
}
```

### Tests Not Running

Verify JUnit 5 configuration in `build.gradle`:
```gradle
test {
    useJUnitPlatform()
}
```
