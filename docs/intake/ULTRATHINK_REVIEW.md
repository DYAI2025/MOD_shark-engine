# Ultrathink Craftsmanship Review

## Triage

**Tief** — Der Kernengpass ist nicht das Umschreiben des PRD, sondern die Trennung von Nutzerintention, abgeleiteter Architektur und ungeklärten Implementierungsdetails bei vollständiger Traceability.

## Problemrahmen

- **Ziel:** Vier getrennte, Plumbline-kompatible Intake-Artefakte mit vollständiger REQ→Vision→Canvas→AC→Evidence-Verknüpfung.
- **Nicht-Ziel:** LAND/WATER implementieren, Looping in Release 1 ziehen oder Runtime-Funktion behaupten.
- **Constraints:** Zieltermin 30.07.2026; Fabric 1.21.1/Java 21; keine simulierte Nutzerbestätigung; Ausgangsrepository runtime-unverifiziert.
- **Erfolgskriterium:** Keine untracebaren Anforderungen, keine verdeckten Annahmen, keine Blocker im Value-Kern, Status höchstens `READY_FOR_USER_CONFIRMATION`.

## Geprüfte Optionen

| Option | Nutzen | Kosten | Risiko | Entscheidung |
|---|---|---|---|---|
| Vorheriges PRD unverändert als Intake deklarieren | Schnell | Gering | Vermischt Vision, Canvas, PRD und Traceability; verletzt Plumbline-Vertrag | Verworfen |
| Vier manuell getrennte Dokumente mit dupliziertem Text | Verständlich | Mittel | Hoher Drift zwischen Anforderungen und Matrix | Verworfen |
| Normalisiertes, source-indexiertes Intake-Modell mit daraus erzeugten vier Artefakten | Konsistente IDs und Traceability | Höherer Initialaufwand | Modell kann überformalisiert werden | Gewählt, aber auf 24 Release-relevante REQs begrenzt |

## Craftsmanship-Prüfung

- **Einfachheit:** LAND/WATER bleiben Dokumentationspfade; keine vorsorgliche Physikabstraktion.
- **Kohäsion:** Vision beschreibt Absicht, Canvas Wert und Scope, PRD Umsetzung, Traceability Nachweis.
- **Explizitheit:** Fünf-Block-Metrik, Färbe-UX, Cockpit-Sichttoleranz und Performance-Baseline sind offen markiert.
- **Testbarkeit:** Jede REQ besitzt mindestens ein Given/When/Then-Kriterium und einen Evidence-Eintrag.
- **Operabilität:** Release bleibt bei fehlendem Build/GameTest/Runtime-Smoke blockiert.
- **Änderbarkeit:** Vehicle Core wird nur an den für AIR benötigten Seams geschnitten.

## Gegenprüfung

- **Stärkstes Gegenargument:** Die Architektur- und Intake-Arbeit könnte den knappen AIR-Termin belasten. Gegenmaßnahme: Das Paket fordert nur minimale Seams und keine LAND/WATER-Implementierung.
- **Failure Mode 1:** Unklare Fünf-Block-Regel → falsche Distanzlogik → Edit Mode unbenutzbar oder exploitable → Release-Blocker. Deshalb OQ-001 vor TASK-Umsetzung klären.
- **Failure Mode 2:** Vehicle Core zu allgemein → zusätzlicher Refactor → AIR-Integration verspätet → Termin verfehlt. Deshalb keine generische Physik-Engine, sondern AIR-bezogene Interfaces.
- **Failure Mode 3:** Nutzerbestätigung simuliert → AgileTeam plant auf falscher Vision → Scope Drift. Deshalb Status `READY_FOR_USER_CONFIRMATION`.
- **Bias-Risiken:** Architecture Bias, Deadline Bias, Confirmation Bias gegenüber dem bereits erstellten PRD und Sunk-Cost Bias am bestehenden `ShipEntity`.

## Konfabulations-Audit

| Claim | Status |
|---|---|
| Zieltermin 30.07.2026 | belegt durch SRC-001 |
| AIR first, LAND/WATER später | belegt durch SRC-001 |
| Looping außerhalb Release 1 | belegt durch SRC-001 |
| Fabric 1.21.1 / Java 21 | belegt durch vorhandenes PRD/Repository-Kontext, Build weiterhin ungeprüft |
| Vollständige Runtime-Funktion | nicht behauptet; EV-024 bleibt offen |
| 16 Farbstoffe | ableitbare Annahme, nicht als bestätigte Produktwahrheit behandelt |
| Fünf-Block-Metrik | ungeklärt; OQ-001 |
| Numerisches Performance-Ziel | nicht behauptet; MISSING |

## Entscheidung

Kleinste robuste Lösung: Vollständiges Intake-Paket mit 24 Release-relevanten Anforderungen, vollständig verlinkt, aber noch nicht AgileTeam-planning-ready ohne explizite Vision-/Canvas-Bestätigung.
