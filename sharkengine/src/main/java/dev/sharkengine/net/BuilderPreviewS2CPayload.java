package dev.sharkengine.net;

import dev.sharkengine.SharkEngineMod;
import dev.sharkengine.ship.part.AssemblyIssue;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record BuilderPreviewS2CPayload(BlockPos wheelPos,
                                       CompoundTag blueprintNbt,
                                       List<BlockPos> invalidBlocks,
                                       int contactPoints,
                                       boolean canAssemble,
                                       int thrusterCount,
                                       int coreNeighbors,
                                       int bugCount,
                                       List<AssemblyIssue> issues,
                                       boolean active,
                                       UUID sessionId) implements CustomPacketPayload {

    public static final Type<BuilderPreviewS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SharkEngineMod.MOD_ID, "builder_preview"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BuilderPreviewS2CPayload> CODEC =
            StreamCodec.of(BuilderPreviewS2CPayload::write, BuilderPreviewS2CPayload::read);

    /**
     * @param sessionId the REQ-003 {@code VehicleBuildSession} id bound to {@code wheelPos}, but
     *                   ONLY if the recipient player is that session's own owner (see {@code
     *                   dev.sharkengine.ship.BuildSessionGate#sessionIdForOwner} — a non-owner or
     *                   an absent session both produce {@code null} here, never someone else's
     *                   real id). The client must echo this back verbatim in {@link
     *                   BuilderAssembleC2SPayload} — the server never trusts a client-invented id.
     */
    public static BuilderPreviewS2CPayload open(BlockPos wheelPos,
                                                CompoundTag blueprintNbt,
                                                List<BlockPos> invalidBlocks,
                                                int contactPoints,
                                                boolean canAssemble,
                                                int thrusterCount,
                                                int coreNeighbors,
                                                int bugCount,
                                                List<AssemblyIssue> issues,
                                                UUID sessionId) {
        return new BuilderPreviewS2CPayload(wheelPos, blueprintNbt, List.copyOf(invalidBlocks),
                contactPoints, canAssemble, thrusterCount, coreNeighbors, bugCount,
                List.copyOf(issues), true, sessionId);
    }

    public static BuilderPreviewS2CPayload close() {
        return new BuilderPreviewS2CPayload(BlockPos.ZERO, null, List.of(), 0, false, 0, 0, 0, List.of(), false, null);
    }

    private static void write(RegistryFriendlyByteBuf buf, BuilderPreviewS2CPayload payload) {
        buf.writeBlockPos(payload.wheelPos);
        boolean hasBlueprint = payload.blueprintNbt != null;
        buf.writeBoolean(hasBlueprint);
        if (hasBlueprint) {
            buf.writeNbt(payload.blueprintNbt);
        }
        ByteBufCodecs.collection(ArrayList::new, BlockPos.STREAM_CODEC)
                .encode(buf, new ArrayList<>(payload.invalidBlocks()));
        buf.writeInt(payload.contactPoints());
        buf.writeBoolean(payload.canAssemble());
        buf.writeInt(payload.thrusterCount());
        buf.writeInt(payload.coreNeighbors());
        buf.writeInt(payload.bugCount());
        writeIssues(buf, payload.issues());
        buf.writeBoolean(payload.active());
        boolean hasSessionId = payload.sessionId() != null;
        buf.writeBoolean(hasSessionId);
        if (hasSessionId) {
            buf.writeUUID(payload.sessionId());
        }
    }

    private static BuilderPreviewS2CPayload read(RegistryFriendlyByteBuf buf) {
        BlockPos wheelPos = buf.readBlockPos();
        CompoundTag blueprint = null;
        boolean hasBlueprint = buf.readBoolean();
        if (hasBlueprint) {
            blueprint = buf.readNbt();
        }
        List<BlockPos> invalidBlocks = ByteBufCodecs.collection(ArrayList::new, BlockPos.STREAM_CODEC).decode(buf);
        int contacts = buf.readInt();
        boolean canAssemble = buf.readBoolean();
        int thrusterCount = buf.readInt();
        int coreNeighbors = buf.readInt();
        int bugCount = buf.readInt();
        List<AssemblyIssue> issues = readIssues(buf);
        boolean active = buf.readBoolean();
        UUID sessionId = buf.readBoolean() ? buf.readUUID() : null;
        return new BuilderPreviewS2CPayload(wheelPos, blueprint, invalidBlocks, contacts,
                canAssemble, thrusterCount, coreNeighbors, bugCount, issues, active, sessionId);
    }

    // ─── AssemblyIssue wire encoding (AIR-022, REQ-S3) ────────────────────────────────────
    //
    // AssemblyIssue itself carries no net.minecraft.network dependency (see its javadoc for
    // why — this repo's test source set cannot compile against net.minecraft.network.* at
    // all), so the codec for it lives here instead, next to this payload's other manually
    // encoded fields (invalidBlocks above uses the same ByteBufCodecs.collection pattern for
    // a type — BlockPos — that IS network-safe to reference from a plain unit test).

    private static void writeIssues(FriendlyByteBuf buf, List<AssemblyIssue> issues) {
        buf.writeVarInt(issues.size());
        for (AssemblyIssue issue : issues) {
            buf.writeEnum(issue.code());
            boolean hasPos = issue.pos() != null;
            buf.writeBoolean(hasPos);
            if (hasPos) {
                buf.writeBlockPos(issue.pos());
            }
            buf.writeVarInt(issue.args().size());
            for (int arg : issue.args()) {
                buf.writeVarInt(arg);
            }
        }
    }

    private static List<AssemblyIssue> readIssues(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<AssemblyIssue> issues = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            AssemblyIssue.Code code = buf.readEnum(AssemblyIssue.Code.class);
            BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
            int argCount = buf.readVarInt();
            List<Integer> args = new ArrayList<>(argCount);
            for (int j = 0; j < argCount; j++) {
                args.add(buf.readVarInt());
            }
            issues.add(new AssemblyIssue(code, pos, args));
        }
        return issues;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
