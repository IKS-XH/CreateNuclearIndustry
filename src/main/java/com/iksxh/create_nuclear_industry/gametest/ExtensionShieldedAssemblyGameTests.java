package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.BasicMaterialContent;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.production.ShieldedAssemblyBlock;
import com.iksxh.create_nuclear_industry.production.ShieldedAssemblyBlockEntity;
import com.iksxh.create_nuclear_industry.production.ShieldedAssemblyLayout;
import com.iksxh.create_nuclear_industry.production.ShieldedAssemblyPartBlock;
import com.iksxh.create_nuclear_industry.production.ShieldedAssemblyPartBlockEntity;
import com.iksxh.create_nuclear_industry.production.ShieldedAssemblyState;
import java.util.List;
import java.util.UUID;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.GameType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** 02E隔离命名空间：真实八格、共享账本、Create漏斗与动力、搬迁和旧数据。 */
@GameTestHolder("create_nuclear_industry_02e")
@PrefixGameTestTemplate(false)
public final class ExtensionShieldedAssemblyGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos STATION = new BlockPos(1, 2, 1);
    private static final BlockPos TOP = STATION.above();
    private ExtensionShieldedAssemblyGameTests() {}

    private static ShieldedAssemblyBlockEntity place(GameTestHelper helper) {
        ShieldedAssemblyBlock block = FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get();
        BlockState state = block.defaultBlockState().setValue(ShieldedAssemblyBlock.EXPANDED, true);
        helper.setBlock(STATION, state);
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(STATION), helper.getBlockState(STATION),
                null, ItemStack.EMPTY);
        ShieldedAssemblyBlockEntity machine = (ShieldedAssemblyBlockEntity) helper.getBlockEntity(STATION);
        require(helper, machine != null && machine.complete(), "一件机器没有形成唯一主控与七个代理");
        return machine;
    }
    private static IItemHandler port(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), side);
    }
    private static void require(GameTestHelper helper, boolean yes, String message) {
        if (!yes) helper.fail(message);
    }

    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry_02e", timeoutTicks = 90)
    public static void occupiedSpaceAndFourOrientations(GameTestHelper helper) {
        helper.setBlock(STATION.below(), Blocks.STONE);
        ItemStack oneMachine = new ItemStack(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get());
        BlockPos floor = helper.absolutePos(STATION.below());
        BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), null, InteractionHand.MAIN_HAND,
                oneMachine, new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false));
        Direction placementFacing = context.getHorizontalDirection().getOpposite();
        BlockPos obstacle = ShieldedAssemblyLayout.position(STATION, placementFacing, 1);
        helper.setBlock(obstacle, Blocks.STONE);
        require(helper, FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get().getStateForPlacement(context) == null,
                "邻格被占仍允许预检放置；点击位置=" + context.getClickedPos()
                        + " 朝向=" + context.getHorizontalDirection());
        FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get().place(context);
        require(helper, helper.getBlockState(STATION).isAir() && helper.getBlockState(obstacle).is(Blocks.STONE)
                && oneMachine.getCount() == 1, "失败放置消耗物品、覆盖邻格或留下残件");
        helper.setBlock(obstacle, Blocks.AIR);
        FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get().place(context);
        ShieldedAssemblyBlockEntity placed = (ShieldedAssemblyBlockEntity) helper.getBlockEntity(STATION);
        require(helper, placed != null && placed.complete() && oneMachine.isEmpty(),
                "空位中一件物品未形成八格完整机器");
        helper.setBlock(STATION, Blocks.AIR);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockState state = FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get().defaultBlockState()
                    .setValue(ShieldedAssemblyBlock.EXPANDED, true)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            helper.setBlock(STATION, state);
            FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get().setPlacedBy(helper.getLevel(),
                    helper.absolutePos(STATION), helper.getBlockState(STATION), null, ItemStack.EMPTY);
            ShieldedAssemblyBlockEntity machine = (ShieldedAssemblyBlockEntity) helper.getBlockEntity(STATION);
            require(helper, machine != null && machine.complete(), "朝向" + facing + "未形成完整八格");
            for (int part = 1; part < 8; part++) {
                BlockPos at = ShieldedAssemblyLayout.position(STATION, facing, part);
                require(helper, helper.getBlockState(at).getValue(ShieldedAssemblyPartBlock.PART) == part,
                        "朝向" + facing + "的第" + part + "格坐标错误");
            }
            helper.setBlock(STATION, Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry_02e", timeoutTicks = 130)
    public static void orphanProxyWaitsThenCleansWithoutLoading(GameTestHelper helper) {
        ShieldedAssemblyBlockEntity machine = place(helper);
        machine.state().insert(0, new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get(), 8), false);
        BlockPos at = STATION.above().east();
        ShieldedAssemblyPartBlockEntity proxy = (ShieldedAssemblyPartBlockEntity) helper.getBlockEntity(at);
        BlockPos unavailable = new BlockPos(10000, 64, 10000);
        require(helper, !helper.getLevel().hasChunkAt(unavailable), "孤儿时序测试的远处主控区块意外加载");
        proxy.bind(unavailable, machine.ownerId());
        helper.runAfterDelay(50, () -> {
            require(helper, helper.getBlockState(at).is(FuelProcessingContent.SHIELDED_ASSEMBLY_PART.get())
                    && !helper.getLevel().hasChunkAt(unavailable), "未加载主控被强载或代理过早清理");
            BlockPos loadedAbsent = helper.absolutePos(STATION.east(3));
            require(helper, helper.getLevel().hasChunkAt(loadedAbsent)
                    && helper.getLevel().getBlockState(loadedAbsent).isAir(), "已加载空主控测试坐标无效");
            proxy.bind(loadedAbsent, machine.ownerId());
        });
        helper.runAfterDelay(95, () -> {
            require(helper, helper.getBlockState(at).isAir()
                    && machine.state().input(0).getCount() == 8
                    && helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(helper.absolutePos(at)).inflate(4)).stream().noneMatch(entity ->
                    entity.getItem().is(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get())),
                    "主控变为已加载且不存在后代理未静默清理或复制了机器");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry_02e", timeoutTicks = 360)
    public static void sharedPortsAndPoweredBatch(GameTestHelper helper) {
        ShieldedAssemblyBlockEntity machine = place(helper);
        require(helper, helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                CreateNuclearIndustry.MOD_ID, "mechanical_crafting/shielded_assembly_station")).isPresent(),
                "21格动力合成配方未加载");
        require(helper, helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                CreateNuclearIndustry.MOD_ID, "crafting/shielded_assembly_station")).isEmpty(),
                "旧9格工作台配方仍可制造");
        require(helper, machine.recipeReady(), "四料生产配方未加载");
        for (int part = 0; part < 8; part++) {
            BlockPos at = ShieldedAssemblyLayout.position(STATION, Direction.NORTH, part);
            if (part > 0) require(helper, helper.getBlockState(at).getValue(ShieldedAssemblyPartBlock.PART) == part,
                    "代理编号或坐标错误: " + part);
            if (part > 0) {
                ShieldedAssemblyPartBlockEntity proxy = (ShieldedAssemblyPartBlockEntity) helper.getBlockEntity(at);
                require(helper, proxy.getUpdatePacket() != null
                        && proxy.getUpdateTag(helper.getLevel().registryAccess()).hasUUID("OwnerId")
                        && proxy.getUpdateTag(helper.getLevel().registryAccess()).getLong("MasterPos")
                        == helper.absolutePos(STATION).asLong(), "代理客户端同步缺失归属: " + part);
            }
            for (Direction side : Direction.values()) {
                IItemHandler handler = port(helper, at, side);
                boolean exposed = ShieldedAssemblyLayout.exterior(part, Direction.NORTH, side);
                require(helper, exposed == (handler != null), "外表面/内部面能力错误: " + part + " " + side);
                if (handler != null) require(helper, handler.getSlots() == (side == Direction.UP ? 4 : 5),
                        "暴露槽数错误: " + part + " " + side);
            }
        }
        require(helper, machine.itemPort(null) == null, "null面错误开放物料能力");
        IItemHandler west = port(helper, STATION, Direction.WEST);
        IItemHandler east = port(helper, STATION.east(), Direction.EAST);
        require(helper, west.insertItem(0,
                new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get(), 8), true).isEmpty()
                && machine.state().input(0).isEmpty(), "侧面模拟投料改写账本");
        require(helper, west.insertItem(0,
                new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get(), 8), false).isEmpty()
                && east.getStackInSlot(0).getCount() == 8, "两个外侧面未共享芯块库存");
        require(helper, east.extractItem(0, 8, false).isEmpty()
                && west.insertItem(4, new ItemStack(Items.IRON_INGOT), false).getCount() == 1,
                "输入可被抽走或输出可被回灌");
        IItemHandler top = port(helper, TOP, Direction.UP);
        BlockPos armPos = new BlockPos(4, 2, 1);
        helper.setBlock(armPos, AllBlocks.MECHANICAL_ARM.get().defaultBlockState());
        ArmInteractionPoint point = ArmInteractionPoint.create(helper.getLevel(), helper.absolutePos(TOP),
                helper.getBlockState(TOP));
        ArmBlockEntity arm = (ArmBlockEntity) helper.getBlockEntity(armPos);
        ItemStack cladding = new ItemStack(BasicMaterialContent.FUEL_CLADDING_TUBE.get(), 4);
        require(helper, point != null && point.isValid() && point.getMode() == ArmInteractionPoint.Mode.DEPOSIT,
                "Create机械臂没有识别顶面代理投料点");
        require(helper, point.insert(arm, cladding, true).isEmpty() && machine.state().input(1).isEmpty(),
                "机械臂模拟投料改写库存");
        require(helper, point.insert(arm, cladding, false).isEmpty()
                && machine.state().input(1).getCount() == 4
                && point.extract(arm, 1, 4, false).isEmpty(), "机械臂真投料/禁止取料失败");
        require(helper, top.insertItem(2, new ItemStack(BasicMaterialContent.SOLDER_INGOT.get(), 2), false).isEmpty()
                && top.insertItem(3, new ItemStack(BasicMaterialContent.STEEL_GRATE.get()), false).isEmpty(),
                "顶部四料投料失败");
        helper.setBlock(STATION.below(), AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.UP));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(STATION.below())).generatedSpeed.setValue(256);
        helper.runAfterDelay(40, () -> {
            require(helper, Math.abs(machine.getSpeed()) >= 32 && machine.state().progress() > 0,
                    "唯一底部Create轴未驱动装配工时");
            for (int part = 1; part < 8; part++)
                require(helper, helper.getBlockState(ShieldedAssemblyLayout.position(STATION, Direction.NORTH, part))
                        .getValue(ShieldedAssemblyPartBlock.WORKING), "运行外壳未同步工作灯: " + part);
            int beforeOverload = machine.state().progress();
            machine.updateFromNetwork(0, 2048, 2);
            machine.tick();
            require(helper, machine.isOverStressed() && machine.state().progress() == beforeOverload
                    && !helper.getBlockState(STATION).getValue(ShieldedAssemblyBlock.WORKING),
                    "过载仍计工时或主控工作灯未熄灭");
            for (int part = 1; part < 8; part++)
                require(helper, !helper.getBlockState(ShieldedAssemblyLayout.position(STATION, Direction.NORTH, part))
                        .getValue(ShieldedAssemblyPartBlock.WORKING), "过载时代理工作灯未熄灭: " + part);
            machine.updateFromNetwork(100_000, 1_024, 2);
        });
        helper.runAfterDelay(250, () -> {
            require(helper, machine.state().output().is(ModItems.FRESH_FUEL_ASSEMBLY.get())
                    && machine.state().output().getCount() == 1
                    && machine.state().output().getDamageValue() == 0
                    && machine.state().output().getMaxDamage() == 216_000, "动力批次未产唯一满耐久组件");
            for (int slot = 0; slot < 4; slot++)
                require(helper, machine.state().input(slot).isEmpty(), "批次原料未守恒扣除: " + slot);
            top.insertItem(0, new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get(), 8), false);
            top.insertItem(1, new ItemStack(BasicMaterialContent.FUEL_CLADDING_TUBE.get(), 4), false);
            top.insertItem(2, new ItemStack(BasicMaterialContent.SOLDER_INGOT.get(), 2), false);
            top.insertItem(3, new ItemStack(BasicMaterialContent.STEEL_GRATE.get()), false);
        });
        helper.runAfterDelay(330, () -> {
            require(helper, machine.state().output().getCount() == 1 && machine.state().progress() == 0,
                    "满输出时仍推进第二批");
            for (int slot = 0; slot < 4; slot++)
                require(helper, machine.state().input(slot).getCount() == ShieldedAssemblyState.COST[slot],
                        "满输出时错误扣除第二批原料: " + slot);
            require(helper, west.extractItem(4, 1, true).is(ModItems.FRESH_FUEL_ASSEMBLY.get())
                    && east.getStackInSlot(4).getCount() == 1, "模拟提取改写唯一成品");
            require(helper, west.extractItem(4, 1, false).is(ModItems.FRESH_FUEL_ASSEMBLY.get())
                    && east.getStackInSlot(4).isEmpty(), "侧面成品提取未共享账本");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry_02e", timeoutTicks = 100)
    public static void createFunnelsAndMissingPart(GameTestHelper helper) {
        ShieldedAssemblyBlockEntity machine = place(helper);
        BlockPos andesite = STATION.west();
        BlockPos brass = STATION.east(2);
        helper.setBlock(andesite, AllBlocks.ANDESITE_FUNNEL.getDefaultState()
                .setValue(FunnelBlock.FACING, Direction.WEST).setValue(FunnelBlock.EXTRACTING, false));
        helper.setBlock(brass, AllBlocks.BRASS_FUNNEL.getDefaultState()
                .setValue(FunnelBlock.FACING, Direction.EAST).setValue(FunnelBlock.EXTRACTING, false));
        // Create的行为组件在方块实体首轮加载后才可被外部查询。
        helper.runAfterDelay(5, () -> {
        ItemStack andesiteRemainder = FunnelBlock.tryInsert(helper.getLevel(), helper.absolutePos(andesite),
                new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get(), 8), false);
        ItemStack brassRemainder = FunnelBlock.tryInsert(helper.getLevel(), helper.absolutePos(brass),
                new ItemStack(BasicMaterialContent.FUEL_CLADDING_TUBE.get(), 4), false);
        require(helper, andesiteRemainder.isEmpty() && brassRemainder.isEmpty()
                && machine.state().input(0).getCount() == 8
                && machine.state().input(1).getCount() == 4,
                "漏斗投料: 安山余" + andesiteRemainder.getCount() + " 黄铜余" + brassRemainder.getCount()
                        + " 主控槽" + machine.state().input(0).getCount() + "/" + machine.state().input(1).getCount()
                        + " 直接端口" + (port(helper, STATION, Direction.WEST) != null)
                        + "/" + (port(helper, STATION.east(), Direction.EAST) != null));
        CompoundTag loaded = machine.savePortableData();
        loaded.put("Output", new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get()).save(helper.getLevel().registryAccess()));
        machine.loadPortableData(loaded);
        helper.setBlock(brass, AllBlocks.BRASS_FUNNEL.getDefaultState()
                .setValue(FunnelBlock.FACING, Direction.EAST).setValue(FunnelBlock.EXTRACTING, true));
        helper.runAfterDelay(40, () -> {
            require(helper, machine.state().outputEmpty(), "黄铜漏斗未从侧面真实抽出成品");
            require(helper, !helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(helper.absolutePos(brass)).inflate(2)).isEmpty(), "黄铜漏斗未生成出货实体");
            IItemHandler stale = port(helper, STATION, Direction.WEST);
            ShieldedAssemblyPartBlockEntity proxy = (ShieldedAssemblyPartBlockEntity)
                    helper.getBlockEntity(STATION.above().east());
            proxy.bind(helper.absolutePos(STATION), UUID.randomUUID());
            require(helper, !machine.complete() && stale.getSlots() == 0
                    && port(helper, STATION, Direction.WEST) == null
                    && machine.state().input(0).getCount() == 8, "缺件时缓存能力仍有效或吞掉原料");
            proxy.bind(helper.absolutePos(STATION), machine.ownerId());
            require(helper, machine.complete(), "归属恢复后结构未重新有效");
            BlockPos unavailable = new BlockPos(10000, 64, 10000);
            require(helper, !helper.getLevel().hasChunkAt(unavailable), "未加载主控模拟坐标意外已加载");
            proxy.bind(unavailable, machine.ownerId());
            require(helper, port(helper, STATION.above().east(), Direction.EAST) == null
                    && !machine.complete() && machine.state().input(0).getCount() == 8
                    && machine.state().outputEmpty(), "主控不可达时代理仍开放能力或另造账本");
            helper.succeed();
        });
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry_02e", timeoutTicks = 90)
    public static void eightPickupPointsAndLegacyUpgrade(GameTestHelper helper) {
        for (int part = 0; part < 8; part++) {
            ShieldedAssemblyBlockEntity machine = place(helper);
            machine.state().insert(0, new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get(), 8), false);
            BlockPos at = ShieldedAssemblyLayout.position(STATION, Direction.NORTH, part);
            if (part < 2) helper.getLevel().destroyBlock(helper.absolutePos(at), true);
            else if (part == 2 || part == 3) {
                var player = helper.makeMockPlayer(GameType.SURVIVAL);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
                BlockState broken = helper.getBlockState(at);
                require(helper, !broken.canHarvestBlock(helper.getLevel(), helper.absolutePos(at), player),
                        "错误工具场景没有触发低采掘等级");
                broken.getBlock().playerWillDestroy(helper.getLevel(), helper.absolutePos(at), broken, player);
                helper.setBlock(at, Blocks.AIR);
            } else helper.setBlock(at, Blocks.AIR);
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(helper.absolutePos(STATION)).inflate(5));
            long machines = drops.stream().filter(e -> e.getItem()
                    .is(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get())).count();
            require(helper, machines == 1, "拆除第" + part + "格没有恰好一个携物机器");
            ItemStack portable = drops.stream().filter(e -> e.getItem()
                    .is(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get())).reduce((a, b) -> b).get().getItem();
            CustomData data = portable.get(DataComponents.CUSTOM_DATA);
            require(helper, data != null && data.copyTag().getCompound("CniShieldedAssembly")
                    .getCompound("Input0").contains("id"), "搬迁快照丢失芯块");
            require(helper, helper.getBlockState(STATION).isAir(), "拆代理后主控未移除");
            for (int other = 1; other < 8; other++)
                require(helper, helper.getBlockState(ShieldedAssemblyLayout.position(STATION, Direction.NORTH, other))
                        .isAir(), "拆代理后留孤儿");
            drops.forEach(ItemEntity::discard);
        }
        helper.setBlock(STATION, FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get().defaultBlockState());
        ShieldedAssemblyBlockEntity old = (ShieldedAssemblyBlockEntity) helper.getBlockEntity(STATION);
        CompoundTag legacy = new CompoundTag();
        legacy.put("Input0", new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get(), 8)
                .save(helper.getLevel().registryAccess()));
        old.loadPortableData(legacy);
        require(helper, !old.expanded() && old.state().input(0).getCount() == 8
                && old.itemPort(Direction.UP) == null, "旧单格没有保留库存并暂停");
        ItemStack oldPortable = ShieldedAssemblyBlock.portable(old);
        helper.setBlock(STATION, Blocks.AIR);
        helper.setBlock(STATION, FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get().defaultBlockState()
                .setValue(ShieldedAssemblyBlock.EXPANDED, true));
        FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get().setPlacedBy(helper.getLevel(),
                helper.absolutePos(STATION), helper.getBlockState(STATION), null, oldPortable);
        ShieldedAssemblyBlockEntity upgraded = (ShieldedAssemblyBlockEntity) helper.getBlockEntity(STATION);
        require(helper, upgraded.complete() && upgraded.state().input(0).getCount() == 8,
                "旧携带物重放未升级为八格或库存丢失");
        IItemHandler top = port(helper, TOP, Direction.UP);
        top.insertItem(1, new ItemStack(BasicMaterialContent.FUEL_CLADDING_TUBE.get(), 4), false);
        top.insertItem(2, new ItemStack(BasicMaterialContent.SOLDER_INGOT.get(), 2), false);
        top.insertItem(3, new ItemStack(BasicMaterialContent.STEEL_GRATE.get()), false);
        CompoundTag progressed = upgraded.savePortableData();
        progressed.putInt("Progress", 1234);
        upgraded.loadPortableData(progressed);
        require(helper, upgraded.state().progress() == 1234, "合法工时样本未读取");
        ItemStack withProgress = ShieldedAssemblyBlock.portable(upgraded);
        helper.setBlock(STATION, Blocks.AIR);
        helper.setBlock(STATION, FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get().defaultBlockState()
                .setValue(ShieldedAssemblyBlock.EXPANDED, true));
        FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get().setPlacedBy(helper.getLevel(),
                helper.absolutePos(STATION), helper.getBlockState(STATION), null, withProgress);
        ShieldedAssemblyBlockEntity restored = (ShieldedAssemblyBlockEntity) helper.getBlockEntity(STATION);
        require(helper, restored.complete() && restored.state().progress() == 1234,
                "机器携带物重放未恢复合法工时");
        for (int slot = 0; slot < 4; slot++)
            require(helper, restored.state().input(slot).getCount() == ShieldedAssemblyState.COST[slot],
                    "机器携带物重放丢失第" + slot + "料");
        CompoundTag mismatch = restored.savePortableData();
        mismatch.put("Input0", new ItemStack(Items.IRON_INGOT, 8).save(helper.getLevel().registryAccess()));
        restored.loadPortableData(mismatch);
        require(helper, !restored.recipeReady() && restored.state().input(0).is(Items.IRON_INGOT),
                "非法配方没有保留真实错误物料");
        helper.runAfterDelay(2, () -> {
            require(helper, restored.state().input(0).getCount() == 8 && restored.state().progress() == 0,
                    "非法配方未清工时或吞掉错误物料");
            helper.succeed();
        });
    }
}
