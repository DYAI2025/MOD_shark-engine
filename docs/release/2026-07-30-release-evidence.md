# AIR Release 1 — Release Evidence Gate (T24/REQ-024)

**Feature slug:** `shark-engine-air-release-1` · **Target:** 30.07.2026 · **Status:** `IN PROGRESS — manual smokes pending`

## The one rule (AC-024)

> Every evidence row below must cite the **SAME** commit SHA. Sign-off is refused if any row's
> SHA differs from the others. Evidence collected piecemeal across different commits — "declared
> evidenced by the union of ever-passing runs" — is the named false positive this gate exists to
> block. **Any new commit/push invalidates ALL rows** (automated rows are cheap to re-run,
> ~5 min; manual rows must be redone).

**Release candidate SHA:** `7234a56849db3c4fbadb7697dd7c5eb125314dcc`
(`feature/shark-engine-air-release-1`, "Feat: T23/REQ-023 looping backlog entry + negative regression guard")

## ⚠️ Open decision BEFORE the manual smokes: release version

`gradle.properties` says `mod_version=0.0.1` — the JAR below is `sharkengine-0.0.1.jar`. If the
release should ship as a different version (e.g. 0.1.0), that bump is a NEW COMMIT → new SHA →
**all rows below are void and must be re-collected**. Decide the version FIRST, bump if wanted,
re-run the automated chain against the new SHA, and only then start the manual smokes. Do not
spend the manual effort on a SHA that is not the release candidate.

## Evidence matrix

| # | Evidence (EV-024) | SHA | Status | Proof |
|---|---|---|---|---|
| 1 | `./gradlew clean runDatagen build` (incl. `compileClientJava`) | `7234a56` | ✅ 2026-07-24 | BUILD SUCCESSFUL in 21s, local JDK 21/Loom 1.7.4; datagen re-run produced **zero** diffs |
| 2 | Unit/Resource tests (`cleanTest test`) | `7234a56` | ✅ 2026-07-24 | **357 tests, 0 failures, 0 skipped** (JUnit XML totals) |
| 3 | Fabric GameTests (`runGametest`) | `7234a56` | ✅ 2026-07-24 | "**All 92 required tests passed :)**", BUILD SUCCESSFUL in 2m 3s |
| 4 | CI on the exact SHA | `7234a56` | ✅ 2026-07-24 | Run [30057869556](https://github.com/DYAI2025/MOD_shark-engine/actions/runs/30057869556), conclusion: success (build + gametest jobs) |
| 5 | JAR inspection + SHA-256 | `7234a56` | ✅ 2026-07-24 | `sharkengine-0.0.1.jar`, SHA-256 `b2a3dc87a9ab1b4984fdb4293f71754719fd7fea063accae41f3ae6c323ed4dc`; 142 `.class` entries, `fabric.mod.json` present, 23 `assets/`+`data/` entries (recipes/tags/lang/blockstates incl. `thruster_coloring`) |
| 6 | Client smoke (manual) | `7234a56` | ☐ | → checklist A below; attach screenshots/GIF + note client log clean |
| 7 | Dedicated-server smoke (manual) | `7234a56` | ☐ | → checklist B below; attach server-log excerpt (no ERROR/stacktrace) |
| 8 | Two-player smoke (manual) | `7234a56` | ☐ | → checklist C below; attach evidence from BOTH clients |
| 9 | Restart proof (manual, true OS-level) | `7234a56` | ☐ | → checklist D below; attach before/after state notes |

Rows 1–5 were collected in one uninterrupted session on the checked-out worktree at exactly this
SHA (fresh `clean` build, no incremental state). Rows 6–9 are human-in-the-loop by design — the
GameTest suite's in-place NBT reload (row 3) is explicitly only a proxy for row 9's real
process restart (disclosed in `VehiclePersistenceRestartGameTest`).

## A — Client smoke (`/mod-deploy`, Prism Launcher)

1. Craft path: thruster + red dye in the crafting grid → ONE thruster item, tooltip/components carry `trail_color` (no `thruster_red` item anywhere).
2. Build the minimal ship: Steering Wheel; ≥4 core blocks horizontal around it (planks); pilot seat at the seat-anchor position; exactly 1 BUG at the outer edge (facing = flight direction); the RED-crafted thruster; optionally a copilot seat.
3. Assemble via builder screen → launch. Verify route popup showed AIR/LAND/WATER earlier and only AIR builds.
4. Fly: W (throttle), A/D (real yaw turn — visual bank follows), Space climb, **Left Alt** descend (Shift must dismount instead), height penalty above Y=100 noticeable.
5. HUD: fuel bar counts down ONLY while thrusting (~0.25–0.75/s effective); speed/status shown.
6. Trail: the RED thruster emits red-tinted dust; an uncolored thruster keeps the default campfire/flame trail. (This is the client-only render path no GameTest can reach.)
7. Refuel: right-click ship with logs/planks as pilot.
8. Anchor (Shift+right-click), dismount (Shift), re-enter, Edit Mode within 5 blocks → builder reopens with the live structure → add a block → commit.
9. Client log: no ERROR/stacktrace from `sharkengine`.

## B — Dedicated-server smoke (`/test-server`, Docker, Port 25566)

1. Container starts clean with the row-5 JAR (verify the jar SHA-256 in `mods/` matches!).
2. Join, run the A2–A5 core loop.
3. Server log: reaches "Done", no `sharkengine` ERROR/stacktrace during assembly/flight/disassembly.

## C — Two-player smoke (2 Prism instances, per AIR-071 conventions)

1. Player 2 right-clicks the ship (empty hand) → mounts the COPILOT seat, visible on both clients.
2. Copilot control denial: player 2's W/A/D/Space/Alt inputs change nothing (server-side denial, rate-limited logging).
3. Copilot dismounts (Shift) and re-enters; pilot dismounts and re-enters own seat (roles preserved, REQ-011).
4. Both clients render the SAME red trail; join-mid-flight client sees ship + trail after tracking starts.

## D — Restart proof (true OS-level, on the B-server)

1. Assemble a ship with: RED thruster, both seats, fuel partially burned (fly ≥30 s), some damage (optional), then anchor and dismount.
2. `docker stop` the container (clean stop → world save), then start it again.
3. Rejoin: the ship exists at position; fuel level (incl. fractional debt behavior: no free fuel refund), trail color, both seat roles, health and anchor state are intact; pilot can re-enter and fly.

## Sign-off (Product Owner)

- [ ] All 9 rows cite `7234a56849db3c4fbadb7697dd7c5eb125314dcc` and are green/checked.
- [ ] Release version decided (see warning above) — if bumped, this document was re-collected for the new SHA.
- [ ] Release artifact = the row-5 JAR (byte-identical, SHA-256 re-verified at publish time).

**Signed off by:** ______ **Date:** ______

*(Publishing — e.g. Modrinth via `tools/modrinth-mcp-server` — only after sign-off, and only on
explicit go: outward-facing action.)*
