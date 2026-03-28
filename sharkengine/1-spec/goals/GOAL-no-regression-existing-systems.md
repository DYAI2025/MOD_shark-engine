# GOAL-no-regression-existing-systems: No Regression in Existing Systems

**Description**: The HovercraftController refactor must not break or degrade any existing system: ship assembly, builder mode, fuel system, mounting/dismounting, or server-side performance. Server operators must be able to upgrade without encountering crashes, unexpected behavior changes, or tick-time regressions.

**Status**: Approved

**Priority**: Must-have

**Source stakeholder**: [STK-server-operator](../stakeholders.md)

## Success Criteria

- [ ] All existing JUnit 5 tests for assembly, fuel, and physics (unrelated to input handling) continue to pass after the refactor
- [ ] `ShipEntity` assembly, mounting, and fuel logic is functionally unchanged
- [ ] Server tick time does not increase measurably due to the introduction of `HovercraftController`
- [ ] No new crashes or exceptions are introduced in the existing ship lifecycle (place wheel → assemble → mount → fly → dismount)

## Related Artifacts

- User stories: [US-no-regression](../user-stories/US-no-regression.md)
- Requirements: [REQ-REL-no-regression](../requirements/REQ-REL-no-regression.md), [REQ-PERF-controller-tick-budget](../requirements/REQ-PERF-controller-tick-budget.md)
