# Task-Breakdown: Feature 001 - Luftfahrzeug-MVP

**Feature:** 001-vertikale-bewegung  
**Phase:** Tasks  
**Datum:** 2026-02-25  
**Gesamt-Tasks:** 32  
**Geschätzte Dauer:** ~16 Stunden  
**Fahrzeug-Klasse:** B (AIR)

---

## Task-Übersicht

| Phase | Tasks | Geschätzte Zeit | Status |
|-------|-------|-----------------|--------|
| **1. Grundgerüst** | 1-8 | 4h | ⏳ Pending |
| **2. Input & Network** | 9-14 | 3h | ⏳ Pending |
| **3. Physik & Logik** | 15-20 | 4h | ⏳ Pending |
| **4. Visuelle Effekte** | 21-25 | 3h | ⏳ Pending |
| **5. Testing & Polish** | 26-32 | 2h | ⏳ Pending |

---

## Task-Liste

### Phase 1: Grundgerüst (Tasks 1-8)

#### Task 1.1: VehicleClass.java Enum erstellen
- **Priorität:** Hoch
- **Geschätzt:** 30 min
- **Abhängigkeiten:** Keine
- **TDD:** Test nicht erforderlich (Enum)
- **Definition of Done:**
  - [ ] Enum mit WATER, AIR, LAND
  - [ ] displayName Feld
  - [ ] Constructor + Getter
  - [ ] Javadoc

```java
// Erwartete Implementierung
public enum VehicleClass {
    WATER("Wasser"),
    AIR("Luft"),
    LAND("Land");
    
    private final String displayName;
    
    VehicleClass(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
```

---

#### Task 1.2: AccelerationPhase.java Enum erstellen
- **Priorität:** Hoch
- **Geschätzt:** 45 min
- **Abhängigkeiten:** Keine
- **TDD:** Test nicht erforderlich (Enum)
- **Definition of Done:**
  - [ ] 5 Phasen (PHASE_1 bis PHASE_5)
  - [ ] Felder: startTick, endTick, speed, particleIntensity, particleType
  - [ ] Static method `fromTick(int ticks)`
  - [ ] Javadoc

```java
// Erwartete Implementierung
public enum AccelerationPhase {
    PHASE_1(0, 40, 5.0f, 0.2f, ParticleTypes.CAMPFIRE_COSY_SMOKE),
    PHASE_2(40, 80, 15.0f, 0.4f, ParticleTypes.CAMPFIRE_COSY_SMOKE),
    PHASE_3(80, 100, 20.0f, 0.6f, ParticleTypes.FLAME),
    PHASE_4(100, 120, 25.0f, 0.8f, ParticleTypes.FLAME),
    PHASE_5(120, -1, 30.0f, 1.0f, ParticleTypes.FLAME);
    
    // Constructor, Getter, fromTick()
}
```

---

#### Task 1.3: WeightCategory.java Enum erstellen
- **Priorität:** Hoch
- **Geschätzt:** 30 min
- **Abhängigkeiten:** Keine
- **TDD:** Test nicht erforderlich (Enum)
- **Definition of Done:**
  - [ ] 4 Kategorien (LIGHT, MEDIUM, HEAVY, OVERLOADED)
  - [ ] Felder: min, max, maxSpeed, warning
  - [ ] Static method `fromBlockCount(int count)`
  - [ ] Warnungen für HEAVY und OVERLOADED

```java
// Erwartete Implementierung
public enum WeightCategory {
    LIGHT(0, 20, 30.0f, null),
    MEDIUM(21, 40, 20.0f, null),
    HEAVY(41, 60, 10.0f, "§eAchtung: Schiff wird langsam"),
    OVERLOADED(61, Integer.MAX_VALUE, 0.0f, "§c⚠️ Zu schwer zum Fliegen!");
    
    // Constructor, Getter, fromBlockCount()
}
```

---

#### Task 1.4: ShipPhysics.java - Grundgerüst
- **Priorität:** Hoch
- **Geschätzt:** 1h
- **Abhängigkeiten:** Task 1.2, 1.3
- **TDD:** ShipPhysicsTest.java erstellen
- **Definition of Done:**
  - [ ] Klasse mit privaten Constructor (Utility)
  - [ ] Methode `calculateMaxSpeed(int blockCount)`
  - [ ] Methode `calculateHeightPenalty(float yPos)`
  - [ ] Methode `calculatePhase(int ticks)`
  - [ ] Methode `checkCollision(Level level, BlockPos pos)`
  - [ ] Unit-Tests für alle Methoden

**Tests:**
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

---

#### Task 1.5: FuelSystem.java erstellen
- **Priorität:** Hoch
- **Geschätzt:** 45 min
- **Abhängigkeiten:** Task 1.2
- **TDD:** FuelSystemTest.java erstellen
- **Definition of Done:**
  - [ ] Konstante `ENERGY_PER_WOOD = 100`
  - [ ] Methode `woodToEnergy(int woodCount)`
  - [ ] Methode `energyToWood(float energy)`
  - [ ] Methode `calculateRemainingFlightTime(int fuelLevel, AccelerationPhase phase)`
  - [ ] Methode `formatFuelDisplay(int fuelLevel, int maxFuel)`
  - [ ] Unit-Tests

**Tests:**
```java
@Test
void testWoodToEnergy() {
    assertEquals(100, FuelSystem.woodToEnergy(1));
    assertEquals(500, FuelSystem.woodToEnergy(5));
}

@Test
void testFormatFuelDisplay() {
    String display = FuelSystem.formatFuelDisplay(80, 100);
    assertTrue(display.contains("80%"));
}
```

---

#### Task 1.6: ModSounds.java erstellen
- **Priorität:** Mittel
- **Geschätzt:** 30 min
- **Abhängigkeiten:** Keine
- **TDD:** Test nicht erforderlich
- **Definition of Done:**
  - [ ] SoundEvent `THRUSTER_IDLE`
  - [ ] SoundEvent `THRUSTER_ACTIVE`
  - [ ] Private Helper-Methode `registerSound()`
  - [ ] Init-Methode (leer)

---

#### Task 1.7: Sound-Dateien bereitstellen
- **Priorität:** Mittel
- **Geschätzt:** 30 min
- **Abhängigkeiten:** Task 1.6
- **TDD:** Manueller Test
- **Definition of Done:**
  - [ ] `assets/sharkengine/sounds/entity/ship/thruster_idle.ogg` exists
  - [ ] `assets/sharkengine/sounds/entity/ship/thruster_active.ogg` exists
  - [ ] Sounds sind < 100KB
  - [ ] Sounds sind Mono, 44.1kHz

**Hinweis:** Sounds können von freesound.org oder ähnlichen Quellen stammen. Lizenz prüfen!

---

#### Task 1.8: ShipEntity.java - Felder hinzufügen
- **Priorität:** Hoch
- **Geschätzt:** 30 min
- **Abhängigkeiten:** Task 1.1, 1.2, 1.3
- **TDD:** Test nicht erforderlich (Felder)
- **Definition of Done:**
  - [ ] `vehicleClass` Feld
  - [ ] `phase` Feld
  - [ ] `accelerationTicks` Feld
  - [ ] `currentSpeed` Feld
  - [ ] `maxSpeed` Feld
  - [ ] `heightPenalty` Feld
  - [ ] `blockCount` Feld
  - [ ] `weightCategory` Feld
  - [ ] `fuelLevel` Feld
  - [ ] `engineOut` Feld
  - [ ] `inputForward` Feld
  - [ ] Getter-Methoden für alle Felder

---

### Phase 2: Input & Network (Tasks 9-14)

#### Task 2.1: HelmInputC2SPayload.java erweitern
- **Priorität:** Hoch
- **Geschätzt:** 30 min
- **Abhängigkeiten:** Keine
- **TDD:** Test nicht erforderlich
- **Definition of Done:**
  - [ ] Neues Feld `forward` (float, 0..1)
  - [ ] Constructor aktualisieren
  - [ ] CODEC aktualisieren (3 Felder)
  - [ ] Javadoc

```java
// Neuer Record
public record HelmInputC2SPayload(
    float throttle,   // -1..+1 (vertikal)
    float turn,       // -1..+1 (rotation)
    float forward     // 0..1 (Beschleunigung)
) implements CustomPacketPayload {
    // CODEC mit 3 Feldern
}
```

---

#### Task 2.2: HelmInputClient.java erweitern
- **Priorität:** Hoch
- **Geschätzt:** 45 min
- **Abhängigkeiten:** Task 2.1
- **TDD:** Test nicht erforderlich (Client-Code)
- **Definition of Done:**
  - [ ] Forward-Input erfassen (W-Taste)
  - [ ] Vertical-Input erfassen (Leertaste/Shift)
  - [ ] Payload mit 3 Parametern senden
  - [ ] Cooldown für Network-Traffic (2 Ticks)

```java
// Forward Input
float forward = client.options.keyUp.isDown() ? 1.0f : 0.0f;

// Vertical Input
float vertical = 0;
if (client.options.keyJump.isDown()) vertical = 1.0f;
if (client.options.keyShift.isDown()) vertical = -1.0f;

// Senden
ClientPlayNetworking.send(new HelmInputC2SPayload(vertical, turn, forward));
```

---

#### Task 2.3: ModNetworking.java aktualisieren
- **Priorität:** Hoch
- **Geschätzt:** 30 min
- **Abhängigkeiten:** Task 2.1
- **TDD:** Test nicht erforderlich
- **Definition of Done:**
  - [ ] Payload-Handler empfängt `forward`
  - [ ] `ship.setInputForward(payload.forward())` aufrufen

---

#### Task 2.4: ShipEntity.java - Setter-Methoden
- **Priorität:** Hoch
- **Geschätzt:** 30 min
- **Abhängigkeiten:** Task 1.8
- **TDD:** Test nicht erforderlich
- **Definition of Done:**
  - [ ] `setInputForward(float forward)`
  - [ ] `setInputVertical(float vertical)`
  - [ ] Input-Felder werden gespeichert

---

#### Task 2.5: ShipBlueprint.java erweitern
- **Priorität:** Hoch
- **Geschätzt:** 30 min
- **Abhängigkeiten:** Keine
- **TDD:** Test nicht erforderlich
- **Definition of Done:**
  - [ ] Feld `blockCount` im Constructor
  - [ ] Getter-Methode `getBlockCount()`
  - [ ] NBT: `toNbt()` schreibt blockCount
  - [ ] NBT: `fromNbt()` liest blockCount

---

#### Task 2.6: ShipAssemblyService.java aktualisieren
- **Priorität:** Hoch
- **Geschätzt:** 30 min
- **Abhängigkeiten:** Task 2.5
- **TDD:** Manueller Test
- **Definition of Done:**
  - [ ] Beim Erstellen von ShipBlueprint: blockCount übergeben
  - [ ] `new ShipBlueprint(origin, blocks, blocks.size())`

---

### Phase 3: Physik & Logik (Tasks 15-20)

#### Task 3.1: ShipEntity.java - updatePhysics() Methode
- **Priorität:** Hoch
- **Geschätzt:** 1.5h
- **Abhängigkeiten:** Task 1.4, 1.5, 2.4
- **TDD:** Integration-Test
- **Definition of Done:**
  - [ ] Beschleunigung-Ticks hochzählen
  - [ ] Phase berechnen
  - [ ] Max-Geschwindigkeit durch Gewicht
  - [ ] Höhen-Penalty berechnen
  - [ ] Aktuelle Geschwindigkeit interpolieren
  - [ ] Treibstoff verbrauchen
  - [ ] Engine-Out bei 0 Energie
  - [ ] Bewegung anwenden

```java
private void updatePhysics() {
    // Beschleunigung
    if (inputForward > 0) {
        accelerationTicks++;
    } else {
        accelerationTicks = 0;
    }
    phase = ShipPhysics.calculatePhase(accelerationTicks);
    
    // Gewicht
    maxSpeed = ShipPhysics.calculateMaxSpeed(blockCount);
    weightCategory = WeightCategory.fromBlockCount(blockCount);
    
    // Höhe
    heightPenalty = ShipPhysics.calculateHeightPenalty(this.getY());
    
    // Geschwindigkeit
    float targetSpeed = maxSpeed * heightPenalty * (phase.speed() / 30.0f);
    currentSpeed = Mth.lerp(0.1f, currentSpeed, targetSpeed);
    
    // Treibstoff
    if (!engineOut && inputForward > 0) {
        int consumption = ShipPhysics.calculateFuelConsumption(phase);
        fuelLevel -= consumption;
        if (fuelLevel <= 0) {
            engineOut = true;
            fuelLevel = 0;
        }
    }
    
    // Bewegung anwenden
    if (!engineOut && inputForward > 0) {
        // Delta-Movement setzen
    }
}
```

---

#### Task 3.2: ShipEntity.java - tick() Methode aktualisieren
- **Priorität:** Hoch
- **Geschätzt:** 30 min
- **Abhängigkeiten:** Task 3.1
- **TDD:** Integration-Test
- **Definition of Done:**
  - [ ] `updatePhysics()` in tick() aufrufen
  - [ ] Kollision prüfen
  - [ ] Bei Kollision: Bewegung stoppen

---

#### Task 3.3: ShipEntity.java - NBT Serialization
- **Priorität:** Hoch
- **Geschätzt:** 45 min
- **Abhängigkeiten:** Task 1.8
- **TDD:** Test nicht erforderlich
- **Definition of Done:**
  - [ ] `addAdditionalSaveData()` speichert alle neuen Felder
  - [ ] `readAdditionalSaveData()` lädt alle neuen Felder
  - [ ] Default-Werte bei fehlenden Daten

---

#### Task 3.4: Weight-Kategorie Warnungen
- **Priorität:** Mittel
- **Geschätzt:** 30 min
- **Abhängigkeiten:** Task 1.3
- **TDD:** Test nicht erforderlich
- **Definition of Done:**
  - [ ] Bei HEAVY (41-60): Chat-Nachricht "Achtung: Schiff wird langsam"
  - [ ] Bei OVERLOADED (61+): Chat-Nachricht "⚠️ Zu schwer zum Fliegen!"
  - [ ] Nachrichten nur einmal beim Betreten der Kategorie

---

#### Task 3.5: Fuel-Refill Mechanismus (Vorbereitung)
- **Priorität:** Niedrig
- **Geschätzt:** 30 min
- **Abhängigkeiten:** Task 1.5
- **TDD:** Test nicht erforderlich
- **Definition of Done:**
  - [ ] Methode `addFuel(int woodCount)`
  - [ ] `fuelLevel` wird erhöht (max. 100)
  - [ ] `engineOut` wird zurückgesetzt
  - [ ] Chat-Nachricht bei Refill

**Hinweis:** Manuelles Nachfüllen per Rechtsklick kommt in späterer Iteration.

---

#### Task 3.6: Unit-Tests für ShipPhysics
- **Priorität:** Hoch
- **Geschätzt:** 1h
- **Abhängigkeiten:** Task 1.4
- **TDD:** Ja (Tests zuerst!)
- **Definition of Done:**
  - [ ] ShipPhysicsTest.java erstellt
  - [ ] Test für `calculateMaxSpeed()`
  - [ ] Test für `calculateHeightPenalty()`
  - [ ] Test für `calculatePhase()`
  - [ ] Test für `checkCollision()`
  - [ ] Coverage > 85%

---

### Phase 4: Visuelle Effekte (Tasks 21-25)

#### Task 4.1: ShipEntityRenderer.java - Partikel
- **Priorität:** Hoch
- **Geschätzt:** 1h
- **Abhängigkeiten:** Task 1.2
- **TDD:** Manueller Test
- **Definition of Done:**
  - [ ] `spawnThrusterParticles()` Methode
  - [ ] Rauch-Partikel im Leerlauf
  - [ ] Flammen-Partikel bei Phase 3-5
  - [ ] Partikel-Intensität skaliert mit Phase
  - [ ] Partikel-Richtung: entgegen Flugrichtung

---

#### Task 4.2: ShipEntityRenderer.java - Sound
- **Priorität:** Hoch
- **Geschätzt:** 45 min
- **Abhängigkeiten:** Task 1.6
- **TDD:** Manueller Test
- **Definition of Done:**
  - [ ] Sound abspielen alle ~1 Sekunde
  - [ ] Lautstärke skaliert mit Phase-Intensität
  - [ ] Pitch-Variation (0.8-1.2)
  - [ ] Geduldet (nicht jeden Tick)

---

#### Task 4.3: FuelHudOverlay.java erstellen
- **Priorität:** Hoch
- **Geschätzt:** 1h
- **Abhängigkeiten:** Task 1.5
- **TDD:** Manueller Test
- **Definition of Done:**
  - [ ] Client-only Klasse
  - [ ] `render()` Methode mit GuiGraphics
  - [ ] Treibstoff-Balken: [████████░░] 80%
  - [ ] Stats: Höhe, Geschwindigkeit, Gewicht
  - [ ] Warnungen bei HEAVY/OVERLOADED
  - [ ] Position: (10, 10) im HUD

---

#### Task 4.4: FuelHudOverlay.java - Rendering integrieren
- **Priorität:** Hoch
- **Geschätzt:** 30 min
- **Abhängigkeiten:** Task 4.3
- **TDD:** Manueller Test
- **Definition of Done:**
  - [ ] Overlay in Client-Mod-Initializer registrieren
  - [ ] Render-Event abonnieren
  - [ ] Nur anzeigen wenn Spieler Schiff steuert

---

#### Task 4.5: Partikel-Limit implementieren
- **Priorität:** Mittel
- **Geschätzt:** 30 min
- **Abhängigkeiten:** Task 4.1
- **TDD:** Performance-Test
- **Definition of Done:**
  - [ ] Max. 50 Partikel pro Tick
  - [ ] Partikel-Zähler zurücksetzen nach Tick
  - [ ] Performance-Monitoring (< 0.2ms)

---

### Phase 5: Testing & Polish (Tasks 26-32)

#### Task 5.1: Integration-Test - Beschleunigung
- **Priorität:** Hoch
- **Geschätzt:** 45 min
- **Abhängigkeiten:** Task 3.1
- **TDD:** Ja
- **Definition of Done:**
  - [ ] Test: Schiff beschleunigt von 0 auf 30 in 6 Sekunden
  - [ ] Test: Phasen-Übergänge sind smooth
  - [ ] Test: Partikel-Intensität skaliert korrekt

---

#### Task 5.2: Integration-Test - Gewichts-System
- **Priorität:** Hoch
- **Geschätzt:** 45 min
- **Abhängigkeiten:** Task 1.3
- **TDD:** Ja
- **Definition of Done:**
  - [ ] Test: 15 Blöcke → 30 Blöcke/sec
  - [ ] Test: 35 Blöcke → 20 Blöcke/sec
  - [ ] Test: 55 Blöcke → 10 Blöcke/sec + Warnung
  - [ ] Test: 65 Blöcke → 0 Blöcke/sec + Fehler

---

#### Task 5.3: Integration-Test - Treibstoff-System
- **Priorität:** Hoch
- **Geschätzt:** 45 min
- **Abhängigkeiten:** Task 1.5
- **TDD:** Ja
- **Definition of Done:**
  - [ ] Test: 100 Energie - 1/sec (Phase 1) = 100 Sekunden
  - [ ] Test: 100 Energie - 3/sec (Phase 5) = 33 Sekunden
  - [ ] Test: Bei 0 Energie → engineOut = true
  - [ ] Test: UI zeigt korrekten Prozentwert

---

#### Task 5.4: Integration-Test - Höhen-Verlangsamung
- **Priorität:** Mittel
- **Geschätzt:** 30 min
- **Abhängigkeiten:** Task 1.4
- **TDD:** Ja
- **Definition of Done:**
  - [ ] Test: Y=50 → 100% Speed
  - [ ] Test: Y=125 → 80% Speed
  - [ ] Test: Y=175 → 60% Speed
  - [ ] Test: Y=225 → 40% Speed

---

#### Task 5.5: Balance-Anpassungen
- **Priorität:** Mittel
- **Geschätzt:** 1h
- **Abhängigkeiten:** Tasks 5.1-5.4
- **TDD:** Playtesting
- **Definition of Done:**
  - [ ] Beschleunigung "fühlt sich gut an"
  - [ ] Gewichts-Limits sind fair
  - [ ] Treibstoff-Verbrauch ist angemessen
  - [ ] Höhen-Penalty ist spürbar aber nicht frustrierend

---

#### Task 5.6: Performance-Optimierung
- **Priorität:** Mittel
- **Geschätzt:** 45 min
- **Abhängigkeiten:** Task 4.5
- **TDD:** Profiling
- **Definition of Done:**
  - [ ] Physik-Berechnung < 0.5ms pro Tick
  - [ ] Partikel-Overhead < 0.2ms
  - [ ] FPS-Impact < 5 FPS
  - [ ] Memory-Usage < 10MB

---

#### Task 5.7: Code-Cleanup
- **Priorität:** Niedrig
- **Geschätzt:** 30 min
- **Abhängigkeiten:** Alle vorherigen Tasks
- **TDD:** Checkstyle
- **Definition of Done:**
  - [ ] Checkstyle: 0 Warnings
  - [ ] Javadoc für alle öffentlichen Methoden
  - [ ] Keine ungenutzten Imports
  - [ ] Code-Formatting konsistent

---

#### Task 5.8: Dokumentation
- **Priorität:** Niedrig
- **Geschätzt:** 30 min
- **Abhängigkeiten:** Alle vorherigen Tasks
- **TDD:** Nicht erforderlich
- **Definition of Done:**
  - [ ] README.md aktualisieren mit neuen Features
  - [ ] CHANGELOG.md Eintrag erstellen
  - [ ] Spec-Flow state.yaml auf 'implement_complete' setzen

---

## Task-Abhängigkeiten (Graph)

```
Phase 1 (Grundgerüst):
  1.1 ─┬─> 1.4 ─┬─> 3.1 ─> 3.2 ─> 5.1
  1.2 ─┤        │        └─> 4.1 ─> 4.2
  1.3 ─┴─> 1.5 ─┴─> 3.3 ─> 5.2
         │             └─> 3.4
         └─> 2.6

Phase 2 (Input & Network):
  2.1 ─> 2.2 ─> 2.3
   │           └─> 2.4
   └─> 2.5 ─> 2.6

Phase 3 (Physik & Logik):
  3.1 ─> 3.2 ─> 3.3 ─> 3.4
   │
   └─> 3.5 ─> 3.6

Phase 4 (Visuelle Effekte):
  4.1 ─> 4.2 ─> 4.3 ─> 4.4 ─> 4.5

Phase 5 (Testing & Polish):
  5.1 ─┬─> 5.5 ─> 5.6 ─> 5.7 ─> 5.8
  5.2 ─┤
  5.3 ─┤
  5.4 ─┘
```

---

## Kritischer Pfad

```
1.1 → 1.4 → 3.1 → 3.2 → 5.1 → 5.5 → 5.6 → 5.7 → 5.8
     ↗      ↗      ↗
1.2 ┘   2.1 ┘   4.1 ┘
```

**Gesamtdauer kritischer Pfad:** ~8 Stunden

---

## Test-Strategie

### Unit-Tests (6 Tests)
- ShipPhysicsTest (4 Tests)
- FuelSystemTest (2 Tests)

### Integration-Tests (5 Tests)
- Beschleunigung
- Gewichts-System
- Treibstoff-System
- Höhen-Verlangsamung
- Multiplayer-Synchronisation

### Manuelle Tests (3 Tests)
- Partikel-Effekte
- Sound-Effekte
- HUD-Overlay

---

## Akzeptanzkriterien pro Task

Jeder Task muss folgende Kriterien erfüllen:
- [ ] Code kompiliert ohne Fehler
- [ ] Checkstyle: 0 Warnings
- [ ] Javadoc vorhanden (wo öffentlich)
- [ ] Tests bestanden (falls TDD)
- [ ] Git-Commit mit konventioneller Message

---

## Nächste Schritte

1. ✅ Task-Liste erstellt (32 Tasks)
2. ⏳ Mit Task 1.1 beginnen (`/implement`)
3. ⏳ TDD-Workflow: Test → Code → Refactor
4. ⏳ Nach jedem Batch (3-4 Tasks): Quality Gates

---

**Tasks genehmigt von:** _______________  
**Datum:** _______________  
**Bereit für Implementation:** ☐ Ja  ☐ Nein
