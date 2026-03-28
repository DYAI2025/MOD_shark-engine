Phase-specific instructions for the **Specification** phase. Extends [../CLAUDE.md](../CLAUDE.md).

## Purpose

This phase defines **what** we're building and **why**. Focus on clarity, measurability, and alignment with stakeholder needs.

## Phase artifacts

| Artifact | Location | Purpose |
|----------|----------|---------|
| Stakeholders | [`stakeholders.md`](stakeholders.md) | Roles with interests and influence |
| Goals | [`goals/`](goals/) | High-level outcomes |
| User Stories | [`user-stories/`](user-stories/) | User-facing capabilities |
| Requirements | [`requirements/`](requirements/) | Testable system requirements |
| Assumptions | [`assumptions/`](assumptions/) | Beliefs taken as true but not verified |
| Constraints | [`constraints/`](constraints/) | Hard limits on design and implementation |

---

## AI Guidelines

### Per-artifact guidance

**Stakeholders**: ask who uses, funds, operates, or is affected by the system. Record influence level honestly — it drives conflict resolution. Add entries to [`stakeholders.md`](stakeholders.md).

**Goals**: decompose vague ideas into concrete, measurable outcomes. Use MoSCoW priority consistently.
Status lifecycle: `Draft → Approved → Achieved → Deprecated`. Only a human can approve or deprecate. The agent marks `Achieved` when all success criteria are met (linked requirements implemented).

**User Stories**: use "As a [role], I want [capability], so that [benefit]." The role must be an existing stakeholder ID. Acceptance criteria at the story level are high-level; detailed criteria live in requirements.
Status lifecycle: `Draft → Approved → Implemented → Deprecated`. Only a human can approve or deprecate. The agent marks `Implemented` when all linked requirements reach `Implemented`.

**Requirements**: use clear, testable language (not "should be fast" — use "response time < 200ms at p95"). Choose the correct requirement class.
Requirement classes: `REQ-F` Functional, `REQ-PERF` Performance, `REQ-SEC` Security, `REQ-REL` Reliability, `REQ-USA` Usability, `REQ-MNT` Maintainability, `REQ-PORT` Portability, `REQ-SCA` Scalability, `REQ-COMP` Compliance.
Status lifecycle: `Draft → Approved → Implemented → Deprecated`. Only a human can approve or deprecate. The agent marks `Implemented` when all linked tasks reach Done.

**Assumptions**: always record the risk level (what happens if wrong?) and a verification plan when possible.
Status lifecycle: `Unverified → Verified | Invalidated`. The agent marks `Verified` when the verification plan confirms the assumption. Only a human can mark `Invalidated` (triggers impact analysis on dependent artifacts).

**Constraints**: consider technical (platforms, dependencies), business (budget, timeline, team size), and operational (hosting, compliance) categories.
Status lifecycle: `Active → Lifted`. Only a human can lift a constraint.

### Conflict resolution

A conflict exists when two or more requirements cannot both be satisfied as stated.

**Never resolve a conflict silently.** Always surface it before acting.

1. **Identify**: note conflicting requirement IDs, source stakeholders, influence levels, and why they are incompatible.
2. **Ask the user**: present what makes them incompatible, stakeholders and influence levels, two or more resolution options, and a recommended option if one is clearly better.
3. **Wait for explicit approval** before modifying any file.
4. **Apply**: update affected requirement files and index rows. Update dependent user stories or goals if affected. Record a decision if the resolution imposes a recurring constraint.
5. **Verify**: no artifacts remain in a conflicting state after resolution.

### Assumption invalidation

When an assumption is found to be wrong or no longer holds:

1. **Identify impact**: list all artifacts (requirements, user stories, decisions) that depend on the invalidated assumption.
2. **Ask the user**: present the invalidated assumption, the affected artifacts, and proposed adjustments or alternatives.
3. **Wait for explicit approval** before modifying any file.
4. **Apply**: change the assumption's Status to `Invalidated`. Update or flag all dependent artifacts as directed.
5. **Verify**: no artifacts remain based on the invalidated assumption without acknowledgment.

### Artifact deprecation

When an artifact (goal, user story, requirement) is no longer relevant:

1. Propose deprecation to the user with rationale and downstream impact.
2. Wait for explicit approval.
3. Change Status to `Deprecated` in the artifact file. Update its index row.
4. Check for dependent artifacts — flag any that reference the deprecated item.

---

## Decisions Relevant to This Phase

| File | Title | Trigger |
|------|-------|---------|
<!-- Add rows as decisions are recorded. File column: [DEC-kebab-name](../decisions/DEC-kebab-name.md) -->

---

## Linking to Other Phases

- Goals, user stories, constraints, assumptions, and requirements are referenced in design documents (`2-design/`)
- Requirements determine the development tasks in `3-code/tasks.md`; each task references the requirements it fulfills
- Acceptance criteria inform test cases (`3-code/`)

---

## Goals Index

| File | Priority | Status | Summary |
|------|----------|--------|---------|
| [GOAL-hovercraft-flight-model](goals/GOAL-hovercraft-flight-model.md) | Must-have | Approved | Replace yaw+thrust model with hovercraft translation relative to player look direction |
| [GOAL-testable-flight-architecture](goals/GOAL-testable-flight-architecture.md) | Must-have | Approved | Extract HovercraftController as pure, Minecraft-independent testable class |
| [GOAL-reliable-flight-tests](goals/GOAL-reliable-flight-tests.md) | Must-have | Approved | Replace mock-based integration tests with real deterministic flight behavior tests (A–J) |
| [GOAL-no-regression-existing-systems](goals/GOAL-no-regression-existing-systems.md) | Must-have | Approved | HovercraftController refactor must not break assembly, fuel, mounting, or server performance |

---

## User Stories Index

| File | Role | Priority | Status | Summary |
|------|------|----------|--------|---------|
| [US-fly-forward-backward](user-stories/US-fly-forward-backward.md) | STK-player | Must-have | Approved | Fly forward/backward relative to player look direction |
| [US-strafe-left-right](user-stories/US-strafe-left-right.md) | STK-player | Must-have | Approved | Strafe left/right without rotating the vehicle |
| [US-vertical-movement](user-stories/US-vertical-movement.md) | STK-player | Must-have | Approved | Control altitude independently of look direction |
| [US-no-drift-at-rest](user-stories/US-no-drift-at-rest.md) | STK-player | Must-have | Approved | Vehicle stays still with zero input; decelerates cleanly on release |
| [US-testable-controller](user-stories/US-testable-controller.md) | STK-mod-developer | Must-have | Approved | Test flight controller without a Minecraft instance |
| [US-flight-behavior-tests](user-stories/US-flight-behavior-tests.md) | STK-mod-developer | Must-have | Approved | Deterministic unit tests A–J for all hovercraft input combinations |
| [US-no-regression](user-stories/US-no-regression.md) | STK-server-operator | Must-have | Approved | Existing systems unaffected by HovercraftController refactor |

---

## Requirements Index

| File | Type | Priority | Status | Summary |
|------|------|----------|--------|---------|
| [REQ-F-input-model](requirements/REQ-F-input-model.md) | Functional | Must-have | Approved | Three-axis input model: moveForward, moveStrafe, moveVertical; no turn channel |
| [REQ-F-forward-by-player-yaw](requirements/REQ-F-forward-by-player-yaw.md) | Functional | Must-have | Approved | Forward input moves along player horizontal yaw direction |
| [REQ-F-backward-movement](requirements/REQ-F-backward-movement.md) | Functional | Must-have | Approved | Backward input (moveForward < 0) moves opposite to look direction |
| [REQ-F-strafe-movement](requirements/REQ-F-strafe-movement.md) | Functional | Must-have | Approved | Strafe input moves orthogonally to look direction; no forward/vertical component |
| [REQ-F-vertical-only](requirements/REQ-F-vertical-only.md) | Functional | Must-have | Approved | Vertical input affects only Y axis; pitch does not influence XZ movement |
| [REQ-F-no-drift-neutral](requirements/REQ-F-no-drift-neutral.md) | Functional | Must-have | Approved | Zero input over 40 ticks produces no movement (speed < 0.001, delta < 0.01) |
| [REQ-F-controlled-deceleration](requirements/REQ-F-controlled-deceleration.md) | Functional | Must-have | Approved | Vehicle stops within N ticks after input release; no overshoot or oscillation |
| [REQ-F-bugblock-orientation-only](requirements/REQ-F-bugblock-orientation-only.md) | Functional | Must-have | Approved | BUG-block determines model orientation only; flight direction derives from player yaw |
| [REQ-F-controller-deadzone](requirements/REQ-F-controller-deadzone.md) | Functional | Must-have | Approved | Stick values below deadzone threshold produce exactly 0 input |
| [REQ-F-keyboard-controller-parity](requirements/REQ-F-keyboard-controller-parity.md) | Functional | Must-have | Approved | Equal logical inputs from keyboard and gamepad produce identical movement |
| [REQ-MNT-hovercraft-controller-class](requirements/REQ-MNT-hovercraft-controller-class.md) | Maintainability | Must-have | Approved | Pure HovercraftController class with no Fabric/MC dependencies |
| [REQ-MNT-ship-entity-delegates](requirements/REQ-MNT-ship-entity-delegates.md) | Maintainability | Must-have | Approved | ShipEntity.tick() delegates all flight computation to HovercraftController |
| [REQ-MNT-flight-behavior-test-suite](requirements/REQ-MNT-flight-behavior-test-suite.md) | Maintainability | Must-have | Approved | JUnit 5 test suite implementing tests A–J against real HovercraftController |
| [REQ-REL-no-regression](requirements/REQ-REL-no-regression.md) | Reliability | Must-have | Approved | All pre-existing tests pass; no new exceptions; tick time within 1ms of baseline |
| [REQ-COMP-fabric-api-compatibility](requirements/REQ-COMP-fabric-api-compatibility.md) | Compliance | Must-have | Approved | All APIs used must be in Fabric 1.21.1 surface; ./gradlew build must succeed |
| [REQ-PERF-controller-tick-budget](requirements/REQ-PERF-controller-tick-budget.md) | Performance | Should-have | Approved | HovercraftController.tick() completes in < 0.5 ms; 10 vehicles < 5 ms total |

---

## Assumptions Index

| File | Category | Status | Risk | Summary |
|------|----------|--------|------|---------|
| [ASM-player-yaw-server-accessible](assumptions/ASM-player-yaw-server-accessible.md) | Technology | Verified | High | Player yaw is accessible server-side via pilot UUID lookup + ServerPlayer.getYRot() |
| [ASM-stable-tick-rate](assumptions/ASM-stable-tick-rate.md) | Environment | Unverified | Medium | Server runs at stable 20 TPS; tick-based physics values are deterministic |
| [ASM-controller-input-stack-reusable](assumptions/ASM-controller-input-stack-reusable.md) | Technology | Unverified | Medium | Existing HelmInputClient/ControllerInput can be adapted to 3-axis model without full rewrite |

---

## Constraints Index

| File | Category | Status | Summary |
|------|----------|--------|---------|
| [CON-fabric-minecraft-1-21-1](constraints/CON-fabric-minecraft-1-21-1.md) | Technical | Active | Mod must target Fabric for Minecraft 1.21.1 only |
| [CON-server-authoritative-physics](constraints/CON-server-authoritative-physics.md) | Technical | Active | Physics runs server-side; client sends input payloads only |
| [CON-preserve-existing-systems](constraints/CON-preserve-existing-systems.md) | Business | Active | Assembly, builder, fuel, mounting must remain functional |
