# ASM-stable-tick-rate: Server Runs at a Stable 20 TPS for Physics Determinism

**Category**: Environment

**Status**: Unverified

**Risk if wrong**: Medium — the 10-tick deceleration threshold (REQ-F-controlled-deceleration) and all tick-based acceptance criteria assume 20 TPS. On a lagging server (< 20 TPS), braking distances increase and the feel of the controls degrades. The mod does not crash, but behavior diverges from the spec.

## Statement

The target server environment runs at a stable 20 TPS under normal play conditions. Tick-based physics values (deceleration within 10 ticks, neutral drift test over 40 ticks) are specified assuming 20 TPS and will behave as designed on a stable server.

## Rationale

Minecraft's standard server tick rate is 20 TPS. Single-player and lightly loaded multiplayer servers maintain this rate. The mod does not target heavily loaded servers as a primary use case for this milestone.

## Verification Plan

No active verification needed — this is an environment assumption that holds for the target use case. Document as a known limitation: on servers running below 20 TPS, tick-based physics values will not match the specified durations. Revisit if time-based physics (seconds instead of ticks) is requested in a future milestone.

## Related Artifacts

- [REQ-F-controlled-deceleration](../requirements/REQ-F-controlled-deceleration.md), [REQ-F-no-drift-neutral](../requirements/REQ-F-no-drift-neutral.md)
