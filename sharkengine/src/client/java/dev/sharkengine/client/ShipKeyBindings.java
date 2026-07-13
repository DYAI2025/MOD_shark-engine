package dev.sharkengine.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Custom key bindings for ship control not covered by reusing vanilla's own
 * keys (movement/jump/sneak).
 *
 * <p><b>Why descend needs its own key (2026-07-13 live playtest report):</b>
 * climb reuses vanilla's Jump key with no issue, but descend used to reuse
 * vanilla's sneak key ({@code Options.keyShift}) — and vanilla auto-dismounts
 * a riding player the instant they press their sneak key, independent of
 * anything this mod does. Since many players rebind sneak away from the
 * default Left Shift (this report's user has it on Left Ctrl), the practical
 * symptom was "pressing my sneak key to descend instead ejects me from the
 * ship" for whatever key sneak happened to be bound to. Reading a dedicated
 * {@link KeyMapping} for descend instead of {@code Options.keyShift} fixes
 * this without touching vanilla's dismount-on-sneak behavior at all — the
 * player just no longer needs to press sneak to descend.</p>
 *
 * <p>Defaults to Left Alt: unbound by vanilla, and not already used anywhere
 * else in this mod. Fully rebindable in Controls options like any other
 * {@link KeyMapping} (registered via {@link KeyBindingHelper}, not a raw
 * hardcoded key check).</p>
 */
public final class ShipKeyBindings {

    public static final KeyMapping DESCEND = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.sharkengine.descend",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            "key.categories.sharkengine"
    ));

    private ShipKeyBindings() {}

    /** Forces class-loading (and therefore the static registration above) at a well-defined point. */
    public static void init() {
    }
}
