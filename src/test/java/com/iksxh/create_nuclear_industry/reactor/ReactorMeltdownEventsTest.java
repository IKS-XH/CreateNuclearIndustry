package com.iksxh.create_nuclear_industry.reactor;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证融毁发布器真实接收客户端 Level 时拒绝发布且不进入事件总线。 */
class ReactorMeltdownEventsTest {
    @Test
    void clientLevelCannotPublishOrMutateTheEventBus() {
        Level clientLevel = uninitializedClientLevel();
        ReactorSnapshot before = ReactorSnapshot.singleFuelColumn(
                new CoreColumnPosition(0, 0),
                new FuelColumnState(FuelAssemblyState.installed(216_000, 0), 0.0D, 3.0D)
        );
        AtomicInteger delivered = new AtomicInteger();
        NeoForge.EVENT_BUS.addListener(
                ReactorMeltdownEvent.class, ignored -> delivered.incrementAndGet());

        boolean published = ReactorMeltdownEvents.publish(
                ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE,
                clientLevel,
                new BlockPos(10, 64, 10),
                new BlockPos(12, 66, 10),
                before
        );

        assertTrue(clientLevel instanceof ClientLevel);
        assertFalse(published);
        assertEquals(0, delivered.get());
        assertEquals(0.0D,
                before.fuelColumns().get(new CoreColumnPosition(0, 0)).integrity(), 1.0E-12D);
        assertEquals(3.0D,
                before.fuelColumns().get(new CoreColumnPosition(0, 0)).cachedHeatHu(), 1.0E-12D);
    }

    @Test
    void clientLevelCannotCommitDangerousDisassembly() {
        ClientLevel clientLevel = uninitializedClientLevel();
        ReactorSnapshot before = ReactorSnapshot.singleFuelColumn(
                new CoreColumnPosition(0, 0),
                new FuelColumnState(FuelAssemblyState.installed(216_000, 0), 0.0D, 3.0D)
        );
        ReactorInstrumentPortBlockEntity instrument = uninitializedInstrument(clientLevel, before);
        AtomicInteger delivered = new AtomicInteger();
        NeoForge.EVENT_BUS.addListener(ReactorMeltdownEvent.class, event -> {
            if (event.reason() == ReactorMeltdownEvent.Reason.DANGEROUS_DISASSEMBLY) {
                delivered.incrementAndGet();
            }
        });

        boolean committed = instrument.tryCommitDangerousDisassembly(
                new BlockPos(10, 64, 10), Blocks.STONE.defaultBlockState());

        assertTrue(clientLevel instanceof ClientLevel);
        assertFalse(committed);
        assertEquals(0, delivered.get());
        assertEquals(before, instrument.snapshot());
    }

    /** 创建真实 ClientLevel 类型但不启动客户端世界，发布器会在服务端类型检查处短路。 */
    private static ClientLevel uninitializedClientLevel() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            ClientLevel level = (ClientLevel) ((Unsafe) field.get(null)).allocateInstance(ClientLevel.class);
            Field clientSide = Level.class.getDeclaredField("isClientSide");
            clientSide.setAccessible(true);
            clientSide.setBoolean(level, true);
            return level;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("无法构造客户端 Level 测试对象", exception);
        }
    }

    /** 创建只初始化客户端边界所需字段的仪表端口，直接覆盖生产危险拆除入口。 */
    private static ReactorInstrumentPortBlockEntity uninitializedInstrument(
            ClientLevel level,
            ReactorSnapshot snapshot
    ) {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            ReactorInstrumentPortBlockEntity instrument = (ReactorInstrumentPortBlockEntity)
                    ((Unsafe) field.get(null)).allocateInstance(ReactorInstrumentPortBlockEntity.class);
            Field levelField = BlockEntity.class.getDeclaredField("level");
            levelField.setAccessible(true);
            levelField.set(instrument, level);
            Field snapshotField = ReactorInstrumentPortBlockEntity.class.getDeclaredField("snapshot");
            snapshotField.setAccessible(true);
            snapshotField.set(instrument, snapshot);
            return instrument;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("无法构造客户端危险拆除测试仪表端口", exception);
        }
    }
}
