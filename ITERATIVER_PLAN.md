# Shark Engine – Iterativer Projektplan

## Ziel
Vollständiger User Flow: **Steuerrad platzieren → Fahrzeug bauen → einsteigen → fahren → auftanken → aussteigen → Baumodus reaktivieren**

---

## PHASE 1: Controller Test-Mod (Unabhängig von Shark Engine)

**Ziel:** Controller-Anbindung isoliert testen und debuggen

| Task | Beschreibung | Status |
|------|-------------|--------|
| 1.1 | Neue Mod `controller-test` erstellen | ⏳ |
| 1.2 | GLFW/Gamepad-Initialisierung implementieren | ⏳ |
| 1.3 | Chat-Nachrichten bei Connect/Disconnect | ⏳ |
| 1.4 | Live-Input-Anzeige (Stick-Position, Buttons) | ⏳ |
| 1.5 | Config für Deadzone, Invert, Vibration | ⏳ |
| 1.6 | Build & Test auf macOS | ⏳ |

**Definition of Done:**
- Mod startet unabhängig von Shark Engine
- Controller wird beim Start erkannt
- Input-Werte werden live im Chat/HUD angezeigt
- Config-Datei wird erstellt

---

## PHASE 2: Ship Assembly Flow analysieren und fixen

**Ziel:** Assembly-Loop identifizieren und beheben

| Task | Beschreibung | Status |
|------|-------------|--------|
| 2.1 | Assembly-Service Logs hinzufügen (Debug-Output) | ⏳ |
| 2.2 | Builder-Preview State Machine dokumentieren | ⏳ |
| 2.3 | "Assemble & Launch" Button-Handler prüfen | ⏳ |
| 2.4 | Server-Client Sync für Assembly prüfen | ⏳ |
| 2.5 | Assembly-Loop fixen (falls vorhanden) | ⏳ |
| 2.6 | Tutorial-Popup Integration prüfen | ⏳ |

**Definition of Done:**
- Assembly funktioniert in einem Durchgang
- Keine Endlosschleifen
- Logs zeigen jeden Schritt klar an

---

## PHASE 3: Automatische Tests für Ship Assembly

**Ziel:** Tests sichern Assembly-Logik ab

| Task | Beschreibung | Status |
|------|-------------|--------|
| 3.1 | Test-Setup für Server-Level erstellen | ⏳ |
| 3.2 | Test: Minimale gültige Struktur (4 Blöcke + Bug + Thruster) | ⏳ |
| 3.3 | Test: Ungültige Struktur (weniger als 4 Blöcke) | ⏳ |
| 3.4 | Test: Kein Bug-Block | ⏳ |
| 3.5 | Test: Mehrere Bug-Blöcke | ⏳ |
| 3.6 | Test: Bug nicht an Kante | ⏳ |
| 3.7 | Test: Kein Thruster | ⏳ |
| 3.8 | Test: Bodenkontakt erkannt | ⏳ |
| 3.9 | Test: Vollständiger Assembly-Flow | ⏳ |
| 3.10 | CI-Integration (./gradlew test) | ⏳ |

**Definition of Done:**
- Alle Tests grün
- Code-Coverage > 80% für Assembly-Logik
- CI läuft bei jedem Commit

---

## PHASE 4: Kompletten User Flow implementieren

**Ziel:** Alle Anforderungen aus MSP-1 umsetzen

### 4.1 Bauen & Assemblieren
| Task | Beschreibung | Status |
|------|-------------|--------|
| 4.1.1 | Steuerrad-Platzierung → Tutorial starten | ⏳ |
| 4.1.2 | Builder-Modus öffnen (Rechtsklick) | ⏳ |
| 4.1.3 | Echtzeit-Validierung (Highlights) | ⏳ |
| 4.1.4 | "Assemble & Launch" Button funktional | ⏳ |

### 4.2 Einsteigen & Fahren
| Task | Beschreibung | Status |
|------|-------------|--------|
| 4.2.1 | Mounting (Rechtsklick auf Schiff) | ⏳ |
| 4.2.2 | Third-Person Kamera automatisch | ⏳ |
| 4.2.3 | Steuerung (Tastatur + Controller) | ⏳ |
| 4.2.4 | Physik (Beschleunigung, Höhe, Gewicht) | ⏳ |
| 4.2.5 | HUD (Fuel, Speed, HP, Controller) | ⏳ |

### 4.3 Auftanken
| Task | Beschreibung | Status |
|------|-------------|--------|
| 4.3.1 | Fuel-Item (Holz/Kohle) definieren | ⏳ |
| 4.3.2 | Tank-Interaktion (Rechtsklick) | ⏳ |
| 4.3.3 | Fuel-Stand im HUD anzeigen | ⏳ |
| 4.3.4 | Engine-Out Verhalten (Sinken) | ⏳ |

### 4.4 Aussteigen & Baumodus reaktivieren
| Task | Beschreibung | Status |
|------|-------------|--------|
| 4.4.1 | Dismount (Shift+Rechtsklick / B-Button) | ⏳ |
| 4.4.2 | Anchor-Toggle (Shift+Rechtsklick) | ⏳ |
| 4.4.3 | Disassemble (anchored + Shift+Rechtsklick) | ⏳ |
| 4.4.4 | Zurück zu Blöcken im Inventar | ⏳ |
| 4.4.5 | Builder-Modus wieder öffnen | ⏳ |

**Definition of Done:**
- Kompletter Loop ohne manuelle Commands
- Alle Interaktionen funktionieren
- Tutorial führt durch den Flow

---

## PHASE 5: Integrationstests und Validierung

**Ziel:** End-to-End Tests sichern Gesamt-Flow

| Task | Beschreibung | Status |
|------|-------------|--------|
| 5.1 | Integrationstest: Vollständiger Flow | ⏳ |
| 5.2 | Performance-Test (512 Blöcke Schiff) | ⏳ |
| 5.3 | Multiplayer-Test (2+ Spieler) | ⏳ |
| 5.4 | Controller-Test (wenn Hardware verfügbar) | ⏳ |
| 5.5 | Dokumentation (README, Controls) | ⏳ |
| 5.6 | Release-Vorbereitung (v0.1.0) | ⏳ |

**Definition of Done:**
- Alle Tests grün
- Dokumentation vollständig
- Release-Ready

---

## Zeitplan (Iterativ)

| Woche | Fokus | Deliverables |
|-------|-------|--------------|
| 1 | Phase 1 + 2 | Controller-Test-Mod, Assembly-Fix |
| 2 | Phase 3 | Test-Suite (80% Coverage) |
| 3 | Phase 4.1 + 4.2 | Bauen + Fahren funktional |
| 4 | Phase 4.3 + 4.4 | Tanken + Aussteigen |
| 5 | Phase 5 | Release v0.1.0 |

---

## Abhängigkeiten

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5
              ↓           ↓
        Assembly    User Flow
        funktioniert  komplett
```

---

## Risiken & Gegenmaßnahmen

| Risiko | Auswirkung | Gegenmaßnahme |
|--------|------------|---------------|
| Controller-Hardware nicht verfügbar | Phase 1 verzögert | Test-Mod trotzdem bauen, Mock-Inputs |
| Assembly-Loop komplex | Phase 2 dauert länger | Debug-Logs, schrittweise Isolation |
| Test-Setup zu komplex | Phase 3 blockiert | Minimal-Tests zuerst, dann erweitern |
| Performance-Probleme | Phase 5 scheitert | Frühzeitig profilieren, optimieren |

---

## Nächste Schritte

1. **Sofort:** Phase 1, Task 1.1 – Controller Test-Mod erstellen
2. **Parallel:** Phase 2, Task 2.1 – Assembly Debug-Logs
3. **Danach:** Task für Task in Reihenfolge abarbeiten
