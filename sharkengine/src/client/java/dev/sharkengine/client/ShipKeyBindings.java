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
 * <p>Defaults to <b>Left Control</b> (changed 2026-07-26 on user request; was Left Alt).
 * Rationale: players arrive from vanilla creative flight, where Space/Shift is the
 * up/down pair — and reaching for Shift here EJECTS them. Left Control sits where the
 * hand already is and is the least surprising descend key.</p>
 *
 * <p><b>Known, accepted overlap:</b> vanilla binds Left Control to {@code key.sprint}.
 * The Controls screen will therefore show a red conflict marker. It is cosmetic: sprint
 * has no effect while riding a vehicle, which is the only situation this binding is read
 * in ({@code HelmInputClient} only sends helm input when the player's vehicle is a
 * {@code ShipEntity}). Rebindable like any other {@link KeyMapping} if a player dislikes it.</p>
 *
 * <p>Dismount stays on vanilla sneak (Shift). Tab was considered and rejected — vanilla
 * binds it to {@code key.playerlist}, and moving dismount off sneak would additionally
 * require suppressing vanilla's dismount-on-sneak and reworking the controller dismount
 * path, which routes through {@code player.input.shiftKeyDown}.</p>
 */
public final class ShipKeyBindings {

    public static final KeyMapping DESCEND = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.sharkengine.descend",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            "key.categories.sharkengine"
    ));

    private ShipKeyBindings() {}

    /** Forces class-loading (and therefore the static registration above) at a well-defined point. */
    public static void init() {
    }
}
