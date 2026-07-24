package dev.sharkengine.ship.session;

import java.util.EnumSet;
import java.util.Set;

/**
 * Result of {@link VehicleBuildSessionValidator#validate}: the complete, independently-evaluated
 * set of failing axes (REQ-003/AC-003). Empty means fully authorized.
 */
public record VehicleBuildSessionValidation(Set<VehicleBuildSessionRejectionReason> reasons) {

    public static final VehicleBuildSessionValidation VALID =
            new VehicleBuildSessionValidation(EnumSet.noneOf(VehicleBuildSessionRejectionReason.class));

    public boolean isValid() {
        return reasons.isEmpty();
    }

    public boolean rejectedFor(VehicleBuildSessionRejectionReason reason) {
        return reasons.contains(reason);
    }
}
