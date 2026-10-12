package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.config.TurbineConfig;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.iksxh.create_nuclear_industry.turbine.*;
import com.iksxh.create_nuclear_industry.turbine.display.TurbineRuntimeSample;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 同一最小真实机组组：自然动力 Post 采样、外源反转、真实供汽/断汽、过载读取及结构生命周期。 */
@GameTestHolder("turbine_runtime_display_probe")
@PrefixGameTestTemplate(false)
public final class TurbineRuntimeDisplayGameTests {
    private static final BlockPos FRONT=new BlockPos(5,5,1);
    private TurbineRuntimeDisplayGameTests(){ }
    @GameTest(template="empty",templateNamespace="turbine_runtime_display_probe",timeoutTicks=180)
    public static void actualExternalSpeedStopAndMemberLifecycle(GameTestHelper h) {
        var tier=TurbineConfig.settings().shortTier();
        ExtensionTurbineGameTests.build(h,FRONT,tier.rotorCount(),false);
        h.runAfterDelay(25,()->{
            var owner=ExtensionTurbineGameTests.owner(h,FRONT);var s=sample(h,0);
            require(h,owner.signedRpm()==256&&s.rpm()==0,"配置RPM被误用为实际停转状态");
            require(h,s.owner().equals(owner.getBlockPos())&&s.generation().equals(owner.runtimeGeneration())
                    &&s.epoch()==owner.runtimeEpoch()&&s.index()==0&&s.memberPos().equals(h.absolutePos(FRONT.south())),"成员归属不完整");
            var saved=rotor(h,0).saveWithoutMetadata(h.getLevel().registryAccess());
            require(h,!saved.contains("TurbineRuntime")&&!saved.contains("RuntimeMember"),"可信显示被持久化");
            var tag=rotor(h,0).getUpdateTag(h.getLevel().registryAccess());
            require(h,TurbineRuntimeSample.decode(tag.getCompound("TurbineRuntime")).orElseThrow().equals(s),"首次跟踪包不是完整同次样本");
            h.setBlock(FRONT.north(),AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING,Direction.SOUTH));
            ((CreativeMotorBlockEntity)h.getBlockEntity(FRONT.north())).generatedSpeed.setValue(-64);
        });
        h.runAfterDelay(50,()->{
            require(h,ExtensionTurbineGameTests.shaft(h,FRONT).getSpeed()==-64,"外源未建立反转实速");
            for(int i=0;i<tier.rotorCount();i++)require(h,sample(h,i).rpm()==-64,"前至后局部+Z没有保留负实速");
            h.setBlock(FRONT.north(),Blocks.AIR);
        });
        h.runAfterDelay(80,()->{
            require(h,ExtensionTurbineGameTests.shaft(h,FRONT).getSpeed()==0&&sample(h,0).rpm()==0,"外源拆除后仍显示配置/历史转速");
            var owner=ExtensionTurbineGameTests.owner(h,FRONT);int oldEpoch=owner.runtimeEpoch();
            var old=rotor(h,0);h.setBlock(FRONT.south(),Blocks.AIR);
            require(h,old.runtimeDisplay().isEmpty()&&rotor(h,1).runtimeDisplay().isEmpty()&&owner.runtimeEpoch()>oldEpoch,"拆转子未撤销整组旧绑定");
            h.setBlock(FRONT.south(),TurbineContent.ROTOR.get().defaultBlockState());
        });
        h.runAfterDelay(110,()->{
            var s=sample(h,0);var oldOwner=ExtensionTurbineGameTests.owner(h,FRONT);var generation=s.generation();
            BlockPos pos=ExtensionTurbineGameTests.controller(FRONT,tier);var state=h.getBlockState(pos);
            h.setBlock(pos,Blocks.AIR);require(h,rotor(h,0).runtimeDisplay().isEmpty(),"拆控制器后样本仍可信");
            h.setBlock(pos,state);
            h.runAfterDelay(25,()->{
                require(h,!sample(h,0).generation().equals(generation)&&ExtensionTurbineGameTests.owner(h,FRONT)!=oldOwner,"重建控制器复用了旧世代");
                h.succeed();
            });
        });
    }
    @GameTest(template="empty",templateNamespace="turbine_runtime_display_probe",timeoutTicks=280)
    public static void steamGenerationStopsWithoutConfiguredRpmFallback(GameTestHelper h) {
        var tier=TurbineConfig.settings().shortTier();ExtensionTurbineGameTests.build(h,FRONT,tier.rotorCount(),false);
        boolean[] feed={true};
        h.onEachTick(()->{
            if(!feed[0])return;
            var input=ExtensionTurbineGameTests.handler(h,ExtensionTurbineGameTests.inlet(FRONT,tier),Direction.WEST);
            var output=ExtensionTurbineGameTests.handler(h,ExtensionTurbineGameTests.exhaust(FRONT,tier),Direction.EAST);
            if(input!=null)input.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(),tier.ratedFlowMbPerTick()),IFluidHandler.FluidAction.EXECUTE);
            if(output!=null)output.drain(tier.ratedFlowMbPerTick(),IFluidHandler.FluidAction.EXECUTE);
        });
        h.runAfterDelay(75,()->{
            require(h,ExtensionTurbineGameTests.owner(h,FRONT).totalSu()>0,"真实供汽未建立SU");
            require(h,ExtensionTurbineGameTests.shaft(h,FRONT).getSpeed()==256&&sample(h,0).rpm()==256,"供汽样本没有对应真实网速");
            feed[0]=false;
        });
        h.runAfterDelay(240,()->{
            require(h,ExtensionTurbineGameTests.owner(h,FRONT).totalSu()==0&&ExtensionTurbineGameTests.shaft(h,FRONT).getSpeed()==0,"断汽实际动力未停止");
            require(h,sample(h,0).rpm()==0,"断汽后显示未可靠归零");h.succeed();
        });
    }
    @GameTest(template="empty",templateNamespace="turbine_runtime_display_probe",timeoutTicks=110)
    public static void actualOverstressedGetterIsZeroAndUnloadRevokes(GameTestHelper h) {
        var tier=TurbineConfig.settings().shortTier();ExtensionTurbineGameTests.build(h,FRONT,tier.rotorCount(),false);
        h.setBlock(FRONT.north(),AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING,Direction.SOUTH));
        ((CreativeMotorBlockEntity)h.getBlockEntity(FRONT.north())).generatedSpeed.setValue(64);
        boolean[] stress={false};
        h.onEachTick(()->{
            if(!stress[0])return;
            // 仅夹具注入原生网络结算的过载状态，正式发布器仍只读 getSpeed，不修改网络。
            ExtensionTurbineGameTests.shaft(h,FRONT).updateFromNetwork(0,100,2);
            ExtensionTurbineGameTests.shaft(h,FRONT.south(tier.length()-1)).updateFromNetwork(0,100,2);
        });
        h.runAfterDelay(30,()->{require(h,sample(h,0).rpm()==64,"未建立外源正常网速");stress[0]=true;});
        h.runAfterDelay(50,()->{
            var shaft=ExtensionTurbineGameTests.shaft(h,FRONT);
            require(h,shaft.isOverStressed()&&shaft.getTheoreticalSpeed()==64&&shaft.getSpeed()==0&&sample(h,0).rpm()==0,"显示绕过了原生过载0");
            var owner=ExtensionTurbineGameTests.owner(h,FRONT);owner.onChunkUnloaded();
            require(h,rotor(h,0).runtimeDisplay().isEmpty(),"服务端卸载钩子未撤销显示");h.succeed();
        });
    }
    private static TurbineRotorBlockEntity rotor(GameTestHelper h,int index){return (TurbineRotorBlockEntity)h.getBlockEntity(FRONT.south(index+1));}
    private static TurbineRuntimeSample sample(GameTestHelper h,int index) {
        var s=rotor(h,index).runtimeDisplay();require(h,s.isPresent(),"缺少完整可靠运行样本：index="+index);
        var value=s.orElseThrow();long age=h.getLevel().getGameTime()-value.serverTick();
        require(h,age>=0&&age<=5,"Post样本心跳超界："+age);return value;
    }
    private static void require(GameTestHelper h,boolean value,String message){if(!value)h.fail(message);}
}
