# Feature-Spezifikation: Vertikale Schiffsbewegung

**Feature ID:** 001  
**Name:** Vertikale Schiffsbewegung  
**Status:** In Entwicklung  
**Erstellt:** 2026-02-25  
**Version:** 1.0  

---

## 1. Zusammenfassung

### 1.1 Feature-Beschreibung
Dieses Feature ermöglicht es Spielern, ihr Schiff vertikal zu bewegen (auf- und absteigen) während der Fahrt.

### 1.2 Business Value
- Spieler können Hindernisse wie Berge oder Schluchten überwinden
- Neue Gameplay-Möglichkeiten (tauchende U-Boote, fliegende Schiffe)
- Erhöhte Spielbarkeit in unterschiedlichen Terrains

### 1.3 Scope
**In Scope:**
- Aufsteigen mit Leertaste
- Absteigen mit Shift-Taste
- Höhenbegrenzung (Y=0 bis Y=256)
- Geschwindigkeitsbegrenzung (max. 10 Blöcke/sec)

**Out of Scope:**
- Automatische Höhenregelung
- Kollisionsvermeidung
- Neigungs-Physik beim Steigen

---

## 2. User Stories

### US-001: Als Spieler möchte ich aufsteigen können
```gherkin
Feature: Schiff aufsteigen
  Als Pilot eines Schiffs
  Möchte ich durch Halten der Leertaste aufsteigen können
  Damit ich Hindernisse überwinden kann

  Szenario: Aufsteigen im freien Raum
    Gegeben das Schiff befindet sich auf Y=64
    Wenn ich die Leertaste gedrückt halte
    Dann steigt das Schiff mit 10 Blöcken/sec auf
    Und die Y-Position erhöht sich kontinuierlich

  Szenario: Maximale Höhe erreichen
    Gegeben das Schiff befindet sich auf Y=255
    Wenn ich versuche weiter aufzusteigen
    Dann bleibt das Schiff bei Y=255 stehen
```

### US-002: Als Spieler möchte ich absteigen können
```gherkin
Feature: Schiff absteigen
  Als Pilot eines Schiffs
  Möchte ich durch Halten der Shift-Taste absteigen können
  Damit ich zu niedrigeren Ebenen gelangen kann

  Szenario: Absteigen im freien Raum
    Gegeben das Schiff befindet sich auf Y=100
    Wenn ich die Shift-Taste gedrückt halte
    Dann steigt das Schiff mit 10 Blöcken/sec ab
    Und die Y-Position verringert sich kontinuierlich

  Szenario: Minimale Höhe erreichen
    Gegeben das Schiff befindet sich auf Y=1
    Wenn ich versuche weiter abzusteigen
    Dann bleibt das Schiff bei Y=0 stehen
```

### US-003: Als Spieler möchte ich meine aktuelle Höhe sehen
```gherkin
Feature: Höhenanzeige
  Als Spieler
  Möchte ich die aktuelle Y-Position meines Schiffs sehen
  Damit ich meine Position im Weltkoordinatensystem kenne

  Szenario: Höhenanzeige während der Fahrt
    Gegeben ich pilotiere ein Schiff
    Wenn ich das Schiff steuere
    Dann wird die aktuelle Y-Position im HUD angezeigt
```

---

## 3. Funktionale Anforderungen

### 3.1 Steuerung
| Eingabe | Aktion | Geschwindigkeit |
|---------|--------|-----------------|
| Leertaste (gehalten) | Aufsteigen | +10 Blöcke/sec |
| Shift (gehalten) | Absteigen | -10 Blöcke/sec |
| Keine Eingabe | Höhe halten | 0 Blöcke/sec |

### 3.2 Höhenbegrenzung
| Grenze | Wert | Verhalten |
|--------|------|-----------|
| Minimum | Y=0 | Stoppt Absteigen |
| Maximum | Y=256 | Stoppt Aufsteigen |

### 3.3 Netzwerk-Synchronisation
- Client-Input wird an Server gesendet (HelmInputC2SPayload erweitern)
- Server validiert Höhenbegrenzung
- Server synchronisiert Y-Position an alle Clients (ShipBlueprintS2CPayload)

---

## 4. Nicht-Funktionale Anforderungen

### 4.1 Performance
- Input-Latenz: < 50ms (Client → Server)
- Position-Update-Rate: 20 Hz (jeder Tick)
- Keine FPS-Einbußen durch vertikale Bewegung

### 4.2 Kompatibilität
- Muss mit existierender WASD-Steuerung kombinierbar sein
- Muss im Singleplayer und Multiplayer funktionieren
- Muss mit Anchor-System kompatibel sein

### 4.3 Code-Qualität
- Test-Abdeckung: > 80% für neue Klassen
- Keine neuen Checkstyle-Warnings
- Javadoc für alle öffentlichen Methoden

---

## 5. Annahmen

| ID | Annahme | Risiko |
|----|---------|--------|
| A-001 | Minecraft Weltkoordinaten verwenden Y-Achse für Höhe | Niedrig |
| A-002 | Existierende ShipEntity kann um Y-Bewegung erweitert werden | Mittel |
| A-003 | Client-Input kann ohne große Latenz zum Server übertragen werden | Niedrig |

---

## 6. Offene Fragen (Geklärt in /clarify Phase)

| ID | Frage | Priorität | Entscheidung | Begründung |
|----|-------|-----------|--------------|------------|
| Q-001 | Soll es eine automatische Kollisionsvermeidung geben? | Niedrig | ❌ Nein | Komplexität gering halten für MVP |
| Q-002 | Soll die maximale Steiggeschwindigkeit konfigurierbar sein? | Mittel | ❌ Nein | 10 Blöcke/sec ist guter Mittelwert |
| Q-003 | Soll es visuelle Effekte beim Steigen geben? | Niedrig | ❌ Nein | "Nice-to-have", kann später kommen |
| Q-004 | Wie wird vertikale mit horizontaler Bewegung kombiniert? | Hoch | ✅ Kombinierbar | Besseres Spielerlebnis, 3D-Bewegung |
| Q-005 | Was passiert bei aktiviertem Anchor? | Mittel | ⚠️ Anchor blockiert | Konsistentes Verhalten |
| Q-006 | Soll Steigen Energie/Treibstoff verbrauchen? | Mittel | ❌ Nein | Kein Treibstoff-System im MVP |

---

## 7. Erfolgsmetriken

| Metrik | Ziel | Messung |
|--------|------|---------|
| Test-Abdeckung | > 80% | JaCoCo Report |
| Build-Stabilität | 100% | CI/CD Pipeline |
| Performance | < 1ms pro Tick | Profiling |
| User-Feedback | Positiv | Playtesting |

---

## 8. Abhängigkeiten

| ID | Abhängigkeit | Status |
|----|--------------|--------|
| D-001 | ShipEntity Klasse | Vorhanden |
| D-002 | HelmInputClient | Vorhanden |
| D-003 | HelmInputC2SPayload | Muss erweitert werden |
| D-004 | ShipEntityRenderer | Vorhanden |

---

## 9. Risiken

| ID | Risiko | Wahrscheinlichkeit | Auswirkung | Mitigation |
|----|--------|-------------------|------------|------------|
| R-001 | Kollision mit Blöcken wird nicht erkannt | Mittel | Hoch | Spätere Iteration |
| R-002 | Multiplayer-Desync bei schneller Bewegung | Niedrig | Mittel | Server-Validierung |
| R-003 | Performance-Probleme bei vielen Schiffen | Niedrig | Mittel | Load-Testing |

---

## 10. Akzeptanzkriterien-Checkliste

- [ ] Aufsteigen mit Leertaste funktioniert
- [ ] Absteigen mit Shift funktioniert
- [ ] Höhenbegrenzung (Y=0 bis Y=256) wird eingehalten
- [ ] Geschwindigkeitsbegrenzung (10 Blöcke/sec) wird eingehalten
- [ ] Funktioniert im Singleplayer
- [ ] Funktioniert im Multiplayer
- [ ] Alle Tests bestehen
- [ ] Code-Review bestanden
- [ ] Performance-Check bestanden

---

## 11. Änderungshistorie

| Version | Datum | Autor | Änderung |
|---------|-------|-------|----------|
| 1.0 | 2026-02-25 | Spec-Flow | Initiale Spezifikation |

---

## 12. Nächste Schritte

1. ✅ Spezifikation erstellt
2. ⏳ `/clarify` - Offene Fragen klären
3. ⏳ `/plan` - Architektur-Planung
4. ⏳ `/tasks` - Task-Zerlegung
5. ⏳ `/implement` - Implementation
6. ⏳ `/optimize` - Quality Gates
7. ⏳ `/ship` - Deployment

---

**Spezifikation genehmigt von:** _______________  
**Datum:** _______________
