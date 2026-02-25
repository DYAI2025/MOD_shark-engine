# Architektur-Plan: Feature 001 - Luftfahrzeug-MVP

**Feature:** 001-vertikale-bewegung  
**Phase:** Plan  
**Datum:** 2026-02-25  
**Version:** 1.0  
**Fahrzeug-Klasse:** B (AIR)

---

## 1. Zusammenfassung

Dieser Plan beschreibt die technische Umsetzung des Luftfahrzeug-MVPs mit allen erforderlichen Klassen, Erweiterungen und Integrationen.

---

## 2. Architektur-Entscheidungen

### 2.1 Design-Patterns

**State Pattern für Beschleunigungs-Phasen:**
```java
public enum AccelerationPhase {
    PHASE_1(0, 2, 5.0f, 0.2f),
    PHASE_2(2, 4, 15.0f, 0.4f),
    PHASE_3(4, 5, 20.0f, 0.6f),
    PHASE_4(5, 6, 25.0f, 0.8f),
    PHASE_5(6, -1, 30.0f, 1.0f);
    
    private final int startTick, endTick;
    private final float speed, particleIntensity;
}
```

**Strategy Pattern für Fahrzeug-Klassen:**
```java
public interface VehicleBehavior {
    float calculateSpeedPenalty(int blockCount, float height);
    int calculateFuelConsumption(AccelerationPhase phase);
    void spawnParticles(Level level, BlockPos pos, AccelerationPhase phase);
}

public class AirVehicleBehavior implements VehicleBehavior {
    // Implementierung für Luftfahrzeuge
}
```

**Component Pattern für Schiff-Systeme:**
```java
public class ShipEntity extends Entity {
    private ShipPhysics physics;
    private FuelSystem fuelSystem;
    private VehicleClass vehicleClass;
    // ...
}
```

### 2.2 Datenfluss

```
┌─────────────────────────────────────────────────────────────┐
│ Client (HelmInputClient)                                    │
│ • Erfasst Input (WASD, Leertaste, Shift)                   │
│ • Sendet HelmInputC2SPayload an Server                     │
└─────────────────────────────────────────────────────────────┘
                          ↓ (Network)
┌─────────────────────────────────────────────────────────────┐
│ Server (ShipEntity)                                         │
│ • Empfängt Input                                            │
│ • ShipPhysics.berechnen()                                   │
│   - Beschleunigung (5 Phasen)                               │
│   - Gewicht (Block-Anzahl)                                  │
│   - Höhe (Penalty ab Y=200)                                 │
│ • FuelSystem.consume()                                      │
│ • Position updaten                                          │
└─────────────────────────────────────────────────────────────┘
                          ↓ (Network)
┌─────────────────────────────────────────────────────────────┐
│ Client (ShipEntityRenderer)                                 │
│ • Empfängt ShipBlueprintS2CPayload                         │
│ • Rendert Schiff mit Blöcken                                │
│ • SpawnPartikel (Rauch/Flammen)                             │
│ • Spielt Sound (Düsengeräusch)                              │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 Daten-Modell

**ShipEntity - Felder:**
```java
// Basis-Informationen
private VehicleClass vehicleClass;      // AIR für MVP
private ShipBlueprint blueprint;        // Block-Positionen
private int blockCount;                 // Anzahl Blöcke

// Physik
private AccelerationPhase phase;        // 1-5
private float currentSpeed;             // Aktuelle Geschwindigkeit
private float maxSpeed;                 // Maximal möglich (durch Gewicht)
private float heightPenalty;            // 0.4-1.0

// Treibstoff
private int fuelLevel;                  // 0-100+
private boolean engineOut;              // Bei 0 Energie

// Input
private float inputVertical;            // -1..+1 (Leertaste/Shift)
private float inputForward;             // 0..1 (W-Taste)
private float inputTurn;                // -1..+1 (A/D)
```

---

## 3. Zu implementierende Klassen

### 3.1 Neue Klassen (7)

#### 1. `VehicleClass.java` (Enum)
**Pfad:** `src/main/java/dev/sharkengine/ship/VehicleClass.java`  
**Größe:** ~30 Zeilen  
**Komplexität:** Niedrig

```java
public enum VehicleClass {
    WATER("Wasser", VehicleType.BOAT),
    AIR("Luft", VehicleType.AIRCRAFT),
    LAND("Land", VehicleType.VEHICLE);
    
    private final String displayName;
    private final VehicleType type;
    
    // Constructor, Getter
}
```

#### 2. `AccelerationPhase.java` (Enum)
**Pfad:** `src/main/java/dev/sharkengine/ship/AccelerationPhase.java`  
**Größe:** ~40 Zeilen  
**Komplexität:** Niedrig

```java
public enum AccelerationPhase {
    PHASE_1(0, 40, 5.0f, 0.2f, ParticleTypes.CAMPFIRE_COSY_SMOKE),
    PHASE_2(40, 80, 15.0f, 0.4f, ParticleTypes.CAMPFIRE_COSY_SMOKE),
    PHASE_3(80, 100, 20.0f, 0.6f, ParticleTypes.FLAME),
    PHASE_4(100, 120, 25.0f, 0.8f, ParticleTypes.FLAME),
    PHASE_5(120, -1, 30.0f, 1.0f, ParticleTypes.FLAME);
    
    private final int startTick, endTick;
    private final float speed, particleIntensity;
    private final ParticleOptions particleType;
    
    // Constructor, Getter, fromTick() static method
}
```

#### 3. `ShipPhysics.java`
**Pfad:** `src/main/java/dev/sharkengine/ship/ShipPhysics.java`  
**Größe:** ~150 Zeilen  
**Komplexität:** Mittel

```java
public final class ShipPhysics {
    
    // Berechnet Maximalgeschwindigkeit basierend auf Gewicht
    public static float calculateMaxSpeed(int blockCount) {
        if (blockCount <= 20) return 30.0f;
        if (blockCount <= 40) return 20.0f;
        if (blockCount <= 60) return 10.0f;
        return 0.0f; // Zu schwer
    }
    
    // Berechnet Höhen-Penalty
    public static float calculateHeightPenalty(float yPos) {
        if (yPos < 100) return 1.0f;
        if (yPos < 150) return 0.8f;
        if (yPos < 200) return 0.6f;
        return 0.4f;
    }
    
    // Berechnet aktuelle Beschleunigungs-Phase
    public static AccelerationPhase calculatePhase(int ticks) {
        return AccelerationPhase.fromTick(ticks);
    }
    
    // Berechnet Treibstoff-Verbrauch
    public static int calculateFuelConsumption(AccelerationPhase phase) {
        return switch(phase) {
            case PHASE_1, PHASE_2 -> 1;
            case PHASE_3, PHASE_4 -> 2;
            case PHASE_5 -> 3;
        };
    }
    
    // Prüft Kollision
    public static boolean checkCollision(Level level, BlockPos pos) {
        return !level.getBlockState(pos).isAir();
    }
}
```

#### 4. `FuelSystem.java`
**Pfad:** `src/main/java/dev/sharkengine/ship/FuelSystem.java`  
**Größe:** ~80 Zeilen  
**Komplexität:** Niedrig

```java
public final class FuelSystem {
    private static final int ENERGY_PER_WOOD = 100;
    
    // Konvertiert Holzblöcke zu Energie
    public static int woodToEnergy(int woodCount) {
        return woodCount * ENERGY_PER_WOOD;
    }
    
    // Konvertiert Energie zu Holzblöcken (für UI)
    public static float energyToWood(float energy) {
        return energy / ENERGY_PER_WOOD;
    }
    
    // Berechnet verbleibende Flugzeit
    public static int calculateRemainingFlightTime(int fuelLevel, AccelerationPhase phase) {
        int consumption = ShipPhysics.calculateFuelConsumption(phase);
        return fuelLevel / consumption;
    }
    
    // Formatiert Treibstoff-Anzeige
    public static String formatFuelDisplay(int fuelLevel, int maxFuel) {
        int percent = (fuelLevel * 100) / maxFuel;
        int bars = percent / 10;
        return "§eTreibstoff: [" + 
               "§a" + "█".repeat(bars) + 
               "§7" + "░".repeat(10 - bars) + 
               "§e] " + percent + "%";
    }
}
```

#### 5. `WeightCategory.java` (Enum)
**Pfad:** `src/main/java/dev/sharkengine/ship/WeightCategory.java`  
**Größe:** ~30 Zeilen  
**Komplexität:** Niedrig

```java
public enum WeightCategory {
    LIGHT(0, 20, 30.0f, null),
    MEDIUM(21, 40, 20.0f, null),
    HEAVY(41, 60, 10.0f, "§eAchtung: Schiff wird langsam"),
    OVERLOADED(61, Integer.MAX_VALUE, 0.0f, "§c⚠️ Zu schwer zum Fliegen!");
    
    private final int min, max;
    private final float maxSpeed;
    private final String warning;
    
    // Constructor, Getter, fromBlockCount() static method
}
```

#### 6. `FuelHudOverlay.java` (Client-only)
**Pfad:** `src/client/java/dev/sharkengine/client/render/FuelHudOverlay.java`  
**Größe:** ~100 Zeilen  
**Komplexität:** Mittel

```java
@OnlyIn(Dist.CLIENT)
public final class FuelHudOverlay {
    
    public static void render(GuiGraphics graphics, Minecraft minecraft) {
        if (!(minecraft.getCameraEntity() instanceof Player player)) return;
        if (!(player.getVehicle() instanceof ShipEntity ship)) return;
        
        int fuel = ship.getFuelLevel();
        int blocks = ship.getBlockCount();
        float speed = ship.getCurrentSpeed();
        float height = player.getY();
        
        // Render HUD bei (10, 10)
        renderFuelBar(graphics, 10, 10, fuel);
        renderStats(graphics, 10, 30, blocks, speed, height);
        renderWarning(graphics, 10, 80, WeightCategory.fromBlockCount(blocks));
    }
    
    private static void renderFuelBar(GuiGraphics g, int x, int y, int fuel) {
        // Balken-Darstellung
    }
    
    private static void renderStats(GuiGraphics g, int x, int y, int blocks, float speed, float height) {
        // Text: Höhe, Geschwindigkeit, Gewicht
    }
    
    private static void renderWarning(GuiGraphics g, int x, int y, WeightCategory category) {
        if (category.getWarning() != null) {
            // Warnung anzeigen
        }
    }
}
```

#### 7. `ModSounds.java`
**Pfad:** `src/main/java/dev/sharkengine/content/ModSounds.java`  
**Größe:** ~40 Zeilen  
**Komplexität:** Niedrig

```java
public final class ModSounds {
    public static final Holder<SoundEvent> THRUSTER_IDLE = 
        registerSound("entity.ship.thruster_idle");
    public static final Holder<SoundEvent> THRUSTER_ACTIVE = 
        registerSound("entity.ship.thruster_active");
    
    private static Holder<SoundEvent> registerSound(String name) {
        SoundEvent event = SoundEvent.createVariableRangeEvent(ResourceLocation.parse(name));
        return BuiltinRegistries.register(BuiltInRegistries.SOUND_EVENT, 
                                          ResourceLocation.parse(name), 
                                          event);
    }
    
    public static void init() {}
}
```

---

### 3.2 Zu erweiternde Klassen (5)

#### 1. `ShipEntity.java` - Erweiterungen

**Neue Felder:**
```java
// Fahrzeug-Klasse
private VehicleClass vehicleClass = VehicleClass.AIR;

// Physik
private AccelerationPhase phase = AccelerationPhase.PHASE_1;
private int accelerationTicks = 0;
private float currentSpeed = 0.0f;
private float maxSpeed = 30.0f;
private float heightPenalty = 1.0f;

// Gewicht
private int blockCount = 0;
private WeightCategory weightCategory = WeightCategory.LIGHT;

// Treibstoff
private int fuelLevel = 100;
private boolean engineOut = false;

// Input
private float inputForward = 0.0f;
```

**Neue Methoden:**
```java
// Getter/Setter
public int getFuelLevel() { return fuelLevel; }
public int getBlockCount() { return blockCount; }
public float getCurrentSpeed() { return currentSpeed; }
public AccelerationPhase getPhase() { return phase; }

// Update-Logik
private void updatePhysics() {
    // Beschleunigung berechnen
    accelerationTicks++;
    phase = ShipPhysics.calculatePhase(accelerationTicks);
    
    // Max-Geschwindigkeit durch Gewicht
    maxSpeed = ShipPhysics.calculateMaxSpeed(blockCount);
    weightCategory = WeightCategory.fromBlockCount(blockCount);
    
    // Höhen-Penalty
    heightPenalty = ShipPhysics.calculateHeightPenalty(this.getY());
    
    // Aktuelle Geschwindigkeit
    float targetSpeed = maxSpeed * heightPenalty * (phase.speed() / 30.0f);
    currentSpeed = Mth.lerp(0.1f, currentSpeed, targetSpeed);
    
    // Treibstoff verbrauchen
    if (!engineOut) {
        int consumption = ShipPhysics.calculateFuelConsumption(phase);
        fuelLevel -= consumption;
        if (fuelLevel <= 0) {
            engineOut = true;
            fuelLevel = 0;
        }
    }
    
    // Bewegung anwenden
    if (!engineOut && inputForward > 0) {
        // Vorwärts-Bewegung mit currentSpeed
    }
    
    // Kollision prüfen
    if (ShipPhysics.checkCollision(level, blockPosition())) {
            setDeltaMovement(Vec3.ZERO);
    }
}

// NBT Serialization
@Override
protected void addAdditionalSaveData(CompoundTag compound) {
    super.addAdditionalSaveData(compound);
    compound.putInt("FuelLevel", fuelLevel);
    compound.putInt("BlockCount", blockCount);
    compound.putInt("AccelerationTicks", accelerationTicks);
    compound.putFloat("CurrentSpeed", currentSpeed);
    compound.putBoolean("EngineOut", engineOut);
}

@Override
protected void readAdditionalSaveData(CompoundTag compound) {
    super.readAdditionalSaveData(compound);
    fuelLevel = compound.getInt("FuelLevel");
    blockCount = compound.getInt("BlockCount");
    accelerationTicks = compound.getInt("AccelerationTicks");
    currentSpeed = compound.getFloat("CurrentSpeed");
    engineOut = compound.getBoolean("EngineOut");
}
```

#### 2. `ShipBlueprint.java` - Erweiterungen

**Neue Felder:**
```java
private final int blockCount;  // Anzahl Blöcke im Schiff
```

**Neue Methoden:**
```java
public int getBlockCount() { return blockCount; }

// In toNbt():
tag.putInt("BlockCount", blockCount);

// In fromNbt():
int blockCount = tag.getInt("BlockCount");
```

#### 3. `HelmInputClient.java` - Erweiterungen

**Neuer Input:**
```java
// In tick()-Methode:
LocalPlayer player = client.player;
if (player == null) return;
if (!(player.getVehicle() instanceof ShipEntity)) return;

// Forward Input (W-Taste für Beschleunigung)
float forward = 0;
if (client.options.keyUp.isDown()) forward = 1.0f;

// Vertical Input (Leertaste/Shift)
float vertical = 0;
if (client.options.keyJump.isDown()) vertical = 1.0f;      // Leertaste
if (client.options.keyShift.isDown()) vertical = -1.0f;    // Shift

// Senden wenn geändert
if (forward != lastForward || vertical != lastVertical || turn != lastTurn) {
    ClientPlayNetworking.send(new HelmInputC2SPayload(throttle, turn, forward));
}
```

#### 4. `HelmInputC2SPayload.java` - Erweiterungen

**Neue Felder:**
```java
private final float forward;  // 0..1 (Beschleunigung)

// Constructor aktualisieren
public record HelmInputC2SPayload(float throttle, float turn, float forward) implements CustomPacketPayload {
    // ...
}
```

**Codec aktualisieren:**
```java
public static final StreamCodec<RegistryFriendlyByteBuf, HelmInputC2SPayload> CODEC =
    StreamCodec.composite(
        ByteBufCodecs.FLOAT, HelmInputC2SPayload::throttle,
        ByteBufCodecs.FLOAT, HelmInputC2SPayload::turn,
        ByteBufCodecs.FLOAT, HelmInputC2SPayload::forward,  // NEU
        HelmInputC2SPayload::new
    );
```

#### 5. `ShipEntityRenderer.java` - Erweiterungen

**Partikel-Effekte:**
```java
@Override
public void render(ShipEntity entity, float entityYaw, float partialTick,
                   PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
    super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    
    // Partikel spawnen (Client-seitig)
    if (entity.level().isClientSide()) {
        spawnThrusterParticles(entity);
    }
    
    // ... existierendes Block-Rendering
}

private void spawnThrusterParticles(ShipEntity entity) {
    AccelerationPhase phase = entity.getPhase();
    ParticleOptions particleType = phase.particleType();
    float intensity = phase.particleIntensity();
    
    // Partikel-Position (unter dem Schiff)
    double x = entity.getX();
    double y = entity.getY() - 0.5;
    double z = entity.getZ();
    
    // Anzahl Partikel basierend auf Phase
    int count = (int)(phase.ordinal() + 1);
    
    Level level = entity.level();
    Random random = level.random;
    
    for (int i = 0; i < count; i++) {
        double offsetX = (random.nextDouble() - 0.5) * 2;
        double offsetZ = (random.nextDouble() - 0.5) * 2;
        
        level.addParticle(
            particleType,
            x + offsetX, y, z + offsetZ,
            0, 0.05 * intensity, 0  // Langsam aufsteigend
        );
    }
    
    // Sound abspielen (geduldet)
    if (random.nextInt(20) == 0) {  // Alle ~1 Sekunde
        float volume = 0.3f * intensity;
        float pitch = 0.8f + (random.nextFloat() * 0.4f);
        level.playSound(
            entity.getX(), entity.getY(), entity.getZ(),
            ModSounds.THRUSTER_ACTIVE.value(),
            SoundSource.BLOCKS,
            volume,
            pitch
        );
    }
}
```

#### 6. `ModNetworking.java` - Erweiterungen

**Payload-Handler aktualisieren:**
```java
ServerPlayNetworking.registerGlobalReceiver(HelmInputC2SPayload.TYPE, (payload, ctx) -> {
    ServerPlayer sp = ctx.player();
    ctx.server().execute(() -> {
        if (!(sp.getVehicle() instanceof ShipEntity ship)) return;
        if (!ship.isPilot(sp)) return;
        
        // Inputs setzen
        ship.setInputs(payload.throttle(), payload.turn());
        ship.setInputForward(payload.forward());  // NEU
    });
});
```

---

## 4. Wiederverwendung existierenden Codes

### 4.1 Wiederverwendbare Komponenten

| Komponente | Wiederverwendung | Anpassung |
|------------|------------------|-----------|
| `ShipEntity` | Basis-Klasse | +10 Felder, +5 Methoden |
| `ShipBlueprint` | NBT-Serialization | +1 Feld (blockCount) |
| `HelmInputClient` | Input-Erfassung | +2 Inputs (forward, vertical) |
| `HelmInputC2SPayload` | Network-Payload | +1 Feld (forward) |
| `ShipEntityRenderer` | Block-Rendering | +Partikel, +Sound |
| `ModNetworking` | Packet-Handler | +1 Feld im Payload |
| `SteeringWheelBlock` | Ship-Assembly | Keine Änderung |

### 4.2 Code-Reuse Analyse

**Geschätzte Wiederverwendung:** ~60%  
**Neuer Code:** ~40% (7 neue Klassen + Erweiterungen)

---

## 5. Risiko-Analyse

### 5.1 Technische Risiken

| Risiko | Wahrscheinlichkeit | Auswirkung | Mitigation |
|--------|-------------------|------------|------------|
| Performance durch Partikel | Mittel | Mittel | Partikel-Limit (50/Tick) |
| Multiplayer-Desync | Niedrig | Hoch | Server-authoritative Physik |
| Balance-Probleme (Gewicht) | Mittel | Mittel | Playtesting, Config-Werte |
| Sound-Loop Probleme | Niedrig | Niedrig | Geduldet alle 20 Ticks |
| NBT-Korruption bei Update | Niedrig | Hoch | Default-Werte bei missing data |

### 5.2 Abhängigkeits-Risiken

| Abhängigkeit | Status | Risiko |
|--------------|--------|--------|
| Fabric API 0.102.0+1.21.1 | Vorhanden | Niedrig |
| Minecraft 1.21.1 | Vorhanden | Niedrig |
| Sound-Dateien | ❌ Fehlend | Mittel |

---

## 6. Implementierungs-Strategie

### 6.1 Phasen-Plan

**Phase 1: Grundgerüst (Tasks 1-8)**
- VehicleClass, AccelerationPhase, WeightCategory Enums
- ShipPhysics, FuelSystem Utility-Klassen
- ShipEntity Erweiterungen (Felder, Getter)

**Phase 2: Input & Network (Tasks 9-14)**
- HelmInputClient erweitern
- HelmInputC2SPayload erweitern
- ModNetworking aktualisieren

**Phase 3: Physik & Logik (Tasks 15-20)**
- ShipEntity.updatePhysics() implementieren
- Beschleunigungs-Logik
- Gewichts-System
- Treibstoff-Verbrauch

**Phase 4: Visuelle Effekte (Tasks 21-25)**
- ShipEntityRenderer Partikel
- ModSounds implementieren
- FuelHudOverlay (UI)

**Phase 5: Testing & Polish (Tasks 26-30)**
- Unit-Tests für ShipPhysics
- Integration-Tests
- Balance-Anpassungen
- Performance-Optimierung

### 6.2 Test-Strategie

**Unit-Tests (ShipPhysicsTest.java):**
```java
@Test
void testCalculateMaxSpeed() {
    assertEquals(30.0f, ShipPhysics.calculateMaxSpeed(15));
    assertEquals(20.0f, ShipPhysics.calculateMaxSpeed(35));
    assertEquals(10.0f, ShipPhysics.calculateMaxSpeed(55));
    assertEquals(0.0f, ShipPhysics.calculateMaxSpeed(65));
}

@Test
void testCalculateHeightPenalty() {
    assertEquals(1.0f, ShipPhysics.calculateHeightPenalty(50));
    assertEquals(0.8f, ShipPhysics.calculateHeightPenalty(125));
    assertEquals(0.6f, ShipPhysics.calculateHeightPenalty(175));
    assertEquals(0.4f, ShipPhysics.calculateHeightPenalty(225));
}
```

**Integration-Tests:**
- Beschleunigung über 6 Sekunden
- Treibstoff-Verbrauch pro Phase
- Gewichts-Limits (20/40/60 Blöcke)

---

## 7. Dateiliste (Zusammenfassung)

### 7.1 Neue Dateien (7)
1. `VehicleClass.java` (~30 Zeilen)
2. `AccelerationPhase.java` (~40 Zeilen)
3. `ShipPhysics.java` (~150 Zeilen)
4. `FuelSystem.java` (~80 Zeilen)
5. `WeightCategory.java` (~30 Zeilen)
6. `FuelHudOverlay.java` (~100 Zeilen, Client-only)
7. `ModSounds.java` (~40 Zeilen)

### 7.2 Zu erweiternde Dateien (6)
1. `ShipEntity.java` (+~150 Zeilen)
2. `ShipBlueprint.java` (+~20 Zeilen)
3. `HelmInputClient.java` (+~30 Zeilen)
4. `HelmInputC2SPayload.java` (+~20 Zeilen)
5. `ShipEntityRenderer.java` (+~80 Zeilen)
6. `ModNetworking.java` (+~10 Zeilen)

### 7.3 Ressourcen-Dateien (2)
1. `assets/sharkengine/sounds/entity/ship/thruster_idle.ogg` (neu)
2. `assets/sharkengine/sounds/entity/ship/thruster_active.ogg` (neu)

---

## 8. Nächste Schritte

1. ✅ Architektur-Plan erstellt
2. ⏳ Zu `/tasks` Phase übergehen
3. ⏳ Task-Liste mit ~30 Tasks erstellen
4. ⏳ TDD-Reihenfolge festlegen (Tests zuerst)

---

**Plan genehmigt von:** _______________  
**Datum:** _______________  
**Bereit für Tasks:** ☐ Ja  ☐ Nein
