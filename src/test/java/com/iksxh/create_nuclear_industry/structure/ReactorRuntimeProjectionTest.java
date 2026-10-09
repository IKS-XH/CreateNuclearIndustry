package com.iksxh.create_nuclear_industry.structure;

import com.iksxh.create_nuclear_industry.reactor.*;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 用正式结算器和缓存扫描验证同龄运行投影；功率 HU/t，库存及容量 mB。 */
class ReactorRuntimeProjectionTest {
    static final BlockPos ORIGIN = new BlockPos(14, 70, 14);
    static final UUID GENERATION = UUID.randomUUID();
    static final CoreColumnPosition ROD = new CoreColumnPosition(1, 0);
    static Map<ReactorStructureDefinition.LocalPosition, String> template() {
        var roles = new TreeMap<CoreColumnPosition, ReactorStructureDefinition.ColumnType>();
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) roles.put(new CoreColumnPosition(x,z),
            x == 1 && z == 1 ? ReactorStructureDefinition.ColumnType.EMPTY : ReactorStructureDefinition.ColumnType.FUEL);
        roles.put(ROD, ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        return ReactorStructureDefinition.templateFor(roles);
    }
    static ReactorSurfaceDescriptor geometry(UUID generation, long revision) {
        return geometry(generation,revision,template());
    }
    static ReactorSurfaceDescriptor geometry(UUID generation, long revision,Map<ReactorStructureDefinition.LocalPosition,String> blocks) {
        var members = new ArrayList<ReactorSurfaceDescriptor.SurfaceMember>();
        BlockPos owner = ORIGIN.offset(2, 2, 0), max = ORIGIN.offset(4,4,4);
        for (var entry : blocks.entrySet()) {
            var p = entry.getKey(); BlockPos pos = ORIGIN.offset(p.x(),p.y(),p.z());
            var faces = ReactorSurfaceDescriptor.outwardFaces(pos, ORIGIN, max);
            if (!faces.isEmpty()) members.add(new ReactorSurfaceDescriptor.SurfaceMember(pos,entry.getValue(),faces));
        }
        return new ReactorSurfaceDescriptor("minecraft:overworld",owner,generation,revision,true,ORIGIN,max,members);
    }
    static ReactorServerTick.Result settled() {
        var scan = ReactorStructureDefinition.scan(template());
        var fuels = new TreeMap<CoreColumnPosition,FuelColumnState>();
        scan.columns().forEach((p,c) -> { if(c.type() == ReactorStructureDefinition.ColumnType.FUEL)
            fuels.put(p,new FuelColumnState(FuelAssemblyState.installed(1,0),1,0,0.999999)); });
        var rods = Map.of(ROD,new ControlRodColumnState(1,1,0.2,true,0));
        var snapshot = new ReactorSnapshot(fuels,rods,100,7,0,false);
        return ReactorServerTick.advance(snapshot,ReactorSimulationParameters.defaults(),input(200));
    }
    static ReactorServerTick.CoolantInput input(long capacity) {
        return new ReactorServerTick.CoolantInput(ReactorCoolantLedger.summarizePorts(List.of(),0),capacity,0.5);
    }
    static ReactorRuntimeDescriptor sample(long revision, long sample) {
        return ReactorRuntimeDescriptorFactory.project(geometry(GENERATION,3),ReactorStructureDefinition.scan(template()),settled(),input(200),revision,sample,100+sample);
    }
    @Test void resultProjectsActualJammedRodExhaustionPowerAndSameCapacity() {
        var result = settled();
        var d = ReactorRuntimeDescriptorFactory.project(geometry(GENERATION,3),ReactorStructureDefinition.scan(template()),result,input(200),1,1,100);
        var rod = d.columns().stream().filter(ReactorRuntimeDescriptor.ControlRodColumn.class::isInstance)
            .map(ReactorRuntimeDescriptor.ControlRodColumn.class::cast).findFirst().orElseThrow();
        assertEquals(0.2,rod.actualDepth()); assertEquals(1,rod.targetDepth()); assertTrue(rod.jammed());
        assertEquals(3,rod.bodyPositions().size());
        for(var c:d.columns()) if(c instanceof ReactorRuntimeDescriptor.FuelColumn f) {
            assertFalse(f.fuelUsable()); assertTrue(f.fissionHeatHuPerTick()>0);
        }
        assertEquals(result.fission().generatedHeatHu(),d.columns().stream().filter(ReactorRuntimeDescriptor.FuelColumn.class::isInstance)
            .map(ReactorRuntimeDescriptor.FuelColumn.class::cast).mapToDouble(ReactorRuntimeDescriptor.FuelColumn::fissionHeatHuPerTick).sum(),1e-12);
        assertEquals(result.snapshot().coldCoolantMb(),d.coldCoolantMb()); assertEquals(result.snapshot().hotCoolantMb(),d.hotCoolantMb());
        assertEquals(result.coolant().settlement().convertedCoolantMb(),d.convertedCoolantMbPerTick()); assertEquals(200,d.coolantCapacityMb());
    }
    @Test void coolantSpaceContainsOnlyEmptyAndControlBodiesAndZeroCapacityIsFaithful() {
        var d = ReactorRuntimeDescriptorFactory.project(geometry(GENERATION,3),ReactorStructureDefinition.scan(template()),settled(),input(0),1,1,100);
        assertEquals(0,d.coolantCapacityMb()); assertEquals(6,d.coolantSpace().size());
        assertTrue(d.columns().stream().filter(ReactorRuntimeDescriptor.FuelColumn.class::isInstance)
            .flatMap(c -> c.bodyPositions().stream()).noneMatch(d.coolantSpace()::contains));
    }
    @Test void invalidScanCannotProducePartialProjection() {
        assertThrows(IllegalArgumentException.class,()->ReactorRuntimeDescriptorFactory.project(geometry(GENERATION,3),ReactorStructureDefinition.ScanResult.notScanned(),settled(),input(0),1,1,100));
    }
    /** 正式模型允许从未装料的列完全不出现在 snapshot/fission map 中，仍是成功零产热样本。 */
    @Test void emptyAuthoritativeMapsStillProjectAllFuelColumnsWithZeroPower() {
        var result=ReactorServerTick.advance(ReactorSnapshot.empty(),ReactorSimulationParameters.defaults(),input(200));
        var d=assertDoesNotThrow(()->ReactorRuntimeDescriptorFactory.project(geometry(GENERATION,3,ReactorStructureDefinition.canonicalTemplate()),
            ReactorStructureDefinition.scan(ReactorStructureDefinition.canonicalTemplate()),result,input(200),1,1,100));
        assertEquals(9,d.columns().size());
        assertTrue(d.columns().stream().filter(ReactorRuntimeDescriptor.FuelColumn.class::isInstance)
            .map(ReactorRuntimeDescriptor.FuelColumn.class::cast).allMatch(c->!c.fuelUsable() && c.fissionHeatHuPerTick()==0));
    }
}
