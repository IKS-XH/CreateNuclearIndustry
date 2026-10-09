package com.iksxh.create_nuclear_industry.structure;

import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 完整信封复制及坏包拒收；传输预算不改变 P1 成型尺寸。 */
class ReactorRuntimeDescriptorTest {
    @Test void immutableRoundTripAndIndependentNbt() {
        var d = ReactorRuntimeProjectionTest.sample(1,1);
        assertEquals(d,ReactorRuntimeDescriptor.decode(d.encode()).orElseThrow());
        assertThrows(UnsupportedOperationException.class,()->d.columns().clear());
        assertThrows(UnsupportedOperationException.class,()->d.columns().getFirst().bodyPositions().clear());
        var tag=d.encode(); tag.putLong("Cold",999); assertNotEquals(999,d.coldCoolantMb());
    }
    @Test void wrongTypesMissingNonFiniteAndDuplicateGeometryFailClosed() {
        var valid=ReactorRuntimeProjectionTest.sample(1,1).encode();
        for(String key:java.util.List.of("Cold","Sample","Available","Owner","Columns")) {
            var bad=valid.copy(); bad.putString(key,"bad"); assertTrue(ReactorRuntimeDescriptor.decode(bad).isEmpty(),key);
        }
        var nan=valid.copy(); nan.putDouble("Converted",Double.NaN); assertTrue(ReactorRuntimeDescriptor.decode(nan).isEmpty());
        var missing=valid.copy(); missing.remove("GeometryRevision"); assertTrue(ReactorRuntimeDescriptor.decode(missing).isEmpty());
        var duplicate=valid.copy(); var list=duplicate.getList("Columns",10); list.set(1,list.getCompound(0).copy());
        assertTrue(ReactorRuntimeDescriptor.decode(duplicate).isEmpty());
        var body=valid.copy(); var positions=body.getList("Columns",10).getCompound(0).getList("Body",10);
        positions.set(1,positions.getCompound(0).copy()); assertTrue(ReactorRuntimeDescriptor.decode(body).isEmpty());
        var wrongId=valid.copy(); wrongId.getList("Columns",10).getCompound(0).putString("BodyId","minecraft:stone");
        assertTrue(ReactorRuntimeDescriptor.decode(wrongId).isEmpty());
    }
    @Test void geometryBudgetAndListBudgetRejectBeforeAllocation() {
        var huge=ReactorRuntimeProjectionTest.sample(1,1).encode(); huge.putIntArray("Max",new int[]{1000,74,18});
        assertTrue(ReactorRuntimeDescriptor.decode(huge).isEmpty());
        var list=new ListTag(); for(int i=0;i<=ReactorRuntimeDescriptor.MAX_COLUMNS;i++) list.add(new CompoundTag());
        huge.put("Columns",list); assertTrue(ReactorRuntimeDescriptor.decode(huge).isEmpty());
        assertTrue(ReactorRuntimeDescriptor.decode(new CompoundTag()).isEmpty());
    }
}
