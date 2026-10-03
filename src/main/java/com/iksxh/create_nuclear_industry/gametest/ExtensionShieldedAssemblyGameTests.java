package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.BasicMaterialContent;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.production.ShieldedAssemblyBlock;
import com.iksxh.create_nuclear_industry.production.ShieldedAssemblyBlockEntity;
import com.iksxh.create_nuclear_industry.production.ShieldedAssemblyRecipe;
import com.iksxh.create_nuclear_industry.production.ShieldedAssemblyState;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** 仅本批命名空间，核对真实注册、漏斗/机械臂、Create动力、输出堵塞和携带账本。 */
@GameTestHolder("create_nuclear_industry_02d")
@PrefixGameTestTemplate(false)
public final class ExtensionShieldedAssemblyGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos STATION = new BlockPos(2, 3, 2);
    private static final BlockPos MOTOR = STATION.below();
    private static final BlockPos HOPPER = STATION.above();
    private static final BlockPos ARM = new BlockPos(4, 2, 2);
    private ExtensionShieldedAssemblyGameTests() {}

    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry_02d", timeoutTicks = 520)
    public static void realBatchAndMaterialPorts(GameTestHelper helper) {
        helper.setBlock(STATION, FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get());
        helper.setBlock(HOPPER, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        ((HopperBlockEntity) helper.getBlockEntity(HOPPER)).setItem(0,
                new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get(), 8));
        helper.setBlock(ARM, AllBlocks.MECHANICAL_ARM.get().defaultBlockState());
        ShieldedAssemblyBlockEntity machine = (ShieldedAssemblyBlockEntity) helper.getBlockEntity(STATION);
        var recipes = helper.getLevel().getRecipeManager();
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID,
                "shielded_assembly/fresh_fuel_assembly");
        require(helper, recipes.byKey(recipeId).isPresent()
                && recipes.byKey(recipeId).get().value() instanceof ShieldedAssemblyRecipe
                && machine.recipeReady(), "专用四料配方未通过游戏数据包加载");
        require(helper, recipes.byKey(ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID,
                "crafting/shielded_assembly_station")).isPresent(), "工作台制造配方未加载");
        for (Direction side : new Direction[]{Direction.DOWN})
            require(helper, helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(STATION), side) == null, "底部错误开放物料能力");
        require(helper, machine.itemPort(null) == null, "未指定面错误开放物料能力");

        helper.runAfterDelay(100, () -> {
            IItemHandler top = port(helper, Direction.UP);
            IItemHandler east = port(helper, Direction.EAST);
            require(helper, machine.state().input(0).getCount() == 8, "真实漏斗没有从顶面送入8芯块");
            require(helper, top != null && top.getSlots() == 4 && east != null && east.getSlots() == 1,
                    "顶面四输入槽或水平单输出槽未暴露");
            require(helper, east.insertItem(0, new ItemStack(Items.IRON_INGOT), false).getCount() == 1
                    && east.extractItem(0, 1, false).isEmpty(), "水平面错误回灌或空提取");
            ItemStack cladding = new ItemStack(BasicMaterialContent.FUEL_CLADDING_TUBE.get(), 4);
            ArmInteractionPoint point = ArmInteractionPoint.create(helper.getLevel(), helper.absolutePos(STATION),
                    helper.getBlockState(STATION));
            ArmBlockEntity arm = (ArmBlockEntity) helper.getBlockEntity(ARM);
            require(helper, point != null && point.isValid() && point.getMode() == ArmInteractionPoint.Mode.DEPOSIT,
                    "真实Create机械臂未发现投料点");
            require(helper, point.insert(arm, cladding, true).isEmpty() && machine.state().input(1).isEmpty(),
                    "机械臂模拟投料改写了库存");
            require(helper, point.insert(arm, cladding, false).isEmpty()
                    && machine.state().input(1).getCount() == 4
                    && point.extract(arm, 1, 4, false).isEmpty(), "机械臂不能单向提交四根包壳");
            require(helper, top.insertItem(2, new ItemStack(BasicMaterialContent.SOLDER_INGOT.get(), 2), true).isEmpty()
                    && machine.state().input(2).isEmpty(), "顶面模拟插入错误写入焊料");
            require(helper, top.insertItem(2, new ItemStack(BasicMaterialContent.SOLDER_INGOT.get(), 2), false).isEmpty()
                    && top.insertItem(3, new ItemStack(BasicMaterialContent.STEEL_GRATE.get()), false).isEmpty(),
                    "顶面未接收焊料和格架");
            require(helper, machine.state().progress() == 0 && machine.state().outputEmpty(),
                    "无动力错误加工");
            helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                    .setValue(CreativeMotorBlock.FACING, Direction.UP));
            ((CreativeMotorBlockEntity) helper.getBlockEntity(MOTOR)).generatedSpeed.setValue(256);
        });

        helper.runAfterDelay(120, () -> {
            require(helper, Math.abs(machine.getSpeed()) >= 32 && machine.state().progress() > 0,
                    "Create真实底部轴未驱动装配台");
            int prior = machine.state().progress();
            // 通过Create公开网络更新入口模拟容量小于负载，验证过载门不会偷计工时。
            machine.updateFromNetwork(0, 2048, 2);
            require(helper, machine.isOverStressed(), "Create未将容量不足判为过载");
            machine.tick();
            require(helper, machine.state().progress() == prior
                    && !helper.getBlockState(STATION).getValue(ShieldedAssemblyBlock.WORKING),
                    "过载仍增加装配工时");
            machine.updateFromNetwork(100_000, 1_024, 2);
        });
        helper.runAfterDelay(270, () -> {
            ItemStack output = machine.state().output();
            require(helper, output.is(ModItems.FRESH_FUEL_ASSEMBLY.get()) && output.getCount() == 1
                    && output.getDamageValue() == 0 && output.getMaxDamage() == 216_000,
                    "真实动力批次未产正式满耐久单件组件");
            for (int slot = 0; slot < 4; slot++) require(helper, machine.state().input(slot).isEmpty(),
                    "完成时未原子扣清第" + slot + "项原料");
            IItemHandler top = port(helper, Direction.UP);
            top.insertItem(0, new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get(), 8), false);
            top.insertItem(1, new ItemStack(BasicMaterialContent.FUEL_CLADDING_TUBE.get(), 4), false);
            top.insertItem(2, new ItemStack(BasicMaterialContent.SOLDER_INGOT.get(), 2), false);
            top.insertItem(3, new ItemStack(BasicMaterialContent.STEEL_GRATE.get()), false);
        });
        helper.runAfterDelay(400, () -> {
            require(helper, machine.state().output().getCount() == 1 && machine.state().progress() == 0,
                    "满输出时错误推进第二批或复制成品");
            for (int slot = 0; slot < 4; slot++) require(helper,
                    machine.state().input(slot).getCount() == ShieldedAssemblyState.COST[slot],
                    "满输出时错误扣除第二批第" + slot + "项原料");
            IItemHandler east = port(helper, Direction.EAST);
            IItemHandler west = port(helper, Direction.WEST);
            require(helper, east.extractItem(0, 1, true).is(ModItems.FRESH_FUEL_ASSEMBLY.get())
                    && west.getStackInSlot(0).getCount() == 1, "多个水平面模拟提取不共享唯一输出");
            require(helper, east.extractItem(0, 1, false).is(ModItems.FRESH_FUEL_ASSEMBLY.get())
                    && west.getStackInSlot(0).isEmpty(), "水平面没有共享唯一成品槽");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry_02d", timeoutTicks = 90)
    public static void portableInventoryAndInvalidRecipe(GameTestHelper helper) {
        helper.setBlock(STATION, FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get());
        ShieldedAssemblyBlockEntity machine = (ShieldedAssemblyBlockEntity) helper.getBlockEntity(STATION);
        IItemHandler top = port(helper, Direction.UP);
        top.insertItem(0, new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get(), 8), false);
        top.insertItem(1, new ItemStack(BasicMaterialContent.FUEL_CLADDING_TUBE.get(), 4), false);
        top.insertItem(2, new ItemStack(BasicMaterialContent.SOLDER_INGOT.get(), 2), false);
        top.insertItem(3, new ItemStack(BasicMaterialContent.STEEL_GRATE.get()), false);
        CompoundTag saved = machine.savePortableData();
        saved.putInt("Progress", 1234);
        machine.loadPortableData(saved);
        require(helper, machine.state().progress() == 1234, "合法携带工时未恢复");
        ItemStack preview = Block.getDrops(helper.getBlockState(STATION), helper.getLevel(),
                helper.absolutePos(STATION), machine).stream().filter(stack ->
                stack.is(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get())).findFirst().orElse(ItemStack.EMPTY);
        CustomData previewData = preview.get(DataComponents.CUSTOM_DATA);
        require(helper, previewData != null && previewData.copyTag().contains("CniShieldedAssembly"),
                "原版掉落查询未携带唯一库存快照");
        helper.setBlock(STATION, Blocks.AIR);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(helper.absolutePos(STATION)).inflate(2));
        long machineDrops = drops.stream().filter(entity -> entity.getItem()
                .is(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get())).count();
        require(helper, machineDrops == 1 && drops.stream().noneMatch(entity ->
                entity.getItem().is(FuelProcessingContent.SINTERED_FUEL_PELLET.get())),
                "拆除没有产生唯一携物机器或额外散落输入");
        ItemStack carried = drops.stream().filter(entity -> entity.getItem()
                .is(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get())).findFirst().get().getItem();
        helper.setBlock(STATION, FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get());
        ShieldedAssemblyBlock block = FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get();
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(STATION), helper.getBlockState(STATION), null, carried);
        ShieldedAssemblyBlockEntity placed = (ShieldedAssemblyBlockEntity) helper.getBlockEntity(STATION);
        require(helper, placed.state().input(0).getCount() == 8 && placed.state().progress() == 1234,
                "机器物品重新放置未恢复四料与进度");
        CompoundTag mismatch = placed.savePortableData();
        mismatch.put("Input0", new ItemStack(Items.IRON_INGOT, 8).save(helper.getLevel().registryAccess()));
        placed.loadPortableData(mismatch);
        require(helper, !placed.recipeReady() && placed.state().input(0).is(Items.IRON_INGOT),
                "失配输入没有保留并停机");
        helper.runAfterDelay(2, () -> {
            require(helper, placed.state().input(0).getCount() == 8 && placed.state().progress() == 0,
                    "重载失配错误删除原料或保留无效工时");
            helper.succeed();
        });
    }

    private static IItemHandler port(GameTestHelper helper, Direction side) {
        return helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(STATION), side);
    }
    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
