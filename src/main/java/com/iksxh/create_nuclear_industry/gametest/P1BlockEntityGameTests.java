package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ControlRodDriveBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.P1MinimalBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyState;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1BlockEntityGameTests {
    private static final String TEMPLATE = "p0_probe_empty";

    private P1BlockEntityGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void formalBlockEntitiesLoadAndRoundTripOnDedicatedServer(GameTestHelper helper) {
        BlockPos instrument = new BlockPos(1, 1, 1);
        BlockPos cold = new BlockPos(2, 1, 1);
        BlockPos hot = new BlockPos(3, 1, 1);
        BlockPos refueling = new BlockPos(4, 1, 1);
        BlockPos drive = new BlockPos(1, 1, 2);

        helper.setBlock(instrument, P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
        helper.setBlock(cold, P1Blocks.REACTOR_COLD_PORT.get().defaultBlockState());
        helper.setBlock(hot, P1Blocks.REACTOR_HOT_PORT.get().defaultBlockState());
        helper.setBlock(refueling, P1Blocks.REACTOR_REFUELING_PORT.get().defaultBlockState());
        helper.setBlock(drive, P1Blocks.CONTROL_ROD_DRIVE.get().defaultBlockState());

        helper.runAfterDelay(2, () -> {
            ReactorInstrumentPortBlockEntity instrumentEntity = require(
                    helper, instrument, ReactorInstrumentPortBlockEntity.class);
            ReactorPortBlockEntity coldEntity = require(helper, cold, ReactorPortBlockEntity.class);
            ReactorPortBlockEntity hotEntity = require(helper, hot, ReactorPortBlockEntity.class);
            ReactorPortBlockEntity refuelingEntity = require(helper, refueling, ReactorPortBlockEntity.class);
            ControlRodDriveBlockEntity driveEntity = require(helper, drive, ControlRodDriveBlockEntity.class);

            roundTrip(helper, instrumentEntity);
            roundTrip(helper, coldEntity);
            roundTrip(helper, hotEntity);
            roundTrip(helper, refuelingEntity);
            roundTrip(helper, driveEntity);

            ReactorSnapshot expected = fixtureSnapshot();
            instrumentEntity.setSnapshot(expected);
            require(helper, instrumentEntity.snapshot().equals(expected),
                    "instrument port did not expose its server snapshot");

            CompoundTag saved = instrumentEntity.saveForServerTest(helper.getLevel().registryAccess());
            require(helper, saved.contains("ReactorSnapshot"),
                    "instrument port did not persist the reactor snapshot");
            require(helper, saved.getCompound("ReactorSnapshot").getInt("FormatVersion") == 1,
                    "instrument port persisted an unexpected snapshot format");

            ReactorInstrumentPortBlockEntity reloaded = new ReactorInstrumentPortBlockEntity(
                    helper.absolutePos(instrument),
                    P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
            reloaded.loadForServerTest(saved, helper.getLevel().registryAccess());
            require(helper, reloaded.snapshot().equals(expected),
                    "instrument port snapshot did not survive a server reload round-trip");

            CompoundTag updateTag = instrumentEntity.getUpdateTag(helper.getLevel().registryAccess());
            instrumentEntity.setSnapshot(ReactorSnapshot.empty());
            instrumentEntity.handleUpdateTag(updateTag, helper.getLevel().registryAccess());
            require(helper, instrumentEntity.snapshot().equals(expected),
                    "instrument port snapshot did not restore from its update tag");

            require(helper, coldEntity.readAuthoritativeSnapshot(instrumentEntity) == instrumentEntity.snapshot(),
                    "cold port did not read the instrument owner");
            require(helper, hotEntity.readAuthoritativeSnapshot(instrumentEntity) == instrumentEntity.snapshot(),
                    "hot port did not read the instrument owner");
            require(helper, refuelingEntity.readAuthoritativeSnapshot(instrumentEntity) == instrumentEntity.snapshot(),
                    "refueling port did not read the instrument owner");
            require(helper, driveEntity.readAuthoritativeSnapshot(instrumentEntity) == instrumentEntity.snapshot(),
                    "control rod drive did not read the instrument owner");
            helper.succeed();
        });
    }

    private static ReactorSnapshot fixtureSnapshot() {
        return new ReactorSnapshot(
                Map.of(
                        new CoreColumnPosition(0, 0),
                        new FuelColumnState(FuelAssemblyState.installed(216_000, 12_345), 0.72D, 18.5D)
                ),
                Map.of(
                        new CoreColumnPosition(1, 1),
                        new ControlRodColumnState(0.91D, 0.63D, 0.48D, false, 4.25D)
                ),
                4_096L,
                512L,
                27L,
                true
        );
    }

    private static void roundTrip(GameTestHelper helper, P1MinimalBlockEntity entity) {
        CompoundTag saved = entity.saveForServerTest(helper.getLevel().registryAccess());
        require(helper, saved.getInt("P1DataVersion") == 1,
                "P1 block entity did not write its persistence version");
        entity.loadForServerTest(saved, helper.getLevel().registryAccess());
    }

    private static <T extends BlockEntity> T require(GameTestHelper helper, BlockPos pos, Class<T> type) {
        BlockEntity blockEntity = helper.getBlockEntity(pos);
        require(helper, type.isInstance(blockEntity),
                "Expected " + type.getSimpleName() + " at " + pos + ", got " + blockEntity);
        return type.cast(blockEntity);
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
