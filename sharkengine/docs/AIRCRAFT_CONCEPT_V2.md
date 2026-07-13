# Shark Engine — Aircraft-Extension Konzept V2

**Status:** Verifiziert gegen Repo-Stand `33c3deb` (2026-07-12, Branch `feature/aircraft-extension`).
Ersetzt/erweitert `minecraft_aircraft_extension_plan.md` (Audit-Pack). Alle Code-Referenzen wurden
gegen den aktuellen Quellcode geprüft, nicht gegen den Audit-Snapshot.

**Wichtiger Nachtrag (2026-07-12):** Zwischen der ersten Fassung dieses Dokuments und jetzt wurde
entdeckt, dass `main` durch einen versehentlichen Force-Push ~2 Wochen gemergter Arbeit verloren
hatte (PRs #4–#10, März 2026: BUG-Frontsystem/„Schiffsbug"-Block, Vehicle-Health, Fuel-Tick-Fix,
Controller-Support, Yaw-Rendering). Diese Arbeit wurde wiederhergestellt (`main` auf `3c0cc2b`
zurückgeführt) und in diesen Branch eingerebast. **Der Bug-Ledger unten ist entsprechend
aktualisiert** — mehrere B-Nummern sind jetzt (teilweise) behoben, siehe Statusspalte.

Zugehöriger Implementierungsplan: `docs/plans/aircraft-extension-implementation.md`

---

## 1. Zielbild

Shark Engine bleibt eine **aus Blöcken gebaute Fahrzeug-Mod**. Die Erweiterung liefert:

1. **Eigene Assets:** 16×16-PNG-Texturen (Industrial Kupfer-Stahl), JSON-Modelle,
   Blockstates, Itemmodelle, de/en-Übersetzungen — deterministisch generiert und CI-geprüft.
2. **Craftbare Bauteile:** 11 Flugzeug-/Hubschrauberteile + 5 Zwischenprodukte,
   platzierbar, ausrichtbar, abbaubar.
3. **Eigenschaftsbasierte Fahrzeugsemantik:** Teile tragen Rollen und Kennwerte
   (Masse, Lift, Schub, Drag, Tankvolumen) statt hartcodierter Block-IDs.
4. **Sofort spielbare Slices:** Jeder Entwicklungs-Slice endet in einem spielbaren
   Zustand (runClient bzw. Testserver), nicht erst das Gesamtpaket.

**Erster spielbarer Ziel-Slice: der craftbare Hubschrauber** (Fixed-Wing folgt danach).

## 2. Verifizierter Ist-Zustand (Bug-Ledger)

Legende: ✅ **FIXED** (bestätigt gelöst) · 🟡 **TEILWEISE** (Kernproblem bleibt, Randfall neu) ·
❌ **OFFEN** (unverändert).

| # | Status | Defekt | Beleg (aktuell, Stand `33c3deb`) | Wirkung |
|---|---|---|---|---|
| B1 | 🟡 | Renderer rotiert Blueprint jetzt zwar mit Yaw (`ShipEntityRenderer.java:61-62`, `poseStack.mulPose(Axis.YN.rotationDegrees(smoothYaw))` mit Partial-Tick-Interpolation), **aber**: Blueprint-Offsets werden in `ShipAssemblyService.java:202-207` roh in Welt-Koordinaten erfasst (`dx = current.getX() - wheelPos.getX()`, unrotiert), und die Entity startet mit `yRot = bugYawDeg` (`ShipAssemblyService.java:137-138`). Für ein Schiff mit BUG-Block Richtung SÜD (`directionToYaw()`: SOUTH=0) ist das korrekt — für WEST/NORD/OST verdreht sich die Hülle im Moment der Montage sichtbar um `bugYawDeg`, obwohl physisch nichts passiert ist. | Genau das in §3.1 (V1) vorhergesagte Problem — nur jetzt mit konkretem, verifiziertem Ursprung: `bugYawDeg` statt „Pilot-Yaw" |
| B2 | ❌ | Kollision & Demontage nutzen weiterhin unrotierte Offsets | `ShipPhysics.java:171-181` (`collectOffsets()` baut Offsets direkt aus `block.dx()/dy()/dz()`), `ShipEntity.java:473` (Demontage: `base.offset(block.dx(), block.dy(), block.dz())`) — keine Rotation, keine Orientierungs-Metadaten in `ShipBlueprint` | 90°-gedrehtes/nicht-SÜD-montiertes Schiff kollidiert/demontiert im Bau-Footprint, nicht im sichtbaren |
| B3 | ✅ **FIXED** | ~~Fuel 20× zu schnell~~ — behoben via Tick-Akkumulator | `ShipEntity.java:654-670`: `fuelConsumptionTick++; if (>= 20) { ...; fuelLevel -= consumption; }` — Verbrauch nur noch 1×/Sekunde | — |
| B4 | 🟡 | Antrieb/Richtung nicht mehr hart auf `thruster` codiert für die **Richtung** (neuer BUG-Block, sauber implementiert — s. u.), **aber** die Antriebs-*Pflicht* bleibt hartcodiert | `ThrusterRequirements.java:8,18` — `THRUSTER_ID.equals(id)`, unverändert seit `eee4bc8` | Neue Motoren/Rotoren als Antriebsquelle weiterhin nur per Codeänderung |
| B5 | ❌ | Entity-Bounds fix 2.5×1.5 bei bis zu 512 Blöcken | `ModEntities.java:16` (`.sized(2.5f, 1.5f)`) vs. `ShipAssemblyService.java:37-38` (`MAX_BLOCKS=512`, `MAX_RADIUS=32`) | Culling/Pick/Interaktion decken nur Mini-Footprint ab |
| B6 | ❌ | Blueprint ohne Schema-Version & ohne BlockEntity-NBT | `ShipBlueprint.java` (`ShipBlock`-Record: nur `dx,dy,dz,state`) | Kein Migrationspfad; stateful Teile unmöglich; **kein** Orientierungsfeld — genau das, was B1/B2 fehlt |
| B7 | ✅ **FIXED** | ~~Tote Ressourcen (Plural-Pfade, `result.item`)~~ — behoben für Thruster, Lenkrad **und** den neu entdeckten dritten Fall `bug.json` (dieselbe Bug-Klasse, unabhängig im BUG-Frontsystem-Commit eingeschleppt) | `data/sharkengine/recipe/{thruster,steering_wheel,bug}.json`, `data/sharkengine/loot_table/blocks/{thruster,steering_wheel,bug}.json` | — |
| B8 | ✅ **FIXED** | ~~Steering Wheel ohne Rezept/Loot-Table~~ | s. o. | — |
| B9 | ✅ **FIXED** | ~~1.21.2+-Format-Leichen in `assets/items/`~~ — für `steering_wheel.json` **und** `bug.json` (dritter Fund, gleiche Ursache) entfernt | — | — |
| B10 | ❌ | Dismount ignoriert Blueprint | `ShipEntity.java:780-786` (`getDismountLocationForPassenger`) — fixer 1,5-Block-Versatz relativ zu `getYRot()`, kein Blueprint-Bezug | Spieler kann bei großen Schiffen in Hüllenblöcken landen |
| B11 | ❌ | Partikel am Schiffszentrum statt an Thruster-Positionen | `ShipEntityRenderer.java` `spawnThrusterParticles()` | Wird bei Rotation sichtbar falsch |
| B12 | 🟡 | Kollisionsprobe testet weiterhin aktuelle statt nächste Position, **aber**: reine Velocity-Zeroing ist behoben — echte Konsequenz via Vehicle-Health-System | `ShipEntity.java` tick(): `ShipPhysics.checkCollision(..., blockPosition(), ...)` läuft vor `this.move(...)`; bei Kollision jetzt zusätzlich `applyCollisionDamage()`, `currentSpeed *= 0.3f` | Clipping-Risiko bei hoher Geschwindigkeit bleibt; Kollision hat jetzt aber Spielkonsequenz statt nur Stopp |
| B13 | 🟡 | Stiller Blockverlust ist nicht mehr *still* — Spieler wird gewarnt (`message.sharkengine.disassembly_partial`) —, **aber** die Blöcke sind weiterhin dauerhaft verloren (kein Drop/Refund) | `ShipEntity.java:472-498` | Materialverlust bleibt; verschärft sich weiterhin mit rotierter Demontage (B2) |

### Neue Architektur-Tatsache: der BUG-Block (Schiffsbug)

Der wiederhergestellte Commit führt einen dritten eigenen Block ein: **`bug`** (deutsch:
„Schiffsbug" — Frontmarker, nicht zu verwechseln mit „Bug" = Fehler). Sauber verifiziert
(`ShipAssemblyService.java`, `BugBlock.java`):

- Genau 1 BUG-Block pro Fahrzeug ist Pflicht (`bugCount == 1`, sonst `assembly_fail_no_bug`/`assembly_fail_multi_bug`).
- Muss am Strukturrand sitzen (`isOnEdge()` — mindestens eine der 6 Nachbarzellen liegt außerhalb der gescannten Blockmenge; das ist eine reine Oberflächen-Definition, bei einer hohlen Hülle erfüllen die meisten Randblöcke das automatisch — schwächer als „an der Spitze").
- `FACING` bestimmt **exklusiv** die Vorwärtsrichtung (`directionToYaw()`: SOUTH=0, WEST=90, NORTH=180, EAST=-90 — Standard-Minecraft-Yaw-Konvention), angewendet als Start-Yaw der Entity bei Montage.
- Thruster sind dadurch **nicht mehr richtungsgebend**, nur noch Schubquelle (Kommentar im Code: „Thrusters are decorative thrust indicators only... no directional authority").

**Für unsere Planung wichtig:** `bugYawDeg` ist bereits genau das, was Abschnitt 3.1 als
„AssemblyYaw" vorschlägt — nur unter anderem Namen und bereits in Entity-NBT persistiert
(`ShipEntity.java:354,378`, Key `BugYaw`). AIR-011/AIR-015 im Implementierungsplan **bauen
darauf auf**, statt ein Konzept neu zu erfinden — siehe aktualisierte Task-Beschreibungen.

**Konsequenz (bestätigt):** „Erst Texturen, dann Fundament“ ist die falsche Reihenfolge.
Lange Flügel und Rotoren machen B1/B2/B5 drastisch sichtbarer. Aber: Das Fundament ist
klein genug, um es als ersten Slice **spielbar** zu liefern — kein Widerspruch zu
„sofort spielbar“, sondern dessen Voraussetzung.

## 3. Architektur (unverändert übernommen + Ergänzungen)

Die Hybrid-Architektur aus dem V1-Konzept bleibt: Blöcke im Bauzustand, `ShipBlueprint`
als Geometrie, statischer Renderpass + animierter Rotor-Pass, Server autoritativ,
kein Rotorwinkel-Paket pro Tick.

### 3.1 Das Assembly-Yaw-Referenzproblem — jetzt am realen Code verifiziert (B1)

**Dies ist kein hypothetisches Risiko mehr, sondern ein bestätigter Bug (B1, Status 🟡).**
Blueprint-Offsets werden bei der Montage in **Welt-Achsen** erfasst
(`ShipAssemblyService.java:202-207`, `dx = current.getX() - wheelPos.getX()`, roh, unrotiert).
Die Entity startet aber mit `yRot = bugYawDeg` — dem Yaw, der aus der `FACING`-Property des
BUG-Blocks abgeleitet wird (`ShipAssemblyService.java:137-138,246,270-278`). Der Renderer
rotiert bereits (`ShipEntityRenderer.java:61-62`, `poseStack.mulPose(...)`) — aber **ohne
Kompensation**: Für BUG-Richtung SÜD (`directionToYaw()`-Konvention: SOUTH=0) ist `bugYawDeg=0`
und alles passt zufällig. Für WEST/NORD/OST verdreht sich die Hülle im Moment der Montage
sichtbar um `bugYawDeg`, obwohl physisch nichts passiert ist — exakt das hier vorhergesagte
Muster, nur mit `bugYawDeg` statt „Pilot-Yaw" als Ursache.

**Lösung (unverändert gültig, jetzt mit realer Datenquelle):** Blueprint v2 speichert
`assemblyYaw`, das **direkt aus dem bereits vorhandenen `bugYawDeg`** übernommen wird
(kein neues Konzept — `bugYawDeg` ist schon in Entity-NBT persistiert, Key `BugYaw`,
`ShipEntity.java:354,378`; v2 macht daraus ein Blueprint-Feld statt eines reinen
Entity-Felds, siehe §11 REQ-M1). Alle Transformationen nutzen die **effektive Rotation**
`θ = wrapDegrees(entityYaw − assemblyYaw)`:

- **Rendering:** kontinuierlich um θ rotieren (ein `mulPose` um den Entity-Ursprung).
- **Kollision:** Offset-Vektor um θ rotieren, dann auf Blockpositionen runden (Set-Dedupe).
- **Demontage:** θ auf das nächste Vielfache von 90° snappen; Blöcke via
  `BlockState.rotate(Rotation)` mitdrehen (FACING/AXIS bleiben konsistent).
- **Legacy-Fallback (v1-NBT ohne `assemblyYaw`):** `assemblyYaw := entity.bugYawDeg`
  (das bereits existierende, unter dem NBT-Key `BugYaw` persistierte Feld —
  **nicht** der live/aktuelle `getYRot()`-Wert, der sich durch Fliegen/Drehen
  ändert und hier falsch wäre). `bugYawDeg` wird in `readAdditionalSaveData`
  gelesen, **nachdem** `ShipBlueprint.fromNbt`/`applyBlueprintStats` gelaufen
  sind (verifiziert: `ShipEntity.java` liest Blueprint zuerst, dann `BugYaw`/
  `ThrustYaw` NBT-Keys) — v1-Schiffe rendern damit exakt wie bisher, kein
  visueller Bruch. *(Präzisiert 2026-07-12 via ultrathink-craftsmanship: die
  vorherige Formulierung „gespeicherter Entity-Yaw" war mit dem Plan-Dokument
  nicht deckungsgleich und ließ offen, ob der live- oder der Montage-Yaw
  gemeint war — das hätte bei der AIR-015-Implementierung zu genau der
  falschen Wahl führen können.)*
  Der Fallback läuft **serverseitig** in `readAdditionalSaveData` direkt nach `fromNbt`
  (bevor Tracking startet) — jeder `ShipBlueprintS2CPayload` trägt damit bereits
  v2-NBT inkl. `AssemblyYaw`; einen clientseitigen v1-Pfad gibt es nie.

### 3.2 `ShipTransform` — einzige Rotationsautorität

```java
public final class ShipTransform {
    // pure math, fully unit-testable — no Minecraft classes needed for core ops
    static Vec3 rotateOffset(double dx, double dy, double dz, float thetaDeg);
    static BlockPos worldBlock(BlockPos origin, ShipBlock b, float thetaDeg);
    static Rotation snapToCardinal(float thetaDeg);          // nearest 90°
    static float effectiveYaw(float entityYaw, float assemblyYaw);
}
```

Konsumenten: Renderer, Kollision, Demontage, Bounds, Partikel/Sound-Positionen,
Dismount, spätere Hardpoints. **Keine zweite Rotationsformel im Codebase — CI-Regel.**

### 3.3 Domänenobjekte (wie V1, präzisiert)

- `PartRole`: `STRUCTURE, SKIN, LIFT_SURFACE, CONTROL_SURFACE, PROPULSION, ROTOR_HUB,
  ROTOR_BLADE, LANDING_GEAR, FUEL_STORAGE, CONTROL`
- `VehiclePartDefinition(role, mass, lift, thrust, drag, fuelCapacity, renderGroup)` —
  Java-Registry (`VehiclePartRegistry`), Block→Definition; Fallback-Definition
  (mass = 1, Rolle STRUCTURE) für generische `ship_eligible`-Blöcke.
  Legacy-`thruster` wird als PROPULSION-Definition registriert — `ThrusterRequirements`
  entfällt ersatzlos (B4).
- `ShipPartAnalyzer`: läuft einmal beim Assembly-Scan → `ShipStats`, `RotorAssembly`-Liste,
  Blueprint-Bounds, strukturierte Validierungscodes.
- `AssemblyIssue(code, blockPos?, args)` statt roher Translation-Keys — HUD/Builder-Screen
  können alle Blocker auflisten; Codes sind testbar.

## 4. Bauteilsatz mit konkreten Kennwerten

Das V1-Konzept nannte keinerlei Zahlen. Diese Tabelle ist die **initiale
Balancing-Basis** — zentral in `VehicleBalance.java`, durch Unit-Tests gelockt,
bewusst einfach und nachvollziehbar:

| ID | Name (de) | Rolle | mass | lift | thrust | drag | fuelCap | Blockstate |
|---|---|---|---|---|---|---|---|---|
| `airframe_panel` | Außenhüllenplatte | SKIN | 1 | – | – | – | – | `facing` |
| `fuselage_frame` | Rumpfrahmen | STRUCTURE | 2 | – | – | – | – | `axis` |
| `wing_root` | Flügelwurzel | LIFT_SURFACE | 2 | 3 | – | 1 | – | `facing` |
| `wing_panel` | Flügelsegment | LIFT_SURFACE | 1 | 4 | – | 1 | – | `facing` |
| `wing_tip` | Flügelspitze | LIFT_SURFACE | 1 | 2 | – | 0 | – | `facing` |
| `tail_fin` | Heckleitwerk | CONTROL_SURFACE | 1 | 0 | – | 1 | – | `facing` |
| `helicopter_engine` | Hubschraubermotor | PROPULSION | 6 | – | 40 | – | – | `facing` |
| `rotor_hub` | Rotor-Hub | ROTOR_HUB | 3 | – | – | – | – | `axis` |
| `rotor_blade` | Rotorblatt | ROTOR_BLADE | 1 | 8 | – | – | – | `facing` |
| `landing_skid` | Landekufe | LANDING_GEAR | 1 | – | – | – | – | `facing` |
| `fuel_tank` | Kraftstofftank | FUEL_STORAGE | 3 | – | – | – | +100 | `axis` |
| `thruster` (Legacy) | Triebwerk | PROPULSION (liftMode `DIRECT`) | 2 | – | 20 | – | – | bestehend |
| `steering_wheel` (Legacy) | Lenkrad | CONTROL | 2 | – | – | – | – | bestehend |
| `bug` (wiederhergestellt) | Schiffsbug | CONTROL | 1 | – | – | – | – | `facing` (bestehend) |
| *(generischer `ship_eligible`-Block)* | — | STRUCTURE | 1 | – | – | – | – | — |

`bug` ist **nicht** Teil des V1-Plans, sondern kam mit der wiederhergestellten März-Arbeit
(§2) dazu: exakt 1 Pflicht pro Fahrzeug, muss am Strukturrand sitzen, `FACING` bestimmt die
Vorwärtsrichtung exklusiv. Ersetzt die früher geplante „Pilot-Yaw-bei-Montage"-Herleitung
vollständig — assemblyYaw (§3.1) kommt jetzt aus `bugYawDeg`, nicht aus dem Blickwinkel des
Spielers.

PROPULSION-Definitionen tragen zusätzlich ein `liftMode`: **`DIRECT`** (Triebwerk hebt
selbst — Legacy-Thruster) oder **`ROTOR`** (`helicopter_engine` — treibt nur Rotoren,
macht allein nicht flugfähig). Die Unterscheidung läuft über die Definition, nie über
Block-IDs (REQ-S1).

**MVP-Regeln (Hubschrauber, Slice „Rotor lebt“):**

- Flugfähig ist ein Fluggerät, wenn **(a)** `Σ thrust(DIRECT) > 0` (Legacy-/Jet-Pfad —
  bestehende Thruster-Schiffe fliegen unverändert weiter) **oder** **(b)** gültige
  Rotor-Topologie **und** `Σ lift(Rotorblätter) ≥ Σ mass`.
  → 2-Blatt-Rotor trägt 16 mass, 4-Blatt 32 mass. Mehrere Rotoren addieren.
  Ein `helicopter_engine` ohne gültigen Rotor wird mit konkretem Code abgelehnt.
- `WeightCategory` wird von Blockanzahl auf `mass` umgestellt; Schwellen skaliert
  (LIGHT ≤ 120, MEDIUM ≤ 240, HEAVY ≤ 360, OVERLOADED > 360) — Werte in
  `VehicleBalance`. Ursprünglich 30/60/90; am 2026-07-13 um das Vierfache
  angehoben (Nutzerwunsch: größere Flugzeuge sollen baubar/flugfähig bleiben).
- Bestehende Speed-/Phasen-Pipeline (`AccelerationPhase`, Höhenmalus) bleibt unverändert.

**Fixed-Wing-Regeln (späterer Slice):** Lift zählt erst ab Phase 3 (≥ 15 b/s);
`tail_fin` Pflicht; Links/Rechts-Lift-Asymmetrie ≤ 25 %. Einfach, testbar, kein
Aerodynamik-Modell.

### Zwischenprodukte & Rezepte (konkret, via Datagen)

| Item | Ausbeute | Rezept (shaped, Kürzel) |
|---|---|---|
| `metal_sheet` | ×4 | `II / CC` (I=iron_ingot, C=copper_ingot) |
| `rotor_shaft` | ×2 | `I / C / I` (Säule) |
| `engine_core` | ×1 | `ICI / CRC / ICI` (R=redstone_block) |
| `bearing_assembly` | ×2 | shapeless: 1 iron_ingot + 2 copper_ingot |
| `reinforced_fabric` | ×2 | shapeless: 2 `#minecraft:wool` + 2 string |
| `airframe_panel` | ×4 | `MM` (M=metal_sheet) |
| `fuselage_frame` | ×4 | `MIM` |
| `helicopter_engine` | ×1 | `M / E / S` (E=engine_core, S=rotor_shaft) |
| `rotor_hub` | ×1 | `B / S` (B=bearing_assembly) |
| `rotor_blade` | ×2 | `SMM` |
| `landing_skid` | ×2 | `I I / MMM` |
| `wing_root` | ×2 | `IM / MM` |
| `wing_panel` | ×4 | `MM / MM` |
| `wing_tip` | ×2 | `M. / MM` |
| `tail_fin` | ×2 | `M / M / I` |
| `fuel_tank` | ×1 | `MMM / M.M / MMM` |
| `steering_wheel` (Reparatur B8) | ×1 | `.P. / PCP / .P.` (P=planks, C=copper_ingot) |
| `thruster` (Format-Reparatur B7) | ×1 | bestehendes Rezept, korrigiert nach `recipe/` + `result.id` |

Grids sind Balancing-Konstanten (Datagen-Provider) — der Ressourcenvertrag testet
*Existenz und Ergebnis-Item*, nicht das Grid.

## 5. Assets: Stil, Produktion, Qualitätssicherung

### 5.1 Stil-Entscheidung: **Industrial Kupfer-Stahl** (bestätigt)

Begründung: Die zwei bestehenden Modelle nutzen bereits `minecraft:block/copper_block`,
`iron_block`, `polished_deepslate`, `stripped_oak_log` — der neue Satz schließt visuell
an, statt zu brechen.

- **16×16 px** pro Materialfläche, keine Auflösungsmischung.
- 3–5 Töne pro Material; Nieten, Paneelfugen, Verstrebungen als Materialkennzeichen;
  keine fotorealistischen Mikrodetails.
- Richtungslesbarkeit: Vorderkante hell, Hinterkante dunkel, Wurzel↔Spitze unterscheidbar.

**Verbindliche Startpalette** (`tools/asset-gen/palette.json`, Feintuning in-game erlaubt,
Änderungen nur am Palette-File — nie ad hoc in Einzeltexturen):

| Familie | light | base | shadow | deep | seam |
|---|---|---|---|---|---|
| `dark_steel` | `#7a8290` | `#565e6a` | `#3f4550` | `#2b303a` | `#1d2129` |
| `copper_brass` | `#e0a06a` | `#c17f4a` | `#96603a` | `#6f452c` | `#503121` |
| `light_alloy` | `#c3c9cf` | `#a9b0b8` | `#8d949d` | `#6e747d` | `#5a5f66` |
| `painted_accent` (Signal/Rost-Rot) | `#d9764a` | `#8c3b2e` | `#63281f` | `#451a15` | — |

Material-Zuordnung: Hülle/Flügel = `dark_steel` + `painted_accent`-Kante; Mechanik
(Engine, Hub, Shaft) = `copper_brass` + `dark_steel`; Kufen/Rahmen = `light_alloy`.

### 5.2 Produktions-Pipeline: deterministisch generierte Pixel-Art

Texturen werden **nicht manuell gepixelt**, sondern von committeten Python-Skripten
erzeugt (`tools/asset-gen/`, Python 3.12 + Pillow):

```text
tools/asset-gen/
├── palette.json                  # einzige Farbquelle
├── generate.py                   # CLI: alle oder einzelne Texturen erzeugen
├── parts/<part>.py               # eine deterministische Zeichenfunktion je Textur
└── README.md                     # Regeln: Nieten-Raster, Fugenbreite, Kantenlogik
```

- Deterministisch (kein ungeseedeter Zufall) → Regenerieren ist diff-stabil.
- Output wird nach `src/main/resources/assets/sharkengine/textures/...` committed.
- **Palette-Konformitätstest** in `ResourceValidationTest`: jedes PNG ist 16×16 und
  enthält ausschließlich Palettenfarben (+ Alpha).
- Optik-Iteration: Skript ändern → regenerieren → `runClient`-Sichtprüfung. Das
  menschliche Auge bleibt Qualitäts-Gate; die Pipeline macht Iteration billig.

### 5.3 Modelle

- Elements-basierte JSON-Modelle (Blockbench-kompatibel), handgeschrieben bzw. per
  Datagen-Provider; ein Block = ein Segment (keine Riesenmodelle über Blockgrenzen).
- `rotor_blade` flach (~3 px hoch), `rotor_hub` kompakt zentriert, `landing_skid`
  Kufenprofil, Flügel keilförmig via `facing`-Varianten.
- Platzierte Blöcke: vereinfachte `VoxelShape`s. Fliegendes Schiff: transformierte
  Blueprint-Hülle, nie Modellgeometrie.

### 5.4 Ressourcenvertrag (CI-hart)

Erweiterung von `ResourceValidationTest` + Datagen als kanonische Quelle:

- Jeder registrierte Block: Blockstate, Blockmodell, Itemmodell, Loot-Table, Rezept
  (wenn craftbar), en+de-Übersetzung, `VehiclePartDefinition`, `ship_eligible`-Tag.
- Alle Texture-Referenzen lösen auf; Dateinamen lowercase; **Pfade singular**
  (`recipe/`, `loot_table/`) — Plural-Verzeichnisse schlagen als Test fehl
  (Ausweitung des bestehenden Tag-Guards auf alle Datenpfade).
- Neue Tags: `aircraft_structure`, `lift_surfaces`, `propulsion`, `rotor_hubs`,
  `rotor_blades` (+ bestehendes `ship_eligible`).
- Generierter Output diff-stabil; fehlende Datei ⇒ CI rot.

## 6. Rotor-Topologie & Animation (konkretisiert)

**Topologie-Regeln** (wie V1): Hub benötigt benachbarten `helicopter_engine`; gültig
sind exakt 2 gegenüberliegende oder 4 kardinale Blattketten; Ketten lückenlos, eine
Ebene, eine Materialvariante; Haupt-/Heckrotor via Hub-Achse (`axis=y` Haupt,
horizontal Heck).

**Animation (konkrete Startwerte, in `VehicleBalance`):**

- `rotorAngle(t) = basePhase + ω · (level.getGameTime() + partialTick)` —
  **Weltzeit, nicht Entity-`tickCount`**: `tickCount` ist client-lokal und startet
  bei jedem Tracking-Beginn bei 0; zwei Clients sähen dauerhaft verschiedene
  Blattwinkel, Re-Tracking erzeugte Sprünge. `getGameTime()` ist server-synchronisiert
  und liefert auf allen Clients denselben Winkel — auch bei Join mitten im Flug.
- ω: Idle **9°/Tick** (0,5 U/s), Volllast **36°/Tick** (2 U/s), linearer Spool-up/-down
  über **40 Ticks**; Spool-Anker = Weltzeit-Timestamp des Statuswechsels
  (`SPOOL_CHANGE_GAME_TIME` in EntityData), nie client-lokale Tick-Deltas.
- Sync ausschließlich über EntityData (`ENGINE_STATE`, Ziel-ω, Spool-Timestamp) bei
  Änderung — keine Winkelpakete. Beide Clients konvergieren, weil die Formel
  deterministisch aus synchronisierter Weltzeit + State rechnet.
- Renderpass: statische Blöcke ohne ROTOR_BLADE-Gruppe rendern → je `RotorAssembly`
  zum Hub pivotieren, um Hub-Achse drehen, Blätter relativ rendern.
- Bodenmodus: platzierte Rotorblätter statisch (kein MVP-Blocker, wie V1).
- Partikel & Sound an transformierte PROPULSION/ROTOR_HUB-Positionen binden (B11).

## 7. Multiplayer & Versionierung (wie V1, bestätigt)

- Mod auf Server **und** Client; Resource Pack ersetzt weder Renderer noch Payloads.
- Blueprint v2: `SchemaVersion: 2` + `AssemblyYaw` im NBT; v1-Fallback (siehe 3.1).
- `protocolVersion` in Handshake/Join-Check; inkompatible Clients erhalten klare
  Ablehnung statt Desync.
- Release-Gate `real-boundary-smoke`: Dedicated Server (Docker, `/test-server`) +
  zwei echte Clients: identisches Fluggerät, identische Rotoranimation, identische
  Demontage; Join/Track/Untrack/Chunk-Reload/Restart.

## 8. Teststrategie (Entscheidung: volle Tiefe)

| Ebene | Werkzeug | Deckt ab |
|---|---|---|
| Unit | JUnit 5 (bestehendes Muster, Stubs) | ShipTransform-Mathe, VehicleBalance, PartRegistry, Analyzer/Stats, Rotor-Topologie, Fuel-Rate, Blueprint-v1/v2-Roundtrip, Validierungscodes |
| Resource | `ResourceValidationTest` (erweitert) | Vollständigkeit, Pfad-/Format-Konventionen, Palette-Konformität, Lang-Parität de/en |
| GameTest | **Fabric GameTest API** (neu, headless Server in CI) | Assembly-Konstellationen aus Struktur-Templates, Kollision & Demontage bei 0/90/180/270°, Fuel über 20 Ticks, Dismount-Sicherheit, Craft/Place/Break je Teil |
| Manuell | `runClient`, `/mc-bugtest`, `/test-server` | Rendering, Rotoranimation, HUD, Optik-Review, Zwei-Client-Smoke |

TDD-Disziplin: **Jeder Task beginnt mit einem fehlschlagenden Test** (RED → GREEN →
REFACTOR). Rendering-Tasks, die nicht automatisiert prüfbar sind, definieren vorab
eine manuelle Verifikations-Checkliste als Akzeptanz-Evidence.

## 9. Slices (Reihenfolge für „sofort spielbar“)

| Slice | Inhalt | Spielbar-Gate |
|---|---|---|
| **0 – Repair & Rails** | Ressourcen-Reparatur (B7/B8/B9), Fuel-Fix (B3), CI-Baseline, GameTest-Infra | Thruster & Steering Wheel erstmals craftbar/droppen; Fuel-Verbrauch exakt testgelockt (Volllast-Flugzeit ≈ 37 s, HUD konsistent) |
| **1 – Foundation** | ShipTransform, Blueprint v2 (+assemblyYaw), Renderer-Yaw, Kollision/Demontage (+B13-Drops), Render-/Cull-Bounds, Dismount (B1/B2/B5/B6/B10/B12/B13) | L-förmiges Testschiff stimmt bei 0/90/180/270° in Bild, Kollision, Demontage; kein Blockverlust |
| **2 – Semantik** | PartRole/Definition/Registry (+liftMode), Analyzer, Stats, Validierungscodes, VehicleBalance inkl. Masse-Umstellung samt HUD-Sync (B4) | Thruster läuft über Definition; Builder zeigt strukturierte Fehler; HUD-Kategorie = Server-Kategorie |
| **3 – Heli-Assets** | Datagen, Asset-Pipeline, Ressourcenvertrag; **zuerst Zwischenprodukte** (`metal_sheet`, `rotor_shaft`, `engine_core`, `bearing_assembly`), dann `airframe_panel`, `fuselage_frame`, `helicopter_engine`, `rotor_hub`, `rotor_blade`, `landing_skid`, `fuel_tank` | Alle 7 Teile craftbar, platzierbar, texturiert, montierbar |
| **4 – Rotor lebt** | Topologie-Validierung, Rotor-Renderpass, RPM-Sync, Effekte an Part-Positionen | Heli fliegt mit sichtbar drehendem Rotor; Lift-Regel greift |
| **5 – Fixed-Wing** | `wing_*`, `tail_fin`, Flugregeln | Flugzeug mit Flügel-Anforderungen fliegt; ungültige Builds klar abgelehnt |
| **6 – MP-Release** | Protokollversion, Zwei-Client-Smoke, Packaging (Modrinth) | `real-boundary-smoke` bestanden |

## 10. Definition of Done — Slice „craftbarer Hubschrauber“ (nach Slice 4)

Unverändert streng wie V1, plus Verifikationsklauseln:

- Hülle, Rumpfrahmen, Motor, Hub, Rotorblätter, Kufen, Tank craftbar mit eigenen
  kohärenten Texturen und Modellen (Palette-konform, CI-geprüft).
- Rotor-Fluggeräte ohne gültigen Motor+Rotor+Lift werden mit konkretem Code
  abgelehnt; reine DIRECT-Thrust-Schiffe (Legacy-Thruster) fliegen unverändert.
- Fluggerät rendert in Bewegungsrichtung; Kollision und Demontage nutzen dieselbe
  Transformation (`ShipTransform`, eine Autorität).
- Rotoren animieren clientseitig deterministisch.
- Fuel-Verbrauch entspricht exakt der dokumentierten Rate (Test-gelockt).
- Evidence: grüner CI-Lauf (Unit + Resource + GameTest) **und** dokumentierter
  Zwei-Client-Smoke. Kein „Done“ auf Basis von Screenshots oder statischer
  JSON-Prüfung allein.

## 11. Requirements-Register

| REQ | Inhalt |
|---|---|
| REQ-F1 | Schiff rendert um effektiven Yaw rotiert (`entityYaw − assemblyYaw`) |
| REQ-F2 | Kollision & Demontage nutzen dieselbe Transformation; Demontage snappt auf 90°, rotiert BlockStates und verliert keine Blöcke (blockierte Positionen droppen als Items, B13) |
| REQ-F3 | Fuel-Verbrauch entspricht dokumentierter per-Sekunde-Rate; HUD-Restzeit konsistent |
| REQ-F4 | Render-/Culling-Bounds folgen Blueprint-Bounds; Entity-Kollisionsbox bleibt klein (keine Phantom-Kollision/Pick-Box — Welt-Kollision läuft über `ShipPhysics`) |
| REQ-F5 | Dismount platziert Spieler kollisionfrei außerhalb der Hülle |
| REQ-S1 | Teile-Semantik über `PartRole`/`VehiclePartDefinition`/Registry; Legacy-Thruster als Definition, kein ID-Hardcode |
| REQ-S2 | `ShipPartAnalyzer` aggregiert `ShipStats` deterministisch |
| REQ-S3 | Assembly-Validierung liefert strukturierte Fehlercodes |
| REQ-S4 | Balancing-Konstanten zentral in `VehicleBalance`, testgelockt |
| REQ-A1 | 11 Teile + 5 Zwischenprodukte craftbar/platzierbar/abbaubar/übersetzt (de+en) |
| REQ-A2 | Eigene 16×16-Texturen, Kupfer-Stahl-Palette, deterministisch generiert, Palette-Konformität CI-geprüft |
| REQ-A3 | Datagen ist kanonische Ressourcenquelle; Ressourcenvertrag CI-hart |
| REQ-A4 | Alle Datenpfade/Formate 1.21.1-konform (`recipe/`, `loot_table/`, `result.id`) |
| REQ-R1 | Rotor-Topologie (2/4-Blatt) validiert; ungültige Ketten mit Code abgelehnt |
| REQ-R2 | Rotoranimation clientseitig deterministisch aus synchronisierter Weltzeit (`getGameTime`) + Sync-State — identischer Winkel auf allen Clients, auch nach Re-Tracking |
| REQ-R3 | Sync kompakt (EntityData bei Änderung), keine Per-Tick-Winkelpakete |
| REQ-R4 | Partikel/Sound an transformierten Part-Positionen |
| REQ-W1 | Fixed-Wing-Regeln: Lift ab Phase 3, Tail-Fin-Pflicht, Symmetrie ≤ 25 % Abweichung |
| REQ-M1 | Blueprint v2 mit `SchemaVersion` + `AssemblyYaw`; v1-Fallback ohne visuellen Bruch |
| REQ-M2 | `protocolVersion`-Prüfung mit klarer Fehlermeldung |
| REQ-M3 | Zwei-Client-Smoke-Evidenz vor MP-Release |
| REQ-T1 | Fabric-GameTest-Infrastruktur läuft headless in CI |
| REQ-G1 | Jeder Slice endet in verifiziert spielbarem Zustand |
