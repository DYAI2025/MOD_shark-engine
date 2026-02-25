# Feature-Spezifikation: Vertikale Schiffsbewegung (Luftfahrzeug-MVP)

**Feature ID:** 001  
**Name:** Vertikale Schiffsbewegung - Luftfahrzeug MVP  
**Status:** Clarify Complete  
**Erstellt:** 2026-02-25  
**Version:** 2.0 (überarbeitet)  
**Fahrzeug-Klasse:** B (Luftfahrzeuge)

---

## 1. Zusammenfassung

### 1.1 Feature-Beschreibung
Dieses Feature ermöglicht es Spielern, ein **Luftfahrzeug (Raumschiff)** zu steuern, das durch Triebwerke in der Luft schweben und sich vertikal bewegen kann. Das Fahrzeug verfügt über ein realistisches Beschleunigungssystem, Gewichts- und Treibstoff-Management.

### 1.2 Business Value
- Spieler können **fliegende Raumschiffe** bauen und steuern
- Tiefes Gameplay durch Physik-Simulation (Beschleunigung, Gewicht, Treibstoff)
- Visuelles Feedback durch Triebwerk-Partikel und Sound
- Erweiterbar auf weitere Fahrzeug-Klassen (Wasser, Land)

### 1.3 Scope - MVP (Klasse B: Luftfahrzeuge)

**In Scope:**
- Triebwerk-basiertes Fliegen mit Partikel-Effekten
- Beschleunigungs-System (5 Phasen über 6 Sekunden)
- Höhen-Verlangsamung (ab Y=200)
- Gewichts-System (jeder Block = 1 Gewichtseinheit)
- Treibstoff-System (Holzblöcke als Energie)
- Visuelle Effekte (Flammen-Partikel bei Beschleunigung)
- Sound-Effekte (Düsengeräusch)
- Stopp bei Kollision (einfach)

**Out of Scope (für MVP):**
- Fahrzeug-Klasse A (Wasser) und C (Land)
- Aktive Kollisionsvermeidung
- Unterschiedliche Block-Gewichte
- Manuelles Treibstoff-Nachfüllen (kommt später)
- Schaden bei Kollision
- Mehrere Schiffstypen innerhalb Klasse B

---

## 2. User Stories

### US-001: Als Spieler möchte ich ein Raumschiff bauen können
```gherkin
Feature: Raumschiff-Konstruktion
  Als Spieler der Luftfahrzeuge bauen möchte
  Möchte ich Blöcke zu einem Schiff zusammenfügen
  Damit ich ein funktionierendes Raumschiff erhalte

  Szenario: Schiff aus Blöcken erstellen
    Gegeben ich habe Blöcke platziert (Flügel, Triebwerke, etc.)
    Wenn ich das Steuerrad aktiviere
    Dann wird die Fahrzeug-Klasse "Luftfahrzeug" gewählt
    Und die Block-Anzahl wird gezählt für Gewichts-Berechnung
```

### US-002: Als Spieler möchte ich beschleunigen können
```gherkin
Feature: Beschleunigungs-System
  Als Spieler der ein Raumschiff steuert
  Möchte ich schrittweise Beschleunigung erleben
  Damit sich das Fluggefühl realistisch anfühlt

  Szenario: Beschleunigung von 0 auf Maximum
    Gegeben das Schiff steht still
    Wenn ich Vorwärts drücke (W)
    Dann beschleunigt das Schiff über 5 Phasen:
      | Phase | Dauer | Geschwindigkeit |
      | 1     | 0-2s  | 5 Blöcke/sec    |
      | 2     | 2-4s  | 15 Blöcke/sec   |
      | 3     | 4-5s  | 20 Blöcke/sec   |
      | 4     | 5-6s  | 25 Blöcke/sec   |
      | 5     | 6s+   | 30 Blöcke/sec   |
    Und Triebwerk-Partikel werden entsprechend intensiver

  Szenario: Höhen-bedingte Verlangsamung
    Gegeben das Schiff steigt auf Y=220
    Wenn es sich mit Maximalgeschwindigkeit bewegt
    Dann wird die Geschwindigkeit auf 40% reduziert
    Und die Partikel werden schwächer
```

### US-003: Als Spieler möchte ich mein Gewicht managen
```gherkin
Feature: Gewichts-System
  Als Spieler der ein schweres Schiff baut
  Möchte ich die Auswirkungen auf die Geschwindigkeit sehen
  Damit ich strategische Bauplanung betreiben kann

  Szenario: Leichtes Schiff (1-20 Blöcke)
    Gegeben mein Schiff hat 15 Blöcke
    Wenn ich fliege
    Dann erreiche ich 30 Blöcke/sec Maximalgeschwindigkeit
    Und keine Warnung wird angezeigt

  Szenario: Schweres Schiff (41-60 Blöcke)
    Gegeben mein Schiff hat 50 Blöcke
    Wenn ich fliege
    Dann erreiche ich nur 10 Blöcke/sec Maximalgeschwindigkeit
    Und die Warnung "Achtung: Schiff wird langsam" erscheint

  Szenario: Zu schweres Schiff (61+ Blöcke)
    Gegeben mein Schiff hat 65 Blöcke
    Wenn ich das Steuerrad aktiviere
    Dann kann das Schiff nicht fliegen
    Und die Warnung "⚠️ Zu schwer zum Fliegen!" erscheint
```

### US-004: Als Spieler möchte ich Treibstoff managen
```gherkin
Feature: Treibstoff-System
  Als Spieler der lange Strecken zurücklegt
  Möchte ich meinen Treibstoffvorrat überwachen
  Damit ich nicht ohne Energie stecken bleibe

  Szenario: Treibstoff verbrauchen
    Gegeben mein Schiff hat 100 Energie (1 Holzblock)
    Wenn ich 50 Sekunden fliege
    Dann habe ich noch 50 Energie (50%)
    Und die Treibstoff-Anzeige zeigt "█████░░░░░ 50%"

  Szenario: Kein Treibstoff mehr
    Gegeben mein Schiff hat 0 Energie
    Wenn ich weiterfliegen will
    Dann fallen die Triebwerke aus (keine Partikel)
    Und das Schiff fällt langsam (wie Sand)
    Und die Warnung "Kein Treibstoff mehr!" erscheint
```

### US-005: Als Spieler möchte ich visuelles Feedback
```gherkin
Feature: Visuelle und Audio-Effekte
  Als Spieler der ein immersives Erlebnis will
  Möchte ich Triebwerk-Effekte sehen und hören
  Damit ich mein Schiff "fühlen" kann

  Szenario: Im Leerlauf schweben
    Gegeben mein Schiff schwebt in der Luft
    Wenn es stillsteht
    Dann steigen kleine Rauchpartikel auf (CAMPFIRE_COSY_SMOKE)
    Und ein leises Düsengeräusch ist hörbar (loop)

  Szenario: Bei Beschleunigung
    Gegeben mein Schiff beschleunigt (Phase 3-5)
    Wenn es vorwärts fliegt
    Dann werden Flammen-Partikel ausgestoßen (ParticleTypes.FLAME)
    Und das Düsengeräusch wird lauter
    Und die Partikel zeigen entgegen der Flugrichtung
```

---

## 3. Funktionale Anforderungen

### 3.1 Fahrzeug-Klasse B: Luftfahrzeuge

**Definition:**
- Fahrzeug-Typ: AIR (Enum: VehicleClass)
- Erforderliche Komponenten: Mindestens 1 Triebwerk-Block
- Optionale Komponenten: Flügel, Rotoren, Düsen

**Fähigkeiten:**
- Vertikale Bewegung (Aufsteigen/Absteigen)
- Schweben im Leerlauf
- 3D-Bewegung (WASD + Vertikal)

### 3.2 Beschleunigungs-System

**Phasen:**
| Phase | Dauer (Sekunden) | Geschwindigkeit (Blöcke/sec) | Partikel-Intensität |
|-------|------------------|------------------------------|---------------------|
| 1     | 0-2              | 5                            | 20%                 |
| 2     | 2-4              | 15                           | 40%                 |
| 3     | 4-5              | 20                           | 60%                 |
| 4     | 5-6              | 25                           | 80%                 |
| 5     | 6+               | 30                           | 100%                |

**Implementierung:**
```java
// ShipPhysics.java
public float calculateAcceleration(int phase) {
    return switch(phase) {
        case 1 -> 5.0f;
        case 2 -> 15.0f;
        case 3 -> 20.0f;
        case 4 -> 25.0f;
        case 5 -> 30.0f;
        default -> 0.0f;
    };
}
```

### 3.3 Höhen-Verlangsamung

**Berechnung:**
| Höhenbereich (Y) | Geschwindigkeits-Modifikator |
|------------------|------------------------------|
| 0-100            | 100% (kein Penalty)          |
| 100-150          | 80%                          |
| 150-200          | 60%                          |
| 200-256          | 40%                          |

**Implementierung:**
```java
// ShipPhysics.java
public float calculateHeightPenalty(float yPos) {
    if (yPos < 100) return 1.0f;
    if (yPos < 150) return 0.8f;
    if (yPos < 200) return 0.6f;
    return 0.4f; // Y=200+
}
```

### 3.4 Gewichts-System

**Berechnung:**
```
Gewicht = Anzahl aller Blöcke im Schiff

Blöcke    | Max-Geschwindigkeit | Warnung
----------|---------------------|---------------------------
1-20      | 30 Blöcke/sec       | Keine
21-40     | 20 Blöcke/sec       | Keine
41-60     | 10 Blöcke/sec       | "Achtung: Schiff wird langsam"
61+       | 0 Blöcke/sec        | "⚠️ Zu schwer zum Fliegen!"
```

**Implementierung:**
```java
// ShipPhysics.java
public float calculateMaxSpeed(int blockCount) {
    if (blockCount <= 20) return 30.0f;
    if (blockCount <= 40) return 20.0f;
    if (blockCount <= 60) return 10.0f;
    return 0.0f; // Zu schwer
}

public String getWeightWarning(int blockCount) {
    if (blockCount > 60) return "§c⚠️ Zu schwer zum Fliegen!";
    if (blockCount > 40) return "§eAchtung: Schiff wird langsam";
    return "";
}
```

### 3.5 Treibstoff-System

**Energie-Berechnung:**
```
1 Holzblock = 100 Energie-Einheiten
1 Sekunde Flug = 1 Energieverbrauch

Verbrauch pro Sekunde:
- Phase 1-2: 1 Energie/sec
- Phase 3-4: 2 Energie/sec
- Phase 5: 3 Energie/sec
```

**UI-Anzeige:**
```
Treibstoff: [████████░░] 80%
```

**Implementierung:**
```java
// ShipEntity.java
public void consumeFuel(int phase) {
    int consumption = switch(phase) {
        case 1, 2 -> 1;
        case 3, 4 -> 2;
        case 5 -> 3;
        default -> 0;
    };
    this.fuelLevel -= consumption;
    
    if (this.fuelLevel <= 0) {
        this.engineOut = true;
        sendSystemMessage(Component.translatable("message.sharkengine.no_fuel"));
    }
}
```

### 3.6 Visuelle Effekte

**Partikel-Typen:**
```java
// ShipEntityRenderer.java

// Im Leerlauf (schweben)
if (isHovering()) {
    level.addParticle(
        ParticleTypes.CAMPFIRE_COSY_SMOKE,
        x, y, z,
        0, 0.05, 0  // Langsam aufsteigend
    );
}

// Bei Beschleunigung (Phase 3-5)
if (accelerationPhase >= 3) {
    int particleCount = accelerationPhase - 2; // Mehr Partikel bei höherer Phase
    for (int i = 0; i < particleCount; i++) {
        level.addParticle(
            ParticleTypes.FLAME,
            x, y, z,
            -motionX * 0.5, -motionY * 0.5, -motionZ * 0.5  // Entgegen Flugrichtung
        );
    }
}
```

**Sound-Effekte:**
```java
// ShipEntity.java
if (isEngineRunning()) {
    float volume = baseVolume * (accelerationPhase / 5.0f);
    level.playSound(
        null, x, y, z,
        SoundEvents.FIRECHARGE_USE,  // Oder custom sound
        SoundSource.BLOCKS,
        volume,
        0.8f + (random.nextFloat() * 0.4f)  // Leichte Variation
    );
}
```

### 3.7 Kollision

**Einfache Kollisionsabfrage:**
```java
// ShipEntity.java
if (isColliding()) {
    setDeltaMovement(Vec3.ZERO);
    // Kein Schaden, kein Abprallen - einfaches Stoppen
}
```

---

## 4. Nicht-Funktionale Anforderungen

### 4.1 Performance
- Partikel-Limit: Max. 50 Partikel pro Tick (bei Phase 5)
- Sound-Limit: Max. 1 Sound pro Sekunde (geduldet)
- Berechnungen: < 0.5ms pro Tick für Physik

### 4.2 Code-Qualität
- Test-Abdeckung: > 85% für ShipPhysics.java
- Javadoc für alle öffentlichen Methoden
- Keine neuen Checkstyle-Warnings

### 4.3 Spielbarkeit
- Input-Latenz: < 50ms (Client → Server)
- UI-Updates: 10 Hz (alle 2 Ticks)
- Smooth Movement: Interpolation zwischen Ticks

---

## 5. Technische Architektur

### 5.1 Neue Klassen

```
sharkengine/
├── ship/
│   ├── ShipEntity.java           ← Erweitern
│   ├── ShipBlueprint.java        ← Erweitern (blockCount)
│   ├── ShipPhysics.java          ← NEU
│   ├── VehicleClass.java         ← NEU (Enum: WATER, AIR, LAND)
│   └── FuelSystem.java           ← NEU
│
├── content/
│   ├── ModBlocks.java            ← TREIBSTOFF_BLOCK
│   └── ModSounds.java            ← NEU (Düsengeräusch)
│
└── client/
    └── render/
        ├── ShipEntityRenderer.java ← Partikel
        └── FuelHudOverlay.java     ← NEU (UI)
```

### 5.2 Erweiterte Klassen

**ShipEntity.java - Neue Felder:**
```java
private int fuelLevel;              // 0-100+
private int blockCount;             // Anzahl Blöcke
private int accelerationPhase;      // 1-5
private float currentSpeed;         // Aktuelle Geschwindigkeit
private float maxSpeed;             // Berechnetes Maximum
private VehicleClass vehicleClass;  // AIR für MVP
private boolean engineOut;          // Bei Treibstoffmangel
```

**HelmInputC2SPayload.java - Neue Felder:**
```java
private float throttle;    // -1..+1 (vertikal)
private float turn;        // -1..+1 (rotation)
private float forward;     // 0..1 (Beschleunigung)
```

---

## 6. Entscheidungen (aus /clarify Phase)

| ID | Frage | Entscheidung | Begründung |
|----|-------|--------------|------------|
| Q-001 | Kollisionsvermeidung | ❌ Stopp bei Kontakt | Komplexität ↓, MVP-fokussiert |
| Q-002 | Konfigurierbare Geschwindigkeit | ❌ Nein (festes System) | Balance durch Design |
| Q-003 | Visuelle Effekte | ✅ Ja (Partikel + Sound) | Spielerlebnis ↑ |
| Q-004 | WASD + Vertikal | ✅ Kombinierbar | 3D-Bewegung ✓ |
| Q-005 | Anchor-Verhalten | ⚠️ Anchor blockiert | Konsistenz ✓ |
| Q-006 | Energie-System | ✅ Ja (Holzblöcke) | Gameplay-Tiefe ↑ |
| Q-007 | Fahrzeug-Klassen | ✅ Klasse B (Luft) MVP | Fokussierter Scope |
| Q-008 | Beschleunigung | ✅ Hybrid (auto + Gas) | Bestes Spielgefühl |
| Q-009 | Höhen-Verlangsamung | ✅ Ab Y=200 | Weniger Frust |
| Q-010 | Gewicht | ✅ Jeder Block = 1 | Einfach zu verstehen |

---

## 7. Erfolgsmetriken

| Metrik | Ziel | Messung |
|--------|------|---------|
| Test-Abdeckung | > 85% | JaCoCo Report |
| FPS-Impact | < 5 FPS | Profiling |
| Partikel-Overhead | < 0.2ms | Profiling |
| User-Feedback | Positiv | Playtesting |
| Beschleunigung "fühlt sich gut an" | ≥ 4/5 | Player Survey |

---

## 8. Abhängigkeiten

| ID | Abhängigkeit | Status |
|----|--------------|--------|
| D-001 | ShipEntity Klasse | Vorhanden (erweitern) |
| D-002 | HelmInputClient | Vorhanden (erweitern) |
| D-003 | HelmInputC2SPayload | Vorhanden (erweitern) |
| D-004 | ShipEntityRenderer | Vorhanden (erweitern) |
| D-005 | VehicleClass Enum | ❌ Neu erstellen |
| D-006 | ShipPhysics | ❌ Neu erstellen |
| D-007 | FuelSystem | ❌ Neu erstellen |
| D-008 | Treibstoff-Block | ❌ Neu erstellen |
| D-009 | Sound-Effekt | ❌ Neu erstellen/finden |
| D-010 | FuelHudOverlay | ❌ Neu erstellen |

---

## 9. Risiken

| ID | Risiko | Wahrscheinlichkeit | Auswirkung | Mitigation |
|----|--------|-------------------|------------|------------|
| R-001 | Performance durch Partikel | Mittel | Mittel | Partikel-Limit (50/Tick) |
| R-002 | Treibstoff-System zu komplex | Niedrig | Hoch | Simpel halten (1 Holz = 100 Energie) |
| R-003 | Gewichts-Balance unausgewogen | Mittel | Mittel | Playtesting, nachjustieren |
| R-004 | Beschleunigung "fühlt sich falsch an" | Mittel | Hoch | Mehrere Testrunden |
| R-005 | Multiplayer-Desync bei Physik | Niedrig | Hoch | Server-authoritative Physik |

---

## 10. Akzeptanzkriterien-Checkliste

### Kern-Funktionalität
- [ ] Aufsteigen mit Leertaste funktioniert
- [ ] Absteigen mit Shift funktioniert
- [ ] WASD + Vertikal kombinierbar
- [ ] Anchor blockiert Vertikal-Bewegung

### Beschleunigungs-System
- [ ] 5 Beschleunigungs-Phasen implementiert
- [ ] Übergänge zwischen Phasen sind smooth
- [ ] Maximalgeschwindigkeit 30 Blöcke/sec erreichbar

### Gewichts-System
- [ ] Block-Anzahl wird korrekt gezählt
- [ ] Max-Geschwindigkeit wird reduziert (21/41/61 Blöcke)
- [ ] Warnung bei 41+ Blöcken
- [ ] Flug-Unfähig bei 61+ Blöcken

### Treibstoff-System
- [ ] Holzblöcke werden zu Energie (1:100)
- [ ] Energie wird verbraucht (1-3/sec je nach Phase)
- [ ] UI-Anzeige funktioniert (10 Hz)
- [ ] Bei 0 Energie: Triebwerke aus, Schiff fällt

### Visuelle Effekte
- [ ] Rauch-Partikel im Leerlauf
- [ ] Flammen-Partikel bei Beschleunigung
- [ ] Partikel-Intensität skaliert mit Phase
- [ ] Düsengeräusch (loop, lautstärke skaliert)

### Höhen-Verlangsamung
- [ ] Ab Y=200: 40% Geschwindigkeit
- [ ] Stufenweise Reduktion (100/150/200)

### Kollision
- [ ] Stoppt bei Block-Kontakt

### Testing
- [ ] Unit-Tests für ShipPhysics (> 85% Coverage)
- [ ] Integration-Test: Beschleunigung
- [ ] Integration-Test: Gewichts-System
- [ ] Integration-Test: Treibstoff-System
- [ ] Multiplayer-Test: Synchronisation
- [ ] Performance-Test: Partikel-Overhead

---

## 11. Änderungshistorie

| Version | Datum | Autor | Änderung |
|---------|-------|-------|----------|
| 1.0 | 2026-02-25 | Spec-Flow | Initiale Spezifikation |
| 2.0 | 2026-02-25 | DYAI + Spec-Flow | **Überarbeitet für Luftfahrzeug-MVP**:<br>• Fahrzeug-Klasse B hinzugefügt<br>• Beschleunigungs-System (5 Phasen)<br>• Gewichts-System (Blöcke zählen)<br>• Treibstoff-System (Holz → Energie)<br>• Visuelle Effekte (Partikel + Sound)<br>• Höhen-Verlangsamung (ab Y=200) |

---

## 12. Nächste Schritte

1. ✅ Spezifikation erstellt (v1.0)
2. ✅ `/clarify` - 10 Fragen beantwortet (v2.0)
3. ⏳ `/plan` - Architektur-Planung
4. ⏳ `/tasks` - Task-Zerlegung
5. ⏳ `/implement` - Implementation mit TDD
6. ⏳ `/optimize` - Quality Gates
7. ⏳ `/ship` - Deployment

---

**Spezifikation genehmigt von:** _______________  
**Datum:** _______________  
**Fahrzeug-Klasse:** B (Luftfahrzeuge) - MVP  
**Nächste Phase:** `/plan` (Architektur-Planung)
