package com.iksxh.create_nuclear_industry.structure;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

/** 共用生产寻边入口，验证偏心四面、相邻设备、缺区块等待与去重读取预算。 */
class ReactorVariableScannerTest {
    @Test void allFourSideFacesAcceptOffCentreInstrumentIncludingFuelBehindIt() {
        for (var owner : new BlockPos[]{new BlockPos(1,1,0),new BlockPos(0,6,1),new BlockPos(5,2,5),new BlockPos(4,3,6)}) {
            var blocks=world(6,8,7,BlockPos.ZERO);
            blocks.put(new BlockPos(1,1,0),id("reactor_casing"));
            blocks.put(owner,id("reactor_instrument_port"));
            var result=scan(owner,blocks);
            assertTrue(result.valid(),owner+": "+result.failureReason());
            assertEquals(BlockPos.ZERO,result.origin());
            assertEquals(new ReactorDimensions(6,8,7),result.contract().dimensions());
        }
    }
    @Test void touchingReactorsKeepSeparateClosedFramesAndOwners() {
        var blocks=world(6,8,7,BlockPos.ZERO);
        blocks.putAll(world(6,8,7,new BlockPos(6,0,0)));
        assertEquals(BlockPos.ZERO,scan(new BlockPos(1,1,0),blocks).origin());
        assertEquals(new BlockPos(6,0,0),scan(new BlockPos(7,1,0),blocks).origin());
        blocks.put(new BlockPos(2,1,0),id("reactor_instrument_port"));
        assertFalse(scan(new BlockPos(1,1,0),blocks).valid());
    }
    @Test void maximumCrossChunkReadsEachCoordinateOnceAndWaitsWithoutReadingUnavailableData() {
        var origin=new BlockPos(8,30,8);
        var blocks=world(16,16,16,origin);
        var owner=origin.offset(1,1,0);
        var reads=new HashSet<BlockPos>();
        var result=ReactorStructureScanner.scanInstrumentPort(owner,pos->{
            assertTrue(reads.add(pos),"coordinate was read twice");
            return blocks.getOrDefault(pos,"minecraft:air");
        },5,16);
        assertTrue(result.valid(),result.failureReason());
        assertEquals(origin,result.origin());
        assertEquals(reads.size(),result.blockReadCount());
        assertTrue(result.blockReadCount()<=ReactorStructureScanner.MAX_BLOCK_READS);
        System.out.println("variable-reactor 16x16x16 origin="+origin+" uniqueReads="+result.blockReadCount()+" budget="+ReactorStructureScanner.MAX_BLOCK_READS);
        var waiting=ReactorStructureScanner.scanInstrumentPort(owner,pos->pos.getX()>=16 ? null : blocks.getOrDefault(pos,"minecraft:air"),5,16);
        assertFalse(waiting.valid());
        assertEquals(ReactorStructureDefinition.DiagnosticCode.CHUNKS_UNAVAILABLE,waiting.diagnosticCode());
        System.out.println("variable-reactor unavailable-reader uniqueReads="+waiting.blockReadCount());
        assertTrue(scan(owner,blocks).valid());
        blocks.remove(origin.offset(15,15,15));
        assertFalse(scan(owner,blocks).valid());
    }
    @Test void invalidJointConfigurationAndDimensionRestrictionsFailClosed() {
        var blocks=world(6,8,7,BlockPos.ZERO);
        assertFalse(ReactorStructureScanner.scanInstrumentPort(new BlockPos(1,1,0),p->blocks.getOrDefault(p,"minecraft:air"),9,8).valid());
        assertFalse(ReactorStructureScanner.scanInstrumentPort(new BlockPos(1,1,0),p->blocks.getOrDefault(p,"minecraft:air"),5,7).valid());
    }
    private static ReactorStructureScanner.WorldScanResult scan(BlockPos owner,Map<BlockPos,String> blocks) {
        return ReactorStructureScanner.scanInstrumentPort(owner,p->blocks.getOrDefault(p,"minecraft:air"),5,16);
    }
    private static Map<BlockPos,String> world(int w,int h,int d,BlockPos origin) {
        var blocks=new HashMap<BlockPos,String>();
        ReactorVariableSizeTest.fixture(w,h,d).forEach((p,b)->blocks.put(origin.offset(p.x(),p.y(),p.z()),b));
        return blocks;
    }
    private static String id(String path) { return "create_nuclear_industry:"+path; }
}
