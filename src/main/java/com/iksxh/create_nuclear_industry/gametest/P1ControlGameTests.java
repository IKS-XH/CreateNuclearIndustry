package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ControlRodDriveBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderPayload;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderResponsePayloadFactory;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderService;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderStatus;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1ControlGameTests {
    private static final BlockPos DRIVE = new BlockPos(2, 4, 2);
    private static final CoreColumnPosition CONTROL_COLUMN = new CoreColumnPosition(1, 1);
    private static final String TEMPLATE = "p0_probe_empty";

    private P1ControlGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void controlRodSliderStartPreviewAndCommitAreServerAuthoritative(GameTestHelper helper) {
        buildControlRodStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ControlRodDriveBlockEntity drive = drive(helper);
            require(helper, instrument.snapshot().controlRodColumns().get(CONTROL_COLUMN).targetDepth() == 1.0D,
                    "newly formed control rod did not default to fully inserted");
            require(helper, drive.slider().getValue() == 100,
                    "newly formed control rod drive did not open at 100%");
            CompoundTag initialUpdate = drive.getUpdateTag(helper.getLevel().registryAccess());
            require(helper, initialUpdate.getInt("ScrollValue") == 100,
                    "initial authoritative 100% was not included in the client update payload");

            Player player = playerAtDrive(helper);
            ControlRodSliderPayload start = ControlRodSliderPayload.start(
                    helper.absolutePos(DRIVE), 1, 1, 100, 41L);
            var started = ControlRodSliderService.handle(player, start);
            require(helper, started.status() == ControlRodSliderStatus.ACCEPTED,
                    "valid drag start was rejected: " + started.reason());
            require(helper, instrument.snapshot().controlRodColumns().get(CONTROL_COLUMN).targetDepth() == 1.0D,
                    "drag start changed the authoritative target");

            var previewed = ControlRodSliderService.handle(player, ControlRodSliderPayload.preview(
                    helper.absolutePos(DRIVE), 1, 1, 60, 41L));
            require(helper, previewed.status() == ControlRodSliderStatus.ACCEPTED,
                    "valid drag preview was rejected: " + previewed.reason());
            require(helper, previewed.depthPercent() == 60,
                    "drag preview did not return the real-time insertion percentage");
            require(helper, instrument.snapshot().controlRodColumns().get(CONTROL_COLUMN).targetDepth() == 1.0D,
                    "drag preview wrote the authoritative snapshot");

            var committed = ControlRodSliderService.handle(player, ControlRodSliderPayload.commit(
                    helper.absolutePos(DRIVE), 1, 1, 60, 41L));
            require(helper, committed.status() == ControlRodSliderStatus.ACCEPTED,
                    "valid drag commit was rejected: " + committed.reason());
            require(helper, instrument.snapshot().controlRodColumns().get(CONTROL_COLUMN).targetDepth() == 0.60D,
                    "drag commit did not update the instrument snapshot");
            require(helper, drive.slider().getValue() == 60,
                    "server-approved target was not mirrored to the Create display cache");

            CompoundTag reopenedUpdate = drive.getUpdateTag(helper.getLevel().registryAccess());
            require(helper, reopenedUpdate.getInt("ScrollValue") == 60,
                    "reopening or another client would not receive the committed 60% value");
            ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(), instrument.getBlockPos());
            require(helper, instrument.snapshot().controlRodColumns().get(CONTROL_COLUMN).targetDepth() == 0.60D,
                    "structure rescan changed the committed authoritative target");
            require(helper, drive.slider().getValue() == 60,
                    "structure rescan did not restore the authoritative 60% display cache");

            Player secondPlayer = playerAtDrive(helper);
            var secondPlayerView = ControlRodSliderService.handle(secondPlayer, ControlRodSliderPayload.start(
                    helper.absolutePos(DRIVE), 1, 1, 60, 42L));
            require(helper, secondPlayerView.accepted() && secondPlayerView.depthPercent() == 60,
                    "a second player did not receive the authoritative 60% target");
            var secondPlayerCommit = ControlRodSliderService.commitFromCreate(
                    secondPlayer, helper.absolutePos(DRIVE), 0, 60);
            require(helper, secondPlayerCommit.accepted() && instrument.snapshot()
                            .controlRodColumns().get(CONTROL_COLUMN).targetDepth() == 0.60D,
                    "Create's final value-setting commit did not preserve the authoritative 60% target");
            ControlRodSliderService.clearSession(secondPlayer);

            CompoundTag savedInstrument = instrument.saveForServerTest(helper.getLevel().registryAccess());
            ReactorInstrumentPortBlockEntity reloadedInstrument = new ReactorInstrumentPortBlockEntity(
                    instrument.getBlockPos(), instrument.getBlockState());
            reloadedInstrument.loadForServerTest(savedInstrument, helper.getLevel().registryAccess());
            require(helper, reloadedInstrument.snapshot().controlRodColumns().get(CONTROL_COLUMN).targetDepth() == 0.60D,
                    "relogin/server reload did not preserve the committed target");

            var savedDrive = drive.saveForServerTest(helper.getLevel().registryAccess());
            require(helper, !savedDrive.contains("ScrollValue"),
                    "control rod drive persisted a second target-depth copy");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void controlRodSliderRejectsIllegalDataAndClearsDisconnectedDrag(GameTestHelper helper) {
        buildControlRodStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            instrument.setSnapshot(snapshot(0.40D, false));
            Player player = playerAtDrive(helper);
            BlockPos absoluteDrive = helper.absolutePos(DRIVE);

            var invalidDepth = ControlRodSliderService.handle(player, new ControlRodSliderPayload(
                    absoluteDrive, 1, 1, 0, 101, 0, 55L));
            require(helper, invalidDepth.status() == ControlRodSliderStatus.INVALID_RANGE,
                    "out-of-range depth was not rejected");
            require(helper, instrument.snapshot().controlRodColumns().get(CONTROL_COLUMN).targetDepth() == 0.40D,
                    "invalid depth changed the authoritative target");
            require(helper, invalidDepth.authoritativeDepth() && invalidDepth.depthPercent() == 40,
                    "rejected request did not carry the authoritative rollback value");
            var rollbackResponse = ControlRodSliderResponsePayloadFactory.from(absoluteDrive, invalidDepth);
            require(helper, rollbackResponse.hasAuthoritativeDepth() && rollbackResponse.depthPercent() == 40,
                    "client rejection response did not mark the authoritative rollback value");
            require(helper, drive(helper).slider().getValue() == 40,
                    "rejected request left the server display cache at a non-authoritative value");

            var invalidRow = ControlRodSliderService.handle(player, new ControlRodSliderPayload(
                    absoluteDrive, 1, 1, 1, 50, 0, 55L));
            require(helper, invalidRow.status() == ControlRodSliderStatus.INVALID_ROW,
                    "invalid slider row was not rejected");

            player.setPos(absoluteDrive.getX() + 100.5D, absoluteDrive.getY() + 0.5D,
                    absoluteDrive.getZ() + 100.5D);
            var outOfReach = ControlRodSliderService.handle(player, ControlRodSliderPayload.start(
                    absoluteDrive, 1, 1, 50, 56L));
            require(helper, outOfReach.status() == ControlRodSliderStatus.OUT_OF_REACH,
                    "out-of-reach player data was not rejected");
            player.setPos(absoluteDrive.getX() + 0.5D, absoluteDrive.getY() + 0.5D,
                    absoluteDrive.getZ() + 2.0D);

            var invalidColumn = ControlRodSliderService.handle(player, ControlRodSliderPayload.start(
                    absoluteDrive, 0, 0, 50, 57L));
            require(helper, invalidColumn.status() == ControlRodSliderStatus.INVALID_COLUMN,
                    "mismatched control-rod number was not rejected");

            var started = ControlRodSliderService.handle(player, ControlRodSliderPayload.start(
                    absoluteDrive, 1, 1, 40, 58L));
            require(helper, started.accepted(), "valid drag did not start before disconnect test");
            ControlRodSliderService.clearSession(player);
            require(helper, ControlRodSliderService.activeSessionCount() == 0,
                    "logout cleanup did not clear the transient drag session");
            var staleCommit = ControlRodSliderService.handle(player, ControlRodSliderPayload.commit(
                    absoluteDrive, 1, 1, 80, 58L));
            require(helper, staleCommit.status() == ControlRodSliderStatus.STALE_SESSION,
                    "commit from a disconnected drag was accepted");
            require(helper, instrument.snapshot().controlRodColumns().get(CONTROL_COLUMN).targetDepth() == 0.40D,
                    "stale commit changed the authoritative target");

            instrument.setSnapshot(snapshot(0.40D, true));
            var jammed = ControlRodSliderService.handle(player, ControlRodSliderPayload.commit(
                    absoluteDrive, 1, 1, 20, 0L));
            require(helper, jammed.status() == ControlRodSliderStatus.INVALID_STATE,
                    "jammed control rod accepted a slider commit");
            helper.succeed();
        });
    }

    private static void buildControlRodStructure(GameTestHelper helper) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.canonicalTemplate().entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
        helper.setBlock(DRIVE, P1Blocks.CONTROL_ROD_DRIVE.get().defaultBlockState());
    }

    private static Block blockForId(String id) {
        return switch (id) {
            case "minecraft:air" -> Blocks.AIR;
            case "create_nuclear_industry:reactor_casing" -> P1Blocks.REACTOR_CASING.get();
            case "create_nuclear_industry:reactor_window" -> P1Blocks.REACTOR_WINDOW.get();
            case "create_nuclear_industry:reactor_instrument_port" -> P1Blocks.REACTOR_INSTRUMENT_PORT.get();
            case "create_nuclear_industry:reactor_cold_port" -> P1Blocks.REACTOR_COLD_PORT.get();
            case "create_nuclear_industry:reactor_hot_port" -> P1Blocks.REACTOR_HOT_PORT.get();
            case "create_nuclear_industry:reactor_refueling_port" -> P1Blocks.REACTOR_REFUELING_PORT.get();
            case "create_nuclear_industry:reactor_fuel_rod" -> P1Blocks.REACTOR_FUEL_ROD.get();
            case "create_nuclear_industry:control_rod_drive" -> P1Blocks.CONTROL_ROD_DRIVE.get();
            default -> throw new IllegalArgumentException("unknown canonical structure block " + id);
        };
    }

    private static ReactorSnapshot snapshot(double targetDepth, boolean jammed) {
        return ReactorSnapshot.singleControlRodColumn(
                CONTROL_COLUMN,
                new ControlRodColumnState(1.0D, targetDepth, targetDepth, jammed, 0.0D));
    }

    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var entity = helper.getBlockEntity(new BlockPos(2, 2, 0));
        require(helper, entity instanceof ReactorInstrumentPortBlockEntity,
                "instrument port block entity was not created");
        return (ReactorInstrumentPortBlockEntity) entity;
    }

    private static ControlRodDriveBlockEntity drive(GameTestHelper helper) {
        var entity = helper.getBlockEntity(DRIVE);
        require(helper, entity instanceof ControlRodDriveBlockEntity,
                "control rod drive block entity was not created");
        return (ControlRodDriveBlockEntity) entity;
    }

    private static Player playerAtDrive(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos absolute = helper.absolutePos(DRIVE);
        player.setPos(absolute.getX() + 0.5D, absolute.getY() + 0.5D, absolute.getZ() + 2.0D);
        return player;
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
