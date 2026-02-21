# Shark Engine – Bestandsaufnahme & Task-Vorschläge Richtung Production-Ready MVP

## 1) Aktueller Stand im Repository (Ist-Zustand)

### Vorhandene Kernbausteine
- **Fabric-Mod-Basis für Minecraft 1.21.1** ist vorhanden (`fabric.mod.json`, Gradle/Loom Setup, Main + Client Entrypoints).
- **Steuerrad-Block** (`steering_wheel`) existiert und triggert auf Rechtsklick eine Assemblierung.
- **Assemblierung per BFS**:
  - sammelt zusammenhängende, tag-basierte Blöcke (`ship_eligible`),
  - limitiert auf `MAX_BLOCKS=512`, Radius `MAX_RADIUS=32`,
  - blockiert Assemblierung bei Weltkontakt (solid neighbor außerhalb der Struktur).
- **ShipEntity** wird als bewegliches Träger-Entity erzeugt, speichert Blueprint, Pilot, Anchor-Status.
- **Input-Netzwerkpfad** ist implementiert:
  - Client liest WASD,
  - sendet `throttle`/`turn` (C2S),
  - Server setzt Inputs am ShipEntity.
- **Bewegung** aktuell 2D-ähnlich:
  - Yaw-Drehung,
  - Vorwärtsbeschleunigung,
  - einfache Drag,
  - keine echte 6DoF-/3D-Steuerung.
- **Client-Rendering** rendert Blueprint-Blöcke am Entity.
- **Disassembly** bei Shift+Rechtsklick (wenn geankert): Struktur wird zurück in die Welt geschrieben.

### Technischer Reifegrad
- **Prototype/MVP-Pre-Alpha**:
  - Solider vertikaler Slice (assemble → bewegen → disassemble),
  - aber noch ohne Produktions-Härtung (Physik, Persistenzdetails, Sicherheitsgrenzen, QA-Tests, Telemetrie).

---

## 2) Gap-Analyse gegen Zielbild (aus Anforderung)

Ziel laut Produktvision:
1. Steuerrad präzise in **allen drei Dimensionen** steuerbar.
2. **Keine Eigenbewegung (Drift)**, keine Ruckler; **flüssig/ruckelfrei**.
3. MVP-Design: **altes Schiffssteuerrad** + **Plasma-/Feuer-Block**.
4. Nur Strukturen mit **Gesamtmasse <= X** und **ohne direkten Bodenkontakt** sind bewegbar.
5. Spieler wählt **Wasser / Rad / Luft**, Eigenschaft als **permanenter, nicht entfernbarer Buff** auf Steuerrad.

### Abweichungen heute
- Keine 3D-Steuerung (nur Vor/Zurück + Yaw).
- Keine robuste Drift-Unterdrückung/Determinismus/Interpolation für „ruckelfrei“.
- Kein Fahrzeugtyp-System (Wasser/Rad/Luft) und kein permanenter Buff-Mechanismus.
- Keine explizite Masseberechnung pro Block (nur Blockanzahl-Limit).
- Kein spezifischer Plasma-/Feuer-Block als Funktionskern.
- Weltkontaktprüfung ist vorhanden, aber noch pragmatisch/simple und nicht auf alle Edge-Cases ausgelegt.

---

## 3) Priorisierte Task-Liste bis Production-Ready MVP

## Phase A – Fundament für Steuerung & Physik (kritisch)

### A1. 6DoF-Steuerungsmodell einführen (Pitch/Yaw/Roll + Thrust X/Y/Z)
**Task:**
- Inputschema von 2 Achsen auf 6 Achsen erweitern (inkl. Keybinding-Config).
- Serverseitiges Integrationsmodell für Rotation + Translation in allen 3 Dimensionen.

**Akzeptanzkriterien:**
- Spieler kann gezielt steigen/sinken, rollen, nicken, drehen.
- Jede Achse einzeln testbar und unabhängig einstellbar.

### A2. Anti-Drift & Stabilisierung
**Task:**
- Dämpfungsmodell überarbeiten (linear + angular damping).
- „Deadzone“ und Auto-Stabilisierung (konfigurierbar) implementieren.
- Optional: PID-ähnliche Stabilizer für Rotationsachsen.

**Akzeptanzkriterien:**
- Bei Null-Input kommt Fahrzeug kontrolliert und reproduzierbar zum Stillstand.
- Keine ungewollte Mikrobewegung über N Ticks.

### A3. Tick-/Netcode-Glättung
**Task:**
- Input-Rate-Limit mit Sequence-ID + letzter verarbeiteter Input.
- Client-seitige Interpolation/Extrapolation für andere Spieler.
- Server-Authority beibehalten, aber Snap-Korrektur minimieren.

**Akzeptanzkriterien:**
- Kein sichtbares Jittern bei normaler Latenz.
- Kaum Rubberbanding bei Richtungswechsel.

---

## Phase B – Spielregel- und Gameplay-MVP

### B1. Massemodell statt nur Blockanzahl
**Task:**
- Pro Block eine Masse definieren (Tag/Datapack-basiert).
- `totalMass <= X` als harte Assemblierungsbedingung.

**Akzeptanzkriterien:**
- Assemblierung schlägt sauber fehl, wenn Massenlimit überschritten.
- Debug-Ausgabe zeigt berechnete Masse und Limit.

### B2. Bodenkontakt-Regel präzisieren
**Task:**
- Kontaktprüfung in eine nachvollziehbare Regel überführen:
  - „Direkter Bodenkontakt“ eindeutig definieren,
  - Sonderfälle (Wasser, Blätter, Teppich, Nicht-Vollblock) dokumentieren.
- Kollisions-/Kontakt-Tests für Grenzfälle ergänzen.

**Akzeptanzkriterien:**
- Vorhersagbares Verhalten, keine „zufälligen“ Falsch-Blockaden.

### B3. Fahrzeugtyp-Auswahl (Wasser/Rad/Luft) als irreversibler Buff
**Task:**
- UX-Flow beim ersten Assemblieren/Platzieren des Steuerrads (UI/Chat-Menü).
- Persistentes NBT-Feld am Steuerrad/Blueprint: `vehicle_class`.
- Sperre gegen nachträgliches Entfernen/Ändern.

**Akzeptanzkriterien:**
- Nach Auswahl dauerhaft gebunden.
- Klassenspezifische Fahrwerte greifen sofort.

### B4. Klassenphysik (Naturgemäße Eigenschaften)
**Task:**
- Wasserfahrzeug: bessere Stabilität auf Wasser, träge Kurven.
- Räderfahrzeug: starke Bodenabhängigkeit, Rollwiderstand.
- Luftfahrzeug: 3D-Freiheit, geringere Translationsdämpfung, höhere Rotationskontrolle.

**Akzeptanzkriterien:**
- Spürbar unterschiedliche Fahrprofile.

### B5. Designblock „Plasma oder Feuer“ als Pflichtkomponente
**Task:**
- Neuen Block/Varianten einführen (`plasma_core`, `fire_core` o.ä.).
- Assemblierung nur mit genau 1 gültigem Core erlauben.

**Akzeptanzkriterien:**
- Fehlende/mehrfache Cores liefern klare Fehlermeldung.

---

## Phase C – Content, Rendering, UX

### C1. Old-Ship-Wheel Art/Model/States
**Task:**
- Neues Modell/Texture für „altes Schiffssteuerrad“.
- Optional: Animation (leichtes Drehen bei Input).

### C2. Feedback-System
**Task:**
- HUD/Actionbar für: Masse, Klasse, Anchor, Geschwindigkeit, Stabilizer-Status.
- Fehlertexte lokalisieren (mind. DE/EN).

### C3. Audio/VFX
**Task:**
- Core-aktiv Sounds, Anker-Setzen/Lösen, Thruster-/Antriebsfeedback.

---

## Phase D – Production-Härtung

### D1. Teststrategie aufbauen
**Task:**
- Unit-Tests für Masserechnung, Kontaktlogik, Klassenregeln.
- Integrations-Tests für Assemble/Disassemble/Replication.
- Determinismus-Checks (gleicher Input → gleiche Trajektorie).

### D2. Performance-Budget
**Task:**
- Profiler-Läufe mit großen Strukturen (nahe Limit).
- Optimierung von BFS/Kontaktprüfung/Renderpfad.

### D3. Multiplayer-Sicherheit
**Task:**
- Servervalidierung sämtlicher Client-Inputs.
- Rate limits, anti-spam, fail-safe bei desync.

### D4. Savegame/Upgrade-Stabilität
**Task:**
- DataFix/Versioning-Strategie für NBT.
- Rückwärtskompatibilität für Blueprint-Formate.

### D5. Release-Prozess
**Task:**
- CI-Pipeline (build, lint, test, artifact).
- Changelog, semantische Versionierung, Smoke-Test-Checkliste.

---

## 4) Konkreter Backlog-Vorschlag (erste 3 Sprints)

### Sprint 1 (Technik-Risiko minimieren)
1. A1 6DoF Input/Physik-Grundlage.
2. A2 Anti-Drift + Stabilizer.
3. B1 Massemodell (inkl. Config X).

### Sprint 2 (Core Gameplay)
1. B3 Fahrzeugtyp + irreversibler Buff.
2. B4 Klassenphysik.
3. B5 Plasma/Feuer-Core Pflicht.

### Sprint 3 (Produktionsreife)
1. A3 Netcode-Glättung.
2. D1 + D2 Test & Performance.
3. C1/C2 UX-Polish + Release-Checkliste.

---

## 5) „Definition of MVP Done“ (messbar)

- 3D-Steuerung auf allen Achsen vorhanden und dokumentiert.
- Null-Input führt reproduzierbar zu stabilem Stillstand (keine Eigenbewegung).
- Fahrzeugbewegung wirkt subjektiv flüssig und objektiv ohne harte Ticksprünge.
- Assemblierung nur bei `mass <= X`, Core vorhanden, kein direkter Bodenkontakt.
- Fahrzeugklasse (Wasser/Rad/Luft) ist einmalig wählbar und permanent gebunden.
- Multiplayer tauglich mit stabiler Replikation.
- Build, Tests und Basis-Dokumentation in CI automatisiert.
