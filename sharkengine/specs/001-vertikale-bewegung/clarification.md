# Clarification Report: Feature 001 - Vertikale Schiffsbewegung

**Feature:** 001-vertikale-bewegung  
**Phase:** Clarify  
**Datum:** 2026-02-25  
**Status:** ❓ Fragen zur Klärung offen  

---

## 📋 Offene Fragen aus der Spezifikation

### Q-001: Automatische Kollisionsvermeidung

**Frage:** Soll es eine automatische Kollisionsvermeidung geben?

**Kontext:**
- Wenn das Schiff beim Aufsteigen auf Blöcke trifft
- Aktuell keine Kollisionserkennung im MVP

**Optionen:**
1. ❌ **Nein** - Spieler muss selbst auf Kollision achten (einfacher)
2. ⚠️ **Teilweise** - Stoppt Bewegung bei Blockkontakt (mittel)
3. ✅ **Ja** - Weicht automatisch aus (komplex)

**Empfehlung:** Option 1 (Nein) für MVP
- Begründung: Komplexität gering halten
- Kann in späterer Iteration hinzugefügt werden
- Spieler lernt sorgfältige Navigation

**Entscheidung:** _______________

---

### Q-002: Konfigurierbare Steiggeschwindigkeit

**Frage:** Soll die maximale Steiggeschwindigkeit konfigurierbar sein?

**Kontext:**
- Standard: 10 Blöcke/sec
- Balance zwischen Spielbarkeit und Realismus

**Optionen:**
1. ❌ **Nein** - Fester Wert (10 Blöcke/sec)
2. ⚠️ **Config-Datei** - Technisch konfigurierbar, aber nicht im Spiel
3. ✅ **Im Spiel** - Über Befehl oder UI einstellbar

**Empfehlung:** Option 1 (Nein) für MVP
- Begründung: Weniger Code, weniger Tests
- 10 Blöcke/sec ist guter Mittelwert
- Kann später als Upgrade hinzugefügt werden

**Entscheidung:** _______________

---

### Q-003: Visuelle Effekte beim Steigen

**Frage:** Soll es visuelle Effekte beim Steigen geben?

**Kontext:**
- Feedback für Spieler verbessern
- Partikel, Sound, oder andere Effekte

**Optionen:**
1. ❌ **Nein** - Keine Effekte (minimal)
2. ⚠️ **Einfach** - Nur Partikel (z.B. Rauchwolken)
3. ✅ **Komplex** - Partikel + Sound + Kamera-Wackeln

**Empfehlung:** Option 1 (Nein) für MVP
- Begründung: Visuelle Effekte sind "nice-to-have"
- Lenkt von Kernfunktionalität ab
- Kann als kosmetisches Upgrade später kommen

**Entscheidung:** _______________

---

## 🔍 Zusätzliche Klärungen (neu entdeckt)

### Q-004: Kombination mit WASD-Steuerung

**Frage:** Wie wird vertikale Bewegung mit horizontaler kombiniert?

**Kontext:**
- Leertaste + W = Aufsteigen während Vorwärtsfahrt?
- Shift + A = Absteigen während Links-Drehung?

**Optionen:**
1. ✅ **Ja, kombinierbar** - Volle 3D-Bewegung (empfohlen)
2. ❌ **Nein, exklusiv** - Nur vertikal ODER horizontal

**Empfehlung:** Option 1 (Ja, kombinierbar)
- Begründung: Besseres Spielerlebnis
- Technische Umsetzung: Input-Vektoren addieren
- Erwartetes Verhalten: Diagonale Bewegung möglich

**Entscheidung:** _______________

### Q-005: Anchor-Verhalten beim vertikalen Steigen

**Frage:** Was passiert wenn man bei aktiviertem Anchor aufsteigt?

**Kontext:**
- Anchor-System existiert bereits
- Verhindert normale Bewegung

**Optionen:**
1. ⚠️ **Anchor blockiert Vertikal-Bewegung** (konsistent)
2. ✅ **Vertikal-Bewegung ignoriert Anchor** (flexibel)
3. ❌ **Anchor wird automatisch deaktiviert** (verwirrend)

**Empfehlung:** Option 1 (Anchor blockiert)
- Begründung: Konsistentes Verhalten
- Spieler muss erst Anchor deaktivieren
- Verhindert versehentliches Aufsteigen

**Entscheidung:** _______________

### Q-006: Energie/Treibstoff-System

**Frage:** Soll vertikales Steigen Energie/Treibstoff verbrauchen?

**Kontext:**
- Balance-Aspekt für Gameplay
- Verhindert endloses Auf/Ab-Steigen

**Optionen:**
1. ❌ **Nein** - Kostenlos (spielerfreundlich)
2. ⚠️ **Ja, aber regenerierend** - Langsame Regeneration
3. ✅ **Ja, mit Tank-System** - Muss nachgefüllt werden

**Empfehlung:** Option 1 (Nein) für MVP
- Begründung: Komplexität gering halten
- Kein Treibstoff-System im aktuellen Design
- Kann als Difficulty-Feature später kommen

**Entscheidung:** _______________

---

## 📊 Zusammenfassung der Entscheidungen

| Frage | Empfehlung | Priorität | Impact |
|-------|------------|-----------|--------|
| Q-001: Kollisionsvermeidung | Nein | Niedrig | Mittel |
| Q-002: Konfigurierbare Geschwindigkeit | Nein | Mittel | Niedrig |
| Q-003: Visuelle Effekte | Nein | Niedrig | Niedrig |
| Q-004: WASD-Kombination | Ja | Hoch | Hoch |
| Q-005: Anchor-Verhalten | Blockiert | Mittel | Mittel |
| Q-006: Energie-System | Nein | Mittel | Hoch |

---

## 🎯 Nächste Schritte

1. **Entscheidungen treffen** für alle 6 Fragen
2. **Spec.md aktualisieren** mit den Antworten
3. **Zu `/plan` Phase übergehen**

---

## 📝 Notizen für die Plan-Phase

Basierend auf den Empfehlungen:

**Architektur-Entscheidungen:**
- Input-Handling muss WASD + Vertikal kombinieren
- Anchor-Check vor Bewegungs-Update
- Keine Kollisionsvermeidung (spart CPU-Zyklen)
- Keine visuellen Effekte (spart Rendering)

**Zu implementierende Klassen:**
- `VerticalInput.java` (neu)
- `ShipEntity.java` (erweitern um Y-Bewegung)
- `HelmInputClient.java` (erweitern um Leertaste/Shift)
- `HelmInputC2SPayload.java` (erweitern um vertical-Feld)

**Tests:**
- Unit-Test: Vertical movement bounds
- Integration-Test: WASD + Vertikal Kombination
- Multiplayer-Test: Synchronisation

---

**Clarification abgeschlossen:** ☐ Ja  ☐ Nein  
**Bereit für Plan-Phase:** ☐ Ja  ☐ Nein  

**Datum:** _______________  
**Von:** _______________
