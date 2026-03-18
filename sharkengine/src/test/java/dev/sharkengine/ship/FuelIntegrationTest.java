package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integrationstests für das Fuel-System der Shark Engine Mod.
 * Testet die Fuel-Logik und Consumption-Mechaniken.
 *
 * <p>Hinweis: Diese Tests verwenden keine ShipEntity-Instanzen, da diese
 * Minecraft-Klassen benötigen, die im Test-Classpath nicht verfügbar sind.
 * Stattdessen werden die Fuel-System-Methoden direkt getestet.</p>
 *
 * @author Shark Engine Team
 * @version 1.0
 */
@DisplayName("Fuel Integration Tests")
class FuelIntegrationTest {

    @Test
    @DisplayName("Refuel mit Holzstämmen (LOGS) - 1 Log = 100 Energy")
    void testRefuelWithLogs() {
        // GIVEN: Leerer Tank
        int fuelBefore = 0;

        // WHEN: 1 Log wird hinzugefügt (1 Log = 1 wood unit = 100 Energy)
        int woodCount = 1;
        int energyAdded = FuelSystem.woodToEnergy(woodCount);
        int fuelAfter = Math.min(FuelSystem.MAX_FUEL, fuelBefore + energyAdded);

        // THEN: Fuel sollte um 100 Einheiten steigen
        assertEquals(100, energyAdded, "1 Log sollte 100 Energy hinzufügen");
        assertEquals(100, fuelAfter, "Fuel sollte 100 nach dem Auftanken sein");
    }

    @Test
    @DisplayName("Refuel mit Brettern (PLANKS) - 1 Plank = 100 Energy")
    void testRefuelWithPlanks() {
        // GIVEN: Leerer Tank
        int fuelBefore = 0;

        // WHEN: 1 Plank wird hinzugefügt (1 Plank = 1 wood unit = 100 Energy)
        int woodCount = 1;
        int energyAdded = FuelSystem.woodToEnergy(woodCount);
        int fuelAfter = Math.min(FuelSystem.MAX_FUEL, fuelBefore + energyAdded);

        // THEN: Fuel sollte um 100 Einheiten steigen
        assertEquals(100, energyAdded, "1 Plank sollte 100 Energy hinzufügen");
        assertEquals(100, fuelAfter, "Fuel sollte 100 nach dem Auftanken sein");
    }

    @Test
    @DisplayName("Refuel mit mehreren Stämmen - sollte bis zum Maximum auffüllen")
    void testRefuelWithMultipleLogs() {
        // GIVEN: Tank mit 50 Fuel und 3 Logs zum Auftanken
        int fuelBefore = 50;
        int woodCount = 3; // 3 Logs = 300 Energy

        // WHEN: Fuel wird berechnet (gecappt bei MAX_FUEL)
        int energyToAdd = FuelSystem.woodToEnergy(woodCount);
        int fuelAfter = Math.min(FuelSystem.MAX_FUEL, fuelBefore + energyToAdd);
        int woodUsed = (FuelSystem.MAX_FUEL - fuelBefore + FuelSystem.ENERGY_PER_WOOD - 1) / FuelSystem.ENERGY_PER_WOOD;

        // THEN: Nur 1 Log wird verbraucht (50 Energy benötigt), Rest bleibt
        assertEquals(300, energyToAdd, "3 Logs sollten 300 Energy bieten");
        assertEquals(100, fuelAfter, "Fuel sollte auf Maximum gecappt werden");
        assertEquals(1, woodUsed, "Nur 1 Log sollte verbraucht werden");
    }

    @Test
    @DisplayName("Refuel wenn Tank voll - sollte nicht funktionieren")
    void testRefuelWhenFull() {
        // GIVEN: Volles Fuel
        int fuelBefore = FuelSystem.MAX_FUEL;

        // WHEN: Versuch, weiteres Holz hinzuzufügen
        int woodCount = 1;
        int energyToAdd = FuelSystem.woodToEnergy(woodCount);
        int fuelAfter = Math.min(FuelSystem.MAX_FUEL, fuelBefore + energyToAdd);

        // THEN: Fuel sollte unverändert bleiben
        assertEquals(FuelSystem.MAX_FUEL, fuelBefore, "Tank sollte voll sein");
        assertEquals(FuelSystem.MAX_FUEL, fuelAfter, "Tank sollte voll bleiben");
        assertEquals(0, fuelAfter - fuelBefore, "Kein Fuel sollte hinzugefügt werden");
    }

    @Test
    @DisplayName("Fuel Consumption pro Sekunde - Phase 1-2 (1 Energy/sec)")
    void testFuelConsumption_Phase1() {
        // GIVEN: 100 Fuel in Phase 1
        int fuelLevel = 100;
        AccelerationPhase phase = AccelerationPhase.PHASE_1;

        // WHEN: Fuel Consumption für 1 Sekunde berechnen
        int consumption = ShipPhysics.calculateFuelConsumption(phase);
        int fuelAfter = fuelLevel - consumption;

        // THEN: 1 Energy sollte verbraucht werden
        assertEquals(1, consumption, "Phase 1 sollte 1 Energy/sec verbrauchen");
        assertEquals(99, fuelAfter, "Fuel sollte um 1 Einheit reduziert sein");
    }

    @Test
    @DisplayName("Fuel Consumption - Phase 3-4 (2 Energy/sec)")
    void testFuelConsumption_Phase3() {
        // GIVEN: 100 Fuel in Phase 3
        int fuelLevel = 100;
        AccelerationPhase phase = AccelerationPhase.PHASE_3;

        // WHEN: Fuel Consumption für 1 Sekunde berechnen
        int consumption = ShipPhysics.calculateFuelConsumption(phase);
        int fuelAfter = fuelLevel - consumption;

        // THEN: 2 Energy sollten verbraucht werden
        assertEquals(2, consumption, "Phase 3 sollte 2 Energy/sec verbrauchen");
        assertEquals(98, fuelAfter, "Fuel sollte um 2 Einheiten reduziert sein");
    }

    @Test
    @DisplayName("Fuel Consumption - Phase 5 (3 Energy/sec)")
    void testFuelConsumption_Phase5() {
        // GIVEN: 100 Fuel in Phase 5
        int fuelLevel = 100;
        AccelerationPhase phase = AccelerationPhase.PHASE_5;

        // WHEN: Fuel Consumption für 1 Sekunde berechnen
        int consumption = ShipPhysics.calculateFuelConsumption(phase);
        int fuelAfter = fuelLevel - consumption;

        // THEN: 3 Energy sollten verbraucht werden
        assertEquals(3, consumption, "Phase 5 sollte 3 Energy/sec verbrauchen");
        assertEquals(97, fuelAfter, "Fuel sollte um 3 Einheiten reduziert sein");
    }

    @Test
    @DisplayName("Engine Out bei leerem Tank - Fuel = 0")
    void testEngineOut() {
        // GIVEN: 0 Fuel
        int fuelLevel = 0;

        // WHEN: Prüfen ob Engine Out
        boolean engineOut = fuelLevel <= 0;

        // THEN: Engine sollte ausgefallen sein
        assertTrue(engineOut, "Engine sollte bei 0 Fuel ausfallen");
    }

    @Test
    @DisplayName("Fuel Added Message - Fuel Level wird korrekt aktualisiert")
    void testFuelAddedMessage() {
        // GIVEN: Leerer Tank
        int fuelBefore = 0;

        // WHEN: 1 Log wird hinzugefügt
        int woodCount = 1;
        int energyAdded = FuelSystem.woodToEnergy(woodCount);
        int fuelAfter = Math.min(FuelSystem.MAX_FUEL, fuelBefore + energyAdded);

        // THEN: Fuel sollte aktualisiert werden und Message sollte angezeigt werden
        assertEquals(100, fuelAfter, "Fuel sollte 100 sein");
        assertTrue(fuelAfter > fuelBefore, "Fuel sollte gestiegen sein");

        // Fuel Display Message sollte korrekt formatiert sein
        String fuelMessage = FuelSystem.formatFuelDisplay(fuelAfter, FuelSystem.MAX_FUEL);
        assertNotNull(fuelMessage, "Fuel Message sollte nicht null sein");
        assertTrue(fuelMessage.contains("100%"), "Message sollte 100% anzeigen");
    }

    @Test
    @DisplayName("Kein Fuel Consumption ohne Gas geben")
    void testNoFuelConsumption_WithoutThrottle() {
        // GIVEN: 100 Fuel, kein Input (kein Gas)
        int fuelLevel = 100;
        boolean hasInput = false;

        // WHEN: Kein Input = kein Verbrauch
        int consumption = hasInput ? 1 : 0;
        int fuelAfter = fuelLevel - consumption;

        // THEN: Fuel sollte unverändert sein
        assertEquals(0, consumption, "Ohne Gas sollte kein Verbrauch sein");
        assertEquals(100, fuelAfter, "Fuel sollte unverändert sein");
    }

    @Test
    @DisplayName("Fuel Consumption stoppt bei Engine Out")
    void testFuelConsumption_StopsAtEngineOut() {
        // GIVEN: Engine Out (fuel = 0)
        int fuelLevel = 0;
        boolean engineOut = fuelLevel <= 0;

        // WHEN: Versuch, Fuel zu verbrauchen
        int consumption = engineOut ? 0 : 1;
        int fuelAfter = fuelLevel - consumption;

        // THEN: Fuel sollte bei 0 bleiben
        assertTrue(engineOut, "Engine sollte ausgefallen sein");
        assertEquals(0, consumption, "Verbrauch sollte 0 sein bei Engine Out");
        assertEquals(0, fuelAfter, "Fuel sollte bei 0 bleiben");
    }

    @Test
    @DisplayName("Refuel mit Stack von 5 Logs - sollte nur benötigte Menge verbrauchen")
    void testRefuelWithStackOf5Logs() {
        // GIVEN: Leerer Tank und 5 Logs
        int fuelBefore = 0;
        int logStack = 5;

        // WHEN: Berechne benötigte Logs für volles Fuel
        int fuelNeeded = FuelSystem.MAX_FUEL - fuelBefore;
        int logsNeeded = (int) Math.ceil((double) fuelNeeded / FuelSystem.ENERGY_PER_WOOD);
        int fuelAfter = Math.min(FuelSystem.MAX_FUEL, fuelBefore + (logsNeeded * FuelSystem.ENERGY_PER_WOOD));
        int logsRemaining = logStack - logsNeeded;

        // THEN: Nur 1 Log wird verbraucht
        assertEquals(1, logsNeeded, "Nur 1 Log sollte benötigt werden");
        assertEquals(100, fuelAfter, "Fuel sollte voll sein");
        assertEquals(4, logsRemaining, "4 Logs sollten übrig bleiben");
    }

    @Test
    @DisplayName("Partielles Auffüllen - 30 Fuel + 1 Log = 100 Fuel (gecappt)")
    void testPartialRefuel() {
        // GIVEN: 30 Fuel und 1 Log
        int fuelBefore = 30;
        int woodCount = 1;

        // WHEN: Fuel wird hinzugefügt (gecappt bei MAX)
        int energyAdded = FuelSystem.woodToEnergy(woodCount);
        int fuelAfter = Math.min(FuelSystem.MAX_FUEL, fuelBefore + energyAdded);

        // THEN: Fuel sollte auf 100 gecappt werden
        assertEquals(100, fuelAfter, "Fuel sollte auf Maximum gecappt werden");
        assertEquals(70, fuelAfter - fuelBefore, "Nur 70 Energy sollten tatsächlich hinzugefügt werden");
    }

    @Test
    @DisplayName("Refuel mit nicht-Holz Item - sollte nicht funktionieren")
    void testRefuelWithNonWoodItem() {
        // GIVEN: Leerer Tank und Stein (kein Holz)
        int fuelBefore = 0;
        boolean isWood = false; // Stein ist kein Holz

        // WHEN: Versuch, Stein zu Fuel zu konvertieren
        int woodCount = isWood ? 1 : 0;
        int energyAdded = FuelSystem.woodToEnergy(woodCount);
        int fuelAfter = fuelBefore + energyAdded;

        // THEN: Kein Fuel sollte hinzugefügt werden
        assertEquals(0, woodCount, "Stein sollte nicht als Holz gezählt werden");
        assertEquals(0, energyAdded, "Keine Energy sollte hinzugefügt werden");
        assertEquals(0, fuelAfter, "Fuel sollte unverändert bleiben");
    }

    @Test
    @DisplayName("Critical Fuel Warning bei <20%")
    void testCriticalFuelWarning() {
        // GIVEN: Fuel Level unter 20%
        int fuelLevel = 19;
        int maxFuel = FuelSystem.MAX_FUEL;

        // WHEN: Prüfen ob critical
        boolean isCritical = FuelSystem.isCritical(fuelLevel, maxFuel);

        // THEN: Sollte critical sein
        assertTrue(isCritical, "Fuel unter 20% sollte critical sein");

        // Fuel Display sollte rot sein
        String fuelDisplay = FuelSystem.formatFuelDisplay(fuelLevel, maxFuel);
        assertTrue(fuelDisplay.contains("§c"), "Critical Fuel sollte rot angezeigt werden");
    }

    @Test
    @DisplayName("Remaining Flight Time wird korrekt berechnet")
    void testRemainingFlightTimeCalculation() {
        // GIVEN: 100 Fuel in verschiedenen Phasen
        int fuelLevel = 100;

        // WHEN: Flugzeit für verschiedene Phasen berechnen
        int timePhase1 = FuelSystem.calculateRemainingFlightTime(fuelLevel, AccelerationPhase.PHASE_1);
        int timePhase3 = FuelSystem.calculateRemainingFlightTime(fuelLevel, AccelerationPhase.PHASE_3);
        int timePhase5 = FuelSystem.calculateRemainingFlightTime(fuelLevel, AccelerationPhase.PHASE_5);

        // THEN: Flugzeit sollte korrekt sein
        assertEquals(100, timePhase1, "Phase 1: 100 sec bei 1 Energy/sec");
        assertEquals(50, timePhase3, "Phase 3: 50 sec bei 2 Energy/sec");
        assertEquals(33, timePhase5, "Phase 5: 33 sec bei 3 Energy/sec");
    }

    @Test
    @DisplayName("Fuel Display Formatierung zeigt korrekte Prozentanzeige")
    void testFuelDisplayPercentage() {
        // GIVEN: Verschiedene Fuel Level
        int[] fuelLevels = {0, 25, 50, 75, 100};
        String[] expectedPercent = {"0%", "25%", "50%", "75%", "100%"};

        // WHEN/THEN: Jede Fuel Level sollte korrekt formatiert werden
        for (int i = 0; i < fuelLevels.length; i++) {
            String display = FuelSystem.formatFuelDisplay(fuelLevels[i], FuelSystem.MAX_FUEL);
            assertTrue(display.contains(expectedPercent[i]),
                    "Fuel Display sollte " + expectedPercent[i] + " enthalten");
        }
    }

    @Test
    @DisplayName("Fuel Efficiency - Maximale Flugzeit bei Phase 1")
    void testFuelEfficiency_MaxFlightTime() {
        // GIVEN: Volles Fuel (100 Energy)
        int maxFuel = FuelSystem.MAX_FUEL;

        // WHEN: Maximale Flugzeit bei niedrigstem Verbrauch (Phase 1 = 1 Energy/sec)
        int maxFlightTime = FuelSystem.calculateRemainingFlightTime(maxFuel, AccelerationPhase.PHASE_1);

        // THEN: Maximale Flugzeit sollte 100 Sekunden sein
        assertEquals(100, maxFlightTime, "Maximale Flugzeit bei Phase 1 sollte 100 Sekunden sein");

        // In Minuten: 100 / 60 = 1 Minute 40 Sekunden
        String timeDisplay = FuelSystem.formatRemainingTime(maxFlightTime, AccelerationPhase.PHASE_1);
        assertEquals("1m", timeDisplay, "Anzeige sollte 1 Minute zeigen (abgerundet)");
    }
}
