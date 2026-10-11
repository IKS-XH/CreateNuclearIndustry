package com.iksxh.create_nuclear_industry.boiler.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** 验证生产显示输入、真实汽区网格、归属缓存和暂停时钟，不重测L1同步。 */
class BoilerSteamVisualStateTest {
    static BoilerSurfaceSnapshot.Member member() {
        return new BoilerSurfaceSnapshot.Member("minecraft:overworld",new BlockPos(10,41,12),UUID.randomUUID(),1,
                new BlockPos(10,40,10),new BlockPos(20,50,20),44,
                "create_nuclear_industry:high_pressure_boiler_controller",new BlockPos(10,41,12),Set.of(Direction.WEST));
    }
    @Test void realCapacityAndBothStocksDetermineConcentrationWithoutLiquidLevel() {
        assertEquals(0,BoilerSteamVisualState.concentration(0,0,9000));
        assertEquals(0,BoilerSteamVisualState.concentration(-1,5,9000));
        assertEquals(0,BoilerSteamVisualState.concentration(5,0,0));
        assertEquals(.5,BoilerSteamVisualState.concentration(1000,3500,9000));
        assertEquals(1,BoilerSteamVisualState.concentration(Integer.MAX_VALUE,Integer.MAX_VALUE,9000));
        assertEquals(.75,BoilerSteamVisualState.supercriticalRatio(1,3));
        assertEquals(0,BoilerSteamVisualState.supercriticalRatio(5,0));
        assertEquals(1,BoilerSteamVisualState.supercriticalRatio(0,5));
        double previous=0;
        for(int n=0;n<=100;n++) {
            double alpha=BoilerSteamVisualState.alpha(n/100D);
            assertTrue(alpha>=previous && alpha<1);previous=alpha;
        }
    }
    @Test void boundedMeshStaysAbovePartitionAndSamplesOnlyRealInternalAirCells() {
        var m=member();var mesh=BoilerSteamVisualState.buildMesh(m);
        assertEquals(14,mesh.quads().size());
        for(var quad:mesh.quads()) for(var p:quad.points()) {
            double x=p.x()+m.ownerPos().getX(),y=p.y()+m.ownerPos().getY(),z=p.z()+m.ownerPos().getZ();
            assertTrue(x>11 && x<20);assertTrue(y>45 && y<50);assertTrue(z>11 && z<20);
            var light=p.lightingCell();
            assertTrue(light.getX()>=11 && light.getX()<20);
            assertTrue(light.getY()>=45 && light.getY()<50);
            assertTrue(light.getZ()>=11 && light.getZ()<20);
            assertTrue(Double.isFinite(p.u()) && Double.isFinite(p.v()));
        }
        var relocated=new BoilerSurfaceSnapshot.Member(m.dimension(),m.ownerPos().offset(29_000_000,0,-29_000_000),m.ownerGeneration(),m.revision(),
                m.min().offset(29_000_000,0,-29_000_000),m.maxInclusive().offset(29_000_000,0,-29_000_000),m.partitionY(),m.expectedBlockId(),
                m.pos().offset(29_000_000,0,-29_000_000),m.outwardFaces());
        var relocatedMesh=BoilerSteamVisualState.buildMesh(relocated);
        assertEquals(mesh.quads().get(0).points().get(0).x(),relocatedMesh.quads().get(0).points().get(0).x());
        assertEquals(mesh.quads().get(0).points().get(0).u(),relocatedMesh.quads().get(0).points().get(0).u());
    }
    @Test void meshCacheDoesNotSurviveRevocationOrAnotherWorldSession() {
        var state=new BoilerSteamVisualState.Cache();var m=member();var key=BoilerSteamVisualState.identity(3,m);
        assertSame(state.mesh(key,m),state.mesh(key,m));
        state.retain(Set.of());assertEquals(0,state.size());
        state.mesh(key,m);state.retain(Set.of(BoilerSteamVisualState.identity(4,m)));assertEquals(0,state.size());
        for(int n=0;n<80;n++)state.mesh(BoilerSteamVisualState.identity(n,m),m);
        assertEquals(64,state.size());
    }
    @Test void pauseFreezesTickAndPartialThenResumesContinuously() {
        var clock=new BoilerSteamVisualState.Clock();clock.tick(false);double before=clock.time(.625f,false);
        clock.tick(true);assertEquals(before,clock.time(.1f,true));assertEquals(before,clock.time(.9f,true));
        clock.tick(true);assertEquals(before,clock.time(0,true));
        clock.tick(false);assertTrue(clock.time(0,false)>=before);
        clock.clear();assertEquals(0,clock.time(0,true));
    }
}
