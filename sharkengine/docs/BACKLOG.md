# Shark Engine — Post-Release Backlog

Deliberately scoped: this file exists to satisfy REQ-023/AC-023 — looping is documented as a
post-release feature, never a Release-1 gate. Do not grow it into a second task tracker; the
live Release-1 plan is `docs/plans/2026-07-18-shark-engine-air-release-1.md` (T01–T24) in the
repo root's `docs/`, and the older gap analysis lives in `PRODUCTION_MVP_TASKS.md`.

## BACKLOG-001 — Looping maneuver (post-Release-1)

**Status:** documented only. NOT implemented, NOT a Release-1 acceptance criterion — "kein
Release-1-Abnahmekriterium" (VIS-010, CAN-013; user-confirmed via SRC-001, REQ-023).

**Confirmed design notes (verbatim scope of REQ-023 — nothing beyond this was ever confirmed):**

- The maneuver is **manually or semi-automatically overridden** ("manuell beziehungsweise
  halbautomatisch übersteuert") — the pilot triggers it; it is not autonomous flight.
- **Entry conditions** ("Eintrittsbedingungen") must gate the maneuver. Their exact shape
  (speed/altitude/fuel thresholds or otherwise) is deliberately UNDESIGNED until this entry is
  picked up — do not invent them earlier.
- A failed or aborted loop has **fatal crash consequences** ("fatale Crash-Folgen") — it ties
  into the existing crash/wreck fall sequence, not a soft recovery.

**Architecture boundary (locked by tests, not just prose):**

- Looping must arrive as its own policy/controller behind a seam (NFR-003, CAN-018) — never as
  a hidden 6th `AccelerationPhase` or a dormant field in the shipped physics.
- `AccelerationPhaseTest.noLoopRelatedPhaseIntroduced` locks the 5-phase ramp and scans the
  phase source for loop tokens (baseline: zero). Additionally, `ShipPhysics`'s exhaustive
  phase switch makes a 6th enum value a COMPILE error — verified by mutation during T23.
- `LoopingBacklogDocumentationTest` locks this entry's existence and content, so the backlog
  record cannot silently vanish in a docs cleanup while REQ-023 claims it exists.
