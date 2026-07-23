package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REQ-022/AC-022 (T19) — architecture-conformance gate, side (a): the vehicle-core seams that
 * actually emerged from T01–T18 ({@code dev.sharkengine.ship.part}: part registry/balance/
 * analyzer, and {@code dev.sharkengine.ship.session}: server-owned build sessions) must be REAL,
 * called code — no dead abstraction may accumulate under a "core" label.
 *
 * <p><b>Adaptation of AC-022's literal rule, disclosed:</b> the rule text targets "every
 * interface/abstract class under a seam package". The seams that actually emerged contain ZERO
 * interfaces/abstract classes — they are concrete records, enums and final classes (deliberate:
 * REQ-022's amended text forbids premature generalization, ASM-005). A literal reading would
 * therefore pass vacuously — exactly the failure the tester's counter-thesis names. This test
 * applies the rule's intent to what exists instead: (1) every seam TYPE must be referenced
 * outside its own defining file, and (2) every seam PACKAGE must have call-sites outside itself
 * in {@code src/main/java}/{@code src/client/java} — proving AIR actually calls the seams.</p>
 *
 * <p><b>Anti-tautology guards:</b> the scanner asserts it actually found the known seam types
 * and source tree before judging anything — a broken path resolves to a loud failure, never a
 * silent vacuous pass (same discipline as {@code ResourceValidationTest}'s source scans).</p>
 *
 * <p><b>Honestly-disclosed limit (per the planner's own T19 note):</b> this is best-effort
 * regex-level static analysis on comment-stripped source. It cannot distinguish a non-trivial
 * call-site from a trivial pass-through wrapper — genuinely ambiguous extraction decisions must
 * be flagged for human architecture review at PR time, not silently auto-passed.</p>
 */
@DisplayName("REQ-022/T19: vehicle-core seams — real call-sites, no dead abstraction")
class VehicleCoreSeamCallSiteTest {

    private static final Path MAIN_ROOT = Path.of("src/main/java");
    private static final Path CLIENT_ROOT = Path.of("src/client/java");
    private static final Path PART_PKG = Path.of("src/main/java/dev/sharkengine/ship/part");
    private static final Path SESSION_PKG = Path.of("src/main/java/dev/sharkengine/ship/session");

    /** Strips block comments, line comments and string literals — best-effort, disclosed. */
    private static String stripCommentsAndStrings(String source) {
        return source
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("//[^\n]*", " ")
                .replaceAll("\"(\\\\.|[^\"\\\\])*\"", "\"\"");
    }

    private static List<Path> allSourceFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        for (Path root : List.of(MAIN_ROOT, CLIENT_ROOT)) {
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
            }
        }
        return files;
    }

    private static List<Path> seamTypeFiles(Path pkg) throws IOException {
        try (Stream<Path> walk = Files.list(pkg)) {
            return walk.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    private static String typeName(Path javaFile) {
        String name = javaFile.getFileName().toString();
        return name.substring(0, name.length() - ".java".length());
    }

    @Test
    @DisplayName("Scanner sanity: seam packages and source tree are actually found (no vacuous pass)")
    void scannerFindsTheRealSeams() throws IOException {
        List<String> partTypes = seamTypeFiles(PART_PKG).stream().map(VehicleCoreSeamCallSiteTest::typeName).toList();
        List<String> sessionTypes = seamTypeFiles(SESSION_PKG).stream().map(VehicleCoreSeamCallSiteTest::typeName).toList();
        assertTrue(partTypes.size() >= 7, "expected >= 7 part-seam types, found " + partTypes);
        assertTrue(sessionTypes.size() >= 8, "expected >= 8 session-seam types, found " + sessionTypes);
        assertTrue(partTypes.contains("VehiclePartRegistry") && partTypes.contains("ShipPartAnalyzer"),
                "known part-seam sentinels missing — scanner path broken? " + partTypes);
        assertTrue(sessionTypes.contains("VehicleBuildSessionRegistry")
                        && sessionTypes.contains("VehicleBuildSessionValidator"),
                "known session-seam sentinels missing — scanner path broken? " + sessionTypes);
        assertTrue(allSourceFiles().stream().anyMatch(p -> p.getFileName().toString().equals("ShipEntity.java")),
                "source walk did not find ShipEntity.java — scanner roots broken");
    }

    @Test
    @DisplayName("AC-022(a): every seam type is referenced outside its own defining file")
    void everySeamTypeHasAtLeastOneReference() throws IOException {
        List<Path> allFiles = allSourceFiles();
        List<Path> seamFiles = new ArrayList<>();
        seamFiles.addAll(seamTypeFiles(PART_PKG));
        seamFiles.addAll(seamTypeFiles(SESSION_PKG));

        List<String> dead = new ArrayList<>();
        for (Path seamFile : seamFiles) {
            String type = typeName(seamFile);
            Pattern usage = Pattern.compile("\\b" + Pattern.quote(type) + "\\b");
            boolean referenced = false;
            for (Path file : allFiles) {
                if (file.toAbsolutePath().normalize().equals(seamFile.toAbsolutePath().normalize())) {
                    continue; // its own definition does not count
                }
                String stripped = stripCommentsAndStrings(Files.readString(file));
                if (usage.matcher(stripped).find()) {
                    referenced = true;
                    break;
                }
            }
            if (!referenced) {
                dead.add(type);
            }
        }
        assertTrue(dead.isEmpty(),
                "dead seam abstraction(s) with zero call-sites outside their own file: " + dead
                        + " — either wire them into AIR or remove them (AC-022)");
    }

    @Test
    @DisplayName("AC-022(a): each seam package is called from OUTSIDE itself by AIR code")
    void eachSeamPackageHasExternalCallSites() throws IOException {
        for (Path pkg : List.of(PART_PKG, SESSION_PKG)) {
            List<String> types = seamTypeFiles(pkg).stream().map(VehicleCoreSeamCallSiteTest::typeName).toList();
            Path pkgAbs = pkg.toAbsolutePath().normalize();
            boolean externallyUsed = false;
            outer:
            for (Path file : allSourceFiles()) {
                if (file.toAbsolutePath().normalize().startsWith(pkgAbs)) {
                    continue; // intra-package usage doesn't prove AIR calls the seam
                }
                String stripped = stripCommentsAndStrings(Files.readString(file));
                for (String type : types) {
                    if (Pattern.compile("\\b" + Pattern.quote(type) + "\\b").matcher(stripped).find()) {
                        externallyUsed = true;
                        break outer;
                    }
                }
            }
            assertTrue(externallyUsed, "seam package " + pkg + " has ZERO call-sites outside itself — "
                    + "a core nobody calls is dead abstraction (AC-022, CAN-011)");
        }
    }
}
