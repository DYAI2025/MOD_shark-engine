# Shark Engine AIR Release 1 — Product Vision

**Feature Slug:** `shark-engine-air-release-1`  
**Confirmation Status:** `user-confirmed`  
**Readiness:** `READY_FOR_AGILETEAM_PLANNING` (Canvas + Vision confirmed; PRD finalization still gated on Phase 0.16 council challenge)  
**Confirmed by:** user (Ben) · 2026-07-18 · exact phrase supplied verbatim in-session

## Product Vision Statement

**VIS-001 [EXPLICIT]:** Shark Engine AIR Release 1 ermöglicht Minecraft-Spielern, ein eigenes blockbasiertes Fluggerät zu craften, zusammenzubauen, zu erweitern, zu betanken und allein oder mit einem passiven Mitspieler zuverlässig zu fliegen; zugleich wird nur die minimale gemeinsame Vehicle-Core-Architektur dokumentiert, die spätere Land- und Wasserfahrzeuge ohne Duplikation ermöglicht.  
_Sources: SRC-001, SRC-002_

## Target Group

- **VIS-002 [EXPLICIT]:** Minecraft-Spieler, die eigene Fahrzeuge aus Blöcken bauen, verändern und steuern möchten.  
  _Sources: SRC-001_
- **VIS-003 [EXPLICIT]:** Mitspieler, die über einen craftbaren Copilotensitz als passive Passagiere mitfliegen.  
  _Sources: SRC-001_

## User Needs

- **VIS-004 [EXPLICIT]:** Der gesamte AIR-Spielablauf muss zusammenhängend funktionieren: Auswahl, Crafting, Bau, Assembly, Cockpit, Flug, Fuel, Ausstieg, Wiedereinstieg, Weiterbau und Persistenz.  
  _Sources: SRC-001, SRC-002_

## Product Value

**VIS-005 [EXPLICIT]:** Ein selbstgebautes Fluggerät wird zu einem belastbaren, wiederverwendbaren Gameplay-System statt zu einem einmaligen Prototyp; Erweiterbarkeit für LAND und WATER wird vorbereitet, ohne Release 1 durch deren Implementierung zu überladen.  
_Sources: SRC-001, SRC-002_

## Business or Project Goals

- **VIS-006 [EXPLICIT]:** AIR Release 1 soll bis zum 30. Juli 2026 alle P0-Akzeptanz- und Runtime-Gates erfüllen.  
  _Sources: SRC-001_

## Success Signals

- **VIS-007 [EXPLICIT]:** Ein Spieler kann das AIR-Fahrzeug auf Client und Dedicated Server end-to-end craften, bauen, fliegen, verlassen, wieder besteigen, im sicheren Edit-Modus erweitern und nach Serverneustart weiterverwenden.  
  _Sources: SRC-001, SRC-002_
- **VIS-008 [EXPLICIT]:** Mindestens ein Copilot kann sichtbar mitfliegen, aussteigen und wieder einsteigen, ohne Steuerberechtigung zu erhalten oder den Fahrzeugzustand zu beschädigen.  
  _Sources: SRC-001_

## Boundaries

- **VIS-009 [EXPLICIT]:** LAND- und WATER-Fahrzeuge werden in Release 1 nicht ausführbar implementiert; nur Auswahlroute und Erweiterungsarchitektur werden erhalten beziehungsweise dokumentiert.  
  _Sources: SRC-001, SRC-002_
- **VIS-010 [EXPLICIT]:** Der Looping ist ein nachgelagertes Feature und kein Release-1-Abnahmekriterium.  
  _Sources: SRC-001_

## Assumptions

- **ASM-001 [ASSUMPTION]:** Bis zur Nutzerpräzisierung wird die Fünf-Block-Regel als maximale räumliche Distanz vom Spieler zum Control Anchor im stationären sicheren Zustand behandelt.  
  _Sources: SRC-001, SRC-002_
- **ASM-002 [ASSUMPTION]:** Der Pilotensitz wird anhand des Steering-Wheel-Facings auf eine deterministische angrenzende Position 'vor' dem Steuerrad abgebildet.  
  _Sources: SRC-001, SRC-002_
- **ASM-003 [ASSUMPTION]:** Release 1 unterstützt die 16 normalen Minecraft-Farbstoffe; der genaue Crafting-/Recolor-Flow ist vor Implementierung zu bestätigen.  
  _Sources: SRC-001, SRC-002_
- **ASM-004 [ASSUMPTION]:** Ein Copilotensitz trägt genau einen Spieler; Release 1 garantiert mindestens einen zusätzlichen Passagier.  
  _Sources: SRC-001, SRC-002_
- **ASM-005 [ASSUMPTION]:** Vehicle Core wird nur bis zu den für AIR unmittelbar benötigten Seams extrahiert; keine vorsorgliche LAND/WATER-Physikabstraktion.  
  _Sources: SRC-002, SRC-003_

## Missing Items

- **OQ-001 [RESOLVED, User decision 2026-07-18]:** Fünf-Block-Regel = euklidische 3D-Distanz vom Spieler zum Control Anchor, ≤5 Blöcke.  
  _Sources: SRC-001_
- **OQ-002 [RESOLVED, User decision 2026-07-18]:** Trail-Farbe ausschließlich per Crafting farbiger Thruster-Varianten.  
  _Sources: SRC-001_
- **OQ-003 [RESOLVED, User decision 2026-07-18]:** Cockpit-Sichttoleranz = ausschließlich Augenhöhe, unabhängig von Rüstung/Skin (einfachste Implementierung).  
  _Sources: SRC-001_

## Confirmation Status

`user-confirmed`

Confirmation phrase supplied verbatim by the user on 2026-07-18:

> Ich bestätige, dass Product Canvas und Product Vision meine Absicht korrekt wiedergeben und als Grundlage für AgileTeam Planning verwendet werden dürfen.
