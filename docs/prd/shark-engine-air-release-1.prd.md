# Shark Engine AIR Release 1 — Plumbline AgileTeam PRD

**Status:** `user-confirmed`  
**Confirmed by:** user (Ben) · 2026-07-18 · explicit confirmation + GO given in-session  
**Feature Slug:** `shark-engine-air-release-1`  
**Owner:** Ben as requirements owner; formal repository Product Owner role is `ASSUMPTION`  
**Target Release:** 30 July 2026  
**Platform Lock:** Fabric · Minecraft 1.21.1 · Java 21 · Gradle  
**Runtime Status:** `UNVERIFIED` until release gates pass

## Source Summary

Konsolidiert aus der sequenziellen Nutzerklärung, dem finalisierten AI-Native PRD, der Anforderungsevaluation und dem statischen Repository-Audit. Runtime-Verhalten bleibt bis zu Build-/GameTest-/Client-/Server-Nachweisen UNVERIFIED.

## Problem Statement

**CAN-001 [EXPLICIT]:** Der AIR-Prototyp deckt wesentliche Teile des Flugzeug-Gameplays ab, ist aber noch nicht als durchgängiger, runtime-verifizierter Release abgeschlossen. Sitzlogik, Copilot, Wiedereinstieg, sicherer Weiterbau, persistente Trail-Farben und eine begrenzte Vehicle-Core-Trennung fehlen oder sind nicht vollständig nachgewiesen.

## Target Users

- **CAN-002 [EXPLICIT]:** Primär: Minecraft-Spieler, die eigene blockbasierte Fluggeräte bauen und steuern.  
  _Sources: SRC-001_
- **CAN-003 [EXPLICIT]:** Sekundär: Mitspieler, die als passive Copiloten mitfliegen.  
  _Sources: SRC-001_

## Goals

- **GOAL-001 [EXPLICIT]:** AIR-Fahrzeug bis 30.07.2026 als vollständigen, runtime-verifizierten Release abschließen.  
  _Sources: SRC-001_
- **GOAL-002 [EXPLICIT]:** Crafting, Bau, Assembly, Cockpit, Flug, Fuel, Aus-/Wiedereinstieg, Weiterbau, Multiplayer und Persistenz zu einem kohärenten End-to-End-Flow verbinden.  
  _Sources: SRC-001, SRC-002_
- **GOAL-003 [EXPLICIT]:** Nur die minimale gemeinsame Vehicle-Core-Architektur schaffen, die spätere LAND/WATER-Routen ohne Kern-Duplikation ermöglicht.  
  _Sources: SRC-001, SRC-002_

## Non-Goals

- **CAN-012 [EXPLICIT]:** Keine ausführbaren LAND- oder WATER-Fahrzeuge in Release 1.  
  _Sources: SRC-001_
- **CAN-013 [EXPLICIT]:** Kein Looping als Release-1-MVP-Anforderung.  
  _Sources: SRC-001_
- **CAN-014 [EXPLICIT]:** Keine physikalisch vollständige Aerodynamiksimulation.  
  _Sources: SRC-002_

## Assumptions

- **ASM-001 [RESOLVED, User decision 2026-07-18]:** Die Fünf-Block-Regel ist die euklidische 3D-Distanz vom Spieler zum Control Anchor, ≤5 Blöcke in jede Richtung (siehe OQ-001).  
  _Sources: SRC-001, SRC-002_
- **ASM-002 [RESOLVED, User decision 2026-07-18]:** Der Pilotensitz ist strikt der einzelne Block direkt vor dem Steering-Wheel-Facing; ist diese Position belegt oder ungültig, schlägt die Assembly explizit fehl statt auf eine andere Position auszuweichen (siehe OQ-004).  
  _Sources: SRC-001, SRC-002_
- **ASM-003 [RESOLVED, User decision + Council-Amendment 2026-07-18]:** Release 1 unterstützt die 16 normalen Minecraft-Farbstoffe ausschließlich craft-zeitlich über eine DyeColor-Datenkomponente auf einem einzelnen Thruster-Item; kein nachträgliches Färben, keine 16 separaten Thruster-Item-IDs (siehe OQ-002 und REQ-018/019/020).  
  _Sources: SRC-001, SRC-002_
- **ASM-004 [ASSUMPTION]:** Ein Copilotensitz trägt genau einen Spieler; Release 1 garantiert mindestens einen zusätzlichen Passagier.  
  _Sources: SRC-001, SRC-002_
- **ASM-005 [ASSUMPTION]:** Vehicle Core wird nur bis zu den für AIR unmittelbar benötigten Seams extrahiert; keine vorsorgliche LAND/WATER-Physikabstraktion.  
  _Sources: SRC-002, SRC-003_

## Open Questions

- **OQ-001 [RESOLVED, User decision 2026-07-18]:** Fünf-Block-Regel = euklidische 3D-Distanz vom Spieler zum Control Anchor, ≤5 Blöcke in jede Richtung.  
  _Sources: SRC-001, SRC-002_
- **OQ-002 [RESOLVED, User decision 2026-07-18]:** Trail-Farbe wird ausschließlich durch Crafting gewählt — Release 1 implementiert dies als ein einziges Thruster-Item mit craft-zeitlicher DyeColor-Datenkomponente statt separater Item-IDs pro Farbe (kein nachträgliches Färben eines platzierten Thrusters); siehe REQ-018/019/020.  
  _Sources: SRC-001_
- **OQ-003 [RESOLVED, User decision 2026-07-18]:** Cockpit-Sichttoleranz = ausschließlich Augenhöhe (Eye-Height-Check), unabhängig von Rüstung/Skin/Third-Person-Kamera — bewusst einfachste korrekte Implementierung, keine Bounding-Box-Sonderfälle pro Rüstung/Skin.  
  _Sources: SRC-001, SRC-002_
- **OQ-004 [RESOLVED, User decision 2026-07-18]:** Pilotensitz-Position = strikt der Block direkt vor dem Steering-Wheel-Facing; ist diese Position belegt/ungültig, schlägt die Assembly explizit fehl (kein stiller Fallback).  
  _Sources: SRC-001, SRC-002_
- **OQ-005 [RESOLVED, User decision 2026-07-18]:** Kein hartes numerisches Performance-Gate für Release 1 — Release 1 schifft ohne gemessene Baseline; die Baseline wird nach echten Playtest-Daten nachgezogen, statt für Release 1 eine Zahl zu erfinden.  
  _Sources: SRC-002_

## Requirements

### REQ-001 — Vehicle route popup (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002  
**Vision:** VIS-001, VIS-004  
**Canvas:** CAN-006, CAN-024  
**Risks:** RISK-001

Beim Platzieren oder Interagieren mit dem Steering Wheel muss das bestehende Popup die Routen AIR, LAND und WATER anzeigen.
### REQ-002 — Release route availability (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002  
**Vision:** VIS-009  
**Canvas:** CAN-006, CAN-012  
**Risks:** RISK-005

Release 1 darf nur AIR als ausführbare Route aktivieren; LAND und WATER bleiben sichtbar, aber nicht fahrzeugerzeugend und werden als zukünftige Routen gekennzeichnet.
### REQ-003 — Server-owned build session (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-002, SRC-003, SRC-004  
**Vision:** VIS-005, VIS-007  
**Canvas:** CAN-011, CAN-017, CAN-018  
**Risks:** RISK-004

Auswahl und Assembly müssen an eine serverseitig validierte VehicleBuildSession mit Spieler, Dimension, Steering-Wheel-Position, Fahrzeugklasse, Status und Ablaufzeit gebunden sein.
### REQ-004 — Complete craft/resource closure (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002  
**Vision:** VIS-004, VIS-007  
**Canvas:** CAN-007, CAN-009, CAN-010, CAN-024  
**Risks:** RISK-001

Alle Release-1-AIR-Komponenten müssen registriert, craftbar oder bewusst anderweitig erhältlich, platzierbar, lokalisiert und mit vollständigen Ressourcen/Datagen versehen sein.
### REQ-005 — Generic pilot seat (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002  
**Vision:** VIS-001, VIS-005  
**Canvas:** CAN-007, CAN-011  
**Risks:** RISK-003, RISK-005

Ein generischer craftbarer Pilotensitz/Cockpit muss für AIR verwendet und architektonisch auch für spätere LAND/WATER-Profile wiederverwendbar sein; AIR-Assembly erfordert genau einen gültigen Pilotensitz.
### REQ-006 — Pilot seat anchor (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002  
**Vision:** VIS-004  
**Canvas:** CAN-007, CAN-016  
**Risks:** RISK-003

Der Pilotensitz muss den SeatAnchor strikt als den einzelnen Block direkt vor dem Steering-Wheel-Facing auflösen und im Blueprint speichern (User-Entscheidung OQ-004, 2026-07-18). Ist diese Position belegt oder ungültig, schlägt die Assembly mit einer expliziten Fehlermeldung fehl; es gibt keinen stillen Fallback auf eine andere angrenzende Position.
### REQ-007 — Cockpit visibility (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002  
**Vision:** VIS-004, VIS-007  
**Canvas:** CAN-007, CAN-019  
**Risks:** RISK-003

Der Pilot muss lokal und für andere Clients so im Cockpit positioniert sein, dass die Figur nicht dauerhaft vollständig auf dem Fahrzeug steht; die Sichtbarkeit wird ausschließlich über einen Eye-Height-Check bestimmt (User-Entscheidung OQ-003, 2026-07-18), unabhängig von Rüstung, Skin oder Third-Person-Kamera — bewusst ohne Bounding-Box-Sonderfälle pro Rüstung oder Skin.
### REQ-008 — Pilot control authority (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002, SRC-004  
**Vision:** VIS-007, VIS-008  
**Canvas:** CAN-007, CAN-017  
**Risks:** RISK-001

Nur der serverseitig zugewiesene Pilot darf Bewegungs-, Throttle-, Anchor-, Edit- und vergleichbare Steuerbefehle wirksam auslösen.
### REQ-009 — Craftable copilot seat (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002  
**Vision:** VIS-003, VIS-008  
**Canvas:** CAN-003, CAN-007  
**Risks:** RISK-003

Ein craftbarer Copilotensitz muss mindestens einen zusätzlichen Passagier aufnehmen und als SeatAnchor im Blueprint repräsentiert werden.
### REQ-010 — Passive copilot behavior (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002  
**Vision:** VIS-003, VIS-008  
**Canvas:** CAN-003, CAN-007  
**Risks:** RISK-003

Ein Copilot darf mitfliegen, normal aussteigen und später wieder einsteigen, erhält aber durch die Belegung keine Steuerberechtigung.
### REQ-011 — Vehicle re-entry (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002  
**Vision:** VIS-004, VIS-007, VIS-008  
**Canvas:** CAN-008, CAN-019  
**Risks:** RISK-003

Nach dem Ausstieg muss ein berechtigter Spieler einen freien Pilot- oder Copilotensitz erneut besteigen können, ohne das Fahrzeug neu zu bauen.
### REQ-012 — Safe edit-mode gate (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002  
**Vision:** VIS-004, VIS-007  
**Canvas:** CAN-008, CAN-017, CAN-024  
**Risks:** RISK-004, RISK-007

Ein Fahrzeug darf nur im stationären, nicht zerstörten und konfliktfreien Zustand in den Edit-Modus wechseln; der autorisierte Spieler muss sich innerhalb der euklidischen 3D-Distanz von ≤5 Blöcken in jede Richtung vom Control Anchor befinden (User-Entscheidung OQ-001, 2026-07-18).
### REQ-013 — Builder reopen (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002  
**Vision:** VIS-004, VIS-007  
**Canvas:** CAN-008, CAN-024  
**Risks:** RISK-004

Der erfolgreiche Wechsel in den Edit-Modus muss das Baumenü mit der bestehenden Fahrzeugstruktur öffnen und eine Erweiterung erlauben.
### REQ-014 — Atomic edit/reassembly (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-002, SRC-004  
**Vision:** VIS-005, VIS-007  
**Canvas:** CAN-008, CAN-017, CAN-018  
**Risks:** RISK-004

Das Beenden des Edit-Modus muss die geänderte Struktur gegen die AIR-Policy prüfen und Änderungen atomar committen oder vollständig zurückrollen.
### REQ-015 — AIR flight controls (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002  
**Vision:** VIS-004, VIS-007  
**Canvas:** CAN-009, CAN-019  
**Risks:** RISK-001

AIR muss Vorwärtsbeschleunigung, Geschwindigkeitsänderung, Steigflug, Sinkflug, Neigung und Kurvenflug über die bestehende oder äquivalent rebindbare Steuerung unterstützen.
### REQ-016 — Fuel and speed loop (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002  
**Vision:** VIS-004, VIS-007  
**Canvas:** CAN-009, CAN-019  
**Risks:** RISK-001

Fuel muss craftbar/refillbar sein, während angetriebenem Flug verbraucht, im HUD sichtbar und über Save/Load erhalten werden; Geschwindigkeit muss wirksam und nachvollziehbar steuerbar sein.
### REQ-017 — Persistence and restart (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002  
**Vision:** VIS-007, VIS-008  
**Canvas:** CAN-017, CAN-019, CAN-021  
**Risks:** RISK-001

VehicleClass, Blueprint, Pilot-/Copilot-Sitze, Belegungseignung, Fuel, Schaden, Trail-Konfiguration und Edit-Zustand müssen konsistent serialisiert und nach Serverneustart wiederhergestellt werden.
### REQ-018 — Single Thruster item with craft-time DyeColor component (P1)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002  
**Vision:** VIS-001, VIS-004  
**Canvas:** CAN-010, CAN-024  
**Risks:** RISK-006, RISK-008

Spieler müssen ein einzelnes Thruster-Item craften können, das beim Crafting eine Craft-Zeit-Datenkomponente (DyeColor) aus den unterstützten Minecraft-Farbstoffen erhält. Es gibt keine 16 separaten craftbaren Thruster-Item-IDs und kein nachträgliches Färben eines bereits platzierten Thrusters in Release 1 (User-Entscheidung OQ-002 plus Council-Amendment, 2026-07-18: ein Item plus Komponente statt 16 Varianten, bei identischem Nutzerverhalten und deutlich geringerer Rezept-/Modell-/Textur-/Lang-/Datagen-Fläche als 16 separate Items, ohne erfundene Präzisionszahl). Rezeptbuch-Sichtbarkeit (Entscheidung, siehe AC-018): Release 1 akzeptiert ausdrücklich genau einen generischen Rezeptbuch-Eintrag für dieses Item (die Eingangsfarbe des Farbstoffs bestimmt die Ausgangsfarbe der Komponente; die 16 Farboptionen sind NICHT als 16 separat durchsuchbare Rezeptbuch-Einträge sichtbar) — konsistent mit CAN-024s Stabilisierungs-Framing statt maximaler UX-Politur, denn 16 separate Rezeptbuch-Einträge würden exakt die Item-Varianten-Explosion wiederherstellen, die dieser Single-Item-Swap gerade vermeiden soll.
### REQ-019 — Persistent colored trail via single render path (P1)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002  
**Vision:** VIS-004, VIS-007  
**Canvas:** CAN-010, CAN-019  
**Risks:** RISK-006, RISK-008

Ein platzierter Thruster mit gesetzter DyeColor-Komponente muss während des Flugs seine Trail-Farbe über einen einzigen tinted-texture/color-provider-Renderpfad anzeigen; ein Thruster ohne gesetzte Komponente verwendet den bestehenden Standardtrail. Es gibt genau eine Renderimplementierung für alle Farben, keine 16 separaten Modelle/Texturen.
### REQ-020 — Trail isolation and bounded rendering (P1)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002  
**Vision:** VIS-005, VIS-007  
**Canvas:** CAN-010, CAN-017, CAN-020  
**Risks:** RISK-006

Die per Craft-Zeit-DyeColor-Komponente am Thruster-Item gewählte Trail-Farbe ist rein kosmetisch und darf Schub, Fuelverbrauch, Masse oder Steuerautorität nicht verändern; Partikelemission muss pro aktivem Fahrzeug sinnvoll begrenzt sein. Für Release 1 gilt dafür ausdrücklich kein hartes numerisches Performance-Gate (User-Entscheidung OQ-005, 2026-07-18) — es wird keine Zahl für Release 1 erfunden; die Baseline wird nach echten Playtest-Daten nachgezogen.
### REQ-021 — Transactional world mutation (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-002, SRC-004  
**Vision:** VIS-005, VIS-007  
**Canvas:** CAN-017, CAN-018, CAN-019  
**Risks:** RISK-004

Assembly, Disassembly und Edit-Reassembly müssen Preflight-Validierung und Rollback verwenden, sodass Fehler keine Blöcke duplizieren oder löschen.
### REQ-022 — Minimal Vehicle Core, no premature generalization (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002, SRC-003  
**Vision:** VIS-005, VIS-009  
**Canvas:** CAN-011, CAN-018, CAN-024  
**Risks:** RISK-005

Release 1 darf nur die Seams extrahieren, die AIR in seiner tatsächlichen Implementierung heute wirklich aufruft, um zu verhindern, dass spätere LAND/WATER-Routen duplizierte Entity-, Assembly-, Persistenz- und Networking-Pipelines benötigen. Dies ist ausdrücklich KEIN Mandat, im Voraus alle sieben zuvor benannten Seams (BuildSession, VehicleClassProfile, AssemblyPolicy, MovementController, SeatController, versionierte Persistenz, TrailEmitter) als P0-Gate zu bauen, falls AIRs reale Implementierung sie noch nicht braucht — vorzeitige Generalisierung für ein System mit derzeit genau einem Konsumenten (AIR) ist nicht das Ziel (Council-Konvergenz Challenger/Advisor/Critic, 3/3 bereits in Runde 1, Phase 0.16, siehe docs/intake/PHASE-0.16-COUNCIL-CHALLENGE.md; direkte Mitigation von RISK-005). Operationale Regel für die falsifizierbare positive Klausel dieser Anforderung siehe AC-022.
### REQ-023 — Looping backlog only (P2)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002, SRC-003  
**Vision:** VIS-010  
**Canvas:** CAN-013  
**Risks:** RISK-005

Der manuell beziehungsweise halbautomatisch übersteuerte Looping mit Eintrittsbedingungen und fatalen Crash-Folgen wird als Post-Release-Anforderung dokumentiert, aber nicht in Release 1 implementiert oder als DoD-Gate verwendet.
### REQ-024 — Release evidence gate (P0)

**Source Type:** `EXPLICIT`  
**Sources:** SRC-001, SRC-002, SRC-004  
**Vision:** VIS-006, VIS-007, VIS-008  
**Canvas:** CAN-015, CAN-017, CAN-019, CAN-020, CAN-021, CAN-023  
**Risks:** RISK-001, RISK-002

Ein Release-Artefakt darf erst freigegeben werden, wenn der exakte Commit Build, Unit-/Resource-Tests, Fabric GameTests, Client-Smoke, Dedicated-Server-Smoke, Zwei-Spieler-Smoke und Restart-Nachweis bestanden hat.
### REQ-025 — Day-0 build/runtime verification gate (P0, blocking)

**Source Type:** `EXPLICIT` (user-adopted council finding, Phase 0.16)  
**Sources:** SRC-002, SRC-004, SRC-005  
**Vision:** VIS-006, VIS-007  
**Canvas:** CAN-017, CAN-020  
**Risks:** RISK-001, RISK-002

Bevor irgendein anderer P0-Task-Branch für dieses Feature geöffnet wird, müssen `./gradlew build` und `./gradlew runGametest` auf dem aktuellen `main` erfolgreich durchlaufen. Dies ist ein Sequenzierungs-Gate für den Start der P0-Arbeit an Tag 0, kein Release-Ende-Gate wie REQ-024. Rationale (Phase-0.16-Council, siehe docs/intake/PHASE-0.16-COUNCIL-CHALLENGE.md): 3/3 Council-Rollen konvergent **bis Runde 2** — nicht Runde-1-Einstimmigkeit. Challenger und Critic benannten das zugrunde liegende Date-before-Baseline-Risiko bereits unabhängig voneinander in Runde 1; Advisor schlug den konkreten Day-0-Gate-Mechanismus explizit in Runde 2 vor, und sowohl Challenger als auch Critic bestätigten/verschärften denselben Mechanismus in Runde 2. Der Termin 30.07.2026 wurde festgelegt, bevor die Build-/Runtime-Baseline verifiziert war (RISK-001-Status UNVERIFIED), und RISK-002 benennt terminbedingte parallele Änderungen als Multiplikator für Integrationsfehler. REQ-025 verwandelt dieses protokollierte, aber bislang nur zur Kenntnis genommene Risiko in eine stundenlange Go/No-Go-Prüfung an Tag 0, statt es erst an Tag 12 zu entdecken. REQ-025 ersetzt REQ-024 nicht: REQ-024 gated weiterhin den finalen Release-Commit, REQ-025 gated den *Start* der P0-Arbeit.


## Acceptance Criteria

### AC-001 → REQ-001

**Given** ein Spieler platziert oder benutzt ein Steering Wheel  
**When** die Fahrzeugauswahl geöffnet wird  
**Then** sind AIR, LAND und WATER als drei erkennbare Routen sichtbar.
### AC-002 → REQ-002

**Given** Release 1 ist installiert  
**When** der Spieler AIR, LAND oder WATER auswählt  
**Then** erzeugt nur AIR eine aktive Build-Session; LAND und WATER zeigen einen nicht irreführenden Zukunftsstatus und verändern die Welt nicht.
### AC-003 → REQ-003

**Given** ein Client sendet eine Fahrzeugauswahl oder Assembly-Anfrage  
**When** Spieler, Dimension, Position, Distanz, Sessionstatus oder Ablaufzeit ungültig sind  
**Then** lehnt der Server die Anfrage ohne Weltmutation ab.
### AC-004 → REQ-004

**Given** der Release-Build wird geprüft  
**When** Rezepte, Modelle, Loot, Texturen, Datagen und EN/DE-Sprachschlüssel validiert werden  
**Then** sind alle Release-1-AIR-Komponenten vollständig auflösbar und craftbar beziehungsweise bewusst dokumentiert erhältlich.
### AC-005 → REQ-005

**Given** eine AIR-Struktur besitzt keinen oder mehr als einen gültigen Pilotensitz  
**When** Assembly gestartet wird  
**Then** schlägt die Assembly mit verständlicher Meldung fehl und lässt die Welt unverändert; genau ein gültiger Sitz wird akzeptiert.
### AC-006 → REQ-006

**Given** die Zielposition direkt vor dem Steering-Wheel-Facing ist frei und gültig  
**When** das Fahrzeug assembliert, gedreht, gespeichert und geladen wird  
**Then** wird genau diese Position als SeatAnchor deterministisch übernommen; ist die Position belegt oder ungültig, schlägt die Assembly mit expliziter Fehlermeldung fehl, statt auf eine andere Position auszuweichen.
### AC-007 → REQ-007

**Given** ein Pilot mit beliebiger Rüstung/Skin sitzt im Cockpit  
**When** der serverseitige Eye-Height-Check unabhängig von Third-Person-Kamera, Rüstung oder Skin ausgewertet wird  
**Then** steht die vollständige Figur nicht dauerhaft oberhalb des Fahrzeugs, und die Sichtbarkeit richtet sich ausschließlich nach der Augenhöhe am Sitzanker — ohne Bounding-Box-Sonderfälle pro Rüstung oder Skin.
### AC-008 → REQ-008

**Given** ein Nichtpilot oder Copilot sendet Steuerbefehle  
**When** der Server die Payload verarbeitet  
**Then** ändern sich Geschwindigkeit, Richtung, Anchor-, Edit- oder Fuelzustand nicht und der Versuch wird begrenzt protokolliert.
### AC-009 → REQ-009

**Given** ein Fahrzeug enthält einen gültigen Copilotensitz  
**When** ein zweiter Spieler interagiert  
**Then** kann genau ein zusätzlicher Passagier diesen Sitz belegen und wird auf allen Clients korrekt angezeigt.
### AC-010 → REQ-010

**Given** ein Copilot ist während des Flugs eingestiegen  
**When** er Steuerinputs sendet oder normal aussteigt  
**Then** bleiben Steuerinputs wirkungslos und der Ausstieg beschädigt weder Rollen- noch Fahrzeugzustand.
### AC-011 → REQ-011

**Given** Pilot oder Copilot hat einen Sitz verlassen  
**When** ein berechtigter Spieler mit dem freien Sitz interagiert  
**Then** kann er ohne Rekonstruktion wieder einsteigen und erhält ausschließlich die zum Sitz gehörige Rolle.
### AC-012 → REQ-012

**Given** ein Fahrzeug ist stationär, sicher und konfliktfrei  
**When** der autorisierte Spieler innerhalb einer euklidischen 3D-Distanz von ≤5 Blöcken in jede Richtung zum Control Anchor Edit Mode anfordert  
**Then** wird Edit Mode geöffnet; bei einer euklidischen Distanz über 5 Blöcken oder im unsicheren Zustand erfolgt keine Zustandsänderung.
### AC-013 → REQ-013

**Given** Edit Mode wurde erfolgreich gestartet  
**When** das Baumenü geöffnet wird  
**Then** zeigt es die bestehende Fahrzeugstruktur und erlaubt zulässige Erweiterungen.
### AC-014 → REQ-014

**Given** der Spieler beendet eine gültige oder ungültige Änderung  
**When** die AIR-Policy und Kollisions-/Weltprüfungen ausgeführt werden  
**Then** wird die gültige Struktur vollständig übernommen oder die vorherige Struktur vollständig erhalten.
### AC-015 → REQ-015

**Given** ein Pilot besitzt Fuel und sitzt im Cockpit  
**When** er Geschwindigkeit, Steigen, Sinken, Neigung und Kurvenflug steuert  
**Then** reagiert das Fluggerät nachvollziehbar und ohne NaN-/Infinity- oder Autoritätsfehler.
### AC-016 → REQ-016

**Given** das Fluggerät besitzt Fuel  
**When** es angetrieben fliegt, gespeichert und erneut geladen wird  
**Then** sinkt Fuel nach der definierten Logik, Geschwindigkeit reagiert auf Inputs und HUD/gespeicherter Wert stimmen überein.
### AC-017 → REQ-017

**Given** ein Fahrzeug mit Sitzen, Fuel, Schaden und Trail-Konfiguration existiert  
**When** Welt und Dedicated Server gespeichert, beendet und neu gestartet werden  
**Then** wird ein konsistenter, weiter nutzbarer Fahrzeugzustand wiederhergestellt.
### AC-018 → REQ-018

**Given** ein Spieler craftet ein Thruster-Item mit einem unterstützten Farbstoff  
**When** das Crafting-Rezept ausgeführt wird  
**Then** entsteht genau ein Thruster-Item mit einer craft-zeitlich gesetzten DyeColor-Datenkomponente; es existiert kein zweites, farbspezifisches Thruster-Item und keine Möglichkeit, die Komponente nach dem Platzieren zu ändern. Das Rezept erscheint im Rezeptbuch als genau ein generischer Eintrag für alle 16 Farbstoffe (nicht als 16 separat durchsuchbare Einträge) — für Release 1 explizit ausreichend (siehe REQ-018).
### AC-019 → REQ-019

**Given** ein Thruster mit gesetzter DyeColor-Komponente und ein Thruster ohne Komponente fliegen gleichzeitig  
**When** lokale und beobachtende Clients die Trails über den gemeinsamen tinted-Renderpfad sehen und der Server neu gestartet wird  
**Then** zeigt der Thruster mit Komponente persistent seine DyeColor-Trail-Farbe und der Thruster ohne Komponente den Standardtrail — beide über denselben Renderpfad, nicht über separate Modelle.
### AC-020 → REQ-020

**Given** zwei sonst identische Fahrzeuge unterscheiden sich nur in der DyeColor-Komponente ihrer Thruster  
**When** Fuel-, Schub- und Autoritätstests laufen  
**Then** sind alle Gameplaywerte identisch; Partikelemission ist pro Fahrzeug qualitativ begrenzt, wird aber für Release 1 ohne hartes numerisches Performance-Gate abgenommen (User-Entscheidung OQ-005) statt gegen eine erfundene Zahl geprüft.
### AC-021 → REQ-021

**Given** Assembly, Disassembly oder Reassembly trifft auf ungültige Struktur, Blockade oder Spawnfehler  
**When** die Operation abbricht  
**Then** gibt es weder Blockverlust noch Duplikation und der vorherige Zustand bleibt kohärent.
### AC-022 → REQ-022

**Given** der Release-Code und die Architekturunterlagen werden geprüft  
**When** die tatsächlich extrahierten Vehicle-Core-Seams gegen das, was AIRs reale Implementierung heute aufruft, abgeglichen werden  
**Then** existieren nur die von AIR tatsächlich benötigten gemeinsamen Seams — keine vorzeitige Generalisierung über das hinaus, was AIRs Implementierung erfordert — und es gibt keine duplizierte Entity-, Assembly-, Persistenz- oder Netzwerkpipeline für nicht implementierte Klassen. **Operationale Regel (macht diese positive Klausel falsifizierbar statt einer bloßen Absichtserklärung):** Eine Seam-Schnittstelle darf nur existieren, wenn AIRs ausgelieferter Code mindestens eine nicht-triviale Call-Site in sie hinein aufweist; eine Schnittstelle mit null Call-Sites oder nur einer trivialen Durchreich-Implementierung gilt als vorzeitige Generalisierung und besteht diese AC nicht.
### AC-023 → REQ-023

**Given** Release-1-Backlog und DoD werden geprüft  
**When** Looping gesucht wird  
**Then** ist es als Post-Release-Feature mit den bestätigten Designnotizen dokumentiert und blockiert keinen Release-1-Test.
### AC-024 → REQ-024

**Given** ein Release Candidate soll freigegeben werden  
**When** ein P0-Test, Build, Client-/Server-Smoke, Multiplayer- oder Restart-Gate fehlt oder fehlschlägt  
**Then** bleibt der Release blockiert; nur ein vollständig evidenzierter exakter Commit kann freigegeben werden.
### AC-025 → REQ-025

**Given** noch kein P0-Task-Branch für `shark-engine-air-release-1` wurde geöffnet  
**When** `./gradlew build` und `./gradlew runGametest` auf dem aktuellen `main` ausgeführt werden  
**Then** müssen beide Kommandos erfolgreich (grün) durchlaufen, bevor irgendein P0-Task-Branch geöffnet werden darf; schlägt einer der beiden Befehle fehl, bleibt jede P0-Arbeit blockiert, bis der Build-/Runtime-Zustand auf `main` repariert und erneut grün verifiziert ist.


## Non-Functional Requirements

- **NFR-001 [EXPLICIT]:** Serverautorität: C2S-Anfragen validieren Identität, Rolle, Distanz, Dimension, geladenes Ziel, endliche Werte, Session und Zustandsübergang.  
  _Sources: SRC-002, SRC-004_
- **NFR-002 [EXPLICIT]:** Kompatibilität: Fabric, Minecraft 1.21.1, Java 21 und persistierte Entity-ID bleiben für Release 1 stabil, sofern keine explizite Migration beschlossen wird.  
  _Sources: SRC-002, SRC-005_
- **NFR-003 [EXPLICIT]:** Maintainability: Keine wachsenden AIR/LAND/WATER-Bedingungsblöcke in ShipEntity.tick(); fachliches Verhalten wird über begrenzte Policies/Controller delegiert.  
  _Sources: SRC-002, SRC-003_
- **NFR-004 [EXPLICIT]:** Persistenzänderungen sind schema-versioniert und migrieren Legacy-Blueprints konservativ oder scheitern kontrolliert.  
  _Sources: SRC-002_
- **NFR-005 [EXPLICIT]:** Multiplayer-Konsistenz: Sitzpositionen, Rollen, Fuel, Geschwindigkeit und Trail-Farbe müssen auf Server und Beobachterclients konsistent sein.  
  _Sources: SRC-002_
- **NFR-006 [RESOLVED, User decision 2026-07-18]:** Performance: Particle-Emission und Passenger-Transforms sind begrenzt; für Release 1 gilt explizit kein hartes numerisches Performance-Gate (User-Entscheidung OQ-005). Release 1 schifft ohne gemessene Baseline; die Baseline wird nach echten Playtest-Daten nachgezogen, statt für Release 1 eine Zahl zu erfinden.  
  _Sources: SRC-002_
- **NFR-007 [EXPLICIT]:** Lokalisierung: Neue UI-, Item-, Fehler- und Control-Texte besitzen EN/DE-Parität.  
  _Sources: SRC-002_
- **NFR-008 [EXPLICIT]:** Accessibility: Neue Aktionen verwenden rebindbare Minecraft-Keybindings; Statusfeedback verlässt sich nicht ausschließlich auf Farbe.  
  _Sources: SRC-002_
- **NFR-009 [EXPLICIT]:** Recoverability: Fehlgeschlagene World-Mutations und Sitzübergänge hinterlassen einen kohärenten vorherigen Zustand oder eine klare Fehlermeldung.  
  _Sources: SRC-002, SRC-004_

## Risks

- **RISK-001 [RESOLVED für den Ausgangszustand, verifiziert 2026-07-18 via EV-025]:** Der bisherige Build-/Runtime-Status war nicht vollständig verifiziert; Feature-Arbeit auf fehlerhafter Basis hätte Folgeschäden erzeugen können. REQ-025s Day-0-Gate wurde ausgeführt: `./gradlew build` und `./gradlew runGametest` auf `main`-äquivalentem Source-Stand sind beide grün (siehe EV-025). Die Baseline ist damit für den Start der P0-Arbeit nachgewiesen verifiziert, nicht mehr nur protokolliert als Risiko. (RISK-002s During-Window-Restrisiko bleibt davon unberührt, siehe dortige Eintrag.)  
  _Sources: SRC-004, SRC-005_
- **RISK-002 [ASSUMPTION]:** Der Termin kann zu parallelen Änderungen in Entity, Networking, Persistenz und Rendering verleiten und Integrationsfehler erhöhen. **Teilweise mitigiert durch REQ-025 (nur der Day-0-Teilfall)**: REQ-025 verifiziert einmalig an Tag 0, dass die Startbasis auf `main` grün ist — es verhindert aber nicht die Integrationsfehler, die WÄHREND des rund 12-tägigen Fensters paralleler P0-Arbeit entstehen können, was der eigentliche Kern von RISK-002 ist. Dieses During-Window-Risiko wird weiterhin durch gewöhnliche Per-Branch-CI und REQ-024s Ende-der-Release-Evidenzgate getragen, nicht durch REQ-025 selbst.  
  _Sources: SRC-002_
- **RISK-003 [ASSUMPTION]:** Cockpit- und Passenger-Transforms können zwischen Clients desynchronisieren oder mit Vanilla-Riding/Camera kollidieren.  
  _Sources: SRC-002, SRC-003_
- **RISK-004 [EXPLICIT]:** Edit-Modus und Reassembly können bei nicht-atomarer Umsetzung Blöcke duplizieren oder verlieren.  
  _Sources: SRC-002, SRC-004_
- **RISK-005 [ASSUMPTION]:** Vorzeitige Generalisierung des Vehicle Core kann das AIR-MVP verzögern. **Direkt mitigiert durch die Abstufung von REQ-022** auf "nur die von AIR tatsächlich benötigten Seams extrahieren" statt eines fixen 7-Seam-P0-Gates — konvergent bestätigt von Challenger, Advisor und Critic (3/3 unabhängige Council-Rollen, bereits in Runde 1, Phase 0.16, siehe docs/intake/PHASE-0.16-COUNCIL-CHALLENGE.md).  
  _Sources: SRC-002, SRC-003_
- **RISK-006 [ASSUMPTION]:** Farbige Partikel können Client-Performance beeinträchtigen. Das Ein-Item-plus-Craft-Zeit-DyeColor-Komponenten-Design (REQ-018/019/020) reduziert zusätzlich das Risiko einer Item-Varianten-Explosion (siehe RISK-008); das reine Partikel-Performance-Risiko selbst besteht unverändert und bleibt für Release 1 ohne hartes numerisches Gate (siehe REQ-020, OQ-005).  
  _Sources: SRC-002_
- **RISK-007 [RESOLVED, User decision 2026-07-18]:** Die Fünf-Block-Metrik ist geklärt (euklidische 3D-Distanz ≤5 Blöcke in jede Richtung vom Control Anchor, siehe OQ-001); das ursprüngliche Risiko unklarer Interaktionslogik und untestbarer Grenzfälle entfällt damit.  
  _Sources: SRC-001_
- **RISK-008 [RESOLVED, User decision + Council-Amendment 2026-07-18]:** Der Färbe-Workflow ist bestätigt: ein einzelnes Thruster-Item mit Craft-Zeit-DyeColor-Datenkomponente statt bis zu 16 separater Thruster-Item-IDs (siehe OQ-002, REQ-018/019/020) — das ursprüngliche Risiko der Item-Varianten-Explosion und inkompatibler Migrationen entfällt damit; nur ein Rezept/Modell/Renderpfad statt bis zu 16 ist zu pflegen.  
  _Sources: SRC-001_

## Evidence Needed

- **EV-001 [EXPLICIT]:** UI-/GameTest-Nachweis des dreiteiligen Popups  
  _Sources: SRC-002_
- **EV-002 [EXPLICIT]:** GameTest/Serverlog für AIR-Aktivierung und LAND/WATER-No-op  
  _Sources: SRC-002_
- **EV-003 [EXPLICIT]:** Unit-/Integrationstests der VehicleBuildSession und Servervalidierung  
  _Sources: SRC-002_
- **EV-004 [EXPLICIT]:** Resource-Closure-Report, Recipe-/Datagen-/Lang-Parität  
  _Sources: SRC-002_
- **EV-005 [EXPLICIT]:** AIR-Assembly-GameTests für Pilotensitz-Invariante  
  _Sources: SRC-002_
- **EV-006 [EXPLICIT]:** Blueprint-v3-Roundtrip und Rotationstest für SeatAnchor  
  _Sources: SRC-002_
- **EV-007 [EXPLICIT]:** Screenshots/Video oder reproduzierbarer visueller Client-Test lokal und remote  
  _Sources: SRC-002_
- **EV-008 [EXPLICIT]:** C2S-Autorisierungstests für Nichtpilot/Copilot  
  _Sources: SRC-002_
- **EV-009 [EXPLICIT]:** Zwei-Client-Smoke für Copilotensitz und Occupancy-Sync  
  _Sources: SRC-002_
- **EV-010 [EXPLICIT]:** Zwei-Client-Smoke für Copilot-Control-Denial und Dismount  
  _Sources: SRC-002_
- **EV-011 [EXPLICIT]:** Re-entry-GameTest/Multiplayer-Smoke  
  _Sources: SRC-002_
- **EV-012 [EXPLICIT]:** Edit-Mode-Grenztests bei ≤5 und >5 Blöcken euklidischer 3D-Distanz nach der bestätigten Metrik (OQ-001)  
  _Sources: SRC-002_
- **EV-013 [EXPLICIT]:** Builder-UI-Smoke mit bestehender Blueprint-Struktur  
  _Sources: SRC-002_
- **EV-014 [EXPLICIT]:** Transaktions-/Rollback-GameTests für gültige und ungültige Änderungen  
  _Sources: SRC-002_
- **EV-015 [EXPLICIT]:** Unit-/Client-Smoke für Geschwindigkeit, Steigen, Sinken, Bank und Turn  
  _Sources: SRC-002_
- **EV-016 [EXPLICIT]:** Fuel-/HUD-/SaveLoad-Regressionstest  
  _Sources: SRC-002_
- **EV-017 [EXPLICIT]:** Dedicated-Server-Restart-Test mit vollständigem VehicleState  
  _Sources: SRC-002_
- **EV-018 [EXPLICIT]:** Recipe-/Datagen-Test für das einzelne Thruster-Item mit Craft-Zeit-DyeColor-Datenkomponente über alle unterstützten Farbstoffe  
  _Sources: SRC-002_
- **EV-019 [EXPLICIT]:** Trail-Rendering- und Persistenz-Smoke auf Beobachterclient  
  _Sources: SRC-002_
- **EV-020 [EXPLICIT]:** Invariantentest für Physik/Fuel (Trail-Farbe ändert keine Gameplaywerte), geprüft ohne hartes numerisches Performance-Gate für Release 1 (OQ-005)  
  _Sources: SRC-002_
- **EV-021 [EXPLICIT]:** Assembly-/Disassembly-/Reassembly-Rollback-GameTests  
  _Sources: SRC-002_
- **EV-022 [EXPLICIT]:** Architekturregel oder deterministischer Quellcheck gegen Duplikation/Branches  
  _Sources: SRC-002_
- **EV-023 [EXPLICIT]:** Backlog-/DoD-Dokumentationsreview  
  _Sources: SRC-002_
- **EV-024 [EXPLICIT]:** CI-/Command-Outputs für clean build, Tests, GameTests, Client, Dedicated Server, Multiplayer, Restart, JAR-Inspektion, SHA-256 und eine dokumentierte Versionsmatrix (CAN-023)  
  _Sources: SRC-002_
- **EV-025 [SATISFIED, verified 2026-07-18]:** CI-/Command-Outputs für `./gradlew build` und `./gradlew runGametest` auf dem aktuellen `main`, dokumentiert vor Öffnung des ersten P0-Task-Branches (Day-0-Gate). Ausgeführt vom Orchestrator direkt auf `feature/shark-engine-air-release-1` (Source-Baum identisch zu `main`, verifiziert per `git diff main --stat` vor Ausführung — keine Code-Änderungen, nur `docs/`). Ergebnis: `./gradlew build` → BUILD SUCCESSFUL in 26s (inkl. `compileClientJava`, siehe Client-Compile-Gotcha in CLAUDE.md); `./gradlew runGametest` → BUILD SUCCESSFUL in 18s, "All 18 required tests passed :)". RISK-001 (UNVERIFIED-Status) ist damit für den Ausgangszustand aufgelöst. Zwei benigne, vorbestehende Log-Zeilen ("No data fixer registered for sharkengine:ship", fehlende server.properties im GameTest-Kontext) beobachtet, nicht test-relevant.  
  _Sources: SRC-002, SRC-004, SRC-005_

## Links

- [Product Vision](../vision/shark-engine-air-release-1.vision.md)
- [Product Canvas](../canvas/shark-engine-air-release-1.canvas.md)
- [Traceability Matrix](../traceability.md)

## User Confirmation Required

Product Canvas and Product Vision for `shark-engine-air-release-1` are already `user-confirmed` (Ben, 2026-07-18), via the following verbatim phrase:

> Ich bestätige, dass Product Canvas und Product Vision meine Absicht korrekt wiedergeben und als Grundlage für AgileTeam Planning verwendet werden dürfen.

This PRD has now been finalized against those confirmed Canvas/Vision decisions, propagating the five resolved Open Questions (OQ-001–OQ-005) and the three Phase 0.16 council-adopted amendments (Day-0 build/runtime gate REQ-025, minimal-Vehicle-Core demotion of REQ-022, single-Thruster-item + craft-time DyeColor design for REQ-018/019/020). It has **not** yet received its own explicit user confirmation — that is a separate, later gate this analyst is not running and does not claim to have obtained. The PRD's `Status` field remains `finalized-pending-user-confirmation` until the user explicitly confirms the PRD itself; no such confirmation is implied, assumed, or put in the user's mouth here.
