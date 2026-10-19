package com.iksxh.create_nuclear_industry.boiler;

import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 不变的锅炉几何/弱注册表不应反复分配列表，回收后也不能保留强owner快照。 */
class BoilerStructureAllocationTest {
    private BoilerStructure.Form form() {
        return new BoilerStructure.Form(BlockPos.ZERO,new BlockPos(4,4,4),2,9,9,List.of(),List.of(),
            List.of(new BlockPos(1,0,0)),List.of(new BlockPos(2,0,0)),List.of(new BlockPos(3,0,0)),List.of(new BlockPos(4,0,0)),List.of(),null);
    }
    @Test void portsReuseAnImmutableOrderedSnapshot() {
        var f=form();assertSame(f.ports(),f.ports(),"静态端口列表仍每次新建");
        assertEquals(List.of(new BlockPos(1,0,0),new BlockPos(2,0,0),new BlockPos(3,0,0),new BlockPos(4,0,0)),f.ports());
        assertThrows(UnsupportedOperationException.class,()->f.ports().add(BlockPos.ZERO));
    }
    @Test void oldConstructorAndAccessorsPreserveGeometryValues() {
        var f=form();assertEquals(5,f.width());assertEquals(5,f.height());assertEquals(5,f.depth());
        assertEquals(List.of(new BlockPos(1,0,0)),f.waterPorts());assertEquals(form(),f);
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    @Test void controllerSnapshotIsWeakReusableAndRebuiltAfterCollection() throws Exception {
        Field field=BoilerStructure.class.getDeclaredField("LOADED");field.setAccessible(true);Map loaded=(Map)field.get(null);
        Method method=BoilerStructure.class.getDeclaredMethod("controllers",Level.class);method.setAccessible(true);
        Object live=new Object();WeakReference<Object> ref=new WeakReference<>(live);Map entries=new HashMap();entries.put(BlockPos.ZERO,ref);loaded.put(null,entries);
        try {
            Object first=method.invoke(null,(Object)null),second=method.invoke(null,(Object)null);
            assertSame(first,second,"注册表未变仍重复建快照");assertTrue(((List<?>)first).getFirst() instanceof WeakReference,"缓存不能强持owner");
            ref.clear();assertTrue(((List<?>)method.invoke(null,(Object)null)).isEmpty());assertTrue(entries.isEmpty());
        } finally {
            loaded.remove(null);
            try {Field snapshots=BoilerStructure.class.getDeclaredField("CONTROLLER_SNAPSHOTS");snapshots.setAccessible(true);((Map)snapshots.get(null)).remove(null);}catch(NoSuchFieldException ignored){}
        }
    }
}
