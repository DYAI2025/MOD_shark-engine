# AIR Release 1 — Release Evidence Gate (T24/REQ-024)

**Feature slug:** `shark-engine-air-release-1` · **Target:** 30.07.2026 · **Status:** `IN PROGRESS — manual smokes pending`

## The one rule (AC-024)

> Every evidence row below must cite the **SAME** commit SHA. Sign-off is refused if any row's
> SHA differs from the others. Evidence collected piecemeal across different commits — "declared
> evidenced by the union of ever-passing runs" — is the named false positive this gate exists to
> block. **Any new commit/push invalidates ALL rows** (automated rows are cheap to re-run,
> ~5 min; manual rows must be redone).

**Release candidate SHA:** `031a31cbd15747a00d37fd082165bdbe94c60449`
(`feature/shark-engine-air-release-1`, "Fix: exclude sources jar from server mods/ — lost fix #3 from the recovery audit")

**Release version:** `0.1.0` (decided 2026-07-24 — the previously flagged open item is resolved;
the earlier evidence set at `7234a56` is void per the single-SHA rule).

## ⚠️ Incident during evidence collection (2026-07-24) — why the JAR row is strict

The first automated collection (at the previous candidate `064e843`; this candidate `031a31c`
differs only by the Dockerfile sources-jar fix that same audit surfaced) used
`./gradlew clean runDatagen build` as one invocation and produced a **silently broken JAR** (zero `data/sharkengine/*.json` — no tags,
recipes, loot, blockstates, lang: structurally valid, gameplay-dead on any real server) plus
64/92 failing GameTests, while CI stayed green on the same SHA (CI never runs
`clean`/`runDatagen`). Root-caused and documented in the repo-root `CLAUDE.md` ("Datagen
ordering race"): no execution-order guarantee between independent CLI tasks; the broken state
persists via UP-TO-DATE. The evidence below was therefore collected with the SAFE sequence —
three SEPARATE invocations (`clean`, then `runDatagen`, then `build`) — and row 5 now includes
the mandatory datapack-content inspection. The broken artifact's hash (`cbff9949…`) is recorded
here only so it can never be mistaken for the release: **do not ship any JAR whose SHA-256 is
not the row-5 value.**

## Evidence matrix

| # | Evidence (EV-024) | SHA | Status | Proof |
|---|---|---|---|---|
| 1 | Build via safe sequence: `./gradlew clean` → `./gradlew runDatagen` → `./gradlew build` (separate invocations, incl. `compileClientJava`) | `031a31c` | ✅ 2026-07-24 | All three BUILD SUCCESSFUL (build 8s), local JDK 21/Loom 1.7.4; datagen output matches the committed `src/main/generated` (zero diff) |
| 2 | Unit/Resource tests (part of `build`) | `031a31c` | ✅ 2026-07-24 | **357 tests, 0 failures, 0 skipped** (JUnit XML totals) |
| 3 | Fabric GameTests (`runGametest`) | `031a31c` | ✅ 2026-07-24 | "**All 92 required tests passed :)**", BUILD SUCCESSFUL in 1m 5s |
| 4 | CI on the exact SHA | `031a31c` | ✅ 2026-07-24 | Run [30124816031](https://github.com/DYAI2025/MOD_shark-engine/actions/runs/30124816031), conclusion: success (build + gametest jobs) |
| 5 | JAR inspection + SHA-256 (incl. datapack-content check) | `031a31c` | ✅ 2026-07-24 | `sharkengine-0.1.0.jar`, SHA-256 `c4ed1921ef90d21b07cecdbadb56ad05ac9af0e03dcd9214193e2ada785abe3b`; `fabric.mod.json` version `0.1.0`; **33 `data/sharkengine` JSONs + 40 `assets/sharkengine` JSONs present** — `ship_eligible.json`, `thruster_coloring.json`, `en_us.json` individually verified |
| 6 | Client smoke (manual) | `031a31c` | ☐ | → checklist A below; attach screenshots/GIF + note client log clean |
| 7 | Dedicated-server smoke (manual) | `031a31c` | ☐ | → checklist B below; attach server-log excerpt (no ERROR/stacktrace) |
| 8 | Two-player smoke (manual) | `031a31c` | ☐ | → checklist C below; attach evidence from BOTH clients |
| 9 | Restart proof (manual, true OS-level) | `031a31c` | ☐ | → checklist D below; attach before/after state notes |

Rows 1–3 and 5 were collected in one uninterrupted session on the checked-out worktree at
exactly this SHA (fresh clean state, safe sequential invocations). Rows 6–9 are
human-in-the-loop by design — the GameTest suite's in-place NBT reload (row 3) is explicitly
only a proxy for row 9's real process restart (disclosed in
`VehiclePersistenceRestartGameTest`).

## A — Client smoke (`/mod-deploy`, Prism Launcher)

0. **First: verify the deployed JAR's SHA-256 equals the row-5 value** (the incident above is
   exactly the failure this catches).
1. Craft path: thruster + red dye in the crafting grid → ONE thruster item carrying
   `trail_color` (no `thruster_red` item anywhere).
2. Build the minimal ship: Steering Wheel; ≥4 core blocks horizontal around it (planks); pilot
   seat at the seat-anchor position; exactly 1 BUG at the outer edge (facing = flight
   direction); the RED-crafted thruster; optionally a copilot seat.
3. Assemble via builder screen → launch. Verify the route popup showed AIR/LAND/WATER earlier
   and only AIR builds.
4. Fly: W (throttle), A/D (real yaw turn — visual bank follows), Space climb, **Left Alt**
   descend (Shift must dismount instead), height penalty above Y=100 noticeable.
5. HUD: fuel bar counts down ONLY while thrusting (~0.25–0.75/s effective); speed/status shown.
6. Trail: the RED thruster emits red-tinted dust; an uncolored thruster keeps the default
   campfire/flame trail. (This is the client-only render path no GameTest can reach.)
7. Refuel: right-click ship with logs/planks as pilot.
8. Anchor (Shift+right-click), dismount (Shift), re-enter, Edit Mode within 5 blocks → builder
   reopens with the live structure → add a block → commit.
9. Client log: no ERROR/stacktrace from `sharkengine`.

## B — Dedicated-server smoke (`/test-server`, Docker, Port 25566)

1. Container starts clean with the row-5 JAR (**verify the jar SHA-256 in `mods/` matches!**).
   *Pre-verified 2026-07-24 for the currently running container (image `093b3986705e`): server
   reached "Done (0.465s)"; `mods/` contains exactly `fabric-api.jar` + `sharkengine-0.1.0.jar`
   (no sources jar — the lost-fix-#3 Dockerfile fix proven in situ); in-container SHA-256 equals
   the row-5 value byte-for-byte. Notable: the Docker-internal Gradle build reproduced the host
   artifact exactly — reproducibility holds across environments.*
2. Join, run the A2–A5 core loop.
3. Server log: reaches "Done", no `sharkengine` ERROR/stacktrace during
   assembly/flight/disassembly.

## C — Two-player smoke (2 Prism instances, per AIR-071 conventions)

1. Player 2 right-clicks the ship (empty hand) → mounts the COPILOT seat, visible on both
   clients.
2. Copilot control denial: player 2's W/A/D/Space/Alt inputs change nothing (server-side
   denial, rate-limited logging).
3. Copilot dismounts (Shift) and re-enters; pilot dismounts and re-enters own seat (roles
   preserved, REQ-011).
4. Both clients render the SAME red trail; a join-mid-flight client sees ship + trail after
   tracking starts.

## D — Restart proof (true OS-level, on the B-server)

1. Assemble a ship with: RED thruster, both seats, fuel partially burned (fly ≥30 s), some
   damage (optional), then anchor and dismount.
2. `docker stop` the container (clean stop → world save), then start it again.
3. Rejoin: the ship exists at position; fuel level (incl. fractional debt behavior: no free
   fuel refund), trail color, both seat roles, health and anchor state are intact; pilot can
   re-enter and fly.

## Sign-off (Product Owner)

- [ ] All 9 rows cite `031a31cbd15747a00d37fd082165bdbe94c60449` and are green/checked.
- [ ] Release artifact = the row-5 JAR (byte-identical, SHA-256
      `c4ed1921ef90d21b07cecdbadb56ad05ac9af0e03dcd9214193e2ada785abe3b` re-verified at
      publish time).

**Signed off by:** ______ **Date:** ______

*(Publishing — e.g. Modrinth via `tools/modrinth-mcp-server` — only after sign-off, and only on
explicit go: outward-facing action.)*
