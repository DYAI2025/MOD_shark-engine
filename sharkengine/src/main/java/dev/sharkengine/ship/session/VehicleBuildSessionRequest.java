package dev.sharkengine.ship.session;

import java.util.UUID;

/**
 * The server-authoritative "claims" a C2S selection/assembly request is checked against
 * (REQ-003/AC-003).
 *
 * <p>Every field here comes from server-trusted state at the moment the request is handled —
 * {@code requestingPlayerId} from the {@code ServerPlayer} object itself, {@code
 * requestingDimensionId}/{@code requestingPlayerPos} from that player's <em>current</em> level and
 * position (never from client-supplied payload fields) — except {@code providedSessionId}, which
 * IS client-supplied and is exactly the field the "wrong/absent session id" axis exists to
 * distrust.</p>
 */
public record VehicleBuildSessionRequest(
        UUID requestingPlayerId,
        String requestingDimensionId,
        BlockCoord requestingPlayerPos,
        UUID providedSessionId,
        long nowMillis
) {}
