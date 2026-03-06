# Spec-Flow Verständnis-Guide für Shark Engine

## Was ist Spec-Flow? 🤔

**Spec-Flow ist ein strukturierter Workflow für KI-gestützte Softwareentwicklung.**

Statt einfach zu sagen "bau mir ein Feature", führt Spec-Flow dich durch einen **bewährten Prozess** mit:
- Klaren Phasen (Spezifikation → Planung → Tasks → Implementation → Tests → Deployment)
- Automatisierten Quality Gates
- Spezialisierten KI-Agenten für jede Phase
- Vollständiger Dokumentation aller Entscheidungen

---

## Die 6 Kern-Phasen 📋

### 1. `/spec` - Spezifikation
**Ziel:** Verstehen, WAS gebaut werden soll

```
/feature "vertikale Schiffsbewegung"
  ↓
/spec
  ↓
Erstellt: spec.md mit:
- User Stories (Als Spieler möchte ich...)
- Akzeptanzkriterien (Das Schiff muss auf/ab steigen können)
- Erfolgsmetriken (95% der Tests müssen passieren)
```

**Output:** `specs/001-vertikale-bewegung/spec.md`

---

### 2. `/clarify` - Klärung (Optional)
**Ziel:** Unklare Anforderungen auflösen

Spec-Flow analysiert die Spezifikation auf:
- Mehrdeutigkeiten (Was bedeutet "schnell"?)
- Fehlende Informationen (Welche Tasten?)
- Widersprüche (Aufsteigen vs. Kollision)

**Automatisch bei Unklarheit > 30%**

---

### 3. `/plan` - Architektur-Planung
**Ziel:** Verstehen, WIE es gebaut wird

```
/plan
  ↓
Erstellt: plan.md mit:
- Architektur-Entscheidungen (Entity-Komponente für Y-Bewegung)
- Betroffene Dateien (ShipEntity.java, HelmInputClient.java)
- Risiken (Performance bei vielen Schiffen)
- Wiederverwendung (Bestehende Physik nutzen)
```

**Output:** `specs/001-vertikale-bewegung/plan.md`

---

### 4. `/tasks` - Task-Zerlegung
**Ziel:** In umsetzbare Schritte zerlegen

```
/tasks
  ↓
Erstellt: tasks.md mit 20-30 konkreten Tasks:
1. [ ] Yaw-Komponente zu ShipEntity hinzufügen (2h)
2. [ ] VerticalInput-Klasse erstellen (1h)
3. [ ] HelmInputC2SPayload um vertical erweitern (1h)
4. [ ] ShipEntity.tick() um Y-Bewegung ergänzen (3h)
5. [ ] Test: Vertical movement basic (2h)
...
```

**Jeder Task hat:**
- Klare Definition of Done
- Geschätzte Zeit
- Abhängigkeiten (Task 3 braucht Task 1)
- TDD-Reihenfolge (Test zuerst!)

**Output:** `specs/001-vertikale-bewegung/tasks.md`

---

### 5. `/implement` - Implementation
**Ziel:** Code schreiben mit Tests

```
/implement
  ↓
Für jeden Task:
1. Test schreiben (RED)
2. Code schreiben (GREEN)
3. Refactoren (REFACTOR)
4. Quality Gates prüfen
```

**Automatische Checks nach je 3-4 Tasks:**
- ✅ Linting
- ✅ Type-Checking
- ✅ Unit Tests
- ✅ Coverage (darf nicht sinken)

**Output:** Implementierter Code in `src/`

---

### 6. `/optimize` - Quality Gates
**Ziel:** Produktionsreife sicherstellen

```
/optimize
  ↓
Parallele Checks:
├─ Performance (Benchmark Ship-Rendering)
├─ Security (Keine vulnerablen Dependencies)
├─ Accessibility (UI-Barrierefreiheit)
├─ Code Review (KI-Review mit 3-Agent-Voting)
├─ E2E Tests (RPA-Tests im echten Spiel)
└─ Migration (Datenbank-Changes reversibel?)
```

**Dauer:** 10-15 Minuten  
**Bei Fehlern:** Blockiert Deployment bis gefixt

**Output:** `specs/001-vertikale-bewegung/optimization-report.md`

---

### 7. `/ship` - Deployment
**Ziel:** In Staging/Production überführen

```
/ship
  ↓
1. Staging Deployment
2. Validation (Health Checks)
3. Production Promotion
4. Rollback-Plan erstellen
```

**Output:** Release auf GitHub/Maven

---

## Spec-Flow Architektur 🏗️

```
┌─────────────────────────────────────────────────────────┐
│                    Spec-Flow Workflow                    │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  User Input: "/feature 'vertikale Bewegung'"            │
│       ↓                                                   │
│  ┌──────────────────────────────────────────────┐       │
│  │  Phase Agents (Spezialisierte KI-Rollen)     │       │
│  │  ├─ spec-agent (Anforderungen)               │       │
│  │  ├─ plan-agent (Architektur)                 │       │
│  │  ├─ tasks-agent (Zerlegung)                  │       │
│  │  ├─ backend-dev (Implementation)             │       │
│  │  ├─ qa-tester (Tests)                        │       │
│  │  └─ code-reviewer (Review)                   │       │
│  └──────────────────────────────────────────────┘       │
│       ↓                                                   │
│  ┌──────────────────────────────────────────────┐       │
│  │  Quality Gates (Automatisierte Checks)       │       │
│  │  ├─ Level 1: Continuous (< 30s)              │       │
│  │  ├─ Level 2: Full Gates (10-15min)           │       │
│  │  └─ Level 3: Critical (< 2min)               │       │
│  └──────────────────────────────────────────────┘       │
│       ↓                                                   │
│  ┌──────────────────────────────────────────────┐       │
│  │  Artifacts (Dokumentation)                   │       │
│  │  ├─ spec.md, plan.md, tasks.md               │       │
│  │  ├─ optimization-report.md                   │       │
│  │  └─ walkthrough.md                           │       │
│  └──────────────────────────────────────────────┘       │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## Wichtige Konzepte 💡

### 1. **Progressive Gates** 🚦

Spec-Flow hat **3 Level** von Quality Checks:

| Level | Wann | Dauer | Bei Fehler |
|-------|------|-------|------------|
| **Continuous** | Nach je 3-4 Tasks | < 30s | Warnung (weitermachen) |
| **Full** | Nach `/implement` | 10-15min | Blockiert (fixen!) |
| **Critical** | Vor `/ship` | < 2min | Blockiert Deployment |

---

### 2. **Ultrathink Philosophie** 🧠

**"Eine Stunde tiefes Denken spart 10 Stunden Refactoring"**

Spec-Flow baut **Denk-Checkpoint**s ein:

```
┌─────────────────────────────────────────┐
│ 💭 ULTRATHINK: Think Different          │
├─────────────────────────────────────────┤
│ Bevor du startest:                      │
│ • Lösen wir das RICHTIGE Problem?       │
│ • Welche Annahmen treffen wir?          │
│ • Gibt's einen einfacheren Weg?         │
└─────────────────────────────────────────┘
```

**Checkpoint-Arten:**
- **Think Different** (in `/spec`) - Problem hinterfragen
- **Obsess Over Details** (in `/plan`) - Codebase-Muster analysieren
- **Simplify Ruthlessly** (in `/tasks`) - Tasks reduzieren
- **Craft, Don't Code** (in `/implement`) - Vor Patterns suchen

---

### 3. **Domain Memory** 💾

Spec-Flow merkt sich **projekt-spezifisches Wissen**:

```yaml
# .spec-flow/learnings/performance-patterns.yaml
- pattern: "Ship-Rendering optimieren"
  confidence: 0.95
  applied: 3x
  description: "BlockRenderDispatcher batchen reduziert Draw-Calls"
  
# Wird automatisch in neuen Features angewendet!
```

**Lernt aus:**
- Erfolgreichen Patterns
- Fehlern (Anti-Patterns)
- Abkürzungen (z.B. "Ship" = Schiff-Entity)

---

### 4. **Worktree Isolation** 🌳

Für **komplexe Features** erstellt Spec-Flow isolierte Git-Worktrees:

```
sharkengine/
├── .git/worktrees/
│   └── feature-vertikal/    # Isoliertes Repository
│       ├── src/             # Eigener Code
│       └── .git/            # Eigener Git-Status
└── src/                     # Haupt-Repository
```

**Vorteile:**
- Keine Merge-Konflikte zwischen Features
- Parallele Entwicklung möglich
- Saubere Historie pro Feature

---

## Spec-Flow für Shark Engine 🦈

### Typischer Workflow

```bash
# 1. Feature starten
/feature "Schiffe können tauchen"

# 2. Spec-Flow erstellt Workspace
specs/002-tauchen/
├── spec.md           # Anforderungen
├── plan.md           # Architektur
├── tasks.md          # 25 Tasks
└── ...

# 3. Durch Phasen navigieren
/spec      # Spezifikation verfeinern
/plan      # Architektur planen
/tasks     # In Tasks zerlegen
/implement # Implementieren
/optimize  # Quality Gates
/ship      # Deployen

# 4. Fortschritt prüfen
cat specs/002-tauchen/tasks.md
```

---

### Spec-Flow + MCProtocolLib Integration

**Später möglich:**

```groovy
// Spec-Flow erstellt Test-Tasks automatisch
tasks.md:
15. [ ] MCProtocolLib Test für Ship-Sync schreiben (3h)
    - Test: Client empfängt Ship-Blueprint
    - Test: Multiplayer Ship-Position
```

---

## Spec-Flow Commands Übersicht 📜

### Core Commands
| Command | Beschreibung | Dauer |
|---------|--------------|-------|
| `/feature "name"` | Feature-Workflow starten | < 16h |
| `/epic "ziel"` | Epic-Workflow (mehrere Sprints) | > 16h |
| `/quick "fix"` | Schnelle Änderung | < 30min |

### Phasen-Commands
| Command | Phase | Output |
|---------|-------|--------|
| `/spec` | Spezifikation | spec.md |
| `/clarify` | Klärung | clarification in spec.md |
| `/plan` | Planung | plan.md |
| `/tasks` | Zerlegung | tasks.md |
| `/implement` | Implementation | Code in src/ |
| `/optimize` | Quality Gates | optimization-report.md |
| `/ship` | Deployment | Release |

### Hilfs-Commands
| Command | Beschreibung |
|---------|--------------|
| `/help` | Kontext-Hilfe |
| `/review` | Code Review (on-demand) |
| `/debug` | Debugging-Session |
| `/roadmap` | GitHub Issues verwalten |

---

## Spec-Flow Dateistruktur 📁

```
sharkengine/
├── .claude/                    # Spec-Flow Konfiguration
│   ├── agents/                 # KI-Agenten (spec-agent, plan-agent...)
│   ├── commands/               # Command-Definitionen
│   ├── skills/                 # Fähigkeiten (git-workflow, testing...)
│   └── hooks/                  # Automatische Hooks
│
├── .spec-flow/                 # Spec-Flow Core
│   ├── config/                 # Konfigurationen
│   │   ├── phases.yaml        # Phasen-Sequenzen
│   │   ├── user-preferences.yaml
│   │   ├── progressive-gates.yaml
│   │   └── ultrathink-integration.yaml
│   ├── memory/                 # Projekt-Wissen
│   │   └── constitution.md    # Projekt-Verfassung
│   ├── scripts/               # Automatisierung
│   └── templates/             # Vorlagen
│
├── specs/                      # Feature-Workspaces
│   ├── 001-vertikale-bewegung/
│   │   ├── spec.md
│   │   ├── plan.md
│   │   ├── tasks.md
│   │   └── optimization-report.md
│   └── 002-tauchen/
│       └── ...
│
└── src/                        # Dein Code
```

---

## Nächste Schritte 🚀

### 1. **Erstes Feature testen**
```bash
cd MOD_shark-engine/sharkengine

# Im Gemini CLI:
/feature "Schiff hat Inventar"
```

### 2. **Spec-Flow verstehen**
- Lies `.claude/commands/core/feature.md`
- Schau dir `.spec-flow/memory/constitution.md` an
- Prüfe die Konfiguration in `.spec-flow/config/`

### 3. **Anpassen für Shark Engine**
- `.spec-flow/config/user-preferences.yaml` anpassen
- Project-spezifische Skills erstellen
- Testing-Strategie definieren

---

## Spec-Flow vs. Vibecraft RPA Testing

| Aspekt | Spec-Flow | Vibecraft RPA |
|--------|-----------|---------------|
| **Fokus** | Strukturierter Entwicklungsprozess | Automatisierte UI-Tests |
| **Testing** | Integriert in `/implement` | Separates Test-Framework |
| **Dokumentation** | Automatisch (spec.md, plan.md...) | Manuell |
| **Quality Gates** | 3 Level (Continuous, Full, Critical) | RPA-Tests im Spiel |
| **KI-Agenten** | Spezialisiert pro Phase | Generalist |

**Empfehlung:** Spec-Flow als **Haupt-Workflow**, Vibecraft RPA als **Test-Tool** in `/implement` Phase verwenden!

---

## Fazit ✅

**Spec-Flow gibt dir:**
- ✅ Strukturierten Prozess für KI-Entwicklung
- ✅ Automatische Dokumentation
- ✅ Quality Gates für Produktionsreife
- ✅ Spezialisierte KI-Agenten pro Phase
- ✅ Lernen aus vergangenen Features

**Spec-Flow ist NICHT:**
- ❌ Ein Test-Framework (dafür Vibecraft RPA)
- ❌ Ein Build-Tool (dafür Gradle)
- ❌ Ein Ersatz für KI (es orchestriert KI)

**Für Shark Engine bedeutet das:**
1. Spec-Flow für Feature-Entwicklung verwenden
2. Vibecraft RPA für Integrationstests
3. MCProtocolLib (später) für Protocol-Tests

---

**Fragen?** Frag mich nach spezifischen Aspekten von Spec-Flow! 🎯
