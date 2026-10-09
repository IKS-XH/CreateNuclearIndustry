package com.iksxh.create_nuclear_industry.structure;

import com.iksxh.create_nuclear_industry.structure.client.*;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 无游戏世界的实例、水位与本地租约状态机回归；真实 chunk 查询由客户端事件层完成。 */
class ReactorRuntimeLifecycleTest {
    private final ReactorRuntimeSnapshots.State state=new ReactorRuntimeSnapshots.State();
    private final Object world=new Object(), token=new Object();
    private final ReactorRuntimeDescriptor first=ReactorRuntimeProjectionTest.sample(1,1);
    private void ready() {
        state.begin(world,first.dimension()); state.ownerLoaded(world,first.ownerPos(),token);
        for(var c:ReactorRuntimeSnapshots.State.required(first,first.ownerPos())) state.chunk(world,c,new Object());
        assertTrue(state.receive(world,token,first,ReactorRuntimeProjectionTest.geometry(first.ownerGeneration(),3)));
    }
    private boolean visible(){return state.snapshot().findOwner(first.ownerPos()).isPresent();}
    @Test void duplicateDoesNotRenewTwentyTickLeaseAndNewSampleRestores() {
        ready(); var captured=state.snapshot(); var cap=first.columns().stream().filter(ReactorRuntimeDescriptor.ControlRodColumn.class::isInstance).findFirst().orElseThrow().capPos();
        assertEquals(first,captured.findControlOwner(cap).orElseThrow());
        assertEquals(first.columns().stream().filter(ReactorRuntimeDescriptor.ControlRodColumn.class::isInstance).findFirst().orElseThrow(),captured.findControlRod(cap).orElseThrow());
        for(int i=0;i<19;i++) state.tick(); assertTrue(visible());
        assertFalse(state.receive(world,token,first,ReactorRuntimeProjectionTest.geometry(first.ownerGeneration(),3)));
        state.tick(); assertFalse(visible());
        assertTrue(state.receive(world,token,ReactorRuntimeProjectionTest.sample(2,2),ReactorRuntimeProjectionTest.geometry(first.ownerGeneration(),3))); assertTrue(visible());
        assertEquals(first,captured.findControlOwner(cap).orElseThrow());
    }
    @Test void memberUnloadAndInstanceReplacementRequireNewReliableSample() {
        ready(); var c=ReactorRuntimeSnapshots.State.required(first,first.ownerPos()).stream().filter(p->!p.equals(new ChunkPos(first.ownerPos()))).findFirst().orElseThrow();
        state.chunk(world,c,null); assertFalse(visible()); state.chunk(world,c,new Object()); assertFalse(visible());
        assertFalse(state.receive(world,token,first,ReactorRuntimeProjectionTest.geometry(first.ownerGeneration(),3)));
        assertTrue(state.receive(world,token,ReactorRuntimeProjectionTest.sample(2,2),ReactorRuntimeProjectionTest.geometry(first.ownerGeneration(),3))); assertTrue(visible());
        state.chunk(world,c,new Object()); assertFalse(visible());
    }
    @Test void ownerUnloadEitherOrderPreservesWatermarkAndOldWorldRejects() {
        for(boolean ownerFirst:new boolean[]{true,false}) {
            ready(); var c=new ChunkPos(first.ownerPos());
            if(ownerFirst) state.ownerRemoved(world,first.ownerPos(),token,true);
            state.chunk(world,c,null);
            if(!ownerFirst) state.ownerRemoved(world,first.ownerPos(),token,true);
            state.chunk(world,c,new Object()); state.ownerLoaded(world,first.ownerPos(),token); assertFalse(visible());
            assertFalse(state.receive(world,token,first,ReactorRuntimeProjectionTest.geometry(first.ownerGeneration(),3)));
        }
        state.begin(new Object(),first.dimension()); assertFalse(state.receive(world,token,ReactorRuntimeProjectionTest.sample(2,2),ReactorRuntimeProjectionTest.geometry(first.ownerGeneration(),3)));
        assertFalse(visible()); assertSame(ReactorRuntimeSnapshot.empty(),ReactorRuntimeSnapshots.capture(null));
    }
    @Test void badPacketAndGeometryMismatchRevokeAndOwnerReplacementRetiresGeneration() {
        ready(); state.unavailable(world,first.ownerPos(),token); assertFalse(visible());
        assertFalse(state.receive(world,token,first,ReactorRuntimeProjectionTest.geometry(first.ownerGeneration(),3)));
        assertTrue(state.receive(world,token,ReactorRuntimeProjectionTest.sample(2,2),ReactorRuntimeProjectionTest.geometry(first.ownerGeneration(),3)));
        assertFalse(state.receive(world,token,ReactorRuntimeProjectionTest.sample(3,3),ReactorRuntimeProjectionTest.geometry(first.ownerGeneration(),4))); assertFalse(visible());
        Object replacement=new Object(); state.ownerLoaded(world,first.ownerPos(),replacement);
        assertFalse(state.receive(world,replacement,ReactorRuntimeProjectionTest.sample(4,4),ReactorRuntimeProjectionTest.geometry(first.ownerGeneration(),3)));
        var geometry=ReactorRuntimeProjectionTest.geometry(java.util.UUID.randomUUID(),1);
        var next=ReactorRuntimeDescriptorFactory.project(geometry,ReactorStructureDefinition.scan(ReactorRuntimeProjectionTest.template()),ReactorRuntimeProjectionTest.settled(),ReactorRuntimeProjectionTest.input(200),1,1,200);
        assertTrue(state.receive(world,replacement,next,geometry)); assertTrue(visible());
        var cap=next.columns().stream().filter(ReactorRuntimeDescriptor.ControlRodColumn.class::isInstance).findFirst().orElseThrow().capPos();
        assertEquals(next.ownerGeneration(),state.snapshot().findControlOwner(cap).orElseThrow().ownerGeneration());
        state.ownerRemoved(world,first.ownerPos(),token,false); assertTrue(visible());
    }
    @Test void overlappingOwnersRevokeBothControlIndexes() {
        ready(); var original=ReactorRuntimeProjectionTest.geometry(first.ownerGeneration(),3);
        var otherPos=original.origin().offset(1,2,0); var members=new java.util.ArrayList<ReactorSurfaceDescriptor.SurfaceMember>();
        for(var m:original.members()) members.add(new ReactorSurfaceDescriptor.SurfaceMember(m.pos(),
            m.pos().equals(otherPos)?"create_nuclear_industry:reactor_instrument_port":m.pos().equals(original.ownerPos())?"create_nuclear_industry:reactor_casing":m.expectedBlockId(),m.outwardFaces()));
        var geometry=new ReactorSurfaceDescriptor(original.dimension(),otherPos,java.util.UUID.randomUUID(),1,true,original.origin(),original.maxInclusive(),members);
        var other=ReactorRuntimeDescriptorFactory.project(geometry,ReactorStructureDefinition.scan(ReactorRuntimeProjectionTest.template()),ReactorRuntimeProjectionTest.settled(),ReactorRuntimeProjectionTest.input(200),1,1,200);
        Object otherToken=new Object(); state.ownerLoaded(world,otherPos,otherToken); assertTrue(state.receive(world,otherToken,other,geometry));
        var cap=first.columns().stream().filter(ReactorRuntimeDescriptor.ControlRodColumn.class::isInstance).findFirst().orElseThrow().capPos();
        assertFalse(visible()); assertTrue(state.snapshot().findOwner(otherPos).isEmpty());
        assertTrue(state.snapshot().findControlRod(cap).isEmpty()); assertTrue(state.snapshot().findControlOwner(cap).isEmpty());
    }
    /** 当前 owner 的合法类型错 dimension 包必须立即撤下三索引；旧会话/旧实例无权撤销。 */
    @Test void wrongDimensionFromCurrentOwnerRevokesImmediatelyAndPreservesWatermark() {
        ready();
        var cap=first.columns().stream().filter(ReactorRuntimeDescriptor.ControlRodColumn.class::isInstance)
            .findFirst().orElseThrow().capPos();
        var geometry=ReactorRuntimeProjectionTest.geometry(first.ownerGeneration(),3);
        var next=ReactorRuntimeProjectionTest.sample(2,2);
        var wrongTag=next.encode(); wrongTag.putString("Dimension","minecraft:the_nether");
        var wrongDimension=ReactorRuntimeDescriptor.decode(wrongTag).orElseThrow();
        assertEquals("minecraft:the_nether",wrongDimension.dimension());

        assertFalse(state.receive(new Object(),token,wrongDimension,geometry));
        assertIndexesPresent(first,cap);
        assertFalse(state.receive(world,new Object(),wrongDimension,geometry));
        assertIndexesPresent(first,cap);

        assertFalse(state.receive(world,token,wrongDimension,geometry));
        assertIndexesAbsent(cap);
        assertFalse(state.receive(world,token,first,geometry));
        assertIndexesAbsent(cap);
        for(int i=0;i<19;i++) state.tick();
        assertFalse(state.receive(world,token,first,geometry));
        assertIndexesAbsent(cap);

        assertTrue(state.receive(world,token,next,geometry));
        assertIndexesPresent(next,cap);
        for(int i=0;i<19;i++) state.tick();
        assertFalse(state.receive(world,token,first,geometry));
        assertIndexesPresent(next,cap);
        state.tick();
        assertIndexesAbsent(cap);
    }
    private void assertIndexesAbsent(net.minecraft.core.BlockPos cap) {
        var captured=state.snapshot();
        assertTrue(captured.findOwner(first.ownerPos()).isEmpty());
        assertTrue(captured.findControlRod(cap).isEmpty());
        assertTrue(captured.findControlOwner(cap).isEmpty());
    }
    private void assertIndexesPresent(ReactorRuntimeDescriptor expected,net.minecraft.core.BlockPos cap) {
        var captured=state.snapshot();
        assertEquals(expected,captured.findOwner(first.ownerPos()).orElseThrow());
        assertEquals(expected,captured.findControlOwner(cap).orElseThrow());
        assertEquals(expected.columns().stream().filter(ReactorRuntimeDescriptor.ControlRodColumn.class::isInstance)
            .findFirst().orElseThrow(),captured.findControlRod(cap).orElseThrow());
    }
}
