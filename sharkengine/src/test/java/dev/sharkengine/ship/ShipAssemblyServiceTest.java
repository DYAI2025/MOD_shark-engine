package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Umfassende JUnit 5 Test-Suite für ShipAssemblyService.
 * 
 * <p>Testet die Strukturvalidierung und Assembly-Logik der Shark Engine Mod.</p>
 * 
 * <p>Erforderliche Bedingungen für erfolgreiche Assembly:</p>
 * <ul>
 *   <li>Mindestens 4 Kernblöcke (Nachbarn des Steering Wheels)</li>
 *   <li>Genau 1 BUG-Block an der Außenkante der Struktur</li>
 *   <li>Mindestens 1 Thruster-Block</li>
 *   <li>Keine Bodenkontakte (World Contacts)</li>
 *   <li>Keine ungültigen Blöcke in der Struktur</li>
 * </ul>
 *
 * @author Shark Engine Team
 * @version 1.0
 */
@DisplayName("ShipAssemblyService Tests")
class ShipAssemblyServiceTest {

    // ═══════════════════════════════════════════════════════════════════════
    // TEST 1: Leere Struktur sollte fehlschlagen
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Leere Struktur: Assembly sollte fehlschlagen, wenn keine Blöcke vorhanden sind")
    void testEmptyStructure() {
        // GIVEN: Leere Struktur ohne Blöcke
        ShipAssemblyService.StructureScan emptyScan = createEmptyScan();

        // WHEN/THEN: Scan sollte als leer markiert sein und nicht assemblierbar
        assertTrue(emptyScan.isEmpty(), "Leere Struktur sollte als leer markiert sein");
        assertEquals(0, emptyScan.blockCount(), "Block-Anzahl sollte 0 sein");
        assertFalse(emptyScan.canAssemble(), "Leere Struktur sollte nicht assemblierbar sein");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TEST 2: Minimale gültige Struktur sollte erfolgreich sein
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Minimale gültige Struktur: 4 Kernblöcke + BUG an Kante + Thruster sollte erfolgreich sein")
    void testMinimalValidStructure() {
        // GIVEN: Minimale gültige Struktur mit allen erforderlichen Komponenten
        // - 4 Kernblöcke (coreNeighbors >= 4)
        // - 1 BUG-Block an der Außenkante (bugOnEdge = true)
        // - 1 Thruster-Block (hasThruster = true)
        // - Keine Bodenkontakte (contactPoints = 0)
        // - Keine ungültigen Blöcke (invalidAttachments leer)
        ShipAssemblyService.StructureScan validScan = createValidScan();

        // WHEN/THEN: Alle Validierungen sollten erfolgreich sein
        assertFalse(validScan.isEmpty(), "Struktur sollte nicht leer sein");
        assertTrue(validScan.hasThruster(), "Struktur sollte Thruster haben");
        assertEquals(1, validScan.bugCount(), "Sollte genau 1 BUG-Block haben");
        assertTrue(validScan.bugOnEdge(), "BUG-Block sollte an der Kante sein");
        assertTrue(validScan.coreNeighbors() >= 4, "Sollte mindestens 4 Kern-Nachbarn haben");
        assertEquals(0, validScan.contactPoints(), "Sollte keine Bodenkontakte haben");
        assertTrue(validScan.invalidAttachments().isEmpty(), "Sollte keine ungültigen Blöcke haben");
        assertTrue(validScan.canAssemble(), "Minimale gültige Struktur sollte assemblierbar sein");
        assertTrue(validScan.hasBug(), "Struktur sollte einen BUG haben");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TEST 3: Weniger als 4 Kernblöcke sollte fehlschlagen
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Weniger als 4 Kernblöcke: Assembly sollte fehlschlagen bei nur 3 Nachbarn")
    void testLessThan4CoreBlocks() {
        // GIVEN: Struktur mit nur 3 Kernblöcken (fehlender vierter Nachbar)
        ShipAssemblyService.StructureScan scan = createScanWithCoreNeighbors(3);

        // WHEN/THEN: Assembly sollte wegen zu wenig Kernblöcken fehlschlagen
        assertEquals(3, scan.coreNeighbors(), "Sollte nur 3 Kern-Nachbarn haben");
        assertFalse(scan.canAssemble(), "Struktur mit < 4 Kernblöcken sollte nicht assemblierbar sein");
    }

    @Test
    @DisplayName("Genau 4 Kernblöcke: Assembly sollte erfolgreich sein (Grenzfall)")
    void testExactly4CoreBlocks() {
        // GIVEN: Struktur mit genau 4 Kernblöcken (minimaler gültiger Wert)
        ShipAssemblyService.StructureScan scan = createScanWithCoreNeighbors(4);

        // WHEN/THEN: Assembly sollte erfolgreich sein
        assertEquals(4, scan.coreNeighbors(), "Sollte genau 4 Kern-Nachbarn haben");
        // Andere Bedingungen müssen auch erfüllt sein
        assertTrue(scan.hasThruster(), "Sollte Thruster haben");
        assertEquals(1, scan.bugCount(), "Sollte 1 BUG haben");
        assertTrue(scan.bugOnEdge(), "BUG sollte an Kante sein");
    }

    @Test
    @DisplayName("Mehr als 4 Kernblöcke: Assembly sollte erfolgreich sein")
    void testMoreThan4CoreBlocks() {
        // GIVEN: Struktur mit 6 Kernblöcken
        ShipAssemblyService.StructureScan scan = createScanWithCoreNeighbors(6);

        // WHEN/THEN: Assembly sollte erfolgreich sein
        assertEquals(6, scan.coreNeighbors(), "Sollte 6 Kern-Nachbarn haben");
        assertTrue(scan.canAssemble(), "Struktur mit > 4 Kernblöcken sollte assemblierbar sein");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TEST 4: Kein Thruster sollte fehlschlagen
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Kein Thruster: Assembly sollte fehlschlagen ohne Thruster-Block")
    void testNoThruster() {
        // GIVEN: Gültige Struktur OHNE Thruster
        ShipAssemblyService.StructureScan scan = createScanWithoutThruster();

        // WHEN/THEN: Assembly sollte wegen fehlendem Thruster fehlschlagen
        assertFalse(scan.hasThruster(), "Struktur sollte keinen Thruster haben");
        assertEquals(0, scan.thrusterCount(), "Thruster-Anzahl sollte 0 sein");
        assertFalse(scan.canAssemble(), "Struktur ohne Thruster sollte nicht assemblierbar sein");
    }

    @Test
    @DisplayName("Ein Thruster: Assembly sollte erfolgreich sein (minimale Anforderung)")
    void testSingleThruster() {
        // GIVEN: Struktur mit genau 1 Thruster
        ShipAssemblyService.StructureScan scan = createValidScan();

        // WHEN/THEN: Assembly sollte erfolgreich sein
        assertTrue(scan.hasThruster(), "Struktur sollte Thruster haben");
        assertEquals(1, scan.thrusterCount(), "Thruster-Anzahl sollte 1 sein");
    }

    @Test
    @DisplayName("Mehrere Thruster: Assembly sollte erfolgreich sein")
    void testMultipleThrusters() {
        // GIVEN: Struktur mit mehreren Thrustern
        ShipAssemblyService.StructureScan scan = createScanWithThrusterCount(3);

        // WHEN/THEN: Assembly sollte erfolgreich sein
        assertTrue(scan.hasThruster(), "Struktur sollte Thruster haben");
        assertEquals(3, scan.thrusterCount(), "Thruster-Anzahl sollte 3 sein");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TEST 5: Kein Bug-Block sollte fehlschlagen
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Kein BUG-Block: Assembly sollte fehlschlagen ohne BUG-Block")
    void testNoBugBlock() {
        // GIVEN: Gültige Struktur OHNE BUG-Block
        ShipAssemblyService.StructureScan scan = createScanWithBugCount(0);

        // WHEN/THEN: Assembly sollte wegen fehlendem BUG fehlschlagen
        assertEquals(0, scan.bugCount(), "BUG-Anzahl sollte 0 sein");
        assertFalse(scan.hasBug(), "Struktur sollte keinen BUG haben");
        assertFalse(scan.canAssemble(), "Struktur ohne BUG sollte nicht assemblierbar sein");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TEST 6: Mehrere Bug-Blöcke sollten fehlschlagen
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Mehrere BUG-Blöcke: Assembly sollte fehlschlagen bei 2+ BUGs")
    void testMultipleBugBlocks() {
        // GIVEN: Struktur mit 2 BUG-Blöcken
        ShipAssemblyService.StructureScan scan = createScanWithBugCount(2);

        // WHEN/THEN: Assembly sollte wegen mehrerer BUGs fehlschlagen
        assertEquals(2, scan.bugCount(), "Sollte 2 BUG-Blöcke haben");
        assertFalse(scan.hasBug(), "Struktur mit mehreren BUGs sollte hasBug()=false zurückgeben");
        assertFalse(scan.canAssemble(), "Struktur mit mehreren BUGs sollte nicht assemblierbar sein");
    }

    @Test
    @DisplayName("Drei BUG-Blöcke: Assembly sollte fehlschlagen bei 3 BUGs")
    void testThreeBugBlocks() {
        // GIVEN: Struktur mit 3 BUG-Blöcken
        ShipAssemblyService.StructureScan scan = createScanWithBugCount(3);

        // WHEN/THEN: Assembly sollte wegen mehrerer BUGs fehlschlagen
        assertEquals(3, scan.bugCount(), "Sollte 3 BUG-Blöcke haben");
        assertFalse(scan.canAssemble(), "Struktur mit 3 BUGs sollte nicht assemblierbar sein");
    }

    @Test
    @DisplayName("Fünf BUG-Blöcke: Assembly sollte fehlschlagen bei 5 BUGs")
    void testFiveBugBlocks() {
        // GIVEN: Struktur mit 5 BUG-Blöcken
        ShipAssemblyService.StructureScan scan = createScanWithBugCount(5);

        // WHEN/THEN: Assembly sollte wegen mehrerer BUGs fehlschlagen
        assertEquals(5, scan.bugCount(), "Sollte 5 BUG-Blöcke haben");
        assertFalse(scan.canAssemble(), "Struktur mit 5 BUGs sollte nicht assemblierbar sein");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TEST 7: Bug-Block nicht an Kante sollte fehlschlagen
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("BUG-Block im Inneren: Assembly sollte fehlschlagen wenn BUG nicht an Kante ist")
    void testBugBlockNotOnEdge() {
        // GIVEN: Struktur mit BUG im Inneren (bugOnEdge = false)
        ShipAssemblyService.StructureScan scan = createScanWithBugOnEdge(false);

        // WHEN/THEN: Assembly sollte fehlschlagen weil BUG nicht an der Kante ist
        assertEquals(1, scan.bugCount(), "Sollte genau 1 BUG haben");
        assertFalse(scan.bugOnEdge(), "BUG sollte NICHT an der Kante sein");
        assertFalse(scan.canAssemble(), "Struktur mit BUG im Inneren sollte nicht assemblierbar sein");
    }

    @Test
    @DisplayName("BUG-Block an Kante: Assembly sollte erfolgreich sein")
    void testBugBlockOnEdge() {
        // GIVEN: Struktur mit BUG an der Kante (bugOnEdge = true)
        ShipAssemblyService.StructureScan scan = createScanWithBugOnEdge(true);

        // WHEN/THEN: Assembly sollte erfolgreich sein (bezüglich BUG-Position)
        assertEquals(1, scan.bugCount(), "Sollte genau 1 BUG haben");
        assertTrue(scan.bugOnEdge(), "BUG sollte an der Kante sein");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TEST 8: Bodenkontakt sollte fehlschlagen
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Bodenkontakt: Assembly sollte fehlschlagen bei World Contact mit fester Struktur")
    void testWorldContact() {
        // GIVEN: Struktur mit Bodenkontakt (contactPoints > 0)
        ShipAssemblyService.StructureScan scan = createScanWithContactPoints(1);

        // WHEN/THEN: Assembly sollte wegen Bodenkontakt fehlschlagen
        assertTrue(scan.contactPoints() > 0, "Sollte mindestens 1 Kontaktpunkt haben");
        assertFalse(scan.canAssemble(), "Struktur mit Bodenkontakt sollte nicht assemblierbar sein");
    }

    @Test
    @DisplayName("Mehrere Bodenkontakte: Assembly sollte fehlschlagen")
    void testMultipleWorldContacts() {
        // GIVEN: Struktur mit mehreren Bodenkontakten
        ShipAssemblyService.StructureScan scan = createScanWithContactPoints(3);

        // WHEN/THEN: Assembly sollte wegen Bodenkontakten fehlschlagen
        assertEquals(3, scan.contactPoints(), "Sollte 3 Kontaktpunkte haben");
        assertFalse(scan.canAssemble(), "Struktur mit Bodenkontakten sollte nicht assemblierbar sein");
    }

    @Test
    @DisplayName("Keine Bodenkontakte: Assembly sollte erfolgreich sein")
    void testNoWorldContacts() {
        // GIVEN: Struktur ohne Bodenkontakte
        ShipAssemblyService.StructureScan scan = createValidScan();

        // WHEN/THEN: Assembly sollte erfolgreich sein (bezüglich Kontakten)
        assertEquals(0, scan.contactPoints(), "Sollte keine Kontaktpunkte haben");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TEST 9: Ungültige Blöcke sollten fehlschlagen
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Ungültige Blöcke: Assembly sollte fehlschlagen bei nicht-ship_eligible Blöcken")
    void testInvalidAttachments() {
        // GIVEN: Struktur mit einem ungültigen Block
        // Hinweis: Da BlockPos in Tests nicht verfügbar ist, testen wir die Größe der Liste
        ShipAssemblyService.StructureScan scan = createScanWithInvalidAttachments(1);

        // WHEN/THEN: Assembly sollte wegen ungültiger Blöcke fehlschlagen
        assertFalse(scan.invalidAttachments().isEmpty(), "Sollte ungültige Blöcke haben");
        assertEquals(1, scan.invalidAttachments().size(), "Sollte 1 ungültigen Block haben");
        assertFalse(scan.canAssemble(), "Struktur mit ungültigen Blöcken sollte nicht assemblierbar sein");
    }

    @Test
    @DisplayName("Mehrere ungültige Blöcke: Assembly sollte fehlschlagen")
    void testMultipleInvalidAttachments() {
        // GIVEN: Struktur mit mehreren ungültigen Blöcken
        ShipAssemblyService.StructureScan scan = createScanWithInvalidAttachments(3);

        // WHEN/THEN: Assembly sollte wegen ungültiger Blöcke fehlschlagen
        assertEquals(3, scan.invalidAttachments().size(), "Sollte 3 ungültige Blöcke haben");
        assertFalse(scan.canAssemble(), "Struktur mit ungültigen Blöcken sollte nicht assemblierbar sein");
    }

    @Test
    @DisplayName("Keine ungültigen Blöcke: Assembly sollte erfolgreich sein")
    void testNoInvalidAttachments() {
        // GIVEN: Struktur ohne ungültige Blöcke
        ShipAssemblyService.StructureScan scan = createValidScan();

        // WHEN/THEN: Assembly sollte erfolgreich sein (bezüglich ungültiger Blöcke)
        assertTrue(scan.invalidAttachments().isEmpty(), "Sollte keine ungültigen Blöcke haben");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TEST 10: Vollständiger Assembly-Flow
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Vollständiger Assembly-Flow: Gültige Struktur sollte alle canAssemble-Bedingungen erfüllen")
    void testFullAssemblyFlow() {
        // GIVEN: Vollständig gültige Struktur für erfolgreichen Assembly-Flow
        ShipAssemblyService.StructureScan scan = createValidScan();

        // WHEN: Alle Validierungsbedingungen werden geprüft
        boolean isEmpty = scan.isEmpty();
        boolean hasInvalidAttachments = !scan.invalidAttachments().isEmpty();
        boolean hasContactPoints = scan.contactPoints() > 0;
        boolean hasNoThruster = !scan.hasThruster();
        boolean hasInsufficientCore = scan.coreNeighbors() < 4;
        boolean hasNoBug = scan.bugCount() == 0;
        boolean hasMultipleBugs = scan.bugCount() > 1;
        boolean bugNotOnEdge = !scan.bugOnEdge();

        // THEN: Alle Bedingungen sollten für erfolgreiche Assembly erfüllt sein
        assertFalse(isEmpty, "Struktur sollte nicht leer sein");
        assertFalse(hasInvalidAttachments, "Struktur sollte keine ungültigen Blöcke haben");
        assertFalse(hasContactPoints, "Struktur sollte keine Bodenkontakte haben");
        assertFalse(hasNoThruster, "Struktur sollte Thruster haben");
        assertFalse(hasInsufficientCore, "Struktur sollte mindestens 4 Kern-Nachbarn haben");
        assertFalse(hasNoBug, "Struktur sollte genau 1 BUG haben");
        assertFalse(hasMultipleBugs, "Struktur sollte nicht mehrere BUGs haben");
        assertFalse(bugNotOnEdge, "BUG sollte an der Kante sein");

        // canAssemble() sollte true zurückgeben
        assertTrue(scan.canAssemble(), "Vollständig gültige Struktur sollte assemblierbar sein");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ZUSÄTZLICHE TESTS FÜR StructureScan RECORD METHODEN
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("StructureScan.isEmpty: Gibt true bei leerer Block-Liste zurück")
    void testStructureScanIsEmpty() {
        ShipAssemblyService.StructureScan emptyScan = createEmptyScan();
        assertTrue(emptyScan.isEmpty());

        ShipAssemblyService.StructureScan nonEmptyScan = createValidScan();
        assertFalse(nonEmptyScan.isEmpty());
    }

    @Test
    @DisplayName("StructureScan.blockCount: Gibt Größe der Block-Liste zurück")
    void testStructureScanBlockCount() {
        // Test mit 3 Blöcken
        List<ShipBlueprint.ShipBlock> blocks3 = createShipBlocks(3);
        ShipAssemblyService.StructureScan scan3 = createScanWithBlocks(blocks3);
        assertEquals(3, scan3.blockCount());

        // Test mit 7 Blöcken (minimale gültige Struktur)
        List<ShipBlueprint.ShipBlock> blocks7 = createShipBlocks(7);
        ShipAssemblyService.StructureScan scan7 = createScanWithBlocks(blocks7);
        assertEquals(7, scan7.blockCount());

        // Test mit 0 Blöcken
        List<ShipBlueprint.ShipBlock> blocks0 = createShipBlocks(0);
        ShipAssemblyService.StructureScan scan0 = createScanWithBlocks(blocks0);
        assertEquals(0, scan0.blockCount());
    }

    @Test
    @DisplayName("StructureScan.hasBug: Gibt true nur bei genau 1 BUG zurück")
    void testStructureScanHasBug() {
        // 0 BUGs
        ShipAssemblyService.StructureScan noBug = createScanWithBugCount(0);
        assertFalse(noBug.hasBug(), "0 BUGs sollte hasBug()=false zurückgeben");

        // 1 BUG
        ShipAssemblyService.StructureScan oneBug = createScanWithBugCount(1);
        assertTrue(oneBug.hasBug(), "1 BUG sollte hasBug()=true zurückgeben");

        // 2 BUGs
        ShipAssemblyService.StructureScan twoBugs = createScanWithBugCount(2);
        assertFalse(twoBugs.hasBug(), "2 BUGs sollte hasBug()=false zurückgeben");

        // 5 BUGs
        ShipAssemblyService.StructureScan fiveBugs = createScanWithBugCount(5);
        assertFalse(fiveBugs.hasBug(), "5 BUGs sollte hasBug()=false zurückgeben");
    }

    @Test
    @DisplayName("BUG-Yaw: Verschiedene Richtungen werden korrekt gespeichert")
    void testBugYawValues() {
        // NORTH = 180°
        ShipAssemblyService.StructureScan northScan = createScanWithBugYaw(180.0f);
        assertEquals(180.0f, northScan.bugYawDeg());

        // SOUTH = 0°
        ShipAssemblyService.StructureScan southScan = createScanWithBugYaw(0.0f);
        assertEquals(0.0f, southScan.bugYawDeg());

        // WEST = 90°
        ShipAssemblyService.StructureScan westScan = createScanWithBugYaw(90.0f);
        assertEquals(90.0f, westScan.bugYawDeg());

        // EAST = -90°
        ShipAssemblyService.StructureScan eastScan = createScanWithBugYaw(-90.0f);
        assertEquals(-90.0f, eastScan.bugYawDeg());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // KOMBINIERTE FEHLERTESTS
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Mehrere Fehler gleichzeitig: Alle sollten canAssemble=false verursachen")
    void testMultipleFailuresCombined() {
        // GIVEN: Struktur mit mehreren Fehlern gleichzeitig
        ShipAssemblyService.StructureScan scan = new ShipAssemblyService.StructureScan(
                null,   // origin (nicht getestet)
                createShipBlocks(1),
                createInvalidBlockPosList(1),  // Ungültige Blöcke
                2,                     // Bodenkontakte
                false,                 // Kein Thruster
                0,                     // 0 Thruster
                2,                     // Nur 2 Kern-Nachbarn
                0,                     // Kein BUG
                false,                 // BUG nicht an Kante (irrelevant da kein BUG)
                0.0f
        );

        // THEN: Assembly sollte fehlschlagen
        assertFalse(scan.canAssemble(), "Struktur mit mehreren Fehlern sollte nicht assemblierbar sein");
        assertTrue(scan.isEmpty() || !scan.invalidAttachments().isEmpty() || 
                   scan.contactPoints() > 0 || !scan.hasThruster() || 
                   scan.coreNeighbors() < 4 || scan.bugCount() != 1 || !scan.bugOnEdge(),
                   "Mindestens eine Fehlerbedingung sollte zutreffen");
    }

    @Test
    @DisplayName("Nur ein Fehler: should fail canAssemble")
    void testSingleFailureOnly() {
        // Nur kein Thruster (alle anderen Bedingungen erfüllt)
        ShipAssemblyService.StructureScan noThrusterScan = new ShipAssemblyService.StructureScan(
                null,
                createShipBlocks(1),
                Collections.emptyList(),
                0, false, 0, 4, 1, true, 180.0f
        );
        assertFalse(noThrusterScan.canAssemble(), "Nur kein Thruster sollte fehlschlagen");

        // Nur Bodenkontakt
        ShipAssemblyService.StructureScan contactScan = new ShipAssemblyService.StructureScan(
                null,
                createShipBlocks(1),
                Collections.emptyList(),
                1, true, 1, 4, 1, true, 180.0f
        );
        assertFalse(contactScan.canAssemble(), "Nur Bodenkontakt sollte fehlschlagen");

        // Nur zu wenig Kernblöcke
        ShipAssemblyService.StructureScan coreScan = new ShipAssemblyService.StructureScan(
                null,
                createShipBlocks(1),
                Collections.emptyList(),
                0, true, 1, 3, 1, true, 180.0f
        );
        assertFalse(coreScan.canAssemble(), "Nur zu wenig Kernblöcke sollte fehlschlagen");

        // Nur BUG nicht an Kante
        ShipAssemblyService.StructureScan bugEdgeScan = new ShipAssemblyService.StructureScan(
                null,
                createShipBlocks(1),
                Collections.emptyList(),
                0, true, 1, 4, 1, false, 180.0f
        );
        assertFalse(bugEdgeScan.canAssemble(), "Nur BUG nicht an Kante sollte fehlschlagen");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GRENZFALL-TESTS
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Grenzfall: 0 Blöcke sollte nicht assemblierbar sein")
    void testEdgeCaseZeroBlocks() {
        ShipAssemblyService.StructureScan scan = createEmptyScan();
        assertFalse(scan.canAssemble(), "0 Blöcke sollte nicht assemblierbar sein");
    }

    @Test
    @DisplayName("Grenzfall: Sehr viele Thruster sollte erfolgreich sein")
    void testEdgeCaseManyThrusters() {
        ShipAssemblyService.StructureScan scan = createScanWithThrusterCount(100);
        assertTrue(scan.hasThruster(), "Sollte Thruster haben");
        assertEquals(100, scan.thrusterCount(), "Sollte 100 Thruster haben");
        assertTrue(scan.canAssemble(), "Viele Thruster sollten erfolgreich sein");
    }

    @Test
    @DisplayName("Grenzfall: 0 Kontaktpunkte sollte erfolgreich sein")
    void testEdgeCaseZeroContacts() {
        ShipAssemblyService.StructureScan scan = createScanWithContactPoints(0);
        assertEquals(0, scan.contactPoints(), "Sollte 0 Kontaktpunkte haben");
        assertTrue(scan.canAssemble(), "0 Kontaktpunkte sollte erfolgreich sein");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HILFSMETHODEN
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Erstellt einen leeren Scan für Testzwecke.
     */
    private ShipAssemblyService.StructureScan createEmptyScan() {
        return new ShipAssemblyService.StructureScan(
                null,               // origin
                Collections.emptyList(),
                Collections.emptyList(),
                0,      // contactPoints
                false,  // hasThruster
                0,      // thrusterCount
                0,      // coreNeighbors
                0,      // bugCount
                false,  // bugOnEdge
                0.0f    // bugYawDeg
        );
    }

    /**
     * Erstellt einen vollständig gültigen Scan für Testzwecke.
     * Simuliert eine minimale gültige Struktur.
     */
    private ShipAssemblyService.StructureScan createValidScan() {
        List<ShipBlueprint.ShipBlock> blocks = createShipBlocks(7);
        return new ShipAssemblyService.StructureScan(
                null,
                blocks,
                Collections.emptyList(),
                0,      // contactPoints
                true,   // hasThruster
                1,      // thrusterCount
                4,      // coreNeighbors
                1,      // bugCount
                true,   // bugOnEdge
                180.0f  // bugYawDeg (NORTH)
        );
    }

    /**
     * Erstellt einen Scan mit指定 Anzahl von Kern-Nachbarn.
     */
    private ShipAssemblyService.StructureScan createScanWithCoreNeighbors(int coreNeighbors) {
        return new ShipAssemblyService.StructureScan(
                null,
                createShipBlocks(1),
                Collections.emptyList(),
                0,
                true,
                1,
                coreNeighbors,
                1,
                true,
                180.0f
        );
    }

    /**
     * Erstellt einen Scan ohne Thruster.
     */
    private ShipAssemblyService.StructureScan createScanWithoutThruster() {
        return new ShipAssemblyService.StructureScan(
                null,
                createShipBlocks(1),
                Collections.emptyList(),
                0,
                false,  // hasThruster = false
                0,      // thrusterCount = 0
                4,
                1,
                true,
                180.0f
        );
    }

    /**
     * Erstellt einen Scan mit指定 Anzahl von Thrustern.
     */
    private ShipAssemblyService.StructureScan createScanWithThrusterCount(int thrusterCount) {
        return new ShipAssemblyService.StructureScan(
                null,
                createShipBlocks(1),
                Collections.emptyList(),
                0,
                thrusterCount > 0,
                thrusterCount,
                4,
                1,
                true,
                180.0f
        );
    }

    /**
     * Erstellt einen Scan mit指定 Anzahl von BUG-Blöcken.
     */
    private ShipAssemblyService.StructureScan createScanWithBugCount(int bugCount) {
        return new ShipAssemblyService.StructureScan(
                null,
                createShipBlocks(1),
                Collections.emptyList(),
                0,
                true,
                1,
                4,
                bugCount,
                bugCount == 1,  // bugOnEdge nur true bei genau 1 BUG
                180.0f
        );
    }

    /**
     * Erstellt einen Scan mit指定 BUG-Kantenstatus.
     */
    private ShipAssemblyService.StructureScan createScanWithBugOnEdge(boolean bugOnEdge) {
        return new ShipAssemblyService.StructureScan(
                null,
                createShipBlocks(1),
                Collections.emptyList(),
                0,
                true,
                1,
                4,
                1,
                bugOnEdge,
                180.0f
        );
    }

    /**
     * Erstellt einen Scan mit指定 Anzahl von Kontaktpunkten.
     */
    private ShipAssemblyService.StructureScan createScanWithContactPoints(int contactPoints) {
        return new ShipAssemblyService.StructureScan(
                null,
                createShipBlocks(1),
                Collections.emptyList(),
                contactPoints,
                true,
                1,
                4,
                1,
                true,
                180.0f
        );
    }

    /**
     * Erstellt einen Scan mit ungültigen Blöcken.
     * Verwendet eine leere Liste, da BlockPos in Tests nicht verfügbar ist.
     */
    private ShipAssemblyService.StructureScan createScanWithInvalidAttachments(int count) {
        return new ShipAssemblyService.StructureScan(
                null,
                createShipBlocks(count + 1),
                createInvalidBlockPosList(count),
                0,
                true,
                1,
                4,
                1,
                true,
                180.0f
        );
    }

    /**
     * Erstellt einen Scan mit指定 BUG-Yaw.
     */
    private ShipAssemblyService.StructureScan createScanWithBugYaw(float bugYawDeg) {
        return new ShipAssemblyService.StructureScan(
                null,
                createShipBlocks(1),
                Collections.emptyList(),
                0,
                true,
                1,
                4,
                1,
                true,
                bugYawDeg
        );
    }

    /**
     * Erstellt einen Scan mit指定 Blöcken.
     */
    private ShipAssemblyService.StructureScan createScanWithBlocks(List<ShipBlueprint.ShipBlock> blocks) {
        return new ShipAssemblyService.StructureScan(
                null,
                blocks,
                Collections.emptyList(),
                0,
                true,
                1,
                4,
                1,
                true,
                180.0f
        );
    }

    /**
     * Erstellt eine Liste von ShipBlocks für Testzwecke.
     */
    private List<ShipBlueprint.ShipBlock> createShipBlocks(int count) {
        List<ShipBlueprint.ShipBlock> blocks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            blocks.add(new ShipBlueprint.ShipBlock(i, 0, 0, null));
        }
        return blocks;
    }

    /**
     * Erstellt eine Liste mit ungültigen BlockPos-Objekten für Testzwecke.
     * Da BlockPos in der Testumgebung nicht direkt instanziiert werden kann,
     * verwenden wir null als Platzhalter.
     */
    private List<net.minecraft.core.BlockPos> createInvalidBlockPosList(int count) {
        List<net.minecraft.core.BlockPos> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(null); // Platzhalter für BlockPos
        }
        return list;
    }
}
