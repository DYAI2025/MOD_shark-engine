package dev.sharkengine.ship;

/**
 * Deterministic single-pass Java source stripper (independent-review finding F6, 2026-07-24):
 * removes line comments, block comments, string literals, text blocks and char literals via a
 * real character state machine. The previous regex approach mishandled text blocks, {@code '"'}
 * char literals and {@code //} inside strings — quote pairing broke and the architecture gates
 * scanned corrupted source, enabling silent false passes in BOTH directions. Shared by
 * {@code VehicleCoreSeamCallSiteTest} and {@code ShipEntityConditionalGrowthTest}; behavior
 * locked by {@code JavaSourceStripperTest}.
 */
final class JavaSourceStripper {

    private JavaSourceStripper() {}

    static String strip(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        int n = source.length();
        while (i < n) {
            char c = source.charAt(i);
            char next = i + 1 < n ? source.charAt(i + 1) : '\0';
            if (c == '/' && next == '/') {
                while (i < n && source.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && next == '*') {
                i += 2;
                while (i + 1 < n && !(source.charAt(i) == '*' && source.charAt(i + 1) == '/')) {
                    i++;
                }
                i = Math.min(n, i + 2);
                out.append(' ');
            } else if (c == '"' && next == '"' && i + 2 < n && source.charAt(i + 2) == '"') {
                // text block """..."""
                i += 3;
                while (i + 2 < n && !(source.charAt(i) == '"'
                        && source.charAt(i + 1) == '"' && source.charAt(i + 2) == '"')) {
                    if (source.charAt(i) == '\\') {
                        i++;
                    }
                    i++;
                }
                i = Math.min(n, i + 3);
                out.append("\"\"");
            } else if (c == '"') {
                i++;
                while (i < n && source.charAt(i) != '"') {
                    if (source.charAt(i) == '\\') {
                        i++;
                    }
                    i++;
                }
                i++;
                out.append("\"\"");
            } else if (c == '\'') {
                i++;
                while (i < n && source.charAt(i) != '\'') {
                    if (source.charAt(i) == '\\') {
                        i++;
                    }
                    i++;
                }
                i++;
                out.append(' ');
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }
}
