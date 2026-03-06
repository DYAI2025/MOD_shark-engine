# 🚀 Feature 001: Vertikale Schiffsbewegung - Status

## Aktueller Status: ✅ Spezifikation Abgeschlossen

```
Status: ██████░░░░░░░░░░░░ 40%
        Spec ✓ → Clarify → Plan → Tasks → Implement → Optimize → Ship
```

---

## 📋 Was wurde gemacht?

### 1. Spec-Flow Workspace erstellt
```
specs/001-vertikale-bewegung/
├── spec.md          ✅ Anforderungsspezifikation (12 Seiten)
├── state.yaml       ✅ Feature-State-Tracker
└── ...              ⏳ Weitere Artefakte folgen
```

### 2. Spezifikation umfasst:
- **3 User Stories** mit Gherkin-Szenarien
- **Funktionale Anforderungen** (Steuerung, Höhenbegrenzung)
- **Nicht-funktionale Anforderungen** (Performance, Kompatibilität)
- **Annahmen, Risiken, offene Fragen**
- **Akzeptanzkriterien-Checkliste** (10 Punkte)

---

## 🎯 Feature-Beschreibung

**Als Spieler möchte ich:**
- Mit **Leertaste** aufsteigen (+10 Blöcke/sec)
- Mit **Shift** absteigen (-10 Blöcke/sec)
- Meine **aktuelle Höhe** im HUD sehen

**Begrenzungen:**
- Minimum: Y=0 (Grundgestein)
- Maximum: Y=256 (Weltlimit)

---

## 📊 Nächste Schritte im Spec-Flow Workflow

### Phase 2: `/clarify` (Offene Fragen klären)
```bash
# Im Gemini CLI:
/clarify
```

**Zu klärende Fragen:**
1. Soll es eine automatische Kollisionsvermeidung geben?
2. Soll die maximale Steiggeschwindigkeit konfigurierbar sein?
3. Soll es visuelle Effekte beim Steigen geben?

---

### Phase 3: `/plan` (Architektur-Planung)
```bash
# Im Gemini CLI:
/plan
```

**Erwarteter Output:**
- Welche Klassen müssen geändert werden?
- Wie wird die Y-Bewegung implementiert?
- Gibt es Wiederverwendungsmöglichkeiten?

**Vermutete Änderungen:**
```
ShipEntity.java          ← Y-Bewegung hinzufügen
HelmInputClient.java     ← Leertaste/Shift erfassen
HelmInputC2SPayload.java ← Vertical-Input hinzufügen
ShipEntityRenderer.java  ← Ggf. Rendering anpassen
```

---

### Phase 4: `/tasks` (Task-Zerlegung)
```bash
# Im Gemini CLI:
/tasks
```

**Erwartete Tasks (ca. 20-30):**
```markdown
1. [ ] VerticalInput-Klasse erstellen (1h)
2. [ ] HelmInputC2SPayload um vertical erweitern (2h)
3. [ ] ShipEntity.inputVertical hinzufügen (1h)
4. [ ] ShipEntity.tick() um Y-Bewegung ergänzen (3h)
5. [ ] Höhenbegrenzung implementieren (2h)
6. [ ] Test: Vertical movement basic (2h)
7. [ ] Test: Height limits (2h)
8. [ ] Test: Network synchronization (3h)
...
```

---

### Phase 5: `/implement` (Implementation mit TDD)
```bash
# Im Gemini CLI:
/implement
```

**Ablauf:**
1. Test schreiben (RED)
2. Code schreiben (GREEN)
3. Refactoren (REFACTOR)
4. Quality Gates (nach je 3-4 Tasks)

---

### Phase 6: `/optimize` (Quality Gates)
```bash
# Im Gemini CLI:
/optimize
```

**Checks:**
- ✅ Performance (Ship-Rendering Benchmark)
- ✅ Security (Dependency Scan)
- ✅ Code Review (3-Agent-Voting)
- ✅ E2E Tests (Vibecraft RPA)
- ✅ Coverage (> 80%)

---

### Phase 7: `/ship` (Deployment)
```bash
# Im Gemini CLI:
/ship
```

**Output:**
- Build auf Maven/Modrinth
- Release Notes auf GitHub
- CHANGELOG Update

---

## 🔧 So arbeitest du mit dem Feature

### Im Gemini CLI (Empfohlen)
```bash
cd MOD_shark-engine/sharkengine

# Feature fortsetzen
/feature continue

# Oder spezifische Phase starten
/clarify
/plan
/tasks
/implement
```

### Manuelles Arbeiten (Nicht empfohlen)
```bash
# Arbeitsverzeichnis
cd specs/001-vertikale-bewegung/

# State manuell updaten (Nur wenn nötig!)
vim state.yaml
```

---

## 📁 Wichtige Dateien

| Datei | Zweck | Status |
|-------|-------|--------|
| `specs/001-vertikale-bewegung/spec.md` | Anforderungsspezifikation | ✅ Fertig |
| `specs/001-vertikale-bewegung/state.yaml` | Feature-State-Tracker | ✅ Fertig |
| `specs/001-vertikale-bewegung/plan.md` | Architektur-Plan | ⏳ Pending |
| `specs/001-vertikale-bewegung/tasks.md` | Task-Liste | ⏳ Pending |
| `src/main/java/.../ShipEntity.java` | Implementation | ⏳ Pending |

---

## 🎯 Erfolgskriterien

### Muss-Kriterien (Definition of Done)
- [ ] Aufsteigen mit Leertaste funktioniert
- [ ] Absteigen mit Shift funktioniert
- [ ] Höhenbegrenzung (Y=0 bis Y=256) wird eingehalten
- [ ] Alle Unit Tests bestehen (> 80% Coverage)
- [ ] E2E Test mit Vibecraft RPA besteht
- [ ] Code-Review bestanden
- [ ] Keine Checkstyle-Warnings

### Kann-Kriterien (Nice-to-have)
- [ ] Visuelle Effekte beim Steigen
- [ ] Konfigurierbare Steiggeschwindigkeit
- [ ] Automatische Kollisionsvermeidung

---

## 📈 Metriken (Live)

| Metrik | Wert | Ziel |
|--------|------|------|
| Gesamt-Tasks | 0 | ~25 |
| Abgeschlossene Tasks | 0 | 25 |
| Test-Abdeckung | 0% | > 80% |
| Dauer (Stunden) | 0 | ~16 |
| Offene Fragen | 3 | 0 |

---

## 🚧 Blocker & Risiken

### Aktuelle Blocker
- Keine

### Potenzielle Risiken
| Risiko | Wahrscheinlichkeit | Auswirkung | Mitigation |
|--------|-------------------|------------|------------|
| Multiplayer-Desync | Niedrig | Mittel | Server-Validierung |
| Kollision mit Blöcken | Mittel | Hoch | Spätere Iteration |
| Performance-Probleme | Niedrig | Mittel | Load-Testing |

---

## 💡 Spec-Flow Integration

### Automatische Hooks
- ✅ State-Tracker wird bei Phasenwechsel aktualisiert
- ✅ Command-History wird protokolliert
- ⏳ Learnings werden gespeichert (nach Feature-Abschluss)

### Manuelle Commands
```bash
# Feature-Status anzeigen
cat specs/001-vertikale-bewegung/state.yaml

# Spec-Flow Health Check
.spec-flow/scripts/bash/workflow-health.sh

# Command History anzeigen
cat .spec-flow/memory/command-history.yaml
```

---

## 📝 Notizen

### 2026-02-25: Feature gestartet
- Spec-Flow Workspace erstellt
- Spezifikation verfasst (12 Seiten)
- State-Tracker initialisiert
- Nächster Schritt: `/clarify` Phase

---

## 🔗 Nützliche Links

- [Spec-Flow Verständnis-Guide](SPEC_FLOW_VERSTAENDNIS.md)
- [Project Constitution](.spec-flow/memory/constitution.md)
- [Spec-Flow Commands](.claude/commands/README.md)
- [Shark Engine README](README.md)

---

**Zuständig:** DYAI  
**Review Required:** Nein (Spec-Flow übernimmt Quality Gates)  
**Nächster Meilenstein:** `/clarify` Phase abgeschlossen

---

## 🎮 So testest du das Feature (sobald implementiert)

### Singleplayer Test
```
1. Minecraft mit Mod starten
2. Kreativwelt erstellen
3. Schiff bauen (mit Steuerrad)
4. Steuerrad rechtsklicken
5. Leertaste drücken → Schiff sollte aufsteigen
6. Shift drücken → Schiff sollte absteigen
```

### Multiplayer Test
```
1. Server mit Mod starten
2. Mit Client verbinden
3. Schiff bauen und besteigen
4. Vertikale Bewegung testen
5. Anderen Client verbinden lassen
6. Synchronisation prüfen
```

---

**Viel Erfolg beim Implementieren! 🚀**

Bei Fragen: Spec-Flow dokumentiert jeden Schritt automatisch!
