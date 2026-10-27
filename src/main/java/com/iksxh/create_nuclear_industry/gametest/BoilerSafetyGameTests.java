package com.iksxh.create_nuclear_industry.gametest;

import java.util.*;
import com.iksxh.create_nuclear_industry.boiler.*;
import com.iksxh.create_nuclear_industry.content.*;
import com.iksxh.create_nuclear_industry.accident.*;
import com.iksxh.create_nuclear_industry.production.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.minecraft.nbt.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 本批隔离真实服务端探针；复用现有结构夹具，用户存档不参与测试。 */
@GameTestHolder("cni_boiler_safety")
@PrefixGameTestTemplate(false)
public final class BoilerSafetyGameTests {
    private static final BlockPos BASE = new BlockPos(4, 2, 4);
    private static final BlockPos FIRST = BASE.offset(1, 4, 1), SECOND = BASE.offset(2, 4, 1), THIRD = BASE.offset(3, 4, 1);
    private BoilerSafetyGameTests() {}
    private static boolean skip(GameTestHelper h,String key){var focus=System.getenv("CNI_BOILER_SAFETY_FOCUS");if(focus==null||focus.isBlank()||Arrays.asList(focus.split(",")).contains(key))return false;System.out.println("[boiler-safety] SKIP fixture "+key);h.succeed();return true;}

    @GameTest(template="boiler_empty", timeoutTicks=45)
    public static void multipleValvesAndEverySurfaceRole(GameTestHelper h) {
        if(skip(h,"valves"))return;
        ExtensionBoilerGameTests.build(h,5,5,5,2);
        h.setBlock(SECOND,BoilerContent.SAFETY_VALVE.get());
        h.runAfterDelay(4,()->{
            var owner=ExtensionBoilerGameTests.owner(h);var form=owner.currentForm();
            h.assertTrue(form!=null&&form.valves().size()==2,"合法双阀未成型");
            h.setBlock(THIRD,BoilerContent.SAFETY_VALVE.get());
            form=owner.currentForm();h.assertTrue(form!=null&&form.valves().size()==3,"合法三阀未成型");
            var projection=BoilerSurfaceDescriptorFactory.project(h.getLevel().dimension().location().toString(),
                    owner.getBlockPos(),java.util.UUID.randomUUID(),1,form);
            h.assertTrue(projection.valid()&&projection.members().stream().filter(m->m.expectedBlockId().endsWith("boiler_safety_valve")).count()==3,
                    "表面投影遗漏新增阀");
            for(var p:List.of(FIRST,SECOND,THIRD))h.setBlock(p,BoilerContent.CASING.get());
            h.assertTrue(owner.currentForm()==null,"没有泄压阀仍成型");
            h.setBlock(BASE.offset(0,4,1),BoilerContent.SAFETY_VALVE.get());
            h.assertTrue(owner.currentForm()==null,"棱边泄压阀错误成型");
            h.setBlock(BASE.offset(0,4,1),BoilerContent.CASING.get());
            h.setBlock(BASE.offset(0,4,0),BoilerContent.SAFETY_VALVE.get());
            h.assertTrue(owner.currentForm()==null,"角点泄压阀错误成型");h.succeed();
        });
    }

    /** 夹具仅恢复当前版本已付HU的真实SC库存；不通过修改压力字段伪造驱动。 */
    private static void resetSteam(BoilerControllerBlockEntity owner) {
        var s=owner.ledger();var tag=s.save();
        ExtensionBoilerGameTests.seedInventories(tag,0,0,s.steamCapacity(),s.steamCapacity());
        tag.putLong("VentTick",Long.MIN_VALUE);tag.putLong("LastTick",Long.MIN_VALUE);s.load(tag);
    }
    private static void assertFlow(GameTestHelper h,int clear) {
        var owner=ExtensionBoilerGameTests.owner(h);resetSteam(owner);owner.tick();var s=owner.ledger();
        h.assertTrue(s.vented()==clear*512,"逐阀固定排量错误："+s.vented());
        h.assertTrue(s.valveVents().size()==clear&&s.valveVents().values().stream().allMatch(n->n==512),"阀粒子收据并非各口实际排量");
        int steam=s.steam();double hu=s.totalHu();long vented=s.totalVented();var receipts=s.valveVents();
        owner.tick();h.assertTrue(steam==s.steam()&&hu==s.totalHu()&&vented==s.totalVented()&&receipts.equals(s.valveVents()),
                "同tick重入控制器重复扣汽/热或刷新泄放收据");
        if(clear==0)h.assertTrue(s.valveBlocked(),"全堵阀未报告阻塞");
    }
    @GameTest(template="boiler_empty", timeoutTicks=45)
    public static void independentNonAirObstructionAndRecovery(GameTestHelper h) {
        if(skip(h,"valves"))return;
        ExtensionBoilerGameTests.build(h,5,5,5,2);
        h.setBlock(SECOND,BoilerContent.SAFETY_VALVE.get());h.setBlock(THIRD,BoilerContent.SAFETY_VALVE.get());
        h.runAfterDelay(4,()->{
            assertFlow(h,3);
            h.setBlock(FIRST.above(),Blocks.STONE);assertFlow(h,2);
            h.setBlock(SECOND.above(),Blocks.OAK_SLAB);assertFlow(h,1);
            h.setBlock(THIRD.above(),Blocks.WATER);assertFlow(h,0);
            h.setBlock(SECOND.above(),Blocks.AIR);assertFlow(h,1);
            h.setBlock(FIRST.above(),Blocks.AIR);h.setBlock(THIRD.above(),Blocks.AIR);assertFlow(h,3);
            h.succeed();
        });
    }
    @GameTest(template="boiler_empty", timeoutTicks=45)
    public static void largerSteamVolumeDoesNotMultiplyOneValveFlow(GameTestHelper h) {
        if(skip(h,"valves"))return;
        ExtensionBoilerGameTests.build(h,6,6,8,2);
        h.runAfterDelay(4,()->{
            var owner=ExtensionBoilerGameTests.owner(h);h.assertTrue(owner.currentForm()!=null,"长方体未成型");
            assertFlow(h,1);
            var s=owner.ledger();var snapshot=s.save();s.load(snapshot);owner.invalidateForm();owner.currentForm();
            var before=s.save();owner.tick();h.assertTrue(before.equals(s.save()),"保存恢复/结构重查刷新同tick泄放额度");
            h.succeed();
        });
    }
    private static void seedHeat(BoilerControllerBlockEntity owner,double pressure,boolean dry){
        var s=owner.ledger();var tag=s.save();int amount=pressure>0?s.steamCapacity():0;
        ExtensionBoilerGameTests.seedInventories(tag,0,0,amount,amount==0?0:amount*s.steamEnthalpy(pressure*s.settings().supercriticalTemperature()));
        tag.putInt("Water",0);tag.putDouble("WaterHu",dry?s.waterHeatCapacity()*1.5:0);
        tag.putLong("VentTick",Long.MIN_VALUE);tag.putLong("LastTick",Long.MIN_VALUE);s.load(tag);
    }
    private static BoilerAccidentLedger savedRecord(GameTestHelper h,UUID generation){
        var data=BoilerAccidentSavedData.get(h.getLevel()).save(new CompoundTag(),h.getLevel().registryAccess());
        for(Tag item:data.getList("Accidents",Tag.TAG_COMPOUND)){var t=(CompoundTag)item;if(t.getUUID("Generation").equals(generation))return BoilerAccidentLedger.load(t,h.getLevel().registryAccess());}return null;
    }
    private static int heatingParts(GameTestHelper h){int count=0;for(var p:BlockPos.betweenClosed(BASE,BASE.offset(4,4,4)))if(h.getBlockState(p).is(HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get())||h.getBlockState(p).is(BoilerContent.HEAT_SECTION.get()))count++;return count;}

    @GameTest(template="boiler_empty",timeoutTicks=850,batch="boiler_thermal")
    public static void continuousRealHotCoolantDriesAndDestroysOnlyOnePart(GameTestHelper h){
        if(skip(h,"dry"))return;ExtensionBoilerGameTests.build(h,5,5,5,2);long[] supplied={0},drained={0};UUID[] generation={null};
        h.onEachTick(()->{var owner=ExtensionBoilerGameTests.owner(h);if(owner==null||owner.currentForm()==null)return;
            if(generation[0]==null)generation[0]=owner.boilerGeneration();long now=h.getLevel().getGameTime();
            supplied[0]+=owner.ledger().fillHot(h.absolutePos(BASE.offset(3,0,0)),2048,false,now);
            drained[0]+=owner.ledger().drainCold(h.absolutePos(BASE.offset(3,2,0)),2048,false,now);
        });
        h.runAfterDelay(650,()->{
            var owner=ExtensionBoilerGameTests.owner(h);var s=owner.ledger();var record=savedRecord(h,generation[0]);
            h.assertTrue(record!=null&&!record.burst&&record.targets.size()==1&&record.complete(),"真实供热20秒干烧没有完成一次局部损毁");
            h.assertTrue(heatingParts(h)==17&&owner.currentForm()==null,"干烧不是恰好损毁一件并使结构失效");
            h.assertTrue(s.hot()+s.cold()+drained[0]==supplied[0],"局部干烧任意丢失/复制冷热工质");
            h.assertTrue(s.totalHu()>0&&s.totalHu()<=s.cold()*.5+drained[0]*.5+.0001,"干烧未保存真实付热或出现免费HU");
            h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(Vec3.atLowerCornerOf(h.absolutePos(BASE)),Vec3.atLowerCornerOf(h.absolutePos(BASE.offset(5,5,5))))).isEmpty(),"干烧掉出完整设备");
            System.out.println("[boiler-safety] real dry feed one part, remainingHU="+s.totalHu()+", coolant="+(s.hot()+s.cold())+", ticks="+record.parameters.dryTicks());h.succeed();
        });
    }
    @GameTest(template="boiler_empty",timeoutTicks=100,batch="boiler_thermal")
    public static void blockedFullSteamPaysHeatThenBurstsAndLosesOnlyOwnFluids(GameTestHelper h){
        if(skip(h,"pressure"))return;ExtensionBoilerGameTests.build(h,5,5,5,2);h.setBlock(FIRST.above(),Blocks.STONE);UUID[] generation={null};long[] supplied={0};
        h.runAfterDelay(4,()->{var owner=ExtensionBoilerGameTests.owner(h);generation[0]=owner.boilerGeneration();seedHeat(owner,1,false);});
        h.onEachTick(()->{if(generation[0]==null)return;var owner=ExtensionBoilerGameTests.owner(h);if(owner==null||owner.currentForm()==null)return;
            supplied[0]+=owner.ledger().fillHot(h.absolutePos(BASE.offset(3,0,0)),2048,false,h.getLevel().getGameTime());
            owner.ledger().drainCold(h.absolutePos(BASE.offset(3,2,0)),2048,false,h.getLevel().getGameTime());
        });
        h.runAfterDelay(55,()->{var plan=savedRecord(h,generation[0]);var s=ExtensionBoilerGameTests.owner(h).ledger();
            h.assertTrue(plan!=null&&plan.burst&&plan.pressure>=1.5&&plan.pressure<1.6,"真实付热没有在瞬时耐压线爆裂");
            h.assertTrue(supplied[0]>0&&s.water()==0&&s.steam()==0&&s.hot()==0&&s.cold()==0&&s.totalHu()==0,"爆裂未恰好清原炉工质/HU");
            h.assertTrue(plan.targets.stream().allMatch(t->plan.inJet(Vec3.atCenterOf(t.pos))),"爆裂计划包含后方球形破坏");
            h.assertTrue(h.getLevel().getBlockState(plan.origin).isAir(),"定向爆裂没有从所选外表面形成破口");
            h.succeed();});
    }

    private static net.minecraft.server.level.ServerPlayer breakPlayer(GameTestHelper h){
        var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BoilerBreak"));
        p.gameMode.changeGameModeForPlayer(GameType.CREATIVE);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));return p;
    }
    private static void wrench(GameTestHelper h,net.minecraft.server.level.ServerPlayer player,BlockPos target){
        var pos=h.absolutePos(target);var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false));
        ((com.simibubi.create.content.equipment.wrench.IWrenchable)h.getBlockState(target).getBlock()).onSneakWrenched(h.getBlockState(target),context);
    }
    private static void dangerousBreak(GameTestHelper h,int mode){
        if(skip(h,"break"))return;ExtensionBoilerGameTests.build(h,5,5,5,2);
        h.runAfterDelay(4,()->{
            var owner=ExtensionBoilerGameTests.owner(h);seedHeat(owner,0,true);var generation=owner.boilerGeneration();var player=breakPlayer(h);
            BlockPos target=mode==0?BASE.offset(0,1,1):mode==1?BASE.offset(1,1,0):BASE.offset(1,0,1);var abs=h.absolutePos(target);
            var before=owner.savePortableData();java.util.function.Consumer<BlockEvent.BreakEvent> cancel=e->{if(e.getLevel()==h.getLevel()&&e.getPos().equals(abs))e.setCanceled(true);};
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,cancel);
            try {if(mode==0)h.assertTrue(!player.gameMode.destroyBlock(abs),"原生最终取消仍破坏外壳");else wrench(h,player,target);
                h.assertTrue(before.equals(owner.savePortableData())&&savedRecord(h,generation)==null,"末位取消仍扣料、改变计时或提交事故");
            }finally{NeoForge.EVENT_BUS.unregister(cancel);}
            if(mode==0)h.assertTrue(player.gameMode.destroyBlock(abs),"许可后的原生拆除失败");else wrench(h,player,target);
            var plan=savedRecord(h,generation);h.assertTrue(plan!=null&&!plan.burst&&plan.targets.size()==1,"危险拆除未直接提交一次干烧损毁");
            h.assertTrue(owner.ledger().totalHu()==before.getCompound("Ledger").getDouble("WaterHu"),"无压干烧拆除清空了整炉HU");
            h.succeed();
        });
    }
    @GameTest(template="boiler_empty",timeoutTicks=45,batch="boiler_break")
    public static void nativeDangerousBreakWaitsForFinalCancellation(GameTestHelper h){dangerousBreak(h,0);}
    @GameTest(template="boiler_empty",timeoutTicks=45,batch="boiler_break")
    public static void controllerSneakWrenchWaitsForFinalCancellation(GameTestHelper h){dangerousBreak(h,1);}
    @GameTest(template="boiler_empty",timeoutTicks=45,batch="boiler_break")
    public static void exchangerSneakWrenchWaitsForFinalCancellation(GameTestHelper h){dangerousBreak(h,2);}

    @GameTest(template="boiler_empty",timeoutTicks=70,batch="boiler_plan")
    public static void independentPlanKeepsDirectionSingleDamageAndProtectsNewGeneration(GameTestHelper h){
        if(skip(h,"plan"))return;ExtensionBoilerGameTests.build(h,5,5,5,2);
        h.runAfterDelay(4,()->{
            var owner=ExtensionBoilerGameTests.owner(h);seedHeat(owner,1.6,false);
            var plan=BoilerAccidentLifecycle.commit(h.getLevel(),owner,owner.currentForm(),BoilerAccidentState.Cause.BURST,BoilerAccidentState.DEFAULT);
            var live=BoilerAccidentSavedData.get(h.getLevel());var copy=BoilerAccidentSavedData.load(live.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess()).byId(plan.id);
            h.assertTrue(copy.direction==plan.direction&&copy.radius==plan.radius&&copy.cursor==plan.cursor&&copy.generation.equals(plan.generation),"事故保存重抽方向/半径/世代/游标");
            var front=EntityType.IRON_GOLEM.create(h.getLevel());var back=EntityType.IRON_GOLEM.create(h.getLevel());front.setNoAi(true);back.setNoAi(true);front.setNoGravity(true);back.setNoGravity(true);
            var center=Vec3.atCenterOf(plan.origin);var delta=Vec3.atLowerCornerOf(plan.direction.getNormal()).scale(1.5);front.setPos(center.add(delta));back.setPos(center.subtract(delta));h.getLevel().addFreshEntity(front);h.getLevel().addFreshEntity(back);
            float hp=front.getHealth(),backHp=back.getHealth();var forced=new HashSet<>(h.getLevel().getForcedChunks());
            BoilerAccidentExecutor.tick(h.getLevel());float after=front.getHealth();front.invulnerableTime=0;BoilerAccidentExecutor.tick(h.getLevel());
            h.assertTrue(after<hp&&front.getHealth()==after&&back.getHealth()==backHp&&plan.damaged.contains(front.getUUID()),"定向/按距离单次生物伤害错误");
            h.assertTrue(forced.equals(h.getLevel().getForcedChunks()),"事故强载了区块");
            // 建立第二份尚未执行计划，立即拆旧控制器重建同址新实例；旧计划必须整体终止。
            h.setBlock(BASE.offset(1,1,0),Blocks.AIR);ExtensionBoilerGameTests.build(h,5,5,5,2);
            h.runAfterDelay(2,()->{
                var next=ExtensionBoilerGameTests.owner(h);h.assertTrue(next.currentForm()!=null,"重建控制器尚未完成onLoad");seedHeat(next,1.6,false);
                var pending=BoilerAccidentLifecycle.commit(h.getLevel(),next,next.currentForm(),BoilerAccidentState.Cause.BURST,BoilerAccidentState.DEFAULT);
                h.setBlock(BASE.offset(1,1,0),Blocks.AIR);ExtensionBoilerGameTests.build(h,5,5,5,2);
                h.runAfterDelay(3,()->{var replacement=ExtensionBoilerGameTests.owner(h);h.assertTrue(pending.complete()&&replacement.currentForm()!=null,"旧事故破坏了同址新世代");h.succeed();});
            });
        });
    }
    @GameTest(template="boiler_empty",timeoutTicks=70,batch="boiler_plan")
    public static void sharedBudgetAndSavedCursorContinueAfterControllerRemoval(GameTestHelper h){
        if(skip(h,"plan"))return;ExtensionBoilerGameTests.build(h,5,5,5,2);
        h.runAfterDelay(4,()->{
            var owner=ExtensionBoilerGameTests.owner(h);seedHeat(owner,1.6,false);
            var plan=BoilerAccidentLifecycle.commit(h.getLevel(),owner,owner.currentForm(),BoilerAccidentState.Cause.BURST,BoilerAccidentState.DEFAULT);
            var shared=ReactorAccidentSavedData.get(h.getLevel());shared.availableBudget(h.getLevel().getGameTime(),3);
            int first=BoilerAccidentExecutor.tick(h.getLevel()),second=BoilerAccidentExecutor.tick(h.getLevel());
            h.assertTrue(first+second<=3&&plan.cursor<plan.targets.size(),"同tick/多入口突破共享收紧预算");
            var restored=BoilerAccidentSavedData.load(BoilerAccidentSavedData.get(h.getLevel()).save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess()).byId(plan.id);
            h.assertTrue(restored.cursor==plan.cursor&&restored.shockComplete==plan.shockComplete&&restored.direction==plan.direction&&restored.damaged.equals(plan.damaged),"正常保存丢动作/伤害收据");
            h.setBlock(BASE.offset(1,1,0),Blocks.AIR);
            h.runAfterDelay(5,()->{h.assertTrue(plan.complete()&&h.getLevel().getBlockState(plan.origin).isAir(),"控制器消失后计划无法独立完成");h.succeed();});
        });
    }
    @GameTest(template="boiler_empty",timeoutTicks=70,batch="boiler_environment")
    public static void sharedEnvironmentTransactionPreservesContainersFuelAndCancellation(GameTestHelper h){
        if(skip(h,"environment"))return;
        var chest=new BlockPos(10,3,10);var denied=new BlockPos(14,3,10);var fuel=new BlockPos(18,3,10);var stationAt=new BlockPos(22,3,10);
        h.setBlock(chest,Blocks.CHEST);h.setBlock(denied,Blocks.CHEST);h.setBlock(fuel,P1Blocks.REACTOR_REFUELING_PORT.get());
        var stationBlock=FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get();h.setBlock(stationAt,stationBlock.defaultBlockState().setValue(ShieldedAssemblyBlock.EXPANDED,true));stationBlock.setPlacedBy(h.getLevel(),h.absolutePos(stationAt),h.getBlockState(stationAt),null,ItemStack.EMPTY);
        h.runAfterDelay(4,()->{
            var level=h.getLevel();var actor=BoilerAccidentLifecycle.environmentActor(level);
            ((net.minecraft.world.level.block.entity.ChestBlockEntity)h.getBlockEntity(chest)).setItem(0,new ItemStack(Items.DIAMOND,3));
            var protectedChest=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getBlockEntity(denied);protectedChest.setItem(0,new ItemStack(Items.EMERALD,5));
            var port=(com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity)h.getBlockEntity(fuel);var component=new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get());component.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("环境原件"));port.setFuelAssembly(component);
            var station=(ShieldedAssemblyBlockEntity)h.getBlockEntity(stationAt);h.assertTrue(station.complete(),"装配台夹具未成型");station.insert(new ItemStack(AccidentContent.ACCIDENT_FUEL_WASTE.get()));var inventory=station.savePortableData();
            java.util.function.Consumer<BlockEvent.BreakEvent> cancel=e->{if(e.getLevel()==level&&e.getPos().equals(h.absolutePos(denied)))e.setCanceled(true);};NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,cancel);
            try{h.assertTrue(MoltenCoriumEffects.removeEnvironmentBlock(level,h.absolutePos(denied),actor,true).writes()==0&&protectedChest.getItem(0).getCount()==5,"环境最终取消吞料/掉料");}finally{NeoForge.EVENT_BUS.unregister(cancel);}
            h.assertTrue(MoltenCoriumEffects.removeEnvironmentBlock(level,h.absolutePos(chest),actor,true).writes()>0,"环境容器移除未提交");
            h.assertTrue(MoltenCoriumEffects.removeEnvironmentBlock(level,h.absolutePos(fuel),actor,true).writes()>0&&port.fuelAssembly().isEmpty(),"环境燃料口未只移出真实单件");
            h.assertTrue(MoltenCoriumEffects.removeEnvironmentBlock(level,h.absolutePos(stationAt),actor,true).writes()>0&&h.getBlockState(stationAt).isAir()&&h.getBlockState(stationAt.above()).isAir(),"多格装配台未共同移除");
            var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(8,2,8))),Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(26,7,14)))));
            h.assertTrue(drops.stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum()==3,"容器钻石不守恒");
            h.assertTrue(drops.stream().filter(e->ItemStack.matches(e.getItem(),component)).mapToInt(e->e.getItem().getCount()).sum()==1,"核燃料原件重复/丢失");
            var machine=drops.stream().filter(e->e.getItem().is(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get())).toList();
            h.assertTrue(machine.size()==1&&machine.getFirst().getItem().get(DataComponents.CUSTOM_DATA).copyTag().getCompound("CniShieldedAssembly").equals(inventory),"装配台携带库存重复/丢失");
            h.setBlock(new BlockPos(10,3,16),Blocks.BEDROCK);h.assertTrue(MoltenCoriumEffects.removeEnvironmentBlock(level,h.absolutePos(new BlockPos(10,3,16)),actor,true).writes()==0,"环境移除损毁基岩");h.succeed();
        });
    }
    @GameTest(template="boiler_empty",timeoutTicks=70,batch="boiler_industrial")
    public static void industrialDeliveryBeforeSafetyAvoidsFalseBurst(GameTestHelper h){
        if(skip(h,"industrial"))return;ExtensionBoilerGameTests.build(h,5,5,5,2);var second=BASE.offset(0,3,2);
        h.setBlock(second,BoilerContent.STEAM_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING,Direction.WEST));
        h.setBlock(BASE.offset(-1,3,1),IndustrialFluidContent.CERAMIC_PIPE.get());h.setBlock(BASE.offset(-2,3,1),IndustrialFluidContent.CERAMIC_PIPE.get());h.setBlock(BASE.offset(-1,3,2),IndustrialFluidContent.CERAMIC_PIPE.get());var tank=BASE.offset(-3,3,1);h.setBlock(tank,com.simibubi.create.AllBlocks.FLUID_TANK.get());
        boolean[] armed={false},sampled={false};RuntimeException[] error={null};
        java.util.function.Consumer<net.neoforged.neoforge.event.tick.LevelTickEvent.Post> before=e->{if(e.getLevel()==h.getLevel()&&armed[0])try{var owner=ExtensionBoilerGameTests.owner(h);owner.selectMinimum(0);seedHeat(owner,1.6,false);owner.tick();}catch(RuntimeException x){error[0]=x;}};
        java.util.function.Consumer<net.neoforged.neoforge.event.tick.LevelTickEvent.Post> after=e->{if(e.getLevel()==h.getLevel()&&armed[0]){armed[0]=false;sampled[0]=true;if(error[0]==null)try{h.assertTrue(ExtensionBoilerGameTests.owner(h).ledger().pressure()<1,"实际工业送汽没有在当tick降到安全炉压");}catch(RuntimeException x){error[0]=x;}}};
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH,before);NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,after);
        h.runAfterDelay(6,()->armed[0]=true);h.runAfterDelay(9,()->{NeoForge.EVENT_BUS.unregister(before);NeoForge.EVENT_BUS.unregister(after);h.assertTrue(sampled[0],"未观察真实Level Post");if(error[0]!=null)throw error[0];var owner=ExtensionBoilerGameTests.owner(h);h.assertTrue(owner.accident().pending()==null&&savedRecord(h,owner.boilerGeneration())==null&&owner.accident().pressureTicks()==0,"事故在实际工业交付前误判");h.succeed();});
    }
    /** 单目标冻结夹具只隔离事务暂缓/身份；热工到自然触发由前面的真实供热用例独立覆盖。 */
    private static BoilerAccidentLedger singleTarget(GameTestHelper h,BlockPos target,boolean burst){
        var owner=ExtensionBoilerGameTests.owner(h);var plan=new BoilerAccidentLedger(UUID.randomUUID(),owner.boilerGeneration(),owner.getBlockPos(),target,Direction.UP,burst,1.5,
                BoilerAccidentState.DEFAULT,List.of(new BoilerAccidentLedger.Target(h.getLevel(),target)));
        plan.shockComplete=true;BoilerAccidentSavedData.get(h.getLevel()).add(plan);owner.commitAccident(plan);return plan;
    }
    @GameTest(template="boiler_empty",timeoutTicks=70,batch="boiler_fix_budget")
    public static void insufficientAssemblyBudgetDefersSameCursorThenCommits(GameTestHelper h){
        if(skip(h,"fix"))return;ExtensionBoilerGameTests.build(h,5,5,5,2);var pos=new BlockPos(16,3,14);var block=FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get();
        h.setBlock(pos,block.defaultBlockState().setValue(ShieldedAssemblyBlock.EXPANDED,true));block.setPlacedBy(h.getLevel(),h.absolutePos(pos),h.getBlockState(pos),null,ItemStack.EMPTY);
        h.runAfterDelay(4,()->{
            var target=h.absolutePos(pos);var station=(ShieldedAssemblyBlockEntity)h.getBlockEntity(pos);station.insert(new ItemStack(AccidentContent.ACCIDENT_FUEL_WASTE.get()));
            var plan=singleTarget(h,target,true);var shared=ReactorAccidentSavedData.get(h.getLevel());shared.availableBudget(h.getLevel().getGameTime(),1);
            h.assertTrue(BoilerAccidentExecutor.tick(h.getLevel())==0&&plan.cursor==0&&!plan.complete()&&station.complete(),"不足整台装配台额度时永久推进了游标");
            h.runAfterDelay(3,()->{h.assertTrue(plan.complete()&&h.getBlockState(pos).isAir()&&h.getBlockState(pos.above()).isAir(),"下一tick足额后没有重试原装配台");h.succeed();});
        });
    }
    @GameTest(template="boiler_empty",timeoutTicks=70,batch="boiler_fix_loading")
    public static void missingReadDomainDefersWithoutLoadingThenResumes(GameTestHelper h){
        if(skip(h,"read_domain"))return;ExtensionBoilerGameTests.build(h,5,5,5,2);
        h.runAfterDelay(4,()->{
            var level=h.getLevel();var start=new net.minecraft.world.level.ChunkPos(h.absolutePos(BASE));int edge=start.x;
            for(int n=1;n<80&&level.getChunkSource().getChunkNow(start.x+n,start.z)!=null;n++)edge=start.x+n;
            h.assertTrue(level.getChunkSource().getChunkNow(edge+1,start.z)==null,"夹具未找到已加载边界");
            var target=new BlockPos(edge*16+14,h.absolutePos(BASE).getY()+8,start.z*16+8);level.setBlock(target,Blocks.CHEST.defaultBlockState(),18);
            level.setBlock(target.east(),Blocks.STONE.defaultBlockState(),18);
            h.assertTrue(level.getChunkSource().getChunkNow(edge+1,start.z)==null,"夹具布置提前加载了导体后的第二邻格");
            var plan=singleTarget(h,target,true);
            h.assertTrue(level.getChunkSource().getChunkNow(edge+1,start.z)==null,"冻结BE凭证经输出信号通知强载第二邻格");
            h.assertTrue(BoilerAccidentExecutor.tick(level)==0&&plan.cursor==0&&level.getChunkSource().getChunkNow(edge+1,start.z)==null,"读取域暂缺被跳过或生产入口强载");
            // 仅隔离测试夹具显式载入缺口；生产入口上述断言已经证明没有加载该区块。
            level.getChunk(edge+1,start.z);
            h.runAfterDelay(3,()->{h.assertTrue(plan.complete()&&level.getBlockState(target).isAir(),"读取域恢复后没有继续原目标");h.succeed();});
        });
    }
    private static void claimedPartReplacement(GameTestHelper h,boolean exchanger){
        if(skip(h,"fix"))return;ExtensionBoilerGameTests.build(h,5,5,5,2);
        h.runAfterDelay(4,()->{
            var level=h.getLevel();var relative=BASE.offset(1,exchanger?0:2,1);var pos=h.absolutePos(relative);var state=level.getBlockState(pos);
            var plan=singleTarget(h,pos,false);var player=breakPlayer(h);player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            var item=exchanger?HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER_ITEM.get():BoilerContent.HEAT_SECTION_ITEM.get();
            ReactorAccidentSavedData.get(level).availableBudget(level.getGameTime(),0);
            h.assertTrue(player.gameMode.destroyBlock(pos),"原事故件的原生拆除失败");
            h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).stream().noneMatch(e->e.getItem().is(item)),"原事故件仍掉完整设备");
            level.setBlock(pos,state,18);var replacement=level.getBlockEntity(pos);
            h.assertTrue(!plan.targets.getFirst().matches(level)&&!BoilerAccidentLifecycle.consumedComponent(level,pos,state,replacement),"同态新件仍被旧认领/计划命中");
            if(exchanger)wrench(h,player,relative);else h.assertTrue(player.gameMode.destroyBlock(pos),"新再加热段无法正常采掘");
            int inventory=player.getInventory().items.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();
            int dropped=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();
            h.assertTrue(inventory+dropped==1,"同态新件不是恰好一个完整掉落："+(inventory+dropped));h.succeed();
        });
    }
    @GameTest(template="boiler_empty",timeoutTicks=70,batch="boiler_fix_be_replacement")
    public static void claimedExchangerDoesNotConsumeSameStateReplacement(GameTestHelper h){claimedPartReplacement(h,true);}
    @GameTest(template="boiler_empty",timeoutTicks=70,batch="boiler_fix_plain_replacement")
    public static void claimedReheatSectionDoesNotConsumeSameStateReplacement(GameTestHelper h){claimedPartReplacement(h,false);}
    @GameTest(template="boiler_empty",timeoutTicks=70,batch="boiler_fix_final_replacement")
    public static void sameStateReplacementInsideFinalProbeIsNotRemoved(GameTestHelper h){
        if(skip(h,"fix"))return;ExtensionBoilerGameTests.build(h,5,5,5,2);
        h.runAfterDelay(4,()->{
            var level=h.getLevel();var pos=h.absolutePos(BASE.offset(1,2,1));var state=level.getBlockState(pos);var plan=singleTarget(h,pos,false);
            java.util.function.Consumer<BlockEvent.BreakEvent> replace=e->{if(e.getLevel()==level&&e.getPos().equals(pos)){
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);level.setBlock(pos,state,18);
            }};
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,replace);
            try{BoilerAccidentExecutor.tick(level);}finally{NeoForge.EVENT_BUS.unregister(replace);}
            h.assertTrue(plan.complete()&&level.getBlockState(pos)==state,"最终许可期间同态新件被旧事务移除");h.succeed();
        });
    }
    @GameTest(template="boiler_empty",timeoutTicks=70,batch="boiler_fix_identity")
    public static void persistentTargetIdentitySurvivesChangingNbtAndRejectsNewInstance(GameTestHelper h){
        if(skip(h,"fix"))return;ExtensionBoilerGameTests.build(h,5,5,5,2);var pos=new BlockPos(14,3,14);h.setBlock(pos,HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get());
        h.runAfterDelay(4,()->{
            var level=h.getLevel();var be=h.getBlockEntity(pos);var target=new BoilerAccidentLedger.Target(level,h.absolutePos(pos));var owner=ExtensionBoilerGameTests.owner(h);
            var original=new BoilerAccidentLedger(UUID.randomUUID(),owner.boilerGeneration(),owner.getBlockPos(),h.absolutePos(pos),Direction.UP,false,0,BoilerAccidentState.DEFAULT,List.of(target));
            var before=be.saveWithFullMetadata(level.registryAccess());
            h.runAfterDelay(3,()->{
                var after=be.saveWithFullMetadata(level.registryAccess());h.assertTrue(!before.equals(after),"原BE没有自然tick改变运行NBT");
                var restored=BoilerAccidentLedger.load(original.save(),level.registryAccess());
                h.assertTrue(restored.targets.getFirst().matches(level),"易变NBT导致重载计划拒绝原对象");
                var state=level.getBlockState(h.absolutePos(pos));level.setBlock(h.absolutePos(pos),state.setValue(com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlock.LIT,!state.getValue(com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlock.LIT)),18);
                h.assertTrue(restored.targets.getFirst().matches(level),"运行LIT属性被误当放置身份");
                BoilerAccidentSavedData.get(level).add(restored);owner.commitAccident(restored);BoilerAccidentExecutor.tick(level);
                h.assertTrue(restored.complete()&&h.getBlockState(pos).isAir(),"重载计划无法实际继续处理原对象");
                h.setBlock(pos,HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get());h.assertTrue(!restored.targets.getFirst().matches(level),"新BE继承了原对象凭证");h.succeed();
            });
        });
    }
    @GameTest(template="boiler_empty",timeoutTicks=90,batch="boiler_fix_relocation")
    public static void carriedPendingControllerReplacesAcrossDimensionWithoutSoftLock(GameTestHelper h){
        if(skip(h,"relocation"))return;ExtensionBoilerGameTests.build(h,5,5,5,2);
        h.runAfterDelay(4,()->{
            var source=h.getLevel();var owner=ExtensionBoilerGameTests.owner(h);seedHeat(owner,0,true);var generation=owner.boilerGeneration();var player=breakPlayer(h);player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            var states=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();for(var offset:BlockPos.betweenClosed(BlockPos.ZERO,new BlockPos(4,4,4)))states.put(offset.immutable(),h.getBlockState(BASE.offset(offset)));
            ReactorAccidentSavedData.get(source).availableBudget(source.getGameTime(),0);wrench(h,player,BASE.offset(1,1,0));
            var slot=java.util.stream.IntStream.range(0,player.getInventory().items.size()).filter(n->player.getInventory().items.get(n).is(BoilerContent.CONTROLLER_ITEM.get())).findFirst().orElseThrow();
            var carried=player.getInventory().removeItem(slot,1);var carriedData=carried.get(DataComponents.CUSTOM_DATA).copyTag().getCompound("CniBoiler");
            h.assertTrue(carriedData.getCompound("Accident").hasUUID("Pending"),"生存扳手未得到含待执行状态的原携带物");var plan=BoilerAccidentSavedData.get(source).byId(carriedData.getCompound("Accident").getUUID("Pending"));
            var destination=source.getServer().getLevel(net.minecraft.world.level.Level.NETHER);var min=new BlockPos(80,100,80);var chunk=new net.minecraft.world.level.ChunkPos(min);boolean wasForced=destination.getForcedChunks().contains(chunk.toLong());
            var ticket=net.minecraft.server.level.TicketType.<net.minecraft.world.level.ChunkPos>create("cni_boiler_relocation",Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong));
            destination.getChunkSource().addRegionTicket(ticket,chunk,2,chunk);
            Runnable cleanup=()->{destination.getChunkSource().removeRegionTicket(ticket,chunk,2,chunk);if(!wasForced)destination.setChunkForced(chunk.x,chunk.z,false);};
            // 目的维度票据仅属隔离夹具，模拟玩家所在的可运行区块；结束后恢复原票据状态。
            if(!wasForced)destination.setChunkForced(chunk.x,chunk.z,true);destination.getChunk(chunk.x,chunk.z);
            var control=min.offset(1,1,0);states.forEach((offset,state)->destination.setBlock(min.offset(offset),offset.equals(new BlockPos(1,1,0))?Blocks.AIR.defaultBlockState():state,18));
            var receiver=net.neoforged.neoforge.common.util.FakePlayerFactory.get(destination,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BoilerRelocate"));receiver.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);receiver.setYRot(0);receiver.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,carried);
            var context=new net.minecraft.world.item.context.UseOnContext(receiver,net.minecraft.world.InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(control.below()).add(0,.5,0),Direction.UP,control.below(),false));
            try{h.assertTrue(((BlockItem)carried.getItem()).place(new net.minecraft.world.item.context.BlockPlaceContext(context)).consumesAction(),"目的维度真实BlockItem放置失败");}
            catch(RuntimeException failure){cleanup.run();throw failure;}
            h.runAfterDelay(70,()->{try{
                var placed=(BoilerControllerBlockEntity)destination.getBlockEntity(control);
                h.assertTrue(placed!=null&&placed.accident().pending()==null&&!placed.boilerGeneration().equals(generation),"跨维度携带没有解除旧现场等待身份");
                h.assertTrue(BoilerStructure.inspect(destination,control)!=null,"目的维度真实重新落位的几何结构不合法");
                h.assertTrue(plan.complete()&&receiver.getMainHandItem().isEmpty()&&placed.ledger().hot()==0&&placed.ledger().cold()==0&&placed.ledger().warmHu()>0,"旧任务未继续或携带物/真实库存重复丢失");
                if(placed.current())h.assertTrue(placed.currentForm()!=null,"已就绪新实例未恢复成型");
                else System.out.println("[boiler-relocate] SUBCASE_UNAVAILABLE actual formed transaction: pending=null newGeneration=true geometry=valid oldPlan=complete item=consumed"
                        +" chunk="+destination.getChunkSource().getChunkNow(chunk.x,chunk.z).getFullStatus()+" blocksTick="+destination.shouldTickBlocksAt(control)
                        +" entitiesLoaded="+destination.areEntitiesLoaded(chunk.toLong())+" border="+destination.getWorldBorder().isWithinBounds(control));
                h.succeed();
            }finally{cleanup.run();}});
            h.runAfterDelay(75,cleanup);
        });
    }
}
