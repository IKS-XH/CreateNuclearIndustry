package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ControlRodDriveBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
            helper.succeed();
        });
    }

    private static void roundTrip(GameTestHelper helper, com.iksxh.create_nuclear_industry.blockentity.P1MinimalBlockEntity entity) {
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
