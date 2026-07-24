package dev.sharkengine.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the F6 stripper against exactly the constructs that corrupted the old regex approach
 * (independent-review finding, 2026-07-24). If any of these regress, the architecture gates
 * scan garbage and can silently false-pass in either direction.
 */
@DisplayName("F6: JavaSourceStripper survives the constructs that broke the regex stripper")
class JavaSourceStripperTest {

    @Test
    @DisplayName("Char literal containing a double quote does not break pairing")
    void quoteCharLiteral() {
        String out = JavaSourceStripper.strip(
                "char q = '\"'; int alive = 1; // trailing comment with secret\nint next = 2;");
        assertTrue(out.contains("int alive = 1;"), "code after the '\"' literal must survive");
        assertTrue(out.contains("int next = 2;"));
        assertFalse(out.contains("secret"), "comment content must be stripped");
    }

    @Test
    @DisplayName("// inside a string literal is not treated as a comment")
    void slashesInsideString() {
        String out = JavaSourceStripper.strip(
                "String s = \"http://example // not a comment\"; int alive = 1;");
        assertFalse(out.contains("not a comment"), "string content must be stripped");
        assertTrue(out.contains("int alive = 1;"), "code after the string must survive");
    }

    @Test
    @DisplayName("Text block content (with quotes, slashes, fake comments) is fully stripped")
    void textBlock() {
        String src = "String t = \"\"\"\n  body with \" quote and // slash and /* star */\n\"\"\";\nint alive = 1;";
        String out = JavaSourceStripper.strip(src);
        assertFalse(out.contains("body with"), "text-block content must be stripped");
        assertTrue(out.contains("int alive = 1;"), "code after the text block must survive");
    }

    @Test
    @DisplayName("Block comment containing an unpaired quote does not swallow following code")
    void quoteInsideBlockComment() {
        String out = JavaSourceStripper.strip(
                "int x = 1; /* rogue \" quote and word looping */ int y = 2;");
        assertTrue(out.contains("int x = 1;") && out.contains("int y = 2;"));
        assertFalse(out.contains("looping"), "comment tokens must not leak into the scan");
    }

    @Test
    @DisplayName("Escaped quotes inside strings are handled")
    void escapedQuotes() {
        String out = JavaSourceStripper.strip(
                "String s = \"she said \\\"hi\\\" // still string\"; int alive = 1;");
        assertFalse(out.contains("still string"));
        assertTrue(out.contains("int alive = 1;"));
    }
}
