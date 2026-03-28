# Fix B1 (Acceleration) + B3 (Fuel Consumption) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add gradual acceleration to the HovercraftController (instead of instant velocity snap) and make fuel consumption work with the new hovercraft input path.

**Architecture:** The controller currently sets velocity instantly to `maxSpeed * inputAxis`. Instead, it should blend from current velocity toward the target using an acceleration rate. Fuel consumption currently only checks the old `inputForward` field in `ShipEntity.updatePhysics()` — it needs to check the new hovercraft input fields. We also add a `fuelConsumptionRate` to `HovercraftState` so the controller can report whether fuel should be consumed.

**Tech Stack:** Java 21, JUnit 5, Fabric 1.21.1

---

## Task 1: Add Acceleration to HovercraftController

**Files:**
- Modify: `src/main/java/dev/sharkengine/ship/HovercraftController.java:12,36-48`
- Test: `src/test/java/dev/sharkengine/ship/HovercraftControllerTest.java`

### Step 1: Write the failing test — acceleration is gradual, not instant

Add a new `@Nested` class in `HovercraftControllerTest.java` after the existing Test G class:

```java
@Nested
@DisplayName("Acceleration — velocity ramps up gradually")
class Acceleration {

    @Test
    @DisplayName("First tick of forward input does not reach max speed")
    void firstTick_doesNotReachMaxSpeed() {
        var input = new HovercraftInput(1f, 0f, 0f, 0f);
        var state = atRest();

        var output = controller.tick(input, state);

        float maxSpeed = WeightCategory.LIGHT.getMaxSpeed() / 20.0f;
        float outputSpeed = (float) Math.sqrt(
                output.newVelX() * output.newVelX() + output.newVelZ() * output.newVelZ());

        assertTrue(outputSpeed < maxSpeed * 0.5f,
                "First tick should be well below max speed, was " + outputSpeed + " vs max " + maxSpeed);
        assertTrue(outputSpeed > 0.001f,
                "First tick should produce some movement");
    }

    @Test
    @DisplayName("Speed increases over multiple ticks toward max")
    void multiTick_speedIncreases() {
        var input = new HovercraftInput(1f, 0f, 0f, 0f);
        var state = atRest();

        float prevSpeed = 0f;
        for (int tick = 0; tick < 20; tick++) {
            var output = controller.tick(input, state);
            float speed = (float) Math.sqrt(
                    output.newVelX() * output.newVelX() + output.newVelZ() * output.newVelZ());
            assertTrue(speed >= prevSpeed - 0.001f,
                    "Speed should increase or stay at tick " + tick);
            prevSpeed = speed;
            state = new HovercraftState(output.newVelX(), output.newVelY(), output.newVelZ(),
                    WeightCategory.LIGHT, 100f);
        }
        float maxSpeed = WeightCategory.LIGHT.getMaxSpeed() / 20.0f;
        assertTrue(prevSpeed > maxSpeed * 0.8f,
                "After 20 ticks should be near max speed, was " + prevSpeed);
    }
}
```

### Step 2: Run test to verify it fails

Run: `cd sharkengine && ./gradlew test --tests "dev.sharkengine.ship.HovercraftControllerTest\$Acceleration" -v`
Expected: FAIL — first tick already reaches max speed

### Step 3: Implement acceleration in HovercraftController

In `HovercraftController.java`, add the constant:

```java
private static final float ACCELERATION_RATE = 0.15f;
```

Replace the instant-velocity block (lines 36-48):

```java
// Old:
newVelX = hx * maxSpeed;
newVelY = input.moveVertical() * maxSpeed;
newVelZ = hz * maxSpeed;

// New — blend from current velocity toward target:
float targetVelX = hx * maxSpeed;
float targetVelY = input.moveVertical() * maxSpeed;
float targetVelZ = hz * maxSpeed;

newVelX = state.velX() + (targetVelX - state.velX()) * ACCELERATION_RATE;
newVelY = state.velY() + (targetVelY - state.velY()) * ACCELERATION_RATE;
newVelZ = state.velZ() + (targetVelZ - state.velZ()) * ACCELERATION_RATE;
```

### Step 4: Run test to verify it passes

Run: `cd sharkengine && ./gradlew test --tests "dev.sharkengine.ship.HovercraftControllerTest" -v`
Expected: ALL PASS (including existing tests)

**Important:** Existing Test G (deceleration) may need adjustment — the deceleration path uses `applyFriction()` which is separate from acceleration and should still work. Verify.

### Step 5: Commit

```bash
git add src/main/java/dev/sharkengine/ship/HovercraftController.java \
        src/test/java/dev/sharkengine/ship/HovercraftControllerTest.java
git commit -m "Fix: add gradual acceleration to HovercraftController (B1)"
```

---

## Task 2: Fix Fuel Consumption for Hovercraft Path

**Files:**
- Modify: `src/main/java/dev/sharkengine/ship/ShipEntity.java:670-710`
- Test: `src/test/java/dev/sharkengine/ship/FuelSystemTest.java` (existing — verify still passes)

### Step 1: Read the existing fuel consumption code

The existing `updatePhysics()` in `ShipEntity.java` (around line 670) has:

```java
if (inputForward > 0 && fuelLevel > 0) {
    // accelerate + consume fuel
} else {
    // decelerate
}
```

This only checks `inputForward` (the old field). The hovercraft path sets `hcMoveForward`/`hcMoveStrafe`/`hcMoveVertical` instead.

### Step 2: Modify the fuel consumption check

Replace the `inputForward > 0` condition with one that also covers hovercraft input:

```java
boolean hasInput = inputForward > 0
        || hcMoveForward != 0.0f
        || hcMoveStrafe != 0.0f
        || hcMoveVertical != 0.0f;

if (hasInput && fuelLevel > 0) {
```

The fuel consumption rate can stay tied to `AccelerationPhase` for now — the hovercraft controller doesn't use phases, but a flat rate of 1 energy/sec is reasonable for the MVP. The `accelerationTicks` counter should increment when any hovercraft input is active:

After the `hasInput` check, inside the block:

```java
if (hasInput && fuelLevel > 0) {
    accelerationTicks++;
    AccelerationPhase phase = ShipPhysics.calculatePhase(accelerationTicks);
    // ... existing fuel consumption code using phase ...
}
```

### Step 3: Run existing fuel tests

Run: `cd sharkengine && ./gradlew test --tests "dev.sharkengine.ship.FuelSystemTest" -v`
Expected: ALL PASS (these test FuelSystem directly, not ShipEntity integration)

### Step 4: Run full test suite

Run: `cd sharkengine && ./gradlew test -v`
Expected: ALL PASS

### Step 5: Commit

```bash
git add src/main/java/dev/sharkengine/ship/ShipEntity.java
git commit -m "Fix: fuel consumption works with hovercraft input path (B3)"
```

---

## Task 3: Cleanup Dead Code (B2 + I3)

**Files:**
- Modify: `src/main/java/dev/sharkengine/ship/ShipEntity.java`

### Step 1: Remove dead `useHovercraftInput` field

Delete the field declaration:
```java
private boolean useHovercraftInput = false;
```

Delete the setter line inside `setHovercraftInputs()`:
```java
this.useHovercraftInput = true;
```

### Step 2: Run build

Run: `cd sharkengine && ./gradlew build`
Expected: BUILD SUCCESSFUL

### Step 3: Commit

```bash
git add src/main/java/dev/sharkengine/ship/ShipEntity.java
git commit -m "Refactor: remove dead useHovercraftInput flag (B2)"
```

---

## Task 4: Fix Yaw Normalization (I1)

**Files:**
- Modify: `src/main/java/dev/sharkengine/net/ModNetworking.java:54`

### Step 1: Fix the modulo to handle negative yaw

Replace:
```java
float yaw = payload.playerYaw() % 360.0f;
```

With:
```java
float yaw = ((payload.playerYaw() % 360.0f) + 360.0f) % 360.0f;
```

### Step 2: Run build

Run: `cd sharkengine && ./gradlew build`
Expected: BUILD SUCCESSFUL

### Step 3: Commit

```bash
git add src/main/java/dev/sharkengine/net/ModNetworking.java
git commit -m "Fix: normalize negative yaw values in server handler (I1)"
```

---

## Task 5: Final Verification

### Step 1: Run full build + test

Run: `cd sharkengine && ./gradlew clean build`
Expected: BUILD SUCCESSFUL, all tests pass

### Step 2: Commit task tracker update

Update `3-code/tasks.md` — add notes about B1/B3 fixes.
Update `CLAUDE.md` Current State to note the fixes.

```bash
git add sharkengine/3-code/tasks.md sharkengine/CLAUDE.md
git commit -m "Docs: update task tracker with B1/B3 fix notes"
```
