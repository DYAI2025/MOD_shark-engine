# Phase 0.16 Council Challenge — shark-engine-air-release-1

Bounded three-role council (Challenger / Advisor / Critic), 2 collision rounds, ≤180 words/role/round.
Run 2026-07-18 against the user-confirmed Canvas + Vision, before PRD finalization.

## Round 1 (independent)

### Challenger
Attacking CAN-001 (the stated problem): it silently fuses two separable problems — shipping missing gameplay (copilot, reentry, edit-mode, trails: REQ-005–017) and proving runtime-verification maturity (CAN-017, REQ-024) — into one P0 set due 2026-07-30. RISK-002 names the danger ("Termin... verleitet zu parallelen Änderungen... erhöht Integrationsfehler") but the PRD doesn't resolve it, just logs it. Worse, RISK-001 admits current build/runtime status is UNVERIFIED — so the date was fixed before a baseline velocity existed. That's estimating blind, not planning.

Attacking scope-as-requirement: REQ-022/CAN-011 spend Release-1 effort on Vehicle Core "seams" for LAND/WATER, which CAN-012/VIS-009 explicitly exclude from this release. RISK-005 names this exact trap ("vorzeitige Generalisierung... kann das AIR-MVP verzögern") — unresolved, not mitigated.

Attacking the user: CAN-002/003 are asserted, not evidenced. Nothing here roots the requirement in actual player behavior — only in the Product Owner's own architecture preference (CAN-018).

### Advisor
Two concrete swaps that cut real risk in the 12-day window without touching the confirmed vision:
1) Demote REQ-022's 7-seam "Minimal Vehicle Core" from a P0 release gate to "extract only what AIR literally calls today." You cannot correctly design LAND/WATER seams against physics/assembly rules you haven't built yet — that's the doc's own RISK-005 materializing inside its P0 list. Ship AIR concrete; extract shared seams when LAND actually starts.
2) Replace the 16 separate craftable colored-thruster items with one Thruster item plus a craft-time data component storing the chosen DyeColor, rendered via a tinted-texture layer. Same craft-time-only behavior already user-decided, roughly 1/16th the datagen and asset-gen surface, same acceptance criteria pass, far less to break before 07-30.

### Critic
The premise is backwards: committing to a hard date (12 days out) before establishing whether the current codebase even builds (RISK-001, UNVERIFIED). Fix the baseline first, then date the release — not the reverse.

Craft-only trail coloring (OQ-002) multiplies at minimum 16 dye-colored thruster variants × recipes/models/textures/lang/datagen entries for a P1 cosmetic feature, while P0 work is still unproven.

REQ-022's "minimal Vehicle Core" is speculative generality for a system with exactly one consumer — flagged as RISK-005, yet still made P0.

24 P0-heavy requirements, atomic rollback, cross-client sync, persistence, AND a full evidence gate — as one indivisible ship-date — is too much surface for 12 days on an unverified base.

## Round 2 (collision)

### Challenger
Converging, then sharpening. All three land on the same root defect: RISK-001 (UNVERIFIED build/runtime) sits inside a P0 set gated to a fixed date, RISK-002 names the danger, nobody resolved it. The date was set before a baseline existed, and no swap fixes that — only re-sequencing does: verify build+runtime first, then date the P0 work off real velocity.
Concede: the Advisor's dye-component swap is right and cheap — dropping the P1-shine framing.
Sharpen against Critic's "slice it": slicing 24 P0 reqs doesn't rescue a broken sequencing premise. The fix is ordinal, not volumetric: gate REQ-024's evidence chain on RISK-001 before any P0 work starts.

### Advisor
Both attacks land partially; neither kills the swaps.
RISK-001 (date fixed before baseline verified): correct, but the date is canvas-confirmed and out of this council's remit to re-litigate. The actionable fix inside the fixed window: make Day-0 literally `./gradlew build` + GameTest on current main before any P0 branch opens — turns RISK-001 from a logged-and-ignored risk into a real go/no-go gate in hours.
REQ-022: Challenger and Critic both independently landed on RISK-005/premature generalization — hold the demotion.
Trail coloring: Critic's "16 variants" objection is exactly what my swap removes.
Final: keep both swaps, add the Day-0 build gate.

### Critic
Concede: Advisor's single-Thruster-plus-DyeColor-component swap kills my OQ-002 objection. Withdraw trail-scope as a top risk.
Sharpen: the load-bearing risk is still date-before-baseline, and REQ-024 makes it worse — an all-or-nothing gate at day 12 with no earlier checkpoint. Fix: require the evidence gate incrementally per-REQ as merged, not once at the end. (Note: this specific incremental-gating proposal was presented to the user and NOT adopted — user kept the original single end-of-release REQ-024 gate.)
Push back: Challenger's "attacking the user" point re-litigates VIS-008/ASM-004, already user-confirmed vision — out of this council's mandate.

## Convergence summary (for accurate citation elsewhere in the PRD)

- **REQ-022 demotion (minimal-as-needed Vehicle Core seams):** all three roles (Challenger, Advisor, Critic) independently named RISK-005/premature-generalization in ROUND 1 already. Genuine 3/3 round-1 convergence. User adopted this proposal.
- **REQ-025 (Day-0 build/runtime gate):** Challenger and Critic named the underlying date-before-baseline risk in round 1; Advisor proposed the concrete Day-0 gate mechanism explicitly in round 2, and both Challenger and Critic reaffirmed/sharpened the same mechanism in round 2. Accurate citation: "3/3 roles converged by round 2" — not round-1 unanimity, do not claim otherwise. User adopted this proposal.
- **Single-item trail color swap:** Advisor proposed in round 1; Critic explicitly conceded it resolves their objection in round 2; Challenger did not object to it at any point. User adopted this proposal.
- **Incremental per-REQ evidence gating:** Critic proposal, round 2 only. Presented to user, NOT adopted — do not cite this as adopted or converged-on anywhere in the PRD.
