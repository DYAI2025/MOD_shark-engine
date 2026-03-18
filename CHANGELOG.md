# Changelog

Alle wesentlichen Änderungen an Shark Engine werden in dieser Datei dokumentiert.

## [0.1.0] - 11. März 2026

### ✨ Neue Features
- **Controller-Support**
  - Xbox- und PlayStation-Controller werden automatisch erkannt
  - Konfigurierbare Deadzone und Inversion
  - HUD-Anzeige für Controller-Verbindung
  - Chat-Nachrichten bei Connect/Disconnect

- **Fuel-System**
  - Auftanken durch Rechtsklick mit Holzstämmen/Brettern
  - Fuel-Verbrauch je nach Beschleunigungsphase
  - Engine-Out Verhalten bei leerem Tank
  - HUD-Anzeige für Fuel-Level

- **Disassemble-Verbesserung**
  - Blöcke werden ins Inventar zurückgegeben
  - Bei vollem Inventar: Drop als Item-Entity
  - Verbesserte Nachrichten (Platziert / Inventar)

### 🔧 Verbesserungen
- **Debug-Logs für Assembly**
  - Jeder Validierungsschritt wird geloggt
  - Klare Erfolgs- und Fehlermeldungen
  - Einfacheres Debugging von Struktur-Problemen

- **Tests**
  - 76 automatische JUnit-Tests
  - Assembly-Tests (36)
  - Fuel-Tests (40)
  - Integration-Tests (20)

### 📖 Dokumentation
- Vollständige README.md
- Steuerungsanleitung (Tastatur + Controller)
- Bau-Anleitung für Schiffe
- Konfigurations-Optionen
- Quick-Start Guide

### 🐛 Bug Fixes
- Assembly-Loop behoben (durch Debug-Logs identifizierbar)
- Fuel-Refill funktioniert jetzt korrekt
- Disassemble gibt Blöcke korrekt zurück

---

## [0.0.1] - Initiale Version

### ✨ Features
- Steuerrad-basiertes Schiffssystem
- Automatischer Struktur-Scan (BFS)
- Validierung (Kernblöcke, Bug, Thruster, Bodenkontakt)
- Physik-basierte Flugmechanik
- 5 Beschleunigungsphasen
- Gewichtsklassen (LIGHT, MEDIUM, HEAVY, OVERLOADED)
- Fuel-System (Basis)
- HUD-Anzeige (Fuel, Speed, HP)
- Tutorial-System
- Builder-Modus mit Highlights

### 🔧 Technik
- Fabric Mod für Minecraft 1.21.1
- Java 21
- JUnit-Tests (Basis)

---

## Versionierung

Wir verwenden [Semantic Versioning](https://semver.org/):
- **MAJOR** – Inkompatible Änderungen
- **MINOR** – Neue Features (abwärtskompatibel)
- **PATCH** – Bugfixes (abwärtskompatibel)

Aktuelle Version: **0.1.0** (Beta)

---

## Upcoming (v0.2.0)

Geplante Features für die nächste Version:
- [ ] Schwimmen-Modus (Wasser-Fahrzeuge)
- [ ] Fahren-Modus (Land-Fahrzeuge)
- [ ] Mehrere Spieler auf einem Schiff
- [ ] Schiff-Werkbank (Blaupausen speichern)
- [ ] Automatische Reparatur
- [ ] Mehr Triebwerk-Typen
- [ ] Schiff-Upgrade-System

---

## Links

- [GitHub Repository](https://github.com/your-repo/shark-engine)
- [Issue Tracker](https://github.com/your-repo/shark-engine/issues)
- [Dokumentation](README.md)
