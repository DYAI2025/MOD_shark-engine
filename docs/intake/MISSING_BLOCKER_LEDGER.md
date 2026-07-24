# Missing / Assumption / Blocker Ledger

| ID | Field | Source Type | Severity | Reason | Reference |
|---|---|---|---|---|---|
| LED-001 | Edit-Mode-Fünf-Block-Metrik | RESOLVED (2026-07-18) | — | User-Entscheidung: euklidische 3D-Distanz zum Control Anchor, ≤5 Blöcke. | OQ-001 / REQ-012 |
| LED-002 | Trail-Färbe-Workflow | RESOLVED (2026-07-18) | — | User-Entscheidung: ausschließlich Crafting farbiger Thruster-Varianten. | OQ-002 / REQ-018 |
| LED-003 | Cockpit-Sichttoleranz | RESOLVED (2026-07-18) | — | User-Entscheidung: ausschließlich Augenhöhe (Eye-Height-Check), unabhängig von Rüstung/Skin/Kameraperspektive. | OQ-003 / REQ-007 |
| LED-004 | Pilotensitz-Position relativ zum Steering Wheel | RESOLVED (2026-07-18) | — | User-Entscheidung: strikt der Block direkt vor dem Facing; belegt/ungültig → Assembly schlägt explizit fehl, kein stiller Fallback. | OQ-004 / REQ-006 |
| LED-005 | Performance-Baseline | RESOLVED (2026-07-18) | — | User-Entscheidung: kein hartes numerisches Gate für Release 1; zurückgestellt bis echte Playtest-Daten vorliegen. | OQ-005 / NFR-006 / REQ-020 |
| LED-006 | Runtime-/Build-Beweis | MISSING | warning | Die Ausgangsbasis wurde statisch geprüft, aber der vollständige Fabric-Build und In-Game-Run waren nicht nachgewiesen. Dies ist ein Release-Gate, kein Intake-Blocker. | REQ-024 / EV-024 |
| LED-007 | Nutzerbestätigung Vision/Canvas | RESOLVED (2026-07-18) | — | User hat die exakte Bestätigungsphrase im Session-Verlauf geliefert; Canvas + Vision auf `user-confirmed`. | User Confirmation Block |
