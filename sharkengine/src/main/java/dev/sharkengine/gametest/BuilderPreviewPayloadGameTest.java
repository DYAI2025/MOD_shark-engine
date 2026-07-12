package dev.sharkengine.gametest;

import dev.sharkengine.net.BuilderPreviewS2CPayload;
import dev.sharkengine.ship.part.AssemblyIssue;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.List;
import java.util.Objects;

/**
 * AIR-022 (REQ-S3): {@link BuilderPreviewS2CPayload}'s wire encoding for the new
 * {@code issues} field (a {@code List<AssemblyIssue>}) roundtrips through
 * {@link BuilderPreviewS2CPayload#CODEC} unchanged.
 *
 * <p>Run as a GameTest, not a plain unit test: encoding requires
 * {@code RegistryFriendlyByteBuf}/{@code RegistryAccess}, and this repo's {@code test} source
 * set cannot compile against {@code net.minecraft.network.*} at all (confirmed empirically —
 * see {@code AssemblyIssue}'s javadoc). This mirrors the established pattern from AIR-015's
 * {@link BlueprintPersistenceGameTest} for the same class of problem.</p>
 */
public final class BuilderPreviewPayloadGameTest implements FabricGameTest {

    @GameTest(template = EMPTY_STRUCTURE)
    public void roundtripsIssuesList(GameTestHelper helper) {
        BlockPos wheelPos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos bugPos = helper.absolutePos(new BlockPos(1, 1, 0));
        List<AssemblyIssue> issues = List.of(
                AssemblyIssue.of(AssemblyIssue.Code.TOO_FEW_CORE_NEIGHBORS, 2),
                AssemblyIssue.of(AssemblyIssue.Code.NO_PROPULSION),
                AssemblyIssue.of(AssemblyIssue.Code.BUG_INSIDE, bugPos)
        );
        BuilderPreviewS2CPayload original = BuilderPreviewS2CPayload.open(
                wheelPos, new CompoundTag(), List.of(), 0, false, 0, 2, 1, issues);

        BuilderPreviewS2CPayload decoded = roundtrip(helper, original);

        if (!Objects.equals(decoded.issues(), issues)) {
            helper.fail("expected issues=" + issues + " to roundtrip unchanged, got " + decoded.issues());
            return;
        }
        if (decoded.issues().size() != 3) {
            helper.fail("expected 3 issues after roundtrip, got " + decoded.issues().size());
            return;
        }
        AssemblyIssue withPos = decoded.issues().get(2);
        if (!bugPos.equals(withPos.pos())) {
            helper.fail("expected BUG_INSIDE issue's pos to roundtrip as " + bugPos + ", got " + withPos.pos());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void roundtripsEmptyIssuesList(GameTestHelper helper) {
        BuilderPreviewS2CPayload original = BuilderPreviewS2CPayload.close();

        BuilderPreviewS2CPayload decoded = roundtrip(helper, original);

        if (!decoded.issues().isEmpty()) {
            helper.fail("expected empty issues list to roundtrip empty, got " + decoded.issues());
            return;
        }
        if (decoded.active()) {
            helper.fail("expected active=false to roundtrip false");
            return;
        }
        helper.succeed();
    }

    private static BuilderPreviewS2CPayload roundtrip(GameTestHelper helper, BuilderPreviewS2CPayload payload) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        BuilderPreviewS2CPayload.CODEC.encode(buf, payload);
        return BuilderPreviewS2CPayload.CODEC.decode(buf);
    }
}
