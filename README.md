# Shark Engine – Dokumentation (v0.1.0)

## 📖 Überblick

Shark Engine ist eine Fabric-basierte Minecraft-Mod (1.21.1), die es Spielern ermöglicht, blockbasierte fliegende Schiffe zu bauen und zu steuern.

### Features
- ✅ Steuerrad-basiertes Schiffssystem
- ✅ Automatischer Struktur-Scan und Validierung
- ✅ Physik-basierte Flugmechanik
- ✅ Fuel-System (Auftanken mit Holz)
- ✅ Controller-Support (Xbox, PlayStation)
- ✅ HUD-Anzeige (Fuel, Speed, HP)
- ✅ Tutorial-System

---

## 🚀 Installation

### Voraussetzungen
- Minecraft 1.21.1
- Fabric Loader ≥0.16.5
- Java 21
- Fabric API 0.114.0+1.21.1

### Server-Installation
1. Fabric Server installieren
2. `sharkengine-0.1.0.jar` in den `mods/`-Ordner kopieren
3. `fabric-api.jar` in den `mods/`-Ordner kopieren
4. Server starten

### Client-Installation
1. Fabric Client installieren
2. `sharkengine-0.1.0.jar` in den `mods/`-Ordner kopieren
3. `fabric-api.jar` in den `mods/`-Ordner kopieren
4. Minecraft starten

---

## 🎮 Steuerung

### Tastatur & Maus
| Taste | Aktion |
|-------|--------|
| **W** | Beschleunigen (Vorwärts) |
| **A** | Nach links drehen |
| **D** | Nach rechts drehen |
| **Leertaste** | Aufsteigen |
| **Shift** | Absteigen |
| **Rechtsklick** | Schiff betreten / Auftanken (mit Holz) |
| **Shift + Rechtsklick** | Anchor umschalten / Zerlegen (wenn geankert) |

### Xbox Controller
| Eingabe | Aktion |
|---------|--------|
| **Linker Stick ↑** | Beschleunigen |
| **Rechter Stick ←/→** | Drehen |
| **Right Trigger (RT)** | Aufsteigen |
| **Left Trigger (LT)** | Absteigen |
| **A-Button** | Anchor umschalten |
| **B-Button** | Aussteigen |
| **Y-Button** | Interagieren |

### PlayStation Controller
| Eingabe | Aktion |
|---------|--------|
| **Linker Stick ↑** | Beschleunigen |
| **Rechter Stick ←/→** | Drehen |
| **R2** | Aufsteigen |
| **L2** | Absteigen |
| **X-Button** | Anchor umschalten |
| **Circle** | Aussteigen |
| **Triangle** | Interagieren |

---

## 🛠️ Schiffsbau

### Erforderliche Komponenten
1. **Steuerrad** (Steering Wheel) – Zentrum des Schiffs
2. **Bug-Block** (Bow) – Frontmarker (genau 1, an der Vorderkante)
3. **Triebwerk** (Thruster) – Mindestens 1 erforderlich
4. **Struktur-Blöcke** – Mindestens 4 um das Steuerrad (N, S, O, W)

### Bau-Anleitung
1. **Steuerrad platzieren**
2. **4 Blöcke um das Steuerrad** (horizontal: Nord, Süd, Ost, West)
3. **Bug-Block an die Vorderkante** (zeigt die Flugrichtung)
4. **Mindestens 1 Triebwerk einbauen**
5. **Rechtsklick auf Steuerrad** → Builder-Modus öffnet sich
6. **Validierung prüfen** (rote Highlights entfernen)
7. **"Assemble & Launch" klicken**

### Validierungsregeln
| Regel | Beschreibung |
|-------|-------------|
| **≥4 Kernblöcke** | Mindestens 4 Blöcke um das Steuerrad |
| **1 Bug-Block** | Genau 1 Bug, muss an der Außenkante sein |
| **≥1 Triebwerk** | Mindestens 1 Thruster erforderlich |
| **Kein Bodenkontakt** | Struktur darf den Boden nicht berühren |
| **≤512 Blöcke** | Maximale Strukturgröße |
| **≤32 Block-Radius** | Maximaler Radius vom Steuerrad |

### Gewichtsklassen
| Klasse | Blöcke | Max. Speed |
|--------|--------|-----------|
| **LIGHT** | 1-20 | 30 Blöcke/sec |
| **MEDIUM** | 21-40 | 25 Blöcke/sec |
| **HEAVY** | 41-60 | 20 Blöcke/sec |
| **OVERLOADED** | 61+ | 0 Blöcke/sec (zu schwer!) |

---

## ⛽ Fuel-System

### Grundlagen
- **1 Holzstamm/Bretter** = 100 Energy
- **Max. Tank** = 100 Energy
- **Verbrauch** = 1-3 Energy/sec (je nach Beschleunigungsphase)

### Auftanken
1. **Holzstamm oder Bretter in die Hand nehmen**
2. **Rechtsklick auf das Schiff**
3. **Fuel wird automatisch aufgefüllt**

### Fuel-Verbrauch pro Phase
| Phase | Speed | Verbrauch | Flugzeit (100 Fuel) |
|-------|-------|-----------|---------------------|
| **Phase 1** | 5 bl/sec | 1/sec | 100 sec |
| **Phase 2** | 10 bl/sec | 1/sec | 100 sec |
| **Phase 3** | 15 bl/sec | 2/sec | 50 sec |
| **Phase 4** | 20 bl/sec | 2/sec | 50 sec |
| **Phase 5** | 30 bl/sec | 3/sec | 33 sec |

### Engine-Out
- Bei **0 Fuel** fällt das Schiff (kontrolliertes Sinken)
- **Landung auf Wasser** ermöglicht schwimmen
- **Auftanken** vor dem Weiterfliegen empfohlen

---

## 🎯 Tutorial

Das Tutorial führt durch den ersten Flug:

1. **Willkommen** – Erklärung der Mod
2. **Modus-Wahl** – Flugmodus wählen (AIR)
3. **Bau-Anleitung** – Struktur erklären
4. **Bereit zum Start** – Validierung prüfen
5. **Flug-Tipps** – Steuerung erklären

---

## 🔧 Debug Commands (OP erforderlich)

| Command | Beschreibung |
|---------|-------------|
| `/shipdebug giveall` | Gibt alle Schiff-Blöcke |
| `/shipdebug assemble` | Assembliert Schiff an Spieler-Position |
| `/shipdebug builder` | Öffnet Builder-Preview |
| `/shipdebug disassemble` | Zerlegt aktuelles Schiff |

---

## ⚙️ Konfiguration (Controller)

Die Controller-Konfiguration wird gespeichert in:
`config/sharkengine-controller.properties`

### Optionen
| Option | Standard | Beschreibung |
|--------|----------|-------------|
| `deadzone` | 0.15 | Stick-Deadzone (0.0-0.5) |
| `invertYaw` | false | Drehung invertieren |
| `invertPitch` | false | Pitch invertieren |
| `vibrationEnabled` | true | Vibrations-Feedback |
| `vibrationIntensity` | 0.5 | Vibrationsstärke (0.0-1.0) |

---

## 🐛 Bekannte Probleme

| Problem | Workaround |
|---------|------------|
| Controller wird nicht erkannt | USB neu einstecken, Minecraft neu starten |
| Assembly-Loop bei Fehlern | Debug-Logs prüfen (`/shipdebug assemble`) |
| Fuel-Refill funktioniert nicht | Nur Holzstämme/Bretter verwenden |

---

## 📊 Performance

### Empfohlene Limits
- **Max. Schiffsgröße:** 512 Blöcke
- **Max. Radius:** 32 Blöcke vom Steuerrad
- **Optimale Größe:** 20-40 Blöcke (MEDIUM)

### Server-Performance
- **Assembly:** <100ms für 512 Blöcke
- **Tick:** <10ms pro Schiff
- **Memory:** <1MB pro Schiff

---

## 🧪 Tests

### Test-Suite
- **76 automatische Tests**
- **Assembly:** 36 Tests
- **Fuel:** 40 Tests
- **Integration:** 20 Tests

### Tests ausführen
```bash
cd sharkengine
./gradlew test
```

---

## 📝 Changelog

### v0.1.0 (11. März 2026)
- ✅ Controller-Support (Xbox, PlayStation)
- ✅ Fuel-Refill mit Holzstämmen
- ✅ Disassemble gibt Blöcke zurück ins Inventar
- ✅ Debug-Logs für Assembly
- ✅ 76 automatische Tests
- ✅ HUD zeigt Controller-Status

### v0.0.1 (Initiale Version)
- Grundlegendes Schiffssystem
- Assembly-Validierung
- Flug-Physik
- Tutorial-System

---

## 🤝 Beiträge

### Entwicklung
```bash
git clone https://github.com/your-repo/shark-engine.git
cd shark-engine/sharkengine
./gradlew build
./gradlew runClient
```

### Pull Requests
1. Feature-Branch erstellen
2. Tests hinzufügen/aktualisieren
3. `./gradlew test` muss grün sein
4. PR mit Beschreibung erstellen

---

## 📄 Lizenz

MIT License – siehe `LICENSE` Datei.

---

## 🎮 Quick Start

1. **Server starten:** `localhost:25565`
2. **Beitreten** im Creative Mode
3. **Blöcke geben:** `/shipdebug giveall`
4. **Schiff bauen** (siehe Bau-Anleitung)
5. **Assemblieren:** `/shipdebug assemble`
6. **Losfliegen!**

Viel Spaß mit Shark Engine! 🦈✈️
