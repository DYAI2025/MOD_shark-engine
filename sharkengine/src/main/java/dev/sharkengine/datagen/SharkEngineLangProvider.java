package dev.sharkengine.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

/**
 * AIR-030: {@code assets/sharkengine/lang/en_us.json} and {@code de_de.json}.
 *
 * <p>{@link FabricLanguageProvider} generates one monolithic lang file per
 * language, so unlike the other AIR-030 providers this one reproduces every
 * existing key (not just the thruster/steering_wheel/bug ones) — the
 * hand-written files are being fully retired, and MC only reads a single
 * lang file per language code. Key/value pairs below were generated
 * mechanically from the current {@code en_us.json}/{@code de_de.json}
 * (via a throwaway script reading both with a JSON parser) rather than
 * retyped by hand, to remove transcription risk across ~70 strings.
 */
final class SharkEngineLangProvider {

    private SharkEngineLangProvider() {}

    static final class English extends FabricLanguageProvider {
        English(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
            super(dataOutput, registryLookup);
        }

        @Override
        public void generateTranslations(HolderLookup.Provider registryLookup, TranslationBuilder b) {
            b.add("block.sharkengine.steering_wheel", "Steering Wheel");
            b.add("block.sharkengine.thruster", "Thruster");
            b.add("entity.sharkengine.ship", "Ship");
            b.add("message.sharkengine.assembly_ok", "Ship assembled (%s blocks).");
            b.add("message.sharkengine.assembly_fail_contact", "Assembly blocked: world contact at %s points.");
            b.add("message.sharkengine.assembly_fail_invalid", "Remove highlighted blocks (%s invalid parts).");
            b.add("message.sharkengine.assembly_fail_thruster", "No thrusters detected – add at least one to launch.");
            b.add("message.sharkengine.assembly_fail_core", "Connect four blocks around the wheel (%s/4).");
            b.add("message.sharkengine.assembly_fail_empty", "Nothing to assemble (check eligible tag).");
            b.add("message.sharkengine.anchor_on", "Anchor: ON");
            b.add("message.sharkengine.anchor_off", "Anchor: OFF");
            b.add("message.sharkengine.disassembly_ok", "Ship disassembled (%s blocks placed).");
            b.add("message.sharkengine.disassembly_partial", "Ship disassembled partially (%s blocks placed, some obstructed).");
            b.add("message.sharkengine.disassembly_fail_no_blueprint", "Cannot disassemble: no blueprint data.");
            b.add("message.sharkengine.builder_open", "Builder mode ready. Review highlighted blocks before launch.");
            b.add("message.sharkengine.mode_locked", "%s mode will be available soon.");
            b.add("message.sharkengine.builder_invalid", "Builder blocked by %s invalid attachments.");
            b.add("message.sharkengine.builder_contacts", "Builder blocked by %s world contacts.");
            b.add("screen.sharkengine.builder.title", "Airship Builder");
            b.add("screen.sharkengine.builder.blocks", "%s connected blocks");
            b.add("screen.sharkengine.builder.invalid", "%s invalid attachments");
            b.add("screen.sharkengine.builder.contacts", "%s terrain contacts");
            b.add("screen.sharkengine.builder.thrusters", "%s thrusters installed");
            b.add("screen.sharkengine.builder.edges", "%s of 4 core blocks attached");
            b.add("screen.sharkengine.builder.instructions", "Place eligible blocks and thrusters. Red highlights must be removed before launch.");
            b.add("screen.sharkengine.builder.action", "Assemble & Launch");
            b.add("screen.sharkengine.builder.action_disabled", "Resolve highlights to enable launch");
            b.add("screen.sharkengine.tutorial.welcome.title", "Welcome to Shark Engine");
            b.add("screen.sharkengine.tutorial.welcome.body", "Use this steering wheel to build powerful vehicles.");
            b.add("screen.sharkengine.tutorial.mode_selection.title", "Choose your vehicle type");
            b.add("screen.sharkengine.tutorial.mode_selection.body", "Pick how you'd like to travel. More modes arrive soon.");
            b.add("screen.sharkengine.tutorial.button.continue", "Continue");
            b.add("screen.sharkengine.tutorial.button.launch", "Launch");
            b.add("screen.sharkengine.tutorial.button.confirm", "Confirm");
            b.add("screen.sharkengine.tutorial.mode.air", "Fly");
            b.add("screen.sharkengine.tutorial.mode.water", "Swim");
            b.add("screen.sharkengine.tutorial.mode.land", "Drive");
            b.add("screen.sharkengine.tutorial.mode.disabled_note", "Other modes unlock in future updates.");
            b.add("screen.sharkengine.tutorial.build_guide.title", "Build Mode");
            b.add("screen.sharkengine.tutorial.build_guide.body", "Attach blocks to each side of the wheel, add a thruster, and place the bow (front marker) at the front.");
            b.add("screen.sharkengine.tutorial.ready.title", "Vehicle Ready");
            b.add("screen.sharkengine.tutorial.ready.body", "All requirements met. Press Launch to take off!");
            b.add("screen.sharkengine.tutorial.flight_tips.title", "Flight Controls");
            b.add("screen.sharkengine.tutorial.flight_tips.body", "WASD to steer, Space to rise, Shift to descend. Monitor fuel and weight warnings.");
            b.add("block.sharkengine.bug", "Bow (Front Marker)");
            b.add("item.sharkengine.bug", "Bow (Front Marker)");
            b.add("item.sharkengine.thruster", "Thruster");
            b.add("message.sharkengine.assembly_fail_no_bug", "No bow block found – place exactly one bow as front marker.");
            b.add("message.sharkengine.assembly_fail_multi_bug", "Configuration error: Multiple bow blocks found (%s). Only one allowed.");
            b.add("message.sharkengine.assembly_fail_bug_inside", "Bow must be on the outer edge – currently placed inside.");
            b.add("screen.sharkengine.builder.bugs", "%s bow block(s) found");
            b.add("message.sharkengine.no_fuel", "⚠ Engine out! Ship is falling – land or refuel.");
            b.add("message.sharkengine.too_heavy", "⚠ Ship too heavy to fly (61+ blocks)!");
            b.add("message.sharkengine.fuel_added", "Fuel refilled: %s");
            b.add("hud.sharkengine.onboarding.title", "Shark Engine V4 · Quick Flight Guide");
            b.add("hud.sharkengine.onboarding.movement", "Movement: W = forward, A/D = turn. Build momentum before climbing.");
            b.add("hud.sharkengine.onboarding.vertical", "Vertical: Space rises, Shift descends. Heavy ships climb slower.");
            b.add("hud.sharkengine.onboarding.fuel", "Fuel: Green = safe, Yellow = low, Red = critical. Refuel before long flights.");
            b.add("hud.sharkengine.onboarding.dismiss", "Press X to dismiss this card.");
            b.add("message.sharkengine.not_pilot", "You are not the pilot of this ship.");
            b.add("screen.sharkengine.builder.issues_header", "Blocking issues:");
            b.add("assembly_issue.sharkengine.empty_structure", "Nothing to assemble (check eligible tag).");
            b.add("assembly_issue.sharkengine.invalid_attachments", "Remove highlighted blocks (%s invalid parts).");
            b.add("assembly_issue.sharkengine.terrain_contact", "Assembly blocked: world contact at %s points.");
            b.add("assembly_issue.sharkengine.no_propulsion", "No thrusters detected – add at least one to launch.");
            b.add("assembly_issue.sharkengine.too_few_core_neighbors", "Connect four blocks around the wheel (%s/4).");
            b.add("assembly_issue.sharkengine.no_bug", "No bow block found – place exactly one bow as front marker.");
            b.add("assembly_issue.sharkengine.multi_bug", "Configuration error: Multiple bow blocks found (%s). Only one allowed.");
            b.add("assembly_issue.sharkengine.bug_inside", "Bow must be on the outer edge – currently placed inside.");
            // AIR-040: crafting-intermediate items
            b.add("item.sharkengine.metal_sheet", "Metal Sheet");
            b.add("item.sharkengine.rotor_shaft", "Rotor Shaft");
            b.add("item.sharkengine.engine_core", "Engine Core");
            b.add("item.sharkengine.bearing_assembly", "Bearing Assembly");
            // AIR-040: airframe_panel (first core placeable part)
            b.add("block.sharkengine.airframe_panel", "Airframe Panel");
            b.add("item.sharkengine.airframe_panel", "Airframe Panel");
            // AIR-040: fuselage_frame (second core placeable part)
            b.add("block.sharkengine.fuselage_frame", "Fuselage Frame");
            b.add("item.sharkengine.fuselage_frame", "Fuselage Frame");
            // AIR-040: helicopter_engine (fourth core placeable part)
            b.add("block.sharkengine.helicopter_engine", "Helicopter Engine");
            b.add("item.sharkengine.helicopter_engine", "Helicopter Engine");
            // AIR-040: rotor_hub (fifth core placeable part)
            b.add("block.sharkengine.rotor_hub", "Rotor Hub");
            b.add("item.sharkengine.rotor_hub", "Rotor Hub");
            // AIR-040: rotor_blade (sixth core placeable part)
            b.add("block.sharkengine.rotor_blade", "Rotor Blade");
            b.add("item.sharkengine.rotor_blade", "Rotor Blade");
            // AIR-040: landing_skid (seventh and last core placeable part)
            b.add("block.sharkengine.landing_skid", "Landing Skid");
            b.add("item.sharkengine.landing_skid", "Landing Skid");
            // 2026-07-13: dedicated descend key (was conflicting with vanilla's
            // sneak-to-dismount when reusing the sneak key, see ShipKeyBindings)
            b.add("key.categories.sharkengine", "Shark Engine");
            b.add("key.sharkengine.descend", "Descend");
        }
    }

    static final class German extends FabricLanguageProvider {
        German(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
            super(dataOutput, "de_de", registryLookup);
        }

        @Override
        public void generateTranslations(HolderLookup.Provider registryLookup, TranslationBuilder b) {
            b.add("block.sharkengine.steering_wheel", "Steuerrad");
            b.add("block.sharkengine.thruster", "Triebwerk");
            b.add("entity.sharkengine.ship", "Schiff");
            b.add("message.sharkengine.assembly_ok", "Schiff zusammengebaut (%s Blöcke).");
            b.add("message.sharkengine.assembly_fail_contact", "Montage blockiert: Bodenkontakt an %s Punkten.");
            b.add("message.sharkengine.assembly_fail_invalid", "Markierte Blöcke entfernen (%s ungültige Teile).");
            b.add("message.sharkengine.assembly_fail_thruster", "Kein Triebwerk erkannt – mindestens eines anbringen.");
            b.add("message.sharkengine.assembly_fail_core", "Vier Blöcke um das Steuerrad anbauen (%s/4).");
            b.add("message.sharkengine.assembly_fail_empty", "Nichts zu montieren (prüfe erlaubte Blöcke).");
            b.add("message.sharkengine.anchor_on", "Anker: EIN");
            b.add("message.sharkengine.anchor_off", "Anker: AUS");
            b.add("message.sharkengine.disassembly_ok", "Schiff zerlegt (%s Blöcke platziert).");
            b.add("message.sharkengine.disassembly_partial", "Schiff teilweise zerlegt (%s Blöcke platziert, einige blockiert).");
            b.add("message.sharkengine.disassembly_fail_no_blueprint", "Kann nicht zerlegen: keine Bauplan-Daten.");
            b.add("message.sharkengine.builder_open", "Baumodus bereit. Prüfe markierte Blöcke vor dem Start.");
            b.add("message.sharkengine.mode_locked", "%s-Modus wird bald verfügbar sein.");
            b.add("message.sharkengine.builder_invalid", "Bau blockiert durch %s ungültige Anbauteile.");
            b.add("message.sharkengine.builder_contacts", "Bau blockiert durch %s Bodenkontakte.");
            b.add("screen.sharkengine.builder.title", "Luftschiff-Baumeister");
            b.add("screen.sharkengine.builder.blocks", "%s verbundene Blöcke");
            b.add("screen.sharkengine.builder.invalid", "%s ungültige Anbauteile");
            b.add("screen.sharkengine.builder.contacts", "%s Geländekontakte");
            b.add("screen.sharkengine.builder.thrusters", "%s Triebwerke installiert");
            b.add("screen.sharkengine.builder.edges", "%s von 4 Kernblöcken angebaut");
            b.add("screen.sharkengine.builder.instructions", "Platziere erlaubte Blöcke und Triebwerke. Rot markierte Teile müssen entfernt werden.");
            b.add("screen.sharkengine.builder.action", "Zusammenbauen & Starten");
            b.add("screen.sharkengine.builder.action_disabled", "Markierungen beheben zum Starten");
            b.add("screen.sharkengine.tutorial.welcome.title", "Willkommen bei Shark Engine");
            b.add("screen.sharkengine.tutorial.welcome.body", "Nutze dieses Steuerrad, um mächtige Fahrzeuge zu bauen.");
            b.add("screen.sharkengine.tutorial.mode_selection.title", "Wähle deinen Fahrzeugtyp");
            b.add("screen.sharkengine.tutorial.mode_selection.body", "Wähle, wie du reisen möchtest. Weitere Modi folgen bald.");
            b.add("screen.sharkengine.tutorial.button.continue", "Weiter");
            b.add("screen.sharkengine.tutorial.button.launch", "Starten");
            b.add("screen.sharkengine.tutorial.button.confirm", "Bestätigen");
            b.add("screen.sharkengine.tutorial.mode.air", "Fliegen");
            b.add("screen.sharkengine.tutorial.mode.water", "Schwimmen");
            b.add("screen.sharkengine.tutorial.mode.land", "Fahren");
            b.add("screen.sharkengine.tutorial.mode.disabled_note", "Weitere Modi werden in zukünftigen Updates freigeschaltet.");
            b.add("screen.sharkengine.tutorial.build_guide.title", "Baumodus");
            b.add("screen.sharkengine.tutorial.build_guide.body", "Baue Blöcke an jede Seite des Steuerrads an, füge ein Triebwerk hinzu und platziere den Bug (Frontmarker) an der Vorderseite.");
            b.add("screen.sharkengine.tutorial.ready.title", "Fahrzeug bereit");
            b.add("screen.sharkengine.tutorial.ready.body", "Alle Anforderungen erfüllt. Drücke Starten zum Abheben!");
            b.add("screen.sharkengine.tutorial.flight_tips.title", "Flugsteuerung");
            b.add("screen.sharkengine.tutorial.flight_tips.body", "WASD zum Lenken, Leertaste zum Steigen, Shift zum Sinken. Achte auf Treibstoff- und Gewichtswarnungen.");
            b.add("block.sharkengine.bug", "Bug (Frontmarker)");
            b.add("item.sharkengine.bug", "Bug (Frontmarker)");
            b.add("item.sharkengine.thruster", "Triebwerk");
            b.add("message.sharkengine.assembly_fail_no_bug", "Kein Bug-Block gefunden – platziere genau einen Bug als Frontmarker.");
            b.add("message.sharkengine.assembly_fail_multi_bug", "Konfigurationsfehler: Mehrere Bug-Blöcke gefunden (%s). Nur genau einer erlaubt.");
            b.add("message.sharkengine.assembly_fail_bug_inside", "Bug muss an der Außenkante platziert werden – aktuell im Inneren.");
            b.add("screen.sharkengine.builder.bugs", "%s Bug-Block(s) gefunden");
            b.add("message.sharkengine.no_fuel", "⚠ Triebwerk ausgefallen! Schiff sinkt – landen oder auftanken.");
            b.add("message.sharkengine.too_heavy", "⚠ Schiff zu schwer zum Fliegen (61+ Blöcke)!");
            b.add("message.sharkengine.fuel_added", "Treibstoff aufgefüllt: %s");
            b.add("hud.sharkengine.onboarding.title", "Shark Engine V4 · Flug-Kurzanleitung");
            b.add("hud.sharkengine.onboarding.movement", "Bewegung: W = vorwärts, A/D = drehen. Erst Schub aufbauen, dann steigen.");
            b.add("hud.sharkengine.onboarding.vertical", "Vertikal: Leertaste steigt, Shift sinkt. Schwere Schiffe steigen langsamer.");
            b.add("hud.sharkengine.onboarding.fuel", "Treibstoff: Grün = sicher, Gelb = niedrig, Rot = kritisch. Vor langen Flügen auftanken.");
            b.add("hud.sharkengine.onboarding.dismiss", "Drücke X, um diese Karte auszublenden.");
            b.add("message.sharkengine.not_pilot", "Du bist nicht der Pilot dieses Schiffs.");
            b.add("screen.sharkengine.builder.issues_header", "Blockierende Probleme:");
            b.add("assembly_issue.sharkengine.empty_structure", "Nichts zu montieren (prüfe erlaubte Blöcke).");
            b.add("assembly_issue.sharkengine.invalid_attachments", "Markierte Blöcke entfernen (%s ungültige Teile).");
            b.add("assembly_issue.sharkengine.terrain_contact", "Montage blockiert: Bodenkontakt an %s Punkten.");
            b.add("assembly_issue.sharkengine.no_propulsion", "Kein Triebwerk erkannt – mindestens eines anbringen.");
            b.add("assembly_issue.sharkengine.too_few_core_neighbors", "Vier Blöcke um das Steuerrad anbauen (%s/4).");
            b.add("assembly_issue.sharkengine.no_bug", "Kein Bug-Block gefunden – platziere genau einen Bug als Frontmarker.");
            b.add("assembly_issue.sharkengine.multi_bug", "Konfigurationsfehler: Mehrere Bug-Blöcke gefunden (%s). Nur genau einer erlaubt.");
            b.add("assembly_issue.sharkengine.bug_inside", "Bug muss an der Außenkante platziert werden – aktuell im Inneren.");
            // AIR-040: crafting-intermediate items
            b.add("item.sharkengine.metal_sheet", "Blechplatte");
            b.add("item.sharkengine.rotor_shaft", "Rotorwelle");
            b.add("item.sharkengine.engine_core", "Triebwerkskern");
            b.add("item.sharkengine.bearing_assembly", "Lagerbaugruppe");
            // AIR-040: airframe_panel (first core placeable part)
            b.add("block.sharkengine.airframe_panel", "Außenhüllenplatte");
            b.add("item.sharkengine.airframe_panel", "Außenhüllenplatte");
            // AIR-040: fuselage_frame (second core placeable part)
            b.add("block.sharkengine.fuselage_frame", "Rumpfrahmen");
            b.add("item.sharkengine.fuselage_frame", "Rumpfrahmen");
            // AIR-040: helicopter_engine (fourth core placeable part)
            b.add("block.sharkengine.helicopter_engine", "Hubschraubermotor");
            b.add("item.sharkengine.helicopter_engine", "Hubschraubermotor");
            // AIR-040: rotor_hub (fifth core placeable part)
            b.add("block.sharkengine.rotor_hub", "Rotor-Hub");
            b.add("item.sharkengine.rotor_hub", "Rotor-Hub");
            // AIR-040: rotor_blade (sixth core placeable part)
            b.add("block.sharkengine.rotor_blade", "Rotorblatt");
            b.add("item.sharkengine.rotor_blade", "Rotorblatt");
            // AIR-040: landing_skid (seventh and last core placeable part)
            b.add("block.sharkengine.landing_skid", "Landekufe");
            b.add("item.sharkengine.landing_skid", "Landekufe");
            // 2026-07-13: dedicated descend key (was conflicting with vanilla's
            // sneak-to-dismount when reusing the sneak key, see ShipKeyBindings)
            b.add("key.categories.sharkengine", "Shark Engine");
            b.add("key.sharkengine.descend", "Sinkflug");
        }
    }
}
