# DEC-gamepad-triggers-for-vertical: Trail

> Companion to `DEC-gamepad-triggers-for-vertical.md`.

## Alternatives considered

### Option A: Triggers LT/RT (chosen)
- Pros: Analog input, intuitive (trigger = throttle metaphor), leaves right stick free
- Cons: Simultaneous vertical + horizontal requires finger on trigger + stick

### Option B: Right stick Y axis
- Pros: Analog, familiar from flight sims
- Cons: Occupies right stick (needed for potential camera control in future)

### Option C: Bumpers LB/RB
- Pros: Easy to reach
- Cons: Binary only (no analog), poor vertical precision

## Reasoning

Triggers provide the best balance of analog precision and ergonomics. The right stick is intentionally left unassigned for future use (camera, model rotation). This is a convention decision that can be revisited without impacting the flight controller.

## Human involvement

**Type**: ai-proposed/human-approved

**Notes**: Proposed during API design, approved as part of design review.

## Changelog

| Date | Change | Involvement |
|------|--------|-------------|
| 2026-03-28 | Initial decision | ai-proposed/human-approved |
