package dev.sharkengine.ship;

import net.minecraft.util.StringRepresentable;

/**
 * REQ-019/T21: the 17-case trail-color domain — 16 dye colors plus {@link #NONE} (the existing
 * default trail). ONE type, ONE rgb-resolution path for every case (AC-019: "one code path
 * handles 17 cases, not 17 separate paths"); doubles as the thruster's BlockState property
 * value, so the color survives placement → blueprint → entity NBT on the existing pipelines
 * with zero extra schema.
 *
 * <p>RGB values are the vanilla dye texture-diffuse colors, locked by {@code TintProviderTest}.
 * Resolution is keyed by the dye's serialized NAME (a primitive {@code String}) rather than the
 * Minecraft {@code DyeColor} type — the T20 Preconditions-§4 decision, keeping this enum fully
 * unit-testable with only the trivial {@code StringRepresentable} stub.</p>
 */
public enum TrailColor implements StringRepresentable {
    NONE("none", 0xFFFFFF),
    WHITE("white", 0xF9FFFE),
    ORANGE("orange", 0xF9801D),
    MAGENTA("magenta", 0xC74EBD),
    LIGHT_BLUE("light_blue", 0x3AB3DA),
    YELLOW("yellow", 0xFED83D),
    LIME("lime", 0x80C71F),
    PINK("pink", 0xF38BAA),
    GRAY("gray", 0x474F52),
    LIGHT_GRAY("light_gray", 0x9D9D97),
    CYAN("cyan", 0x169C9C),
    PURPLE("purple", 0x8932B8),
    BLUE("blue", 0x3C44AA),
    BROWN("brown", 0x835432),
    GREEN("green", 0x5E7C16),
    RED("red", 0xB02E26),
    BLACK("black", 0x1D1D21);

    private final String serializedName;
    private final int rgb;

    TrailColor(String serializedName, int rgb) {
        this.serializedName = serializedName;
        this.rgb = rgb;
    }

    /**
     * The single tint-resolution entry point: dye name → trail color; {@code null} or unknown
     * names conservatively resolve to {@link #NONE} (default trail, never a guessed color).
     */
    public static TrailColor fromDyeName(String dyeName) {
        if (dyeName == null) {
            return NONE;
        }
        for (TrailColor color : values()) {
            if (color != NONE && color.serializedName.equals(dyeName)) {
                return color;
            }
        }
        return NONE;
    }

    public boolean isColored() {
        return this != NONE;
    }

    public int rgb() {
        return rgb;
    }

    public float red() {
        return ((rgb >> 16) & 0xFF) / 255.0f;
    }

    public float green() {
        return ((rgb >> 8) & 0xFF) / 255.0f;
    }

    public float blue() {
        return (rgb & 0xFF) / 255.0f;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
