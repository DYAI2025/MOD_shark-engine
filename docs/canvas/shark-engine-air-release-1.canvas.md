# Shark Engine AIR Release 1 — Product Canvas

**Feature Slug:** `shark-engine-air-release-1`  
**Status:** `user-confirmed`  
**Confirmed by:** user (Ben) · 2026-07-18 · exact phrase supplied verbatim in-session

## Problem

**CAN-001 [EXPLICIT]:** Der AIR-Prototyp deckt wesentliche Teile des Flugzeug-Gameplays ab, ist aber noch nicht als durchgängiger, runtime-verifizierter Release abgeschlossen. Sitzlogik, Copilot, Wiedereinstieg, sicherer Weiterbau, persistente Trail-Farben und eine begrenzte Vehicle-Core-Trennung fehlen oder sind nicht vollständig nachgewiesen.  
_Sources: SRC-002, SRC-003, SRC-004_

## Users / Customers

- **CAN-002 [EXPLICIT]:** Primär: Minecraft-Spieler, die eigene blockbasierte Fluggeräte bauen und steuern.  
  _Sources: SRC-001_
- **CAN-003 [EXPLICIT]:** Sekundär: Mitspieler, die als passive Copiloten mitfliegen.  
  _Sources: SRC-001_

## Value Promise

**CAN-004 [EXPLICIT]:** Ein vollständiges, craftbares, erweiterbares und persistentes AIR-Fahrzeug mit Cockpit, Fuel, Multiplayer-Passagier und individualisierbarer Kondensstreifenfarbe – ohne die spätere LAND/WATER-Erweiterung durch kopierte Kernlogik zu verbauen.  
_Sources: SRC-001, SRC-002_

## Current Alternatives

- **CAN-005 [EXPLICIT]:** Aktueller Projektzustand: AIR-fokussierter Prototyp mit vorhandener Grundlogik, aber ohne vollständig nachgewiesenen Release-Flow und ohne ausreichend getrennte Fahrzeugklassen-Architektur.  
  _Sources: SRC-003, SRC-004, SRC-005_

## Key Capabilities

- **CAN-006 [EXPLICIT]:** AIR/LAND/WATER-Popup mit nur AIR als ausführbarer Route in Release 1.  
  _Sources: SRC-001_
- **CAN-007 [EXPLICIT]:** Generischer Pilotensitz/Cockpit und craftbarer Copilotensitz mit serverseitiger Rollenautorität.  
  _Sources: SRC-001_
- **CAN-008 [EXPLICIT]:** Ausstieg, Wiedereinstieg und sicherer Edit-Modus zum Weiterbauen.  
  _Sources: SRC-001_
- **CAN-009 [EXPLICIT]:** Geschwindigkeit, Fuel, Steigflug, Sinkflug, Neigung und Kurvenflug.  
  _Sources: SRC-001_
- **CAN-010 [EXPLICIT]:** Craftbare, persistente farbige Kondensstreifen beziehungsweise Thruster-Trails.  
  _Sources: SRC-001_
- **CAN-011 [EXPLICIT]:** Minimale gemeinsame Vehicle-Core-Schnittstellen für spätere AIR/LAND/WATER-Profile.  
  _Sources: SRC-001, SRC-002_

## Non-Goals

- **CAN-012 [EXPLICIT]:** Keine ausführbaren LAND- oder WATER-Fahrzeuge in Release 1.  
  _Sources: SRC-001_
- **CAN-013 [EXPLICIT]:** Kein Looping als Release-1-MVP-Anforderung.  
  _Sources: SRC-001_
- **CAN-014 [EXPLICIT]:** Keine physikalisch vollständige Aerodynamiksimulation.  
  _Sources: SRC-002_

## Constraints

- **CAN-015 [EXPLICIT]:** Zieltermin 30. Juli 2026.  
  _Sources: SRC-001_
- **CAN-016 [EXPLICIT]:** Platform Lock: Fabric, Minecraft 1.21.1, Java 21 und Gradle; Änderungen nur nach expliziter Migrationsentscheidung.  
  _Sources: SRC-002, SRC-005_
- **CAN-017 [EXPLICIT]:** Runtime-, Build- und GameTest-Nachweise sind vor Release zwingend; statische Prüfung allein genügt nicht.  
  _Sources: SRC-002, SRC-004_
- **CAN-018 [EXPLICIT]:** Keine duplizierten AIR/LAND/WATER-Entities oder kopierten Assembly-, Persistence- und Networking-Pipelines.  
  _Sources: SRC-002, SRC-003_

## Risks

- **RISK-001 [EXPLICIT]:** Der bisherige Build-/Runtime-Status ist nicht vollständig verifiziert; Feature-Arbeit auf fehlerhafter Basis kann Folgeschäden erzeugen.  
  _Sources: SRC-004, SRC-005_
- **RISK-002 [ASSUMPTION]:** Der Termin kann zu parallelen Änderungen in Entity, Networking, Persistenz und Rendering verleiten und Integrationsfehler erhöhen.  
  _Sources: SRC-002_
- **RISK-003 [ASSUMPTION]:** Cockpit- und Passenger-Transforms können zwischen Clients desynchronisieren oder mit Vanilla-Riding/Camera kollidieren.  
  _Sources: SRC-002, SRC-003_
- **RISK-004 [EXPLICIT]:** Edit-Modus und Reassembly können bei nicht-atomarer Umsetzung Blöcke duplizieren oder verlieren.  
  _Sources: SRC-002, SRC-004_
- **RISK-005 [ASSUMPTION]:** Vorzeitige Generalisierung des Vehicle Core kann das AIR-MVP verzögern.  
  _Sources: SRC-002, SRC-003_
- **RISK-006 [ASSUMPTION]:** Farbige Partikel können Client-Performance beeinträchtigen.  
  _Sources: SRC-002_

## Success Signal

**CAN-019 [EXPLICIT]:** Alle P0-Anforderungen und Akzeptanzkriterien sind auf dem exakten Release-Commit durch Build, Tests, Client-/Dedicated-Server-Smoke, Multiplayer und Restart nachgewiesen; der Product Owner akzeptiert den Release bis 30.07.2026.  
_Sources: SRC-001, SRC-002_

## Evidence

- **CAN-020 [EXPLICIT]:** Erfolgreicher ./gradlew clean build, Unit-/Resource-Tests und Fabric GameTests.  
  _Sources: SRC-002_
- **CAN-021 [EXPLICIT]:** Dokumentierte Client-, Dedicated-Server-, Zwei-Spieler- und Restart-Smokes.  
  _Sources: SRC-002_
- **CAN-022 [EXPLICIT]:** Architekturprüfung gegen LAND/WATER-Branches und Kern-Duplikation.  
  _Sources: SRC-002, SRC-003_
- **CAN-023 [EXPLICIT]:** Release-JAR-Inspektion, SHA-256 und Versionsmatrix.  
  _Sources: SRC-002_

## Allowed Scope

- **CAN-024 [EXPLICIT]:** AIR Release 1: Stabilisierung, Vehicle-Build-Session, minimale Vehicle-Core-Seams, Pilotensitz/Cockpit, Copilot, Wiedereinstieg, Edit-Modus, Fuel/Flugsteuerung, farbige Trails, Persistenz, Multiplayer, Tests und Dokumentation.  
  _Sources: SRC-001, SRC-002_

## Unresolved Questions

- **OQ-001 [RESOLVED, User decision 2026-07-18]:** Fünf-Block-Regel = **euklidische 3D-Distanz** vom Spieler zum Control Anchor, ≤5 Blöcke in jede Richtung.  
  _Sources: SRC-001, SRC-002_
- **OQ-002 [RESOLVED, User decision 2026-07-18]:** Trail-Farbe wird **ausschließlich durch Crafting farbiger Thruster-Varianten** gewählt (kein nachträgliches Färben platzierter Thruster in Release 1).  
  _Sources: SRC-001_
- **OQ-003 [RESOLVED, User decision 2026-07-18]:** Cockpit-Sichttoleranz = **ausschließlich Augenhöhe** (Eye-Height-Check), unabhängig von Rüstung/Skin — bewusst einfachste Implementierung, keine Bounding-Box-Sonderfälle pro Rüstung/Skin/Third-Person-Kamera.  
  _Sources: SRC-001, SRC-002_
- **OQ-004 [RESOLVED, User decision 2026-07-18]:** Pilotensitz-Position = **strikt der Block direkt vor dem Steering-Wheel-Facing**; ist diese Position belegt/ungültig, schlägt die Assembly explizit fehl (kein stiller Fallback).  
  _Sources: SRC-001, SRC-002_
- **OQ-005 [RESOLVED, User decision 2026-07-18]:** Kein hartes numerisches Performance-Gate für Release 1 — Performance-Baseline wird **zurückgestellt** und nach echten Playtest-Daten nachgezogen.  
  _Sources: SRC-002_

## User Confirmation Required

> Ich bestätige, dass Product Canvas und Product Vision meine Absicht korrekt wiedergeben und als Grundlage für AgileTeam Planning verwendet werden dürfen.
