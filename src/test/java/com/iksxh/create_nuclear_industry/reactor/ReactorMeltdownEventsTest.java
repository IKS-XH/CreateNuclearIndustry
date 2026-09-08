package com.iksxh.create_nuclear_industry.reactor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
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

    /** 创建真实 ClientLevel 类型但不启动客户端世界，发布器会在服务端类型检查处短路。 */
    private static Level uninitializedClientLevel() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Level) ((Unsafe) field.get(null)).allocateInstance(ClientLevel.class);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("无法构造客户端 Level 测试对象", exception);
        }
    }
}
