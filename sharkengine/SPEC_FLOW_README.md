# Spec-Flow Workflow für Shark Engine

Dieses Projekt verwendet **Spec-Flow** für strukturierte, KI-gestützte Entwicklung mit dem Gemini CLI.

## Schnellstart

### Erstes Feature erstellen

```bash
# Im sharkengine Verzeichnis
cd /home/dyai/Dokumente/Pers.Tests-Page/social-role/DYAI_home/DEV/TOOLS/Minecraft_Development-2025.3-1.8.11/Minecraft Development/MOD_shark-engine/sharkengine

# Feature im Gemini CLI starten
/feature "vertikale Schiffsbewegung"
```

### Verfügbare Befehle im Gemini CLI

| Befehl | Beschreibung |
|--------|-------------|
| `/feature "name"` | Startet einen Feature-Workflow (< 16 Stunden) |
| `/epic "ziel"` | Startet einen Epic-Workflow (mehrere Sprints) |
| `/quick "fix"` | Schnelle Änderung (< 30 Minuten) |
| `/help` | Kontextbezogene Hilfe |

### Phasen eines Feature-Workflows

1. **Spezifikation** (`/spec`) - Anforderungen in Gherkin formulieren
2. **Planung** (`/plan`) - Architektur und Komponenten designen
3. **Tasks** (`/tasks`) - In TDD-Aufgaben zerlegen
4. **Implementierung** (`/implement`) - Test-First Entwicklung
5. **Optimierung** (`/optimize`) - Quality Gates durchlaufen
6. **Deployment** (`/ship`) - In Staging/Production überführen

## Beispiel: Vertikale Bewegung implementieren

```
/feature "Schiffe können auf und ab steigen"

→ Spec-Flow erstellt automatisch:
  - specs/001-vertikale-bewegung/spec.md (Spezifikation)
  - specs/001-vertikale-bewegung/plan.md (Plan)
  - specs/001-vertikale-bewegung/tasks.md (Tasks)
  - Implementierung im Code
  - Tests und Quality Gates
```

## Projekt-Konstitution

Die Datei `.spec-flow/memory/constitution.md` enthält:
- Projektübersicht und Architektur
- Technische Stack-Informationen
- Entwicklungsprinzipien
- Quality Gates
- Zukünftige Roadmap

## Spec-Flow mit Gemini CLI

Dieses Projekt ist konfiguriert für die Verwendung mit:
- **Gemini CLI Extension:** `gemini extensions install https://github.com/marcusgoll/Spec-Flow`
- **Spezialisierte Agents:** Backend, Frontend, Testing, Security
- **Quality Feedback Loop:** Automatische Code-Reviews und Tests

## Best Practices

1. **Spezifikation zuerst:** Immer mit `/spec` beginnen
2. **Test-First:** Tests vor Implementierung schreiben
3. **Kleine Features:** Lieber viele kleine als wenige große Features
4. **Quality Gates:** Alle Checks müssen bestehen
5. **Dokumentation:** Jede Entscheidung wird festgehalten

## Fortsetzung unterbrochener Arbeit

```bash
# Nach einer Pause einfach fortsetzen
/feature continue
```

## Spezifische Use Cases für Shark Engine

### Neue Schiffsfunktion
```
/feature "Schiff hat inventar"
```

### Performance-Optimierung
```
/feature "rendering-performance-verbessern"
```

### Bug Fix mit Regression Test
```
/quick "fix schiff-kollision-client-seite"
```

### Großes Feature (Epic)
```
/epic "mehrere steuerräder mit kontroll-übergabe"
```

## Nächste Schritte

1. **Gemini CLI öffnen**
2. **Erstes Feature starten:** `/feature "deine idee"`
3. **Spec-Flow macht den Rest** 🚀

---

**Dokumentation:** Siehe `.claude/commands/` für alle Befehle  
**Agents:** Siehe `.claude/agents/` für spezialisierte Personas  
**Skills:** Siehe `.claude/skills/` für spezielle Fähigkeiten
