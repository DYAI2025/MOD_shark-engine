# MSP-1 Plan: Luftfahrzeug-Baukasten

## Ziel
Ein vollständiger Loop vom Platzieren des Steuerrads über ein geführtes Tutorial bis zum ersten Flug. Der Spieler versteht intuitiv, dass er eine Fahrzeug-Mod nutzt, lernt das Bauen anhand von Popups und kann anschließend ein Luftfahrzeug mit allen Physik-Mechaniken steuern.

## Deliverables
1. **Intuitive Einstiegserfahrung**
   - Steuerrad erhält ein deutlich anderes Modell/Textur sowie Item-Icon.
   - Popup 1 (nach Platzierung): "Willkommen bei Shark Engine – baue Fahrzeuge" + Zweck der Mod.
   - Popup 2 (nach Aktivierung): Radiobutton-Auswahl Fliegen/Schwimmen/Fahren (nur Fliegen aktiv, andere disabled).
   - Popup 3: Erklärung zum Bauen und zu Steuerungen, inklusive Fortschrittsanzeige für Bauziele.
   - Popup 4: "Dein Fahrzeug ist bereit" nach erfüllten Bedingungen.

2. **Builder-Regeln & Feedback**
   - Echtzeit-Feedback bei Blockplatzierung (positiver Sound/Highlight, negativer Effekt bei ungültiger Verbindung oder Weltkontakt).
   - Anforderungen: Mindestens 4 Blöcke an den horizontalen Nachbarn des Steuerrads + mindestens ein Thruster + keine Terrainkontakte.
   - UI zeigt erfüllte Anforderungen (Checkboxen, rot/grün).

3. **Flug-Gameplay**
   - Mounting setzt automatisch Third-Person-Kamera.
   - Physik inklusive 5 Beschleunigungsphasen, Höhenabbremsung, Fuel/Weight-Regeln.
   - HUD mit Fuel %, Geschwindigkeit, Gewichtswarnungen.
   - Thruster-Partikel/Sounds aktiv nur mit eingebauten Thrusters.

4. **Quality & Tooling**
   - Steuerrad droppt 2 Thruster beim Platzieren (Tutoriumshilfe).
   - Lokalisierte Texte für Popups, Warnungen, Feedback.
   - `./gradlew test` im CI sichert Funktionalität.

## Task Breakdown (Priorität → Komplexität)
1. **Neue Steuerrad-Assets (High → Med)**
   - Block- und Item-Modell mit eigenem Icon, Texturen, Creative-Tab-Highlight.

2. **Popup-System & Tutorialtexte (High → High)**
   - Framework für sequenzielle Popups (max. eins gleichzeitig).
   - Popup #1–#4 mit Buttons, Radiobuttons, disabled States.

3. **Builder Feedback-Loop (High → High)**
   - Struktur-Scan trackt horizontale Nachbarschaft, Thruster, Terrainkontakt.
   - Echtzeit-Highlights + Sounds für valide/ungültige Blöcke.
   - UI-Anzeige für Requirements und Fortschritt.

4. **Fahrzeugbereit & Launch Flow (High → Med)**
   - Popup "Fahrzeug bereit" startet Montage + Mounting.
   - Schließt andere Popups, orientiert Spieler mit Kameraswitch.

5. **Flight UX & HUD (Med → Med)**
   - Sicherstellen, dass HUD (Fuel/Speed/Weight) sichtbar und aktualisiert wird.
   - Third-Person Camera Handler aktiv beim Piloten.

6. **Localization & Messaging (Med → Low)**
   - Texte für Popups, Buttons, disabled Radiobuttons ("Bald verfügbar"), Warnmeldungen.

7. **Testing & CI (Med → Med)**
   - Unit-Tests: Builder-Regeln (4 Blöcke, Thruster) / Thruster-Requirement.
   - Integration-Test: gültiger vs. ungültiger Aufbau.
   - CI bereits aktiv (`ci.yml`), sicherstellen, dass Tests abgedeckt sind.

## Rollout
1. Assets + Popup-Grundlage.
2. Builder-Regeln mit Feedback.
3. Tutorial-Inhalte & Radiobutton-Auswahl.
4. Ready-Popup + Launch.
5. HUD/Flight-Polish.
6. Tests & Dokumentation.

Mit diesem MSP-1 ist die Mod "spielbar" im Sinne eines vollständigen Spielprinzips: geführtes Bauen, notwendige Komponentenvalidierung und funktionierende Luftfahrzeug-Steuerung.
