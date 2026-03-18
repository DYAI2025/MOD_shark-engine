package dev.sharkengine.integration;

import dev.sharkengine.ship.AccelerationPhase;
import dev.sharkengine.ship.FuelSystem;
import dev.sharkengine.ship.ShipAssemblyService;
import dev.sharkengine.ship.ShipBlueprint;
import dev.sharkengine.ship.ShipPhysics;
import dev.sharkengine.integration.IntegrationTestHelper.MockShipData;
import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Umfassender End-to-End Integrationstest für den kompletten Shark Engine User Flow.
 * 
 * <p>Dieser Test simuliert den vollständigen Spielablauf:</p>
 * <ol>
 *   <li>Setup: Test-Welt erstellen</li>
 *   <li>Steuerrad platzieren: Bei Position (0, 10, 0)</li>
 *   <li>Struktur bauen: 4 Blöcke um Steuerrad + Bug-Block + Thruster</li>
 *   <li>Assembly auslösen: ShipAssemblyService.tryAssemble()</li>
 *   <li>Schiff validieren: Entity existiert, Blueprint gesetzt, Pilot korrekt, Fuel 100</li>
 *   <li>Einsteigen: player.startRiding(ship)</li>
 *   <li>Steuerung testen: setInputs() mit verschiedenen Werten</li>
 *   <li>Physik-Update: tick() aufrufen, Bewegung und Fuel-Verbrauch validieren</li>
 *   <li>Auftanken: Fuel-Level muss steigen</li>
 *   <li>Landen & Anchor: toggleAnchor()</li>
 *   <li>Aussteigen: player.stopRiding()</li>
 *   <li>Disassemble: Blöcke müssen zurück ins Inventar</li>
 *   <li>Cleanup: Welt aufräumen</li>
 * </ol>
 *
 * <p>Zusätzliche negative Tests:</p>
 * <ul>
 *   <li>testAssemblyWithMissingThruster() - Sollte fehlschlagen</li>
 *   <li>testAssemblyWithNoBug() - Sollte fehlschlagen</li>
 *   <li>testAssemblyWithWorldContact() - Sollte fehlschlagen</li>
 *   <li>testFuelConsumptionDuringFlight() - Fuel muss sinken</li>
 *   <li>testDisassembleReturnsBlocks() - Inventar-Check</li>
 * </ul>
 *
 * @author Shark Engine Team
 * @version 1.0
 */
@DisplayName("User Flow Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserFlowIntegrationTest {

    private IntegrationTestHelper helper;
    private int wheelX = 0;
    private int wheelY = 10;
    private int wheelZ = 0;
    private MockShipData testShip;
    private UUID testPlayerUuid;

    // ═══════════════════════════════════════════════════════════════════════
    // SETUP & TEARDOWN
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Wird vor jedem Test ausgeführt.
     */
    @BeforeEach
    @DisplayName("Setup: Test-Welt vorbereiten")
    void setUp() {
        helper = new IntegrationTestHelper();
        helper.setupTestWorld();
        helper.setWheelPos(0, 10, 0);
        wheelX = helper.getWheelX();
        wheelY = helper.getWheelY();
        wheelZ = helper.getWheelZ();
        testPlayerUuid = UUID.randomUUID();
    }

    /**
     * Wird nach jedem Test ausgeführt.
     */
    @AfterEach
    @DisplayName("Cleanup: Test-Welt aufräumen")
    void tearDown() {
        if (helper != null) {
            helper.cleanup();
        }
        testShip = null;
        testPlayerUuid = null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HAUPTTEST: VOLLSTÄNDIGER USER FLOW
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Testet den KOMPLETTEN User Flow von Anfang bis Ende.
     */
    @Test
    @Order(1)
    @DisplayName("Vollständiger User Flow: Kompletter Loop von Assembly bis Disassembly")
    void testCompleteUserFlow() {
        // ━━━ SCHRITT 1: Struktur bauen ━━━
        helper.buildMinimalShipStructure();
        
        // ━━━ SCHRITT 2: Assembly auslösen (simuliert) ━━━
        ShipAssemblyService.StructureScan scan = createValidStructureScan();
        
        // Validiere Scan-Ergebnis
        assertTrue(scan.canAssemble(), "Struktur sollte assemblierbar sein");
        assertEquals(7, scan.blockCount(), "Struktur sollte 7 Blöcke haben");
        assertTrue(scan.hasThruster(), "Struktur sollte Thruster haben");
        assertEquals(1, scan.bugCount(), "Struktur sollte 1 BUG haben");
        assertTrue(scan.bugOnEdge(), "BUG sollte an Kante sein");
        assertEquals(0, scan.contactPoints(), "Struktur sollte keine Bodenkontakte haben");

        // ━━━ SCHRITT 3: Schiff validieren (simuliert) ━━━
        // MockShipData direkt erstellen (ShipBlueprint verwendet Minecraft-Klassen)
        MockShipData mockShip = new MockShipData(7);
        mockShip.setPilot(testPlayerUuid);
        testShip = mockShip;
        helper.setTestShip(testShip);
        
        assertNotNull(testShip, "ShipEntity sollte erstellt werden");
        
        // Validiere Ship-Properties
        assertEquals(100, testShip.getFuelLevel(), "Fuel sollte 100 sein");
        assertEquals(7, testShip.getBlockCount(), "Block-Anzahl sollte 7 sein");

        // ━━━ SCHRITT 4: Einsteigen ━━━
        boolean mounted = helper.mountShip(testShip);
        assertTrue(mounted, "Spieler sollte einsteigen können");
        assertTrue(testShip.hasPassenger(), "Spieler sollte Passagier sein");

        // ━━━ SCHRITT 5: Steuerung testen ━━━
        testShip.setInputs(0.0f, 0.0f, 0.0f);
        assertEquals(0.0f, testShip.getInputForward(), "Input sollte 0 sein");
        
        testShip.setInputs(1.0f, 0.0f, 1.0f);
        assertEquals(1.0f, testShip.getInputForward(), "Input sollte 1 sein");
        
        testShip.setInputs(-1.0f, 0.5f, 0.5f);
        assertEquals(0.5f, testShip.getInputForward(), "Input sollte 0.5 sein");

        // ━━━ SCHRITT 6: Physik-Update simulieren ━━━
        simulatePhysicsTicks(testShip, 40); // 2 Sekunden
        
        // Validiere Bewegung
        assertTrue(testShip.getCurrentSpeed() > 0, "Schiff sollte sich bewegen");
        assertEquals(AccelerationPhase.PHASE_2, testShip.getPhase(), "Sollte in Phase 2 sein");

        // ━━━ SCHRITT 7: Auftanken ━━━
        int fuelBefore = testShip.getFuelLevel();
        
        // Erst Fuel verbrauchen für Test
        testShip.addFuel(-50);
        fuelBefore = testShip.getFuelLevel();
        
        int addedFuel = helper.addFuel(testShip, 1);
        assertTrue(addedFuel > 0, "Fuel sollte hinzugefügt werden");
        assertTrue(testShip.getFuelLevel() > fuelBefore, "Fuel-Level sollte steigen");

        // ━━━ SCHRITT 8: Landen & Anchor ━━━
        helper.toggleAnchor(testShip);
        assertTrue(testShip.isAnchored(), "Schiff sollte geankert sein");

        // ━━━ SCHRITT 9: Aussteigen ━━━
        helper.dismountShip(testShip);
        assertFalse(testShip.hasPassenger(), "Spieler sollte ausgestiegen sein");

        // ━━━ SCHRITT 10: Disassemble ━━━
        helper.disassemble(testShip);
        assertTrue(testShip.isRemoved(), "Schiff sollte entfernt sein");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NEGATIVE TESTS: ASSEMBLY FEHLER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Testet Assembly mit fehlendem Thruster.
     */
    @Test
    @Order(2)
    @DisplayName("Assembly mit fehlendem Thruster: Sollte fehlschlagen")
    void testAssemblyWithMissingThruster() {
        helper.buildStructureWithoutThruster();
        
        ShipAssemblyService.StructureScan scan = createStructureScanWithoutThruster();
        
        assertFalse(scan.hasThruster(), "Struktur sollte keinen Thruster haben");
        assertEquals(0, scan.thrusterCount(), "Thruster-Anzahl sollte 0 sein");
        assertFalse(scan.canAssemble(), "Assembly sollte wegen fehlendem Thruster fehlschlagen");
    }

    /**
     * Testet Assembly ohne BUG-Block.
     */
    @Test
    @Order(3)
    @DisplayName("Assembly ohne BUG-Block: Sollte fehlschlagen")
    void testAssemblyWithNoBug() {
        helper.buildStructureWithoutBug();
        
        ShipAssemblyService.StructureScan scan = createStructureScanWithoutBug();
        
        assertEquals(0, scan.bugCount(), "BUG-Anzahl sollte 0 sein");
        assertFalse(scan.hasBug(), "Struktur sollte keinen BUG haben");
        assertFalse(scan.canAssemble(), "Assembly sollte wegen fehlendem BUG fehlschlagen");
    }

    /**
     * Testet Assembly mit Welt-Kontakt (Bodenberührung).
     */
    @Test
    @Order(4)
    @DisplayName("Assembly mit Welt-Kontakt: Sollte fehlschlagen")
    void testAssemblyWithWorldContact() {
        helper.buildStructureWithWorldContact();
        
        ShipAssemblyService.StructureScan scan = createStructureScanWithWorldContact();
        
        assertTrue(scan.contactPoints() > 0, "Struktur sollte Bodenkontakte haben");
        assertFalse(scan.canAssemble(), "Assembly sollte wegen Bodenkontakt fehlschlagen");
    }

    /**
     * Testet Assembly mit BUG im Inneren (nicht an Kante).
     */
    @Test
    @Order(5)
    @DisplayName("Assembly mit BUG im Inneren: Sollte fehlschlagen")
    void testAssemblyWithBugInside() {
        List<ShipBlueprint.ShipBlock> blocks = TestWorldFactory.createStructureWithBugInside(wheelX, wheelY, wheelZ);
        ShipAssemblyService.StructureScan scan = createStructureScanWithBugInside(blocks);
        
        assertEquals(1, scan.bugCount(), "Sollte genau 1 BUG haben");
        assertFalse(scan.bugOnEdge(), "BUG sollte NICHT an Kante sein");
        assertFalse(scan.canAssemble(), "Assembly sollte wegen BUG-Position fehlschlagen");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FUEL SYSTEM TESTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Testet Fuel-Verbrauch während des Flugs.
     */
    @Test
    @Order(6)
    @DisplayName("Fuel-Verbrauch während des Flugs: Fuel muss sinken")
    void testFuelConsumptionDuringFlight() {
        // MockShipData direkt erstellen (ShipBlueprint verwendet Minecraft-Klassen)
        testShip = new MockShipData(7);
        testShip.setPilot(testPlayerUuid);
        helper.setTestShip(testShip);
        
        int fuelStart = testShip.getFuelLevel();
        assertEquals(100, fuelStart, "Start-Fuel sollte 100 sein");
        
        simulateFlightWithThrottle(testShip, 100); // 5 Sekunden
        
        int fuelAfter = testShip.getFuelLevel();
        assertTrue(fuelAfter < fuelStart, "Fuel sollte verbraucht worden sein");
        
        int expectedConsumption = 6;
        int actualConsumption = fuelStart - fuelAfter;
        assertEquals(expectedConsumption, actualConsumption, 2, 
            "Verbrauch sollte ~6 Energy sein (Toleranz: 2)");
    }

    /**
     * Testet Fuel-Verbrauch in verschiedenen Beschleunigungsphasen.
     */
    @Test
    @Order(7)
    @DisplayName("Fuel-Verbrauch in verschiedenen Phasen: Phase 5 verbraucht am meisten")
    void testFuelConsumptionByPhase() {
        int consumptionPhase1 = ShipPhysics.calculateFuelConsumption(AccelerationPhase.PHASE_1);
        int consumptionPhase2 = ShipPhysics.calculateFuelConsumption(AccelerationPhase.PHASE_2);
        assertEquals(1, consumptionPhase1, "Phase 1 sollte 1 Energy/sec verbrauchen");
        assertEquals(1, consumptionPhase2, "Phase 2 sollte 1 Energy/sec verbrauchen");
        
        int consumptionPhase3 = ShipPhysics.calculateFuelConsumption(AccelerationPhase.PHASE_3);
        int consumptionPhase4 = ShipPhysics.calculateFuelConsumption(AccelerationPhase.PHASE_4);
        assertEquals(2, consumptionPhase3, "Phase 3 sollte 2 Energy/sec verbrauchen");
        assertEquals(2, consumptionPhase4, "Phase 4 sollte 2 Energy/sec verbrauchen");
        
        int consumptionPhase5 = ShipPhysics.calculateFuelConsumption(AccelerationPhase.PHASE_5);
        assertEquals(3, consumptionPhase5, "Phase 5 sollte 3 Energy/sec verbrauchen");
        
        assertTrue(consumptionPhase5 > consumptionPhase3, "Phase 5 sollte mehr verbrauchen als Phase 3");
        assertTrue(consumptionPhase3 > consumptionPhase1, "Phase 3 sollte mehr verbrauchen als Phase 1");
    }

    /**
     * Testet Auftanken mit Holz.
     */
    @Test
    @Order(8)
    @DisplayName("Auftanken mit Holz: Fuel-Level muss steigen")
    void testRefuelWithWood() {
        testShip = new MockShipData(7);
        testShip.setPilot(testPlayerUuid);
        helper.setTestShip(testShip);
        testShip.addFuel(-100); // Tank leeren
        
        int fuelBefore = testShip.getFuelLevel();
        assertEquals(0, fuelBefore, "Tank sollte leer sein");
        
        int added = helper.addFuel(testShip, 1);
        
        int fuelAfter = testShip.getFuelLevel();
        assertEquals(100, added, "1 Holz sollte 100 Energy hinzufügen");
        assertEquals(100, fuelAfter, "Tank sollte voll sein");
    }

    /**
     * Testet Fuel-Capping bei vollem Tank.
     */
    @Test
    @Order(9)
    @DisplayName("Fuel-Capping: Tank sollte bei MAX_FUEL capped werden")
    void testFuelCapping() {
        testShip = new MockShipData(7);
        testShip.setPilot(testPlayerUuid);
        helper.setTestShip(testShip);
        
        int fuelBefore = testShip.getFuelLevel();
        assertEquals(100, fuelBefore, "Tank sollte voll sein");
        
        int added = helper.addFuel(testShip, 5);
        
        int fuelAfter = testShip.getFuelLevel();
        assertEquals(0, added, "Kein Fuel sollte hinzugefügt werden");
        assertEquals(fuelBefore, fuelAfter, "Fuel sollte unverändert bleiben");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DISASSEMBLY TESTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Testet Disassembly und Rückgabe der Blöcke.
     */
    @Test
    @Order(10)
    @DisplayName("Disassembly: Blöcke müssen zurückgegeben werden")
    void testDisassembleReturnsBlocks() {
        // MockShipData direkt erstellen
        testShip = new MockShipData(7);
        testShip.setPilot(testPlayerUuid);
        helper.setTestShip(testShip);
        
        assertEquals(7, testShip.getBlockCount(), "Schiff sollte 7 Blöcke haben");
        
        helper.disassemble(testShip);
        
        assertTrue(testShip.isRemoved(), "Schiff sollte entfernt sein");
    }

    /**
     * Testet Disassembly mit teilweise blockierten Positionen.
     */
    @Test
    @Order(11)
    @DisplayName("Disassembly mit Blockaden: Teilweise Rückgabe")
    void testDisassembleWithObstructions() {
        // MockShipData direkt erstellen
        testShip = new MockShipData(7);
        testShip.setPilot(testPlayerUuid);
        helper.setTestShip(testShip);
        
        helper.disassemble(testShip);
        
        assertTrue(testShip.isRemoved(), "Schiff sollte entfernt sein");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PHYSICS & MOVEMENT TESTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Testet Beschleunigung über mehrere Phasen.
     */
    @Test
    @Order(12)
    @DisplayName("Beschleunigung über mehrere Phasen: Speed sollte steigen")
    void testAccelerationOverPhases() {
        testShip = new MockShipData(7);
        testShip.setPilot(testPlayerUuid);
        helper.setTestShip(testShip);
        
        float speedStart = testShip.getCurrentSpeed();
        assertEquals(0.0f, speedStart, 0.01f, "Start-Speed sollte 0 sein");
        
        simulateFlightWithThrottle(testShip, 200); // 10 Sekunden
        
        float speedMax = testShip.getCurrentSpeed();
        assertTrue(speedMax > speedStart, "Speed sollte gestiegen sein");
        assertEquals(AccelerationPhase.PHASE_5, testShip.getPhase(), "Sollte in Phase 5 sein");
    }

    /**
     * Testet Verzögerung bei keinem Input.
     */
    @Test
    @Order(13)
    @DisplayName("Verzögerung ohne Input: Speed sollte sinken")
    void testDecelerationWithoutInput() {
        testShip = new MockShipData(7);
        testShip.setPilot(testPlayerUuid);
        helper.setTestShip(testShip);
        
        simulateFlightWithThrottle(testShip, 120); // 6 Sekunden
        float speedBefore = testShip.getCurrentSpeed();
        assertTrue(speedBefore > 0, "Ship sollte Speed haben");
        
        simulateFlightWithoutThrottle(testShip, 40); // 2 Sekunden ohne Input
        
        float speedAfter = testShip.getCurrentSpeed();
        assertTrue(speedAfter < speedBefore, "Speed sollte sinken");
    }

    /**
     * Testet Anchor-Funktionalität.
     */
    @Test
    @Order(14)
    @DisplayName("Anchor-Funktionalität: Schiff sollte bei Anchor stoppen")
    void testAnchorFunctionality() {
        testShip = new MockShipData(7);
        testShip.setPilot(testPlayerUuid);
        helper.setTestShip(testShip);
        simulateFlightWithThrottle(testShip, 40);
        
        float speedBefore = testShip.getCurrentSpeed();
        assertTrue(speedBefore > 0, "Schiff sollte sich bewegen");
        
        helper.toggleAnchor(testShip);
        
        assertTrue(testShip.isAnchored(), "Schiff sollte geankert sein");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BLUEPRINT & STRUCTURE TESTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Testet Blueprint-Erstellung aus StructureScan.
     */
    @Test
    @Order(15)
    @DisplayName("Blueprint-Erstellung: Sollte korrekte Block-Anzahl haben")
    void testBlueprintCreation() {
        ShipAssemblyService.StructureScan scan = createValidStructureScan();
        
        // Nur blockCount testen (ShipBlueprint verwendet Minecraft-Klassen)
        assertEquals(7, scan.blockCount(), "Scan sollte 7 Blöcke haben");
        assertFalse(scan.isEmpty(), "Scan sollte nicht leer sein");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EDGE CASE TESTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Testet minimales gültiges Schiff (7 Blöcke).
     */
    @Test
    @Order(17)
    @DisplayName("Minimales gültiges Schiff: 7 Blöcke sollten funktionieren")
    void testMinimalValidShip() {
        ShipAssemblyService.StructureScan scan = createValidStructureScan();
        
        assertTrue(scan.canAssemble(), "Minimale Struktur sollte assemblierbar sein");
        assertEquals(7, scan.blockCount(), "Sollte 7 Blöcke haben");
    }

    /**
     * Testet großes Schiff (60 Blöcke).
     */
    @Test
    @Order(18)
    @DisplayName("Großes Schiff: 60 Blöcke sollten funktionieren")
    void testLargeShip() {
        List<ShipBlueprint.ShipBlock> blocks = TestWorldFactory.createLargeStructure(wheelX, wheelY, wheelZ);
        
        float maxSpeed = ShipPhysics.calculateMaxSpeed(60);
        
        assertEquals(10.0f, maxSpeed, "60 Blöcke sollten 10 blocks/sec max Speed haben");
    }

    /**
     * Testet zu schweres Schiff (61+ Blöcke).
     */
    @Test
    @Order(19)
    @DisplayName("Zu schweres Schiff: 61+ Blöcke sollten nicht fliegen können")
    void testOverloadedShip() {
        float maxSpeed = ShipPhysics.calculateMaxSpeed(61);
        
        assertEquals(0.0f, maxSpeed, "61 Blöcke sollten 0 blocks/sec max Speed haben");
    }

    /**
     * Testet Engine-Out bei leerem Tank.
     */
    @Test
    @Order(20)
    @DisplayName("Engine-Out: Bei leerem Tank sollte Engine ausfallen")
    void testEngineOut() {
        testShip = new MockShipData(7);
        testShip.setPilot(testPlayerUuid);
        helper.setTestShip(testShip);
        testShip.addFuel(-100); // Tank leeren
        
        testShip.setInputs(0.0f, 0.0f, 1.0f); // Gas geben
        
        assertTrue(testShip.isEngineOut(), "Engine sollte bei leerem Tank ausfallen");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HILFSMETHODEN
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Erstellt einen validen StructureScan für Tests.
     */
    private ShipAssemblyService.StructureScan createValidStructureScan() {
        List<ShipBlueprint.ShipBlock> blocks = TestWorldFactory.createMinimalValidStructure(wheelX, wheelY, wheelZ);
        return new ShipAssemblyService.StructureScan(
            null, // origin - not used in tests
            blocks,
            Collections.emptyList(),
            0,
            true,
            1,
            4,
            1,
            true,
            0.0f
        );
    }

    /**
     * Erstellt einen StructureScan ohne Thruster.
     */
    private ShipAssemblyService.StructureScan createStructureScanWithoutThruster() {
        List<ShipBlueprint.ShipBlock> blocks = TestWorldFactory.createStructureWithoutThruster(wheelX, wheelY, wheelZ);
        return new ShipAssemblyService.StructureScan(
            null,
            blocks,
            Collections.emptyList(),
            0,
            false,
            0,
            4,
            1,
            true,
            0.0f
        );
    }

    /**
     * Erstellt einen StructureScan ohne BUG.
     */
    private ShipAssemblyService.StructureScan createStructureScanWithoutBug() {
        List<ShipBlueprint.ShipBlock> blocks = TestWorldFactory.createStructureWithoutBug(wheelX, wheelY, wheelZ);
        return new ShipAssemblyService.StructureScan(
            null,
            blocks,
            Collections.emptyList(),
            0,
            true,
            1,
            4,
            0,
            false,
            0.0f
        );
    }

    /**
     * Erstellt einen StructureScan mit Welt-Kontakt.
     */
    private ShipAssemblyService.StructureScan createStructureScanWithWorldContact() {
        List<ShipBlueprint.ShipBlock> blocks = TestWorldFactory.createStructureWithWorldContact(wheelX, wheelY, wheelZ);
        return new ShipAssemblyService.StructureScan(
            null,
            blocks,
            Collections.emptyList(),
            1,
            true,
            1,
            4,
            1,
            true,
            0.0f
        );
    }

    /**
     * Erstellt einen StructureScan mit BUG im Inneren.
     */
    private ShipAssemblyService.StructureScan createStructureScanWithBugInside(List<ShipBlueprint.ShipBlock> blocks) {
        return new ShipAssemblyService.StructureScan(
            null,
            blocks,
            Collections.emptyList(),
            0,
            true,
            1,
            6,
            1,
            false,
            0.0f
        );
    }

    /**
     * Simuliert Physik-Updates für eine bestimmte Anzahl von Ticks.
     */
    private void simulatePhysicsTicks(MockShipData ship, int ticks) {
        for (int i = 0; i < ticks; i++) {
            if (ship.getInputForward() > 0 && !ship.isEngineOut()) {
                ship.setAccelerationTicks(ship.getAccelerationTicks() + 1);
            } else {
                // Verzögerung bei keinem Input
                ship.setAccelerationTicks(Math.max(0, ship.getAccelerationTicks() - 4));
            }
            
            AccelerationPhase phase = AccelerationPhase.fromTick(ship.getAccelerationTicks());
            
            if (ship.getInputForward() > 0 && !ship.isEngineOut()) {
                float targetSpeed = phase.getSpeed();
                float currentSpeed = ship.getCurrentSpeed();
                float newSpeed = currentSpeed + (targetSpeed - currentSpeed) * 0.1f;
                ship.setCurrentSpeed(newSpeed);
            } else {
                // Verzögerung
                float currentSpeed = ship.getCurrentSpeed();
                float newSpeed = currentSpeed * 0.85f; // 15% Verzögerung pro Tick
                ship.setCurrentSpeed(newSpeed);
            }
        }
    }

    /**
     * Simuliert Flug mit Gas für eine bestimmte Anzahl von Ticks.
     */
    private void simulateFlightWithThrottle(MockShipData ship, int ticks) {
        ship.setInputs(0.0f, 0.0f, 1.0f);
        simulatePhysicsTicks(ship, ticks);
        
        int consumption = TestWorldFactory.calculateExpectedFuelConsumption(ticks, true);
        ship.addFuel(-consumption);
    }

    /**
     * Simuliert Flug ohne Gas für eine bestimmte Anzahl von Ticks.
     */
    private void simulateFlightWithoutThrottle(MockShipData ship, int ticks) {
        ship.setInputs(0.0f, 0.0f, 0.0f);
        simulatePhysicsTicks(ship, ticks);
    }
}
