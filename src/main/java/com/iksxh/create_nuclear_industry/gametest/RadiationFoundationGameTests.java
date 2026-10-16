package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.radiation.*;
import com.iksxh.create_nuclear_industry.blockentity.*;
import com.iksxh.create_nuclear_industry.content.*;
import com.iksxh.create_nuclear_industry.storage.*;
import com.iksxh.create_nuclear_industry.reactor.*;
import com.iksxh.create_nuclear_industry.structure.*;
import com.iksxh.create_nuclear_industry.mixin.*;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.*;

/** 独立真实服域：不登录未握手连接，不触碰携带的用户世界。 */
@PrefixGameTestTemplate(false)
public final class RadiationFoundationGameTests {
    private static final RadiationParameters P=RadiationParameters.DEFAULT;
    private static final BlockPos OWNER=new BlockPos(2,2,0),CAP=new BlockPos(1,4,1);
    private RadiationFoundationGameTests() {}
    private static ItemStack spent(){return new ItemStack(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get());}
    private static ItemStack used(){return FuelUseHistory.markUsedCopy(new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get()));}
    private static double sample(GameTestHelper h,Vec3 target) {
        RadiationSourceIndex.forget(h.getLevel());var s=RadiationSourceIndex.get(h.getLevel()).sampleAt(h.getLevel(),target,P);
        h.assertTrue(s.complete(),"标准场景采样超预算");return s.fieldDosePerSecond();
    }
    private static void near(GameTestHelper h,double expected,double actual,String why){h.assertTrue(Math.abs(expected-actual)<1e-8,why+" expected="+expected+" actual="+actual);}
    private static void small(GameTestHelper h) {
        Map<CoreColumnPosition,ReactorStructureDefinition.ColumnType> layout=new HashMap<>();
        for(int x=0;x<3;x++)for(int z=0;z<3;z++)layout.put(new CoreColumnPosition(x,z),x==0&&z==0?ReactorStructureDefinition.ColumnType.FUEL:ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        ReactorStructureDefinition.templateFor(layout).forEach((pos,id)->h.setBlock(new BlockPos(pos.x(),pos.y(),pos.z()),BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse(id))));
    }
    private static ReactorInstrumentPortBlockEntity owner(GameTestHelper h){return (ReactorInstrumentPortBlockEntity)h.getBlockEntity(OWNER);}
    private static ReactorPortBlockEntity port(GameTestHelper h){return (ReactorPortBlockEntity)h.getBlockEntity(CAP);}
    private static void ready(GameTestHelper h,Runnable action) {
        small(h);h.runAfterDelay(5,()->{ReactorStructureLifecycle.rescanAroundNow(h.getLevel(),h.absolutePos(OWNER));h.assertTrue(owner(h).structureValid(),"小堆未成型");action.run();});
    }
    @GameTest(template="radiation_empty",templateNamespace="cni_radiation_foundation",timeoutTicks=100)
    public static void fractionalBurnMarksOnlyCommittedFuel(GameTestHelper h) {
        ready(h,()->{
            var owner=owner(h);var port=port(h);var original=new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get());
            original.set(DataComponents.CUSTOM_NAME,Component.literal("真实组件历史"));port.setFuelAssembly(original);
            var rods=new TreeMap<CoreColumnPosition,ControlRodColumnState>();owner.snapshot().controlRodColumns().forEach((key,value)->rods.put(key,new ControlRodColumnState(1,.99,.99,false,0)));
            owner.setSnapshot(owner.snapshot().withColumns(owner.snapshot().fuelColumns(),rods));owner.tickReactor();
            var actual=port.fuelAssembly();h.assertTrue(FuelUseHistory.isUsed(actual),"首次真实小数燃耗未标记");h.assertTrue(actual.getDamageValue()==0,"夹具不是不足1点耐久");
            h.assertTrue(!FuelUseHistory.isUsed(original),"原件被副本准备污染");
            var saved=actual.save(h.getLevel().registryAccess());var loaded=ItemStack.parse(h.getLevel().registryAccess(),saved).orElseThrow();
            h.assertTrue(ItemStack.matches(actual,loaded)&&FuelUseHistory.isUsed(loaded),"当前物品正常保存加载丢历史");
            var allInserted=new TreeMap<CoreColumnPosition,ControlRodColumnState>();rods.forEach((key,value)->allInserted.put(key,ControlRodColumnState.fullyInserted()));
            owner.setSnapshot(owner.snapshot().withColumns(owner.snapshot().fuelColumns(),allInserted));
            var arm=FuelRefuelingArmInteractionPoint.TYPE.createPoint(h.getLevel(),h.absolutePos(CAP),port.getBlockState());
            var extracted=arm.extract(null,0,1,false);h.assertTrue(FuelUseHistory.isUsed(extracted)&&port.fuelAssembly().isEmpty(),"真实臂端点提取丢历史");
            h.assertTrue(arm.insert(null,new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get()),false).isEmpty(),"新件无法装入");
            owner.tickReactor();h.assertTrue(!FuelUseHistory.isUsed(port.fuelAssembly()),"全插棒/邻列传热给新件继承历史");
            port.clearBinding(owner,true);var before=port.fuelAssembly();h.assertTrue(!owner.tickReactor()&&ItemStack.matches(before,port.fuelAssembly()),"取消tick修改了真实物品");
            System.out.println("[radiation] fractional-burn damage=0 marker=true arm-extract=true current-item-save-load=true rejected-commit-unchanged=true");h.succeed();
        });
    }
    @GameTest(template="radiation_empty",templateNamespace="cni_radiation_foundation",timeoutTicks=100)
    public static void containmentTracksShellNotRuntime(GameTestHelper h) {
        ready(h,()->{
            port(h).setFuelAssembly(used());Vec3 inside=RadiationContainment.sourcePosition(h.getLevel(),h.absolutePos(CAP));Vec3 outside=inside.add(-3,0,0);
            near(h,0,sample(h,outside),"完整停机外壳未屏蔽");long reads=RadiationContainment.get(h.getLevel()).shellReads();
            near(h,0,sample(h,outside),"稳定壳缓存改变");h.assertTrue(reads==RadiationContainment.get(h.getLevel()).shellReads(),"稳定壳被重复扫描");
            near(h,1,sample(h,inside),"堆内被当成安全区");
            h.setBlock(new BlockPos(3,2,3),Blocks.STONE);ReactorStructureLifecycle.rescanAroundNow(h.getLevel(),h.absolutePos(OWNER));
            near(h,0,sample(h,outside),"堆内机构失效取消了完整外壳屏蔽");
            var hole=new BlockPos(0,1,1);h.setBlock(hole,Blocks.AIR);h.assertTrue(sample(h,outside)>0,"真实壳破口未取消屏蔽");
            h.setBlock(hole,P1Blocks.REACTOR_CASING.get());near(h,0,sample(h,outside),"补壳未恢复屏蔽");
            BlockPos cap=new BlockPos(2,4,2);h.setBlock(cap,P1Blocks.REACTOR_WINDOW.get());
            h.assertTrue(sample(h,outside)>0,"非法顶帽观察窗仍获得完整有效外壳零外泄");
            h.setBlock(cap,P1Blocks.CONTROL_ROD_DRIVE.get());near(h,0,sample(h,outside),"合法顶帽恢复后没有重新屏蔽");
            var data=RadiationContainment.get(h.getLevel()).save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());h.assertTrue(!data.getList("Frames",10).isEmpty(),"已知边界没有保存");
            System.out.println("[radiation] containment stopped/invalid-interior/breach/repair/cache/persist-geometry assertions passed");h.succeed();
        });
    }
    private static double adapted(GameTestHelper h,net.minecraft.world.level.block.entity.BlockEntity be) {
        double[] result={0};RadiationSourceAdapters.read(be,s->result[0]+=RadiationItemSources.describe(s.stack(),P).dosePerSecond()*(s.rackShielded()?P.rackTransmission():1));return result[0];
    }
    @GameTest(template="radiation_empty",templateNamespace="cni_radiation_foundation",timeoutTicks=100)
    public static void handoffCountsPayloadOnce(GameTestHelper h) {
        BlockPos armPos=new BlockPos(2,2,2),beltPos=new BlockPos(5,2,2),depotPos=new BlockPos(8,2,2),boxPos=new BlockPos(11,2,2),rackPos=new BlockPos(14,2,2);
        h.setBlock(armPos,AllBlocks.MECHANICAL_ARM.get());h.setBlock(beltPos,AllBlocks.BELT.get());h.setBlock(depotPos,AllBlocks.DEPOT.get());h.setBlock(boxPos,Blocks.CHEST);h.setBlock(rackPos,SpentFuelStorageContent.DRY_STORAGE_RACK.get());
        var initialBelt=(BeltBlockEntity)h.getBlockEntity(beltPos);initialBelt.setController(initialBelt.getBlockPos());initialBelt.beltLength=1;
        h.runAfterDelay(2,()->{
            try {
                var arm=(ArmBlockEntity)h.getBlockEntity(armPos);var held=ArmBlockEntity.class.getDeclaredField("heldItem");held.setAccessible(true);held.set(arm,used());near(h,1,adapted(h,arm),"臂真实持物未读到");
                ItemStack moving=((RadiationArmAccessor)arm).radiation$getHeldItem();held.set(arm,ItemStack.EMPTY);
                var belt=(BeltBlockEntity)h.getBlockEntity(beltPos);belt.setController(belt.getBlockPos());belt.beltLength=1;
                var transported=new TransportedItemStack(moving);transported.beltPosition=.5f;belt.getInventory().addItem(transported);
                near(h,1,adapted(h,belt),"带待入队载荷漏读");
                var queues=(RadiationBeltAccessor)belt.getInventory();belt.getInventory().getTransportedItems().add(transported);
                near(h,1,adapted(h,belt),"同一运输实例在待入队和正式表双算");
                queues.radiation$getToInsert().clear();queues.radiation$getToRemove().add(transported);near(h,0,adapted(h,belt),"带已取走队列仍放射");
                belt.getInventory().getTransportedItems().clear();queues.radiation$getToRemove().clear();
                var depot=(DepotBlockEntity)h.getBlockEntity(depotPos);depot.setHeldItem(moving);near(h,1,adapted(h,depot),"置物台载荷漏读");depot.setHeldItem(ItemStack.EMPTY);
                var box=(ChestBlockEntity)h.getBlockEntity(boxPos);var nested=new ItemStack(Items.SHULKER_BOX);nested.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(moving)));box.setItem(0,nested);near(h,1,adapted(h,box),"普通嵌套容器额外屏蔽");
                box.setItem(0,SpentFuelPayload.seal(spent()));near(h,.1,adapted(h,box),"普通箱中的合法桶未恰好屏蔽一次");
                var rack=(DryStorageBlockEntity)h.getBlockEntity(rackPos);ItemStack sealed=box.removeItemNoUpdate(0);h.assertTrue(rack.insertOne(sealed),"架插入真实桶失败");near(h,0,adapted(h,rack),"完整架未完全屏蔽");
                h.assertTrue(box.isEmpty()&&((RadiationArmAccessor)arm).radiation$getHeldItem().isEmpty()&&depot.getHeldItem().isEmpty(),"交接残留第二份载荷");
                System.out.println("[radiation] real BE arm/belt-pending/belt-identity/removal/depot/chest/nested/cask/rack projections passed; staged inventory handoffs, powered animation remains manual");h.succeed();
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        });
    }
    @GameTest(template="radiation_empty",templateNamespace="cni_radiation_foundation",timeoutTicks=100)
    public static void wallAndUnloadedChunkAndInventoryIdentity(GameTestHelper h) {
        BlockPos pos=new BlockPos(4,2,4);h.setBlock(pos,Blocks.CHEST);var box=(ChestBlockEntity)h.getBlockEntity(pos);box.setItem(0,spent());
        Vec3 source=Vec3.atCenterOf(h.absolutePos(pos)),target=source.add(2,0,0);near(h,.5,sample(h,target),"2格距离不为1/4");
        h.setBlock(pos.east(),ReactorCraftingContent.SHIELDING_CONCRETE.get());near(h,.025,sample(h,target),"一格墙重复/漏乘");
        var forced=new HashSet<>(h.getLevel().getForcedChunks());sample(h,new Vec3(30_000_000,200,30_000_000));h.assertTrue(forced.equals(h.getLevel().getForcedChunks()),"源索引强载了区块");
        box.removeItemNoUpdate(0);h.setBlock(pos,Blocks.AIR);h.setBlock(pos,Blocks.CHEST);near(h,0,sample(h,target),"同坐标新BE继承幽灵源");
        h.setBlock(pos.east(4),AllBlocks.ITEM_VAULT.get());var vault=(ItemVaultBlockEntity)h.getBlockEntity(pos.east(4));vault.getInventoryOfBlock().setStackInSlot(0,spent());near(h,2,adapted(h,vault),"保险库局部库存漏读");
        h.setBlock(pos.south(4),AllBlocks.BASIN.get());var basin=(BasinBlockEntity)h.getBlockEntity(pos.south(4));basin.getInputInventory().setStackInSlot(0,spent());basin.getOutputInventory().setStackInSlot(0,spent());near(h,4,adapted(h,basin),"盆输入输出真实库存漏读");
        System.out.println("[radiation] inverse-square/wall/unloaded-chunks/replaced-BE/vault/basin assertions passed");h.succeed();
    }
    private static FakePlayer fake(GameTestHelper h,String name) {var player=new VulnerableFakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),name));player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);player.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(20,3,20))));return player;}
    /** FakePlayer原版固定免伤且不tick；测试只解除这两项夹具保护，不建立真人连接。 */
    private static final class VulnerableFakePlayer extends FakePlayer {
        VulnerableFakePlayer(net.minecraft.server.level.ServerLevel level,GameProfile profile) {
            super(level,profile);
            try {var timer=net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");timer.setAccessible(true);timer.setInt(this,0);}
            catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}
    }
    @GameTest(template="radiation_empty",templateNamespace="cni_radiation_foundation",timeoutTicks=100)
    public static void doseLifecycleAndDisabledRecovery(GameTestHelper h) {
        var player=fake(h,"RadiationDose");player.setHealth(20);RadiationServerEvents.save(player,new RadiationDoseState(85,0,0));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,200,1));RadiationServerEvents.applySymptoms(player,RadiationServerEvents.state(player),P,true);
        h.assertTrue(player.getEffect(MobEffects.WEAKNESS).getAmplifier()==1,"辐射覆盖更强的其他来源效果");
        for(int i=0;i<40;i++)RadiationServerEvents.tick(new PlayerTickEvent.Post(player));h.assertTrue(player.getHealth()==19,"40在线tick高剂量未扣1HP");
        player.removeAllEffects();h.assertTrue(RadiationServerEvents.state(player).dose()>0,"牛奶/效果移除清除了剂量");
        var savedPlayer=player.saveWithoutId(new net.minecraft.nbt.CompoundTag());var loadedPlayer=fake(h,"RadiationLoaded");loadedPlayer.load(savedPlayer);
        h.assertTrue(RadiationServerEvents.state(player).equals(RadiationServerEvents.state(loadedPlayer)),"当前玩家完整NBT保存加载丢失剂量或计时");
        var next=fake(h,"RadiationClone");NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(next,player,false));near(h,RadiationServerEvents.state(player).dose(),RadiationServerEvents.state(next).dose(),"非死亡克隆丢剂量");
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(next,player,true));near(h,0,RadiationServerEvents.state(next).dose(),"死亡未清剂量");
        player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(RadiationContent.HELMET.get()));player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(RadiationContent.CHESTPLATE.get()));player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(RadiationContent.LEGGINGS.get()));player.setItemSlot(EquipmentSlot.FEET,new ItemStack(RadiationContent.BOOTS.get()));near(h,.95,RadiationEquipment.reduction(player,P),"真实穿戴防护比例不正确");
        player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);near(h,.70,RadiationEquipment.reduction(player,P),"背包/其他槽错误叠加头部防护");
        h.assertTrue(new net.minecraft.world.damagesource.DamageSource(h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(RadiationServerEvents.DAMAGE)).is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR),"辐射伤害不绕过普通护甲");
        System.out.println("[radiation] FakePlayer damage/effects/milk/clone/death/suit/damage-tag assertions passed; no client login used");h.succeed();
    }
    @GameTest(template="radiation_empty",templateNamespace="cni_radiation_foundation",timeoutTicks=100)
    public static void radioactiveDropsSurviveVanillaDamageAndPickup(GameTestHelper h) {
        var position=Vec3.atCenterOf(h.absolutePos(new BlockPos(4,3,4)));var item=new ItemEntity(h.getLevel(),position.x,position.y,position.z,SpentFuelPayload.seal(spent()));h.getLevel().addFreshEntity(item);
        h.assertTrue(item.getAge()==-32768,"真实加入事件没有阻止自然消失");NeoForge.EVENT_BUS.post(new ItemExpireEvent(item));h.assertTrue(item.getAge()==-32768,"自然消失入口未保护载荷");
        for(var source:List.of(h.getLevel().damageSources().inFire(),h.getLevel().damageSources().lava(),h.getLevel().damageSources().cactus(),h.getLevel().damageSources().explosion(null,null)))
            h.assertTrue(!item.hurt(source,100)&&item.isAlive(),"放射性封装被火/岩浆/仙人掌/爆炸销毁");
        var player=fake(h,"RadiationPickup");item.setNoPickUpDelay();item.playerTouch(player);h.assertTrue(item.isRemoved()&&player.getInventory().contains(s->s.is(SpentFuelStorageContent.SEALED_SPENT_FUEL_CASK.get())),"保护阻碍正常拾取");
        var ordinary=new ItemEntity(h.getLevel(),position.x,position.y,position.z,new ItemStack(Items.STICK));h.getLevel().addFreshEntity(ordinary);h.assertTrue(ordinary.getAge()!=-32768,"普通物品意外无限寿命");ordinary.hurt(h.getLevel().damageSources().cactus(),100);h.assertTrue(!ordinary.isAlive(),"普通物品被意外保护");
        System.out.println("[radiation] real ItemEntity join/expiry/fire/lava/cactus/explosion/pickup and ordinary-item assertions passed");h.succeed();
    }
    @GameTest(template="radiation_empty",templateNamespace="cni_radiation_foundation",timeoutTicks=100)
    public static void oversizedNestedInventoryCompletesWithoutSafeRecovery(GameTestHelper h) {
        var pos=new BlockPos(4,2,4);h.setBlock(pos,Blocks.CHEST);
        ItemStack inner=new ItemStack(Items.SHULKER_BOX);inner.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(Collections.nCopies(256,spent())));
        ItemStack outer=new ItemStack(Items.SHULKER_BOX);outer.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(Collections.nCopies(256,inner)));
        ((ChestBlockEntity)h.getBlockEntity(pos)).setItem(0,outer);
        h.runAfterDelay(10,()->{
            var player=fake(h,"RadiationBudget");player.setPos(Vec3.atCenterOf(h.absolutePos(pos)));player.setHealth(20);RadiationServerEvents.save(player,new RadiationDoseState(85,0,0));
            for(int tick=0;tick<40;tick++)RadiationServerEvents.tick(new PlayerTickEvent.Post(player));
            var state=RadiationServerEvents.state(player);h.assertTrue(state.safeTicks()==0&&state.dose()==85,"未完成采样被当作0启动恢复/改剂量");h.assertTrue(player.getHealth()==19,"采样拥堵停止已有高剂量伤害");
            long started=h.getLevel().getGameTime();awaitBudget(h,player,started,0);
        });
    }
    private static void awaitBudget(GameTestHelper h,FakePlayer player,long started,int attempt) {
        h.runAfterDelay(1,()->{
            var sample=RadiationSourceIndex.get(h.getLevel()).sample(player,P);
            if(sample.complete()) {
                near(h,10,sample.fieldDosePerSecond(),"续接后真实深层载荷未计入场强");
                h.assertTrue(h.getLevel().getGameTime()-started<=5,"嵌套队列没有公平续接");
                System.out.println("[radiation-budget] 65536 nested spent components completed after real ticks="+(h.getLevel().getGameTime()-started)+" counters="+RadiationSourceIndex.get(h.getLevel()).counters()+" incomplete-dose-frozen=true existing-damage-active=true");h.succeed();
            } else {h.assertTrue(attempt<5,"嵌套源永久饿死");awaitBudget(h,player,started,attempt+1);}
        });
    }
    @GameTest(template="radiation_empty",templateNamespace="cni_radiation_foundation",timeoutTicks=100)
    @SuppressWarnings("unchecked")
    public static void actualConfigReloadValidatesWholeGroup(GameTestHelper h) {
        h.runAfterDelay(12,()->{
            try {
                var field=com.iksxh.create_nuclear_industry.config.RadiationConfig.class.getDeclaredField("VALUES");field.setAccessible(true);
                var values=(Map<String,net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<?>>)field.get(null);
                var enabled=(net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<Boolean>)values.get("enabled");
                var warning=(net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<Double>)values.get("warningDose");
                var config=net.neoforged.fml.config.ModConfigs.getModConfigs("create_nuclear_industry").stream().filter(c->c.getSpec()==com.iksxh.create_nuclear_industry.config.RadiationConfig.SPEC).findFirst().orElseThrow();
                var bus=net.neoforged.fml.ModList.get().getModContainerById("create_nuclear_industry").orElseThrow().getEventBus();
                try {
                    enabled.set(false);bus.post(new net.neoforged.fml.event.config.ModConfigEvent.Reloading(config));
                    h.assertTrue(!com.iksxh.create_nuclear_industry.config.RadiationConfig.parameters().enabled(),"真实重载事件没有更新配置");
                    var player=fake(h,"RadiationDisabled");player.setHealth(20);RadiationServerEvents.save(player,new RadiationDoseState(85,0,0));
                    for(int i=0;i<120;i++)RadiationServerEvents.tick(new PlayerTickEvent.Post(player));
                    h.assertTrue(player.getHealth()==20,"关闭辐射仍造成伤害");near(h,84,RadiationServerEvents.state(player).dose(),"关闭辐射没有按安全规则恢复");
                    warning.set(90.0);bus.post(new net.neoforged.fml.event.config.ModConfigEvent.Reloading(config));
                    h.assertTrue(com.iksxh.create_nuclear_industry.config.RadiationConfig.parameters().equals(P),"非法阈值组合没有整组回退默认");
                } finally {enabled.set(true);warning.set(25.0);bus.post(new net.neoforged.fml.event.config.ModConfigEvent.Reloading(config));}
                System.out.println("[radiation-config] actual SERVER config reload / disabled recovery / invalid-group-default fallback passed, values restored");h.succeed();
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        });
    }
    private static String performancePhase;
    private static GameTestHelper performanceHelper;
    private static Vec3 performanceTarget;
    private static long tickStarted;
    private static final Map<String,List<Long>> TICK_COSTS=new LinkedHashMap<>();
    @net.neoforged.fml.common.EventBusSubscriber(modid="create_nuclear_industry")
    public static final class PerformanceObserver {
        private PerformanceObserver() {}
        @net.neoforged.bus.api.SubscribeEvent
        public static void pre(net.neoforged.neoforge.event.tick.ServerTickEvent.Pre event) {tickStarted=System.nanoTime();}
        @net.neoforged.bus.api.SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
        public static void post(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
            if(performancePhase==null)return;
            if(!performancePhase.endsWith("disabled")&&performanceHelper.getLevel().getGameTime()%20==0) {
                var sample=RadiationSourceIndex.get(performanceHelper.getLevel()).sampleAt(performanceHelper.getLevel(),performanceTarget,P);
                performanceHelper.assertTrue(sample.complete(),"默认20tick采样没有完成");
            }
            TICK_COSTS.computeIfAbsent(performancePhase,k->new ArrayList<>()).add(System.nanoTime()-tickStarted);
        }
    }
    private static void phase(GameTestHelper h,String name) {performanceHelper=h;performancePhase=name;}
    private static void printPerformance() {
        for(var entry:TICK_COSTS.entrySet()) {
            var values=entry.getValue();Collections.sort(values);int n=values.size();
            System.out.println("[radiation-tick-perf] phase="+entry.getKey()+" n="+n+" p50Ns="+values.get(n/2)+" p95Ns="+values.get(Math.min(n-1,(int)(n*.95)))+" p99Ns="+values.get(Math.min(n-1,(int)(n*.99)))+" maxNs="+values.get(n-1));
        }
        performancePhase=null;performanceHelper=null;
    }
    @GameTest(template="radiation_empty",templateNamespace="cni_radiation_foundation",timeoutTicks=300)
    public static void sixteenCubeAndFullRackPerformance(GameTestHelper h) {
        performanceTarget=Vec3.atCenterOf(h.absolutePos(new BlockPos(20,3,8)));
        h.runAfterDelay(15,()->{
            phase(h,"no-16-cube-sampling-disabled");
            h.runAfterDelay(40,()->{
                phase(h,"no-16-cube-sampling-enabled");
                h.runAfterDelay(40,()->buildPerformanceCube(h));
            });
        });
    }
    private static void buildPerformanceCube(GameTestHelper h) {
        performancePhase=null;BlockPos base=new BlockPos(0,1,0);
        for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++) {
            boolean shell=x==0||y==0||z==0||x==15||y==15||z==15;
            h.setBlock(base.offset(x,y,z),shell?P1Blocks.REACTOR_CASING.get():P1Blocks.REACTOR_FUEL_ROD.get());
        }
        for(int x=1;x<15;x++)for(int z=1;z<15;z++)h.setBlock(base.offset(x,15,z),P1Blocks.REACTOR_REFUELING_PORT.get());
        h.setBlock(base.offset(2,2,0),P1Blocks.REACTOR_INSTRUMENT_PORT.get());h.setBlock(base.offset(1,2,15),P1Blocks.REACTOR_COLD_PORT.get());h.setBlock(base.offset(3,2,15),P1Blocks.REACTOR_HOT_PORT.get());
        h.runAfterDelay(8,()->{
            var owner=(ReactorInstrumentPortBlockEntity)h.getBlockEntity(base.offset(2,2,0));ReactorStructureLifecycle.rescanAroundNow(h.getLevel(),owner.getBlockPos());h.assertTrue(owner.structureValid()&&owner.structureScan().dimensions().width()==16,"16³真实结构未成型");
            phase(h,"idle-16-cube-enabled");
            h.runAfterDelay(40,()->{
                for(int x=1;x<15;x++)for(int z=1;z<15;z++)((ReactorPortBlockEntity)h.getBlockEntity(base.offset(x,15,z))).setFuelAssembly(used());
                BlockPos rackPos=base.offset(18,1,8);h.setBlock(rackPos,SpentFuelStorageContent.DRY_STORAGE_RACK.get());var rack=(DryStorageBlockEntity)h.getBlockEntity(rackPos);for(int i=0;i<16;i++)h.assertTrue(rack.insertOne(SpentFuelPayload.seal(spent())),"满架准备失败");
                near(h,0,sample(h,performanceTarget),"16³完整壳/满架泄漏");long scanned=RadiationContainment.get(h.getLevel()).shellReads();
                phase(h,"used-196-fuel-16-cube-full-rack-enabled");
                h.runAfterDelay(40,()->{
                    near(h,0,sample(h,performanceTarget),"稳定满堆/架泄漏");h.assertTrue(RadiationContainment.get(h.getLevel()).shellReads()==scanned,"无变化满堆反复核壳");
                    var counters=RadiationSourceIndex.get(h.getLevel()).counters();
                    System.out.println("[radiation-perf] real 16³ / 196 used components / 16 sealed casks counters="+counters+" stableShellReads=0 packets="+RadiationServerEvents.packetsSent()+" maxDefaultSampleLatencyTicks=20; four real players and client-view performance NOT TESTED; server tick event intervals include other loaded fixtures");
                    phase(h,"used-196-fuel-full-rack-pair-disabled");
                    h.runAfterDelay(40,()->{
                        phase(h,"used-196-fuel-full-rack-pair-enabled");
                        h.runAfterDelay(40,()->{System.out.println("[radiation-review-pair] same 16-cube/196-used/full-rack scene; disabled then 20tick sampler enabled; no real logged-in players; counters="+RadiationSourceIndex.get(h.getLevel()).counters());printPerformance();h.succeed();});
                    });
                });
            });
        });
    }
    /** 审查整改独立域：减小技术预算，仍调用正式世界/源/射线算法与发送入口。 */
    @PrefixGameTestTemplate(false)
    public static final class ReviewFix {
        private ReviewFix() {}
        @GameTest(template="radiation_empty",templateNamespace="cni_radiation_foundation",timeoutTicks=120)
        public static void movingTargetAndUnrelatedUpdatesCannotStarveRays(GameTestHelper h) {
            var index=new RadiationSourceIndex(new RadiationSourceIndex.WorkLimits(4096,4096,8));
            var player=fake(h,"RadiationMoving");Vec3 target=Vec3.atCenterOf(h.absolutePos(new BlockPos(2,3,2)));player.setPos(target);
            for(int i=0;i<12;i++) {
                Vec3 p=target.add(14+i*.25,0,i*.13);var item=new ItemEntity(h.getLevel(),p.x,p.y,p.z,used());item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);h.getLevel().addFreshEntity(item);
            }
            RadiationServerEvents.save(player,RadiationDoseState.ZERO);
            for(int tick=0;tick<20;tick++)RadiationServerEvents.tick(new PlayerTickEvent.Post(player),index);
            h.assertTrue(RadiationServerEvents.state(player).dose()==0,"夹具未真正超过单帧射线预算");
            resumeRays(h,index,player,target,0);
        }
        private static void resumeRays(GameTestHelper h,RadiationSourceIndex index,FakePlayer player,Vec3 target,int tick) {
            player.setPos(target.add(tick*.02,0,tick*.01));h.setBlock(new BlockPos(50,2,50),tick%2==0?Blocks.STONE:Blocks.AIR);
            RadiationServerEvents.tick(new PlayerTickEvent.Post(player),index);
            var state=RadiationServerEvents.state(player);
            if(state.dose()>0) {
                h.assertTrue(tick>0,"夹具未真正跨tick续接");
                System.out.println("[radiation-review-r1] moving target/unrelated updates completed tick="+tick+" actual-player-dose="+state.dose()+" counters="+index.counters());h.succeed();
            }else {h.assertTrue(tick<70,"移动目标/无关方块使射线任务永久饿死");h.runAfterDelay(1,()->resumeRays(h,index,player,target,tick+1));}
        }
        @GameTest(template="radiation_empty",templateNamespace="cni_radiation_foundation",timeoutTicks=120)
        public static void staticEntityTailBeyondBudgetEventuallyCounts(GameTestHelper h) {
            var index=new RadiationSourceIndex(new RadiationSourceIndex.WorkLimits(8,4096,4096));
            var player=fake(h,"RadiationEntityTail");Vec3 target=Vec3.atCenterOf(h.absolutePos(new BlockPos(3,3,3)));player.setPos(target);
            for(int i=0;i<20;i++){Vec3 p=target.add(i*.03,0,0);var item=new ItemEntity(h.getLevel(),p.x,p.y,p.z,i==19?used():new ItemStack(Items.STICK));item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setUnlimitedLifetime();h.getLevel().addFreshEntity(item);}
            resumeEntities(h,index,player,0);
        }
        private static void resumeEntities(GameTestHelper h,RadiationSourceIndex index,FakePlayer player,int tick) {
            var sample=index.sample(player,P);
            if(sample.complete()) {h.assertTrue(tick>0&&sample.fieldDosePerSecond()>0,"预算后缀的真实源没有被读到");System.out.println("[radiation-review-r2] entity tail completed tick="+tick+" field="+sample.fieldDosePerSecond());h.succeed();}
            else {h.assertTrue(tick<50,"静态实体前缀反复读取，尾部永久饿死");h.runAfterDelay(1,()->resumeEntities(h,index,player,tick+1));}
        }
        @GameTest(template="radiation_empty",templateNamespace="cni_radiation_foundation",timeoutTicks=120)
        public static void relatedShieldChangesAndSourceRemovalInvalidatePendingWork(GameTestHelper h) {
            var index=new RadiationSourceIndex(new RadiationSourceIndex.WorkLimits(4096,4096,2));var player=fake(h,"RadiationRelevant");
            Vec3 target=Vec3.atCenterOf(h.absolutePos(new BlockPos(2,3,2)));player.setPos(target);Vec3 source=target.add(20,0,0);
            var item=new ItemEntity(h.getLevel(),source.x,source.y,source.z,used());item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);h.getLevel().addFreshEntity(item);
            h.assertTrue(!index.sample(player,P).complete(),"相关变更夹具没有pending路径");
            h.setBlock(new BlockPos(21,3,2),ReactorCraftingContent.SHIELDING_CONCRETE.get());
            finishRelated(h,index,player,item,0,false);
        }
        private static void finishRelated(GameTestHelper h,RadiationSourceIndex index,FakePlayer player,ItemEntity item,int tick,boolean removed) {
            h.runAfterDelay(1,()->{
                var sample=index.sample(player,P);
                if(sample.complete()) {
                    if(!removed) {
                        near(h,1.0/400*.05,sample.fieldDosePerSecond(),"已处理射线上的真实新墙没有失效并重算");
                        h.assertTrue(!index.sample(player,P).complete(),"第二轮来源移除夹具没有pending");item.discard();finishRelated(h,index,player,item,0,true);
                    }else {
                        near(h,0,sample.fieldDosePerSecond(),"移除源后旧任务继续产生幽灵剂量");
                        BlockPos beltPos=new BlockPos(22,3,2);h.setBlock(beltPos,AllBlocks.BELT.get());var belt=(BeltBlockEntity)h.getBlockEntity(beltPos);belt.setController(belt.getBlockPos());belt.beltLength=1;
                        var transported=new TransportedItemStack(used());transported.beltPosition=.5f;belt.getInventory().addItem(transported);
                        h.assertTrue(!index.sample(player,P).complete(),"传送带源提走夹具未进入pending");((RadiationBeltAccessor)belt.getInventory()).radiation$getToInsert().clear();
                        finishRemovedBelt(h,index,player,0);
                    }
                }else {h.assertTrue(tick<40,"相关路径变更/源移除后无法完成");finishRelated(h,index,player,item,tick+1,removed);}
            });
        }
        private static void finishRemovedBelt(GameTestHelper h,RadiationSourceIndex index,FakePlayer player,int tick) {
            h.runAfterDelay(1,()->{
                var sample=index.sample(player,P);
                if(sample.complete()){near(h,0,sample.fieldDosePerSecond(),"带载荷提走后旧任务仍计源");System.out.println("[radiation-review-lifecycle] relevant shield recomputed; removed entity and belt transport contribute zero");h.succeed();}
                else {h.assertTrue(tick<40,"提走带载荷后任务不完成");finishRemovedBelt(h,index,player,tick+1);}
            });
        }
        @GameTest(template="radiation_empty",templateNamespace="cni_radiation_foundation",timeoutTicks=40)
        public static void completeAndPendingPublishAtMostOncePerTwentyTicks(GameTestHelper h) {
            var player=fake(h,"RadiationSendClock");player.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(RadiationContent.METER.get()));
            List<RadiationExposurePayload> sent=new ArrayList<>();
            for(int tick=0;tick<60;tick++)RadiationServerEvents.deliver(player,new RadiationExposurePayload(h.getLevel().dimension().location(),tick,1,1,1,tick>=30),tick,sent::add);
            h.assertTrue(sent.size()==3,"完整/incomplete重试共享发送入口未按20tick节流 actual="+sent.size());
            h.assertTrue(!sent.get(0).complete()&&sent.get(2).complete(),"完成状态没有经同一发送节拍送达");
            var late=fake(h,"RadiationLateComplete");late.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(RadiationContent.METER.get()));List<RadiationExposurePayload> lateSent=new ArrayList<>();
            var dimension=h.getLevel().dimension().location();
            RadiationServerEvents.deliver(late,new RadiationExposurePayload(dimension,0,0,0,0,false),0,lateSent::add);
            RadiationServerEvents.deliver(late,new RadiationExposurePayload(dimension,20,0,0,0,false),20,lateSent::add);
            RadiationServerEvents.deliver(late,new RadiationExposurePayload(dimension,5,1,1,1,true),21,lateSent::add);
            RadiationServerEvents.deliver(late,new RadiationExposurePayload(dimension,40,0,0,0,false),40,lateSent::add);
            h.assertTrue(lateSent.size()==3&&lateSent.get(2).complete()&&lateSent.get(2).supersedes(lateSent.get(1)),"较旧完成结果被pending覆盖或被客户端顺序规则永久拒绝");
            System.out.println("[radiation-review-r3] 60 online ticks sent="+sent.size()+" pending+complete same gate; late completion accepted; counting transport only, no client login");h.succeed();
        }
    }


    /** 默认仍注册既有13项；只有显式本轮属性才注册两项区块生命周期回归，均使用原模板。 */
    @net.neoforged.fml.common.EventBusSubscriber(modid="create_nuclear_industry",bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
    public static final class TestRegistration {
        private TestRegistration() {}
        @net.neoforged.bus.api.SubscribeEvent public static void register(net.neoforged.neoforge.event.RegisterGameTestsEvent event) {
            boolean chunkOnly=Boolean.getBoolean("cni.radiation.reviewChunkOnly");
            if(chunkOnly)event.register(ChunkLifecycleReview.class);
            else {event.register(RadiationFoundationGameTests.class);event.register(ReviewFix.class);}
            System.out.println("[radiation-test-selection] chunkOnly="+chunkOnly+" expected="+(chunkOnly?2:13));
        }
    }
    /** 受控可见性与真实事件类型验证生产入口；不冒称操作真实区块装载器。 */
    @PrefixGameTestTemplate(false)
    public static final class ChunkLifecycleReview {
        private ChunkLifecycleReview() {}
        private static RadiationSourceIndex index(Set<Long> hidden) {
            return new RadiationSourceIndex(new RadiationSourceIndex.WorkLimits(2,4096,4096),
                    (level,x,z)->!hidden.contains(net.minecraft.world.level.ChunkPos.asLong(x,z)));
        }
        private static ItemEntity source(GameTestHelper h,Vec3 pos) {
            var item=new ItemEntity(h.getLevel(),pos.x,pos.y,pos.z,used());item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);h.getLevel().addFreshEntity(item);return item;
        }
        private static Object onlyJob(RadiationSourceIndex index) {
            try {var f=RadiationSourceIndex.class.getDeclaredField("jobs");f.setAccessible(true);return ((Map<?,?>)f.get(index)).values().iterator().next();}
            catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        private static int stage(RadiationSourceIndex index) {
            try{Object job=onlyJob(index);var f=job.getClass().getDeclaredField("stage");f.setAccessible(true);return f.getInt(job);}
            catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        private static void waitGeometry(GameTestHelper h,RadiationSourceIndex index,Vec3 target,int tick,Runnable ready) {
            var sample=index.sampleAt(h.getLevel(),target,P);h.assertTrue(!sample.complete(),"夹具未在几何结果后暂停");
            if(stage(index)==4)ready.run();
            else {h.assertTrue(tick<60,"夹具未到已算完几何的stage4");h.runAfterDelay(1,()->waitGeometry(h,index,target,tick+1,ready));}
        }
        private static void finish(GameTestHelper h,RadiationSourceIndex index,Vec3 target,int tick,java.util.function.DoubleConsumer check) {
            h.runAfterDelay(1,()->{var sample=index.sampleAt(h.getLevel(),target,P);
                if(sample.complete())check.accept(sample.fieldDosePerSecond());
                else {h.assertTrue(tick<60,"区块事件后任务未完成");finish(h,index,target,tick+1,check);}
            });
        }
        @GameTest(template="radiation_empty",templateNamespace="cni_radiation_foundation",timeoutTicks=180)
        public static void unloadedOtherShellChunkInvalidatesComputedContainment(GameTestHelper h) {
            BlockPos base=h.absolutePos(new BlockPos(0,2,0));BlockPos min=new BlockPos((base.getX()&~15)+14,base.getY(),(base.getZ()&~15)+4);
            Map<CoreColumnPosition,ReactorStructureDefinition.ColumnType> layout=new HashMap<>();
            for(int x=0;x<3;x++)for(int z=0;z<3;z++)layout.put(new CoreColumnPosition(x,z),x==0&&z==0?ReactorStructureDefinition.ColumnType.FUEL:ReactorStructureDefinition.ColumnType.CONTROL_ROD);
            ReactorStructureDefinition.templateFor(layout).forEach((pos,id)->h.getLevel().setBlockAndUpdate(min.offset(pos.x(),pos.y(),pos.z()),BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse(id)).defaultBlockState()));
            h.runAfterDelay(5,()->{
                ReactorStructureLifecycle.rescanAroundNow(h.getLevel(),min.offset(OWNER));
                var owner=(ReactorInstrumentPortBlockEntity)h.getLevel().getBlockEntity(min.offset(OWNER));h.assertTrue(owner.structureValid(),"跨区块小堆没有形成已知合法壳");
                Vec3 start=Vec3.atCenterOf(min.offset(3,2,2)),target=start.add(16,0,0);source(h,start);
                Set<Long> hidden=new HashSet<>();var index=index(hidden);var geometry=RadiationContainment.get(h.getLevel());
                near(h,0,geometry.transmission(h.getLevel(),start,target,P),"夹具没有事先获得完整壳零屏蔽");
                waitGeometry(h,index,target,0,()->{
                    int x=min.getX()>>4,z=min.getZ()>>4;var chunk=h.getLevel().getChunkSource().getChunkNow(x,z);h.assertTrue(chunk!=null,"受控区块实际未加载");
                    h.assertTrue((net.minecraft.core.BlockPos.containing(start).getX()>>4)!=x&&(net.minecraft.core.BlockPos.containing(target).getX()>>4)!=x,"隐藏块错误包含源或目标");
                    h.assertTrue(h.getLevel().hasChunkAt(BlockPos.containing(start))&&h.getLevel().hasChunkAt(BlockPos.containing(target)),"事件前真实源/目标区块必须仍加载");
                    hidden.add(net.minecraft.world.level.ChunkPos.asLong(x,z));NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.ChunkEvent.Unload(chunk));
                    h.assertTrue(geometry.transmission(h.getLevel(),start,target,P,(level,cx,cz)->!hidden.contains(net.minecraft.world.level.ChunkPos.asLong(cx,cz)))>0,"不可见壳仍被几何查询授予完整屏蔽");
                    finish(h,index,target,0,value->{h.assertTrue(value>0,"其他外壳区块卸载后旧Job仍交付零外泄");
                        h.assertTrue(h.getLevel().hasChunkAt(BlockPos.containing(start))&&h.getLevel().hasChunkAt(BlockPos.containing(target)),"事件后真实源/目标区块意外卸载");
                        hidden.clear();NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.ChunkEvent.Load(chunk,false));
                        System.out.println("[radiation-review-rr1-shell] controlled shielding visibility + posted ChunkEvent.Unload; source/target chunks real-loaded; pending geometry invalidated field="+value+"; NOT actual chunk unload");h.succeed();});
                });
            });
        }
        @GameTest(template="radiation_empty",templateNamespace="cni_radiation_foundation",timeoutTicks=180)
        public static void loadedProcessedPathChunkRecomputesWallTransmission(GameTestHelper h) {
            BlockPos base=h.absolutePos(new BlockPos(0,5,0));Vec3 start=Vec3.atCenterOf(new BlockPos((base.getX()&~15)+4,base.getY(),(base.getZ()&~15)+4));Vec3 target=start.add(28,0,0);
            int x=(BlockPos.containing(start).getX()>>4)+1,z=BlockPos.containing(start).getZ()>>4;var chunk=h.getLevel().getChunkSource().getChunkNow(x,z);h.assertTrue(chunk!=null,"路径夹具实际区块未加载");
            BlockPos wall=new BlockPos((x<<4)+3,base.getY(),BlockPos.containing(start).getZ());h.getLevel().setBlockAndUpdate(wall,ReactorCraftingContent.SHIELDING_CONCRETE.get().defaultBlockState());source(h,start);
            Set<Long> hidden=new HashSet<>();hidden.add(net.minecraft.world.level.ChunkPos.asLong(x,z));var index=index(hidden);
            waitGeometry(h,index,target,0,()->{
                hidden.clear();NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.ChunkEvent.Load(chunk,false));
                finish(h,index,target,0,value->{near(h,.05/(28*28),value,"已处理路径块加载墙后旧Job仍交付未衰减场");
                    System.out.println("[radiation-review-rr1-path] controlled shielding visibility + posted ChunkEvent.Load; preexisting real wall read after visibility restored; field="+value+"; NOT actual disk chunk load");h.succeed();});
            });
        }
    }

}
