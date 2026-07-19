package dev.sharkengine.ship.part;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A single structured reason a ship structure cannot currently be assembled (REQ-S3).
 *
 * <p>Replaces the previous design where {@code ShipAssemblyService.tryAssemble} picked
 * exactly one blocking condition (in a fixed priority order) and reported it as a single
 * translated chat message. {@code ShipAssemblyService.StructureScan#issues()} instead reports
 * every currently-failing condition at once, so the builder preview can list all blockers
 * simultaneously rather than making the player fix them one at a time.</p>
 *
 * <p><b>Deliberately carries no {@code net.minecraft.network}/{@code net.minecraft.nbt}
 * dependency</b> — only {@link BlockPos} (already proven plain-unit-testable elsewhere in this
 * package, e.g. {@code ShipAssemblyServiceTest}). This is not a style preference: this repo's
 * {@code test} source set genuinely cannot compile against {@code net.minecraft.network.*}
 * (verified empirically — a plain {@code import net.minecraft.network.FriendlyByteBuf;} in
 * {@code src/test} fails with "package net.minecraft.network does not exist", even though
 * {@code src/main} compiles against it fine). Since a record's static initializer runs the
 * moment the class is loaded — e.g. from any {@code AssemblyIssue.of(...)} call in a plain
 * unit test — putting a {@code StreamCodec}/{@code FriendlyByteBuf} field directly on this
 * class would throw {@code NoClassDefFoundError} the instant ANY test touches it, even tests
 * that never encode/decode anything (the same class of bug AIR-015 hit with
 * {@code ShipBlueprint}). Wire encoding for the codes this class carries lives instead in
 * {@link dev.sharkengine.net.BuilderPreviewS2CPayload}, which already imports the network
 * package for its other fields and is only exercised by GameTests, not plain unit tests.</p>
 *
 * @param code the issue's identity; also its translation-key suffix
 * @param pos  optional world position the issue points at (e.g. an offending block); {@code null}
 *             when the issue has no single associated position
 * @param args numeric arguments substituted into the code's translated message (e.g. a count);
 *             empty for codes whose message takes no argument. Never {@code null} (normalized to
 *             {@link List#of()} by the canonical constructor).
 */
public record AssemblyIssue(Code code, BlockPos pos, List<Integer> args) {

    public AssemblyIssue {
        Objects.requireNonNull(code, "code");
        args = args == null ? List.of() : List.copyOf(args);
    }

    public static AssemblyIssue of(Code code) {
        return new AssemblyIssue(code, null, List.of());
    }

    public static AssemblyIssue of(Code code, int arg) {
        return new AssemblyIssue(code, null, List.of(arg));
    }

    public static AssemblyIssue of(Code code, BlockPos pos) {
        return new AssemblyIssue(code, pos, List.of());
    }

    public static AssemblyIssue of(Code code, BlockPos pos, int arg) {
        return new AssemblyIssue(code, pos, List.of(arg));
    }

    /** Translation key for this issue's message, e.g. {@code assembly_issue.sharkengine.no_bug}. */
    public String translationKey() {
        return code.translationKey();
    }

    /** {@code args} as an {@code Object[]}, ready for {@code Component.translatable(key, args)}. */
    public Object[] translationArgs() {
        return args.toArray();
    }

    /**
     * The identity of a blocking condition. Mirrors, one-to-one, the checks in
     * {@code ShipAssemblyService.StructureScan#canAssemble()} — see that method and
     * {@code #issues()} for the exact conditions that produce each code.
     */
    public enum Code {
        /** The scan found no eligible blocks at all. */
        EMPTY_STRUCTURE("empty_structure"),
        /** One or more non-eligible blocks are attached to the structure; must be removed. */
        INVALID_ATTACHMENTS("invalid_attachments"),
        /** The structure (or its footprint) touches world terrain and cannot lift off. */
        TERRAIN_CONTACT("terrain_contact"),
        /** No part with {@link PartRole#PROPULSION} is present. */
        NO_PROPULSION("no_propulsion"),
        /** No part with {@link PartRole#PILOT_SEAT} is present (REQ-005). */
        NO_PILOT_SEAT("no_pilot_seat"),
        /** More than one part with {@link PartRole#PILOT_SEAT} is present; exactly one is required (REQ-005). */
        MULTI_PILOT_SEAT("multi_pilot_seat"),
        /** Fewer than 4 ship-eligible blocks are attached directly to the steering wheel. */
        TOO_FEW_CORE_NEIGHBORS("too_few_core_neighbors"),
        /** No BUG (bow) block found in the structure. */
        NO_BUG("no_bug"),
        /** More than one BUG block found; exactly one is required. */
        MULTI_BUG("multi_bug"),
        /** The single BUG block is not on the structure's outer edge. */
        BUG_INSIDE("bug_inside"),
        /**
         * The pilot seat is not resolved at its one required position — the single block
         * directly in front of the BUG's resolved facing (REQ-006). Covers both "occupied by a
         * non-seat block" and "empty/otherwise invalid": there is no fallback search for an
         * alternate position, only this exact position is ever consulted.
         */
        SEAT_ANCHOR_INVALID("seat_anchor_invalid"),
        /**
         * The pilot seat anchor is valid (resolves to a real {@link PartRole#PILOT_SEAT} part),
         * but leaves an occupant with a standard eye height fully exposed above the hull -- no
         * adjacent block conceals them (REQ-007/AC-007, T08 remediation). Only reported once
         * {@link #SEAT_ANCHOR_INVALID} does not already apply -- an invalid/missing anchor has
         * nothing coherent to check visibility on top of.
         */
        COCKPIT_VISIBILITY_INSUFFICIENT("cockpit_visibility_insufficient");

        private final String id;

        Code(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public String translationKey() {
            return "assembly_issue.sharkengine." + id;
        }
    }

    /** Lowercase code id + args, for compact logging/debug output. */
    @Override
    public String toString() {
        return code.name().toLowerCase(Locale.ROOT) + args;
    }
}
