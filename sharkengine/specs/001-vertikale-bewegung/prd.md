# Product Requirements Document – MSP_1 Luftfahrzeug-MVP

## 1. Problem Statement
Spieler können zwar Schiffe zusammenbauen, erhalten aber keine vollständige Luftfahrzeug-Erfahrung: vertikale Steuerung fehlt, Gewicht/Treibstoff haben keinen Einfluss und der Builder-Flow kommuniziert nicht, ob ein Schiff flugtauglich ist. MSP_1 liefert den Luftfahrzeug-MVP mit klarer Bauphase, Thruster-Anforderungen, Steuerung und Feedback.

## 2. Ziele & Nicht-Ziele
- **Ziele**
  1. Responsives Fluggefühl mit kombinierten WASD + vertikalen Eingaben, fünf Beschleunigungsphasen und Höhenstrafe.
  2. Builder-Modus validiert eligible Blöcke/Thruster vor dem Start (visuelle Highlights, UI-Status, Messaging).
  3. Tieferes Gameplay durch Fuel/Weight HUD, Warnungen und Thruster-basierte Partikel/Sounds.
  4. Vollständig getestete Server-Physik + Client UX (`./gradlew test`).
- **Nicht-Ziele**
  - Keine Kollisionsvermeidung oder Kollisionsschaden.
  - Keine Land-/Wasserfahrzeuge im MSP_1-Scope.
  - Kein Pilot-Wechsel oder Multiplayer-Control-Handoff.

## 3. User Experience Overview
1. Spieler platziert ein Steuerrad (erhält automatisch 2 Thruster).  
2. Rechtsklick öffnet Builder-Screen: weiße Highlights = valide, rote = ungültig; UI zeigt Blockzahl, Invalid/Contact counts, Button nur aktiv wenn launch-ready.  
3. Nach erfolgreichem Assemblieren startet Flug im Third-Person-Modus mit HUD (Fuel %, Höhe, Speed, Gewicht). Thruster-Partikel & Sound reagieren auf Beschleunigungsphase, Fuel sinkt je Phase.  
4. Engine-Out → Schiff fällt, Warnung im Chat/HUD. Anchor blockiert Bewegung.

## 4. Functional Requirements & Tasks
Priorisiert nach **Impact → Aufwand/Komplexität**.

| # | Requirement | Key Tasks | Impact | Complexity |
|---|-------------|-----------|--------|------------|
| 1 | Core Flight Controls | - Extend `ShipEntity` physics loop<br>- Cache blueprint stats (count, thrusters)<br>- Integrate collision + anchor gating | High | Medium |
| 2 | Vertical Input Pipeline | - Fix `HelmInputClient` caching<br>- Extend `HelmInputC2SPayload` & server `setInputs` | High | Medium |
| 3 | Builder Scan & Assembly | - New `StructureScan` with invalid/contact detection<br>- Rework `ShipAssemblyService.tryAssemble`<br>- Steering wheel triggers builder preview | High | High |
| 4 | Builder UI & Highlighting | - `BuilderPreviewS2CPayload`/`BuilderAssembleC2SPayload`<br>- Client overlay + non-pausing screen, gating logic | High | High |
| 5 | Thruster Assets & Requirements | - Register thruster block/item/recipe/loot/tag<br>- Wheel drops thrusters on placement<br>- Renderer/Sounds gated on `hasThrusters()` | Medium | Medium |
| 6 | HUD & Messaging | - Fuel/weight HUD overlay<br>- Builder/assembly localization strings | Medium | Low |
| 7 | Third-Person Flight Camera | - Force camera on pilot mount, revert on exit | Medium | Medium |
| 8 | Testing & Tooling | - Add JUnit deps + stub Minecraft particle classes<br>- Keep `ShipPhysicsTest`/`FuelSystemTest` green | Medium | Medium |

## 5. Success Metrics
- `./gradlew test` passes (no classpath errors).  
- Manual playtest: builder clearly communicates invalid blocks; thruster-less assemblies rejected.  
- Flight feels smooth (no jitter), Partikels/Audio scale with phase.  
- Fuel/Weight HUD updates realtime; engine-out behavior triggered <= 1s after fuel depletion.

## 6. Dependencies & Risks
- Depends on Fabric API 0.102.0+1.21.1 (already in repo).  
- Sound assets placeholders; runtime warns if missing (acceptable).  
- Risk: Builder overlay mis-synced → mitigation: server authoritative scan + payload refresh on failure.

## 7. Rollout Plan
1. Implement High-impact controls (Req.1–2).  
2. Ship Builder preview/assembly (Req.3–4).  
3. Integrate thrusters + HUD/camera polish (Req.5–7).  
4. Testing/tooling + manual verification (Req.8 + playtest).  
5. Update Spec-Flow `state.yaml` to `implement_complete` once shipped.

