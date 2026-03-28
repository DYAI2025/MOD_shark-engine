# REQ-PERF-controller-tick-budget: HovercraftController Tick Budget

**Type**: Performance

**Status**: Approved

**Priority**: Should-have

**Source**: [GOAL-no-regression-existing-systems](../goals/GOAL-no-regression-existing-systems.md)

**Source stakeholder**: [STK-server-operator](../stakeholders.md)

## Description

A single invocation of `HovercraftController.tick(input, state)` must complete within 0.5 ms on the server thread. Minecraft's server tick budget is 50 ms (20 TPS). The controller must not consume a measurable fraction of that budget, even with multiple active vehicles on the same server.

## Acceptance Criteria

- Given a single active vehicle, when `HovercraftController.tick()` is invoked, then execution time is < 0.5 ms (measured as wall-clock time on a standard development machine)
- Given 10 concurrently active vehicles, when all controllers tick in the same server tick, then total controller time is < 5 ms
- Given a profiling run with `System.nanoTime()` wrapping the controller call, when 1000 ticks are measured, then mean and p99 are both below the 0.5 ms threshold
