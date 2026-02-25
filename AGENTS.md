# Repository Guidelines

## Project Structure & Module Organization
All active code lives under `sharkengine/`, a Fabric Loom module. Core logic, registries, and server-safe assets reside in `sharkengine/src/main/java` and `sharkengine/src/main/resources`, while HUD and renderer hooks belong in `sharkengine/src/client/java`. Unit tests mirror their packages within `sharkengine/src/test/java`. Feature briefs, scenarios, and acceptance checkpoints sit in `specs/001-vertikale-bewegung/`; keep design notes, release plans, and backlogs in `docs/` or root-level Markdown. Avoid sprinkling gameplay logic outside the module so the Gradle build and Loom remapping stay deterministic.

## Build, Test, and Development Commands
`cd sharkengine && ./gradlew build` compiles main and client source sets, remaps the jar, and validates resources. Use `./gradlew runClient` for Fabric dev-client sessions when tweaking overlays or physics feel. The fast feedback path is `./gradlew test`, while `./gradlew check` mirrors CI by chaining tests and static analyzers. Stick to Java 21; Loom enforces the toolchain and will fail if another JVM is picked up.

## Coding Style & Naming Conventions
Follow 4-space indentation with braces on the same line. Classes and enums use PascalCase (`ShipBlueprint`, `VehicleClass`); methods, fields, and resource identifiers use lowerCamelCase or Fabric’s lowercase_with_underscores. Keep registry bootstrap in `dev.sharkengine.ship.*` helpers such as `ModBlocks` and isolate client-only hooks under `dev.sharkengine.client`. Favor concise, descriptive comments for non-obvious math or networking code.

## Testing Guidelines
Tests rely on JUnit 5 and live beside the packages they validate (`dev.sharkengine.ship.ShipPhysicsTest`). Provide clear `@DisplayName` strings tying failures to gameplay behavior, and mock Fabric abstractions when unit isolation requires it. Run `./gradlew test` before PRs and add coverage whenever physics math, serialization, or systems such as `FuelSystem` change.

## Commit & Pull Request Guidelines
Commits use imperative scopes (`Fix: …`, `Spec-Flow Feature 001: …`). Keep each change focused, note affected specs, and mention gradle/fabric bumps explicitly. Pull requests should describe gameplay impact, list local checks (tests, `runClient` when UI shifts), and attach screenshots or clips for HUD adjustments. Reference the relevant Spec-Flow task numbers to keep QA and writing agents aligned.

## Documentation & Spec Notes
Before coding, read `specs/001-vertikale-bewegung/plan.md` and `state.yaml`, then update them whenever mechanics shift. High-level discussions belong in `docs/` or top-level notes so downstream agents inherit a single source of truth.
