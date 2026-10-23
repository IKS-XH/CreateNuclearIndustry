package com.iksxh.create_nuclear_industry.accident;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 只验证世界本体的数量、时钟、独立寿命和交接；真实流动/取消/容器由既有真实域验证。 */
class MoltenCoriumStateTest {
    private MoltenCoriumState state(){return new MoltenCoriumState(UUID.randomUUID(),null,BlockPos.ZERO,new ItemStack(Items.DIAMOND),MoltenCoriumParameters.DEFAULT);}
    @Test void realWasteHalfCeilHasNoAdditionalCap(){var p=MoltenCoriumParameters.DEFAULT;
        assertEquals(1,p.moltenCount(1));assertEquals(1,p.moltenCount(2));assertEquals(98,p.moltenCount(196));assertEquals(5000,p.moltenCount(10000));}
    @Test void independentLoadedClockAndProcessingIntervals(){var s=state();for(int i=0;i<19;i++)s.advance(i,true,false);
        assertEquals(19,s.moveProgress);assertEquals(5981,s.remaining);s.advance(19,true,false);assertEquals(20,s.moveProgress);
        for(int i=20;i<40;i++)s.advance(i,true,false);assertEquals(40,s.corrosionProgress);s.corroded();assertEquals(0,s.corrosionProgress);
        s.advance(99999,false,true);assertEquals(5960,s.remaining);s.advance(100000,true,false);s.advance(100000,true,true);assertEquals(5959,s.remaining);}
    @Test void cooling6000ThenIndependentHotAndWaterFourTimes(){var s=state();for(int i=0;i<1500;i++)s.advance(i,true,true);
        assertEquals(0,s.remaining);assertFalse(s.dangerous());s.heat().startSolid(s.pos);s.handedOff();assertTrue(s.active());assertTrue(s.payload().isEmpty());
        var restored=MoltenCoriumState.load(s.save(RegistryAccess.EMPTY),RegistryAccess.EMPTY);assertTrue(restored.source==null&&restored.active());
        for(int i=0;i<2400;i++)restored.heat().advanceSolid(restored.heat().solid(s.pos),i,true,false);assertEquals(0,restored.heat().solid(s.pos).remaining);}
    @Test void worldPositionBeyondOldBoundsAndMoreThan64Corrosions(){var s=state();for(int i=0;i<100;i++){
        for(int j=0;j<40;j++)s.advance(i*40+j,true,false);assertEquals(40,s.corrosionProgress);s.corroded();s.moved(new BlockPos(i+10,-i-10,12));}
        assertTrue(s.pos.getY() < -6&&s.pos.getX()>2);assertEquals(2000,s.remaining);assertEquals(1,s.payload().getCount());}
    @Test void zeroPayloadAndAdministratorTerminationCannotMint(){var s=new MoltenCoriumState(UUID.randomUUID(),null,BlockPos.ZERO,ItemStack.EMPTY,MoltenCoriumParameters.DEFAULT);
        assertTrue(s.dangerous());s.adminRemoved();assertFalse(s.active());assertTrue(s.payload().isEmpty());s.handedOff();assertTrue(s.payload().isEmpty());}
    @Test void maximumConfiguredIntervalsSaturateWithoutOverflow(){
        var p=new MoltenCoriumParameters(.5,Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE,4,5,AccidentThermalParameters.DEFAULT);
        var s=new MoltenCoriumState(UUID.randomUUID(),null,BlockPos.ZERO,ItemStack.EMPTY,p);
        s.moveProgress=Integer.MAX_VALUE;s.corrosionProgress=Integer.MAX_VALUE;s.advance(1,true,false);
        assertEquals(Integer.MAX_VALUE,s.moveProgress);assertEquals(Integer.MAX_VALUE,s.corrosionProgress);
        assertEquals(Integer.MAX_VALUE-1,s.remaining);assertEquals(p,MoltenCoriumParameters.load(p.save()));
    }
}
