package com.iksxh.create_nuclear_industry.industrial;

import static org.junit.jupiter.api.Assertions.*;
import com.iksxh.create_nuclear_industry.boiler.BoilerState;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

/** 冷热和水汽必须使用不同额度，同时保留物理口共享消费与模拟零写。 */
class IndustrialPortRateTest {
    @Test void boilerDefaultSeparatesWaterAndHotPhysicalPorts() {
        var state = new BoilerState(); state.setGeometry(9, 9, 8, 8, 4000, 4000);
        assertEquals(4096, state.fillWater(BlockPos.ZERO, 20000, true, 1));
        assertEquals(2048, state.fillHot(BlockPos.ZERO.east(), 20000, true, 1));
        assertEquals(0, state.water()); assertEquals(0, state.hot());
        assertEquals(1024, state.fillWater(BlockPos.ZERO, 1024, false, 1));
        assertEquals(3072, state.fillWater(BlockPos.ZERO, 20000, false, 1));
        assertEquals(2048, state.fillHot(BlockPos.ZERO.east(), 20000, false, 1));
        assertEquals(0, state.fillHot(BlockPos.ZERO.east(), 1, false, 1));
    }
}
