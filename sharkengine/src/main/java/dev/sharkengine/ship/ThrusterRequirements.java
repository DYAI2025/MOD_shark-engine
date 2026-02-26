package dev.sharkengine.ship;

import dev.sharkengine.SharkEngineMod;

import java.util.Collection;

final class ThrusterRequirements {
    private static final String THRUSTER_ID = SharkEngineMod.MOD_ID + ":thruster";

    private ThrusterRequirements() {}

    static int countThrusters(Collection<String> blockIds) {
        int count = 0;
        if (blockIds == null) {
            return 0;
        }
        for (String id : blockIds) {
            if (THRUSTER_ID.equals(id)) {
                count++;
            }
        }
        return count;
    }

    static boolean hasThruster(Collection<String> blockIds) {
        return countThrusters(blockIds) > 0;
    }
}
