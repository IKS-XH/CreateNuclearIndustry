package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 唯一真实仪表集成用例：在独立测试世界核对完整运行包、成功无变化、撤销及恢复。 */
@GameTestHolder("reactor_runtime_display_probe")
@PrefixGameTestTemplate(false)
public final class ReactorRuntimeDisplayGameTests {
    private static final BlockPos OWNER = new BlockPos(2,2,0);
    private ReactorRuntimeDisplayGameTests() { }
    @GameTest(template="empty", timeoutTicks=80)
    public static void completedRuntimeSampleTracksAuthorityAndLifecycle(GameTestHelper helper) {
        for(var entry:ReactorStructureDefinition.canonicalTemplate().entrySet()) {
            var p=entry.getKey(); helper.setBlock(new BlockPos(p.x(),p.y(),p.z()),
                BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.getValue())).defaultBlockState());
        }
        require(helper,owner(helper).runtimeDescriptor().isEmpty(),"首次仪表未结算却存在运行样本");
        helper.runAfterDelay(2,()->{
            var owner=owner(helper); rescan(helper);
            require(helper,owner.runtimeDescriptor().isEmpty() || !owner.runtimeDescriptor().orElseThrow().available(),"重扫未撤销旧运行样本");
            owner.tickReactor(); var first=packet(owner);
            require(helper,first.available() && first.columns().size()==9,"成功后缺少完整运行包");
            require(helper,first.coolantSpace().size()==3,"合法空列空间不正确");
            require(helper,first.coldCoolantMb()==owner.snapshot().coldCoolantMb() && first.hotCoolantMb()==owner.snapshot().hotCoolantMb(),"结算库存与包不同龄");
            require(helper,first.coolantCapacityMb()==owner.coolantCapacityMb(),"同次容量未投影");
            boolean changed=owner.tickReactor(); var stable=packet(owner);
            require(helper,!changed && stable.available() && stable.sample()==first.sample()+1,"零变化成功tick被当作失败");
            for(int i=0;i<5;i++) owner.tickReactor();
            require(helper,packet(owner).sample()==stable.sample()+5 && counter(owner)<5,"稳定零产热缺少五成功tick心跳");
            var latest=packet(owner); var authoritative=owner.snapshot(); var telemetry=owner.telemetry();
            internalScopeProbe(helper,owner,latest,authoritative);
            owner.setSnapshot(authoritative);
            require(helper,!packet(owner).available() && owner.telemetry().equals(telemetry),"外部快照改写未仅撤L2");
            owner.tickReactor(); require(helper,packet(owner).available(),"成功新样本未恢复");
            owner.setSnapshot(authoritative.withColumns(authoritative.fuelColumns(),java.util.Map.of(
                new com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition(0,0),
                com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState.fullyInserted())));
            require(helper,!owner.tickReactor() && !packet(owner).available() && owner.telemetry().equals(telemetry),"提交前早退改变旧遥测或未撤销");
            owner.setSnapshot(authoritative); owner.tickReactor(); require(helper,packet(owner).available(),"早退后不能恢复");
            // 缓存仍有效但真实换料端口已经缺失，hydrate 的早退不得保留上一动画。
            BlockPos cap=new BlockPos(1,4,1); var capState=helper.getBlockState(cap);
            helper.setBlock(cap,Blocks.AIR.defaultBlockState());
            require(helper,!owner.tickReactor() && !packet(owner).available(),"端口缺失早退未撤L2");
            helper.setBlock(cap,capState); rescan(helper); owner.tickReactor();
            require(helper,packet(owner).available(),"端口重建未恢复");
            var saved=owner.saveForServerTest(helper.getLevel().registryAccess());
            require(helper,!saved.contains("ReactorRuntime"),"运行显示错误持久化");
            var fuel=P1Blocks.REACTOR_FUEL_ROD.get().defaultBlockState(); BlockPos fuelPos=helper.absolutePos(new BlockPos(1,1,1));
            require(helper,!fuel.canOcclude() && fuel.getLightBlock(helper.getLevel(),fuelPos)==1,"燃料遮挡/默认阻光不正确");
            require(helper,fuel.getCollisionShape(helper.getLevel(),fuelPos).toAabbs().equals(Shapes.block().toAabbs())
                && fuel.getShape(helper.getLevel(),fuelPos).toAabbs().equals(Shapes.block().toAabbs()),"燃料碰撞或选框被改变");
            helper.setBlock(BlockPos.ZERO,Blocks.AIR.defaultBlockState()); rescan(helper);
            require(helper,!packet(owner).available(),"拆坏未撤销运行样本");
            helper.setBlock(BlockPos.ZERO,P1Blocks.REACTOR_CASING.get().defaultBlockState()); rescan(helper); owner.tickReactor();
            require(helper,packet(owner).available(),"外壳重建未恢复");
            var generation=packet(owner).ownerGeneration();
            helper.setBlock(OWNER,Blocks.AIR.defaultBlockState()); helper.setBlock(OWNER,P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
            var replacement=owner(helper); rescan(helper); replacement.tickReactor();
            require(helper,packet(replacement).available() && !packet(replacement).ownerGeneration().equals(generation),"新owner复用旧generation");
            helper.succeed();
        });
    }
    /** 测试专用反射观察内部作用域；不增加正式setter，也不改正式提交顺序。 */
    private static void internalScopeProbe(GameTestHelper helper,ReactorInstrumentPortBlockEntity owner,
            ReactorRuntimeDescriptor frozen,ReactorSnapshot authoritative) {
        try {
            var field=ReactorInstrumentPortBlockEntity.class.getDeclaredField("runtimeSettlementInProgress"); field.setAccessible(true);
            field.setBoolean(owner,true);
            try {
                owner.setSnapshot(authoritative.withCoolantInventories(1,2));
                require(helper,packet(owner).equals(frozen),"内部中间包混用了新库存与旧运行样本");
                owner.setSnapshot(authoritative);
            } finally { field.setBoolean(owner,false); }
        } catch(ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
    }
    private static int counter(ReactorInstrumentPortBlockEntity owner) {
        try { var f=ReactorInstrumentPortBlockEntity.class.getDeclaredField("runtimeTicksSinceLastSync"); f.setAccessible(true); return f.getInt(owner); }
        catch(ReflectiveOperationException exception){throw new IllegalStateException(exception);}
    }
    private static ReactorInstrumentPortBlockEntity owner(GameTestHelper helper){return (ReactorInstrumentPortBlockEntity)helper.getBlockEntity(OWNER);}
    private static ReactorRuntimeDescriptor packet(ReactorInstrumentPortBlockEntity owner){return ReactorRuntimeDescriptor.decode(owner.getUpdatePacket().getTag().getCompound("ReactorRuntime")).orElseThrow();}
    private static void rescan(GameTestHelper helper){ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(),helper.absolutePos(OWNER));}
    private static void require(GameTestHelper helper,boolean condition,String message){if(!condition)helper.fail(message);}
}
