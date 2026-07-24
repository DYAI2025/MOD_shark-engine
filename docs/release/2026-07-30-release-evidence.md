# AIR Release 1 — Release Evidence Gate (T24/REQ-024)

**Feature slug:** `shark-engine-air-release-1` · **Target:** 30.07.2026 · **Status:** `ALL 9 ROWS RESOLVED (7 fresh + 2 PO-reclassified) — signed off, cleared for merge`

## The one rule (AC-024)

> Every evidence row below must cite the **SAME** commit SHA. Sign-off is refused if any row's
> SHA differs from the others. Evidence collected piecemeal across different commits — "declared
> evidenced by the union of ever-passing runs" — is the named false positive this gate exists to
> block. **Any new commit/push invalidates ALL rows.**

**Release candidate SHA:** `e68ec34762ec3da7bd60a306e0ebe3a4cb19d699`
(`feature/shark-engine-air-release-1`, "Fix: all 6 confirmed findings from the independent merge review (F1-F6)")

**Release version:** `0.1.0`.

## Gate history (why the candidate moved twice — the gate working as designed)

1. **Datagen ordering race (at `064e843`):** a single-invocation `clean runDatagen build`
   produced a silently gameplay-dead JAR (zero datapack files) while CI stayed green; repaired,
   root-caused, documented as the repo-root `CLAUDE.md` "Datagen ordering race" gotcha. The
   poisoned hash `cbff9949…` is recorded so it can never be shipped. Since then all evidence
   builds use three SEPARATE invocations and row 5 includes a datapack-content inspection.
2. **Sources-jar Dockerfile fix (`031a31c`):** the T24 recovery audit found the abandoned
   line's sources-jar exclusion had been lost — fixed; candidate moved.
3. **Independent merge review (`e68ec34`, current):** merge-when-true gate 4 demanded an
   independent review of the previously self-reviewed T15–T24 diff. 3 lenses, adversarial
   verification: 6 confirmed findings (2 MAJOR), all fixed RED-first — including a full rework
   of the spawn preflight (cell-set intersection against parked ships' rotated blueprint cells;
   the old hull-AABB-vs-entity-box check was wrong in both directions) and two corrupt-save
   hardenings (unrecoverable NaN CurrentSpeed; unclamped FuelLevel). See the `e68ec34` commit
   message for the full record. **Production behavior changed → every prior evidence row,
   including the manual smokes attested at `031a31c`, is void per the single-SHA rule.**

## Evidence matrix

| # | Evidence (EV-024) | SHA | Status | Proof |
|---|---|---|---|---|
| 1 | Build via safe sequence: `./gradlew clean` → `runDatagen` → `build` (separate invocations, incl. `compileClientJava`) | `e68ec34` | ✅ 2026-07-25 | All three BUILD SUCCESSFUL (build 5s), JDK 21/Loom 1.7.4; datagen output matches committed `src/main/generated` |
| 2 | Unit/Resource tests | `e68ec34` | ✅ 2026-07-25 | **362 tests, 0 failures, 0 skipped** (JUnit XML totals) |
| 3 | Fabric GameTests (`runGametest`) | `e68ec34` | ✅ 2026-07-25 | "**All 94 required tests passed :)**" — incl. the four new review-finding locks (both preflight directions, NaN speed, fuel clamps) |
| 4 | CI on the exact SHA | `e68ec34` | ✅ 2026-07-25 | Runs [30129998290](https://github.com/DYAI2025/MOD_shark-engine/actions/runs/30129998290) and [30129996169](https://github.com/DYAI2025/MOD_shark-engine/actions/runs/30129996169), both conclusion: success |
| 5 | JAR inspection + SHA-256 (incl. datapack check) | `e68ec34` | ✅ 2026-07-25 | `sharkengine-0.1.0.jar`, SHA-256 `cfc0b7837334a78dd9aae90fdbe0da7d860c8e078fbf110af768d310aac30199`; 33 `data/sharkengine` JSONs present |
| 6 | Client smoke (manual) | `e68ec34` | ✅ 2026-07-25 (reclassified) | PO decision (b), explicit in-session: the `031a31c` attestation carries over as a DOCUMENTED DEVIATION — justified by the verified EMPTY client-source delta between the candidates; the entire fix surface is two server-side files, machine-locked by the 94 GameTests |
| 7 | Dedicated-server smoke (manual) | `e68ec34` | ✅ 2026-07-25 | Checklist B re-performed and attested GREEN by Ben on the REBUILT container (image `e9c7aee10943`): "Done (0.411s)", `mods/` exactly fabric-api + sharkengine-0.1.0.jar, in-container SHA-256 **equals row 5** (`cfc0b783…`). Two observed benign startup log lines, disclosed: the known "No data fixer registered" (EV-025) and "No key layers in MapLike[{}]" (flat-preset parsing; no functional effect observed) |
| 8 | Two-player smoke (manual) | `e68ec34` | ✅ 2026-07-25 (reclassified) | Same PO decision (b) and justification as row 6 |
| 9 | Restart proof (manual, true OS-level) | `e68ec34` | ✅ 2026-07-25 | Checklist D re-performed and attested GREEN by Ben (`docker stop`/`start` of the rebuilt container; fuel/color/seat/anchor state intact) |

*Rows 7/9 evidence scope: Product-Owner self-attestations delivered verbally in-session
(attested-not-artifacted, same disclosure discipline as before), performed against the
byte-verified rebuilt server.*

## Rows 6/8 decision (Product Owner)

The prior A/C attestations ran at `031a31c`. **Verified fact:** `git diff 031a31c..e68ec34 --
sharkengine/src/client/` is EMPTY — the entire candidate delta is two server-side files
(`ShipAssemblyService` spawn preflight, `ShipEntity` NBT clamps), both machine-locked by the
94 GameTests. The deployed Prism client JAR still carries the `031a31c` build (`c4ed1921…`).
Two honest paths — only the PO may choose:

- **(a) Re-attest:** agent redeploys `cfc0b783…` via `/mod-deploy`; PO re-runs checklist A
  (and C with a second instance) against the rebuilt server. Cleanest; ~15 min.
- **(b) Reclassify:** PO explicitly accepts the prior A/C attestations as carrying over,
  recorded as a documented deviation justified by the empty client delta and the
  server-side-only fix surface. The watcher discipline applies: only the user may downgrade;
  the agent will not do this silently.

**DECISION (2026-07-25): the PO chose (b)** — verbatim in-session: "(b) reklassifizieren —
Client-Delta ist leer". Rows 6/8 above carry the reclassification note accordingly.

## A — Client smoke (`/mod-deploy`, Prism Launcher)

0. **First: verify the deployed JAR's SHA-256 equals the row-5 value `cfc0b783…`.**
1. Craft path: thruster + red dye → ONE thruster item carrying `trail_color` (no `thruster_red` item anywhere).
2. Build the minimal ship: Steering Wheel; ≥4 core blocks horizontal; pilot seat at the anchor position; exactly 1 BUG at the outer edge (facing = flight direction); the RED-crafted thruster; optionally a copilot seat.
3. Assemble via builder → launch (route popup showed AIR/LAND/WATER; only AIR builds).
4. Fly: W throttle, A/D real yaw turn (bank follows), Space climb, **Left Alt** descend (Shift dismounts), height penalty above Y=100.
5. HUD: fuel counts down ONLY while thrusting (~0.25–0.75/s); speed/status shown.
6. Trail: RED thruster emits red-tinted dust; uncolored keeps default trail (client-only path, GameTest-unreachable).
7. Refuel: right-click with logs/planks as pilot.
8. Anchor (Shift+right-click), dismount, re-enter, Edit Mode ≤5 blocks → builder reopens → add block → commit.
9. Client log: no `sharkengine` ERROR/stacktrace.

## B — Dedicated-server smoke (`/test-server`, Docker, Port 25566)

1. Container runs the row-5 JAR (**SHA-256 in `mods/` verified** — done for image `e9c7aee10943`, see row 7).
2. Join, run the A2–A5 core loop.
3. Server log: "Done" reached, no `sharkengine` ERROR/stacktrace during assembly/flight/disassembly.

## C — Two-player smoke (2 Prism instances)

1. Player 2 right-clicks the ship (empty hand) → mounts the COPILOT seat, visible on both clients.
2. Copilot control denial: player 2's inputs change nothing (server-side, rate-limited logging).
3. Copilot dismounts and re-enters; pilot dismounts and re-enters own seat (roles preserved).
4. Both clients render the SAME red trail; join-mid-flight client sees ship + trail after tracking.

## D — Restart proof (true OS-level, on the B-server)

1. Assemble a ship with RED thruster, both seats, fuel partially burned, then anchor and dismount.
2. `docker stop` (clean save), then start again.
3. Rejoin: ship exists; fuel (no free refund), trail color, seat roles, health, anchor intact; pilot can re-enter and fly.

## Sign-off (Product Owner)

Prior sign-off (given for `031a31c`) is void with that candidate. To be renewed once rows 6/8
are resolved:

- [x] All 9 rows cite `e68ec34762ec3da7bd60a306e0ebe3a4cb19d699` and are green/checked (rows
      6/8 carry the explicit PO reclassification note).
- [x] Release artifact = the row-5 JAR (SHA-256 `cfc0b783…` re-verified at publish time).

**Signed off by:** Ben (Product Owner) — acceptance via the standing in-session merge order plus
the explicit rows-6/8 reclassification decision; recorded by the agent on his instruction, not
hand-signed. **Date:** 2026-07-25

*(Publishing — e.g. Modrinth — only after sign-off, and only on explicit go.)*

## Publish record (Modrinth, 2026-07-25)

- **Project:** `sharkengine` (id `xwlGFlcw`), created via API in the PO's account (DYAI2025),
  metadata per the session-approved draft incl. the "Honest current limits" section.
- **Version:** `0.1.0` "AIR Release 1" (id `bpmKXmLT`), loaders `fabric`, game version
  `1.21.1`, required dependency Fabric API (`P7dR8mSH`).
- **Publish-time re-verification (sign-off requirement):** the uploaded file was verified
  BYTE-IDENTICAL to the row-5 artifact — server-side SHA-512 prefix and file size
  (402,149 bytes) match the local `sharkengine-0.1.0.jar` (SHA-256 `cfc0b783…`) exactly.
- **Status:** submitted for review (`processing`, HTTP 204 on submit). Public visibility at
  https://modrinth.com/mod/sharkengine pends Modrinth's human moderation (typically hours to
  a few days — outside our control).
- **Token hygiene:** the PAT used lives only in `~/.env`; the stray copy found in a Downloads
  clone was removed during the session. Recommendation: revoke the PAT after moderation
  clears.
