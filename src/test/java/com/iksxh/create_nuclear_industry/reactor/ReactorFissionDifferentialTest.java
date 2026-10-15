package com.iksxh.create_nuclear_industry.reactor;

import com.iksxh.create_nuclear_industry.structure.ReactorDimensions;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 冻结原 Java 算法逐字段严格对照；覆盖随机布局、反馈变更和完整权威 tick 的后继状态。 */
class ReactorFissionDifferentialTest {
    static ReactorSimulationParameters change(ReactorSimulationParameters p,int field,double value) {
        double[] f={p.controlResponseExponent(),p.overclockHeatMultiplier(),p.overclockBurnMultiplier(),p.overclockFeedbackGain(),p.overclockFeedbackExponent()};
        if(field>=0) f[field]=value;
        return new ReactorSimulationParameters(p.baseHeatPerFuelBlockHuPerTick(),p.burnHoursPerBlock(),p.damageHeatThresholdHuPerTick(),
                p.damageRatePerTickHuLoad(),p.fuelColumnDamageHeatMultiplier(),p.fuelColumnDamageBurnMultiplier(),p.damageTransferRate(),
                p.controlRodFailureThreshold(),p.meltdownTriggerFraction(),p.meltdownCountdownTicks(),f[0],f[1],f[2],f[3],f[4],p.totalHeatMultiplierCap());
    }

    static ReactorSnapshot randomState(Random random,int width,int height,int depth) {
        var fuels=new TreeMap<CoreColumnPosition,FuelColumnState>();
        var rods=new TreeMap<CoreColumnPosition,ControlRodColumnState>();
        for(int x=0;x<width-2;x++) for(int z=0;z<depth-2;z++) {
            var pos=new CoreColumnPosition(x,z);
            int type=random.nextInt(8);
            if(type<5) {
                var assembly=type==0?FuelAssemblyState.empty():FuelAssemblyState.installed(100,type==1?100:random.nextInt(100));
                fuels.put(pos,new FuelColumnState(assembly,random.nextInt(11)/10.,random.nextDouble()*50,random.nextDouble(),0));
            } else if(type<7) rods.put(pos,new ControlRodColumnState(random.nextInt(11)/10.,random.nextDouble(),random.nextDouble(),random.nextBoolean(),random.nextDouble()*20));
        }
        return new ReactorSnapshot(fuels,rods,50000,0,0,false,Map.of(),false,false,new ReactorDimensions(width,height,depth));
    }

    @Test void fixedSeedLayoutsAndAllFeedbackParametersMatchOriginalBits() {
        Random random=new Random(0x53F15510L);
        var p=ReactorSimulationParameters.defaults();
        for(int i=0;i<700;i++) {
            var state=randomState(random,5+random.nextInt(12),5+random.nextInt(12),5+random.nextInt(12));
            var params=change(p,i%5,switch(i%5){case 0->.25+random.nextDouble()*3;case 1,2->1+random.nextDouble()*20;case 3->random.nextDouble()*.7;default->.25+random.nextDouble()*3;});
            var expected=ReactorFissionReference.calculate(state,params);
            assertEquals(expected,ReactorFissionCalculator.calculate(state,params),"seed case "+i);
            assertEquals(expected,ReactorFissionCalculator.calculate(state,params),"cached seed case "+i);
        }
        System.out.println("strict randomized fission comparisons=1400");
    }

    @Test void evolvingTicksMatchAllStagesAcrossRodMotionScramJamDepletionAndMaintenance() {
        var p=ReactorSimulationParameters.defaults();
        var ports=ReactorCoolantLedger.summarizePorts(List.of(ReactorCoolantLedger.Port.cold("c",50000),ReactorCoolantLedger.Port.hot("h",50000)),50000);
        var input=new ReactorServerTick.CoolantInput(ports,200000,0.5);
        int steps=0;
        for(int scenario=0;scenario<6;scenario++) {
            ReactorSnapshot a=randomState(new Random(500+scenario),scenario<3?5:16,16,scenario<3?7:16);
            ReactorSnapshot b=a;
            for(int tick=0;tick<100;tick++) {
                if(tick%10==0) {
                    var rods=new TreeMap<>(a.controlRodColumns());
                    int current=tick;
                    rods.replaceAll((pos,r)->new ControlRodColumnState(r.integrity(),current%30==0?1:.4,r.actualDepth(),current>=50&&pos.x()%2==0,r.cachedHeatHu()));
                    a=a.withColumns(a.fuelColumns(),rods); b=b.withColumns(b.fuelColumns(),rods);
                }
                if(tick==25 || tick==70) {
                    var fuels=new TreeMap<>(a.fuelColumns());
                    int current=tick;
                    fuels.replaceAll((pos,f)->new FuelColumnState(FuelAssemblyState.installed(1000,current==25?999:0),f.integrity(),f.cachedHeatHu(),f.fuelBurnRemainder(),f.quantizedHeatRemainderHu()));
                    a=a.withColumns(fuels,a.controlRodColumns()); b=b.withColumns(fuels,b.controlRodColumns());
                }
                var coolant=scenario%2==0?input:ReactorServerTick.CoolantInput.none();
                var before=ReactorTickReference.advance(a,p,coolant,tick%3==0?1:0);
                var after=ReactorServerTick.advance(b,p,coolant,tick%3==0?1:0);
                assertEquals(before,after,"scenario="+scenario+" tick="+tick);
                a=before.snapshot(); b=after.snapshot(); steps++;
            }
        }
        System.out.println("strict full-tick stage comparisons="+steps);
    }
}
