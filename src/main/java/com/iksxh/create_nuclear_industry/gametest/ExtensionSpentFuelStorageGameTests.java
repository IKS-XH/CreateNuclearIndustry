package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.config.SpentFuelStorageConfig;
import com.iksxh.create_nuclear_industry.content.BasicMaterialContent;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.SpentFuelStorageContent;
import com.iksxh.create_nuclear_industry.production.ShieldedAssemblyBlock;
import com.iksxh.create_nuclear_industry.production.ShieldedAssemblyBlockEntity;
import com.iksxh.create_nuclear_industry.production.ShieldedAssemblyRecipe;
import com.iksxh.create_nuclear_industry.storage.DryStorageBlock;
import com.iksxh.create_nuclear_industry.storage.DryStorageBlockEntity;
import com.iksxh.create_nuclear_industry.storage.SpentFuelPayload;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** STORE-01真实注册、原始载荷、动力装配、共用能力与拆除守恒的隔离验收证据。 */
@GameTestHolder("create_nuclear_industry_store01")
@PrefixGameTestTemplate(false)
public final class ExtensionSpentFuelStorageGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos POS = new BlockPos(1, 2, 1);
    private ExtensionSpentFuelStorageGameTests() {}
    private static void require(GameTestHelper helper, boolean condition, String message) { if (!condition) helper.fail(message); }
    private static ItemStack spent() {
        ItemStack spent = new ItemStack(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get());
        spent.set(DataComponents.CUSTOM_NAME, Component.literal("原始组件记录"));
        return spent;
    }
    private static ItemStack sealed() { return SpentFuelPayload.seal(spent()); }
    private static ShieldedAssemblyBlockEntity machine(GameTestHelper helper) {
        ShieldedAssemblyBlock block = FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get();
        helper.setBlock(POS, block.defaultBlockState().setValue(ShieldedAssemblyBlock.EXPANDED, true));
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(POS), helper.getBlockState(POS), null, ItemStack.EMPTY);
        ShieldedAssemblyBlockEntity machine = (ShieldedAssemblyBlockEntity) helper.getBlockEntity(POS);
        require(helper, machine.complete(), "装配台没有形成完整八格");
        return machine;
    }
    private static DryStorageBlockEntity rack(GameTestHelper helper) {
        helper.setBlock(POS, SpentFuelStorageContent.DRY_STORAGE_RACK.get().defaultBlockState());
        return (DryStorageBlockEntity) helper.getBlockEntity(POS);
    }
    private static IItemHandler port(GameTestHelper helper, Direction side) {
        return helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(POS), side);
    }
    private static ShieldedAssemblyRecipe recipe(GameTestHelper helper, String name) {
        return (ShieldedAssemblyRecipe) helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                CreateNuclearIndustry.MOD_ID, "shielded_assembly/" + name)).orElseThrow().value();
    }
    private static List<ItemEntity> drops(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(POS)).inflate(4));
    }

    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry_store01")
    public static void payloadRejectsMalformedAndPreservesOriginal(GameTestHelper helper) {
        ItemStack original = spent();
        ItemStack bucket = SpentFuelPayload.seal(original);
        require(helper, SpentFuelPayload.isValid(bucket) && original.getCount() == 1, "封装校验修改原始栈或产物无效");
        original.set(DataComponents.CUSTOM_NAME, Component.literal("随后变化"));
        require(helper, Component.literal("原始组件记录").equals(bucket.get(DataComponents.CONTAINER).stream().findFirst().orElseThrow()
                .get(DataComponents.CUSTOM_NAME)), "载荷和输入共享可变栈");
        require(helper, SpentFuelPayload.seal(new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get())).isEmpty()
                && SpentFuelPayload.seal(spent().copyWithCount(2)).isEmpty(), "新燃料或多件组件被封装");
        ItemStack invalid = new ItemStack(SpentFuelStorageContent.SEALED_SPENT_FUEL_CASK.get());
        require(helper, !SpentFuelPayload.isValid(invalid), "缺载荷被接收");
        for (List<ItemStack> content : List.of(List.of(new ItemStack(Items.STONE)), List.of(spent().copyWithCount(2)),
                List.of(spent(), spent()), List.of(new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get())))) {
            invalid.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(content));
            require(helper, !SpentFuelPayload.isValid(invalid), "非法或多件载荷被接收");
        }
        ItemStack restored = ItemStack.parseOptional(helper.getLevel().registryAccess(),
                (CompoundTag) bucket.save(helper.getLevel().registryAccess()));
        require(helper, ItemStack.isSameItemSameComponents(bucket, restored), "封装桶当前版本保存丢失完整载荷");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry_store01")
    public static void everyFirstMaterialSelectsSameOperationAndSimulationDoesNot(GameTestHelper helper) {
        ShieldedAssemblyBlockEntity machine = machine(helper);
        IItemHandler top = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(POS.above()), Direction.UP);
        ItemStack[] materials = {spent(), new ItemStack(SpentFuelStorageContent.VITRIFICATION_MEDIUM.get()),
                new ItemStack(SpentFuelStorageContent.LEAD_SHIELDING_CASK.get())};
        for (int first = 0; first < 3; first++) {
            require(helper, top.insertItem(first, materials[first], true).isEmpty() && machine.state().operation().isEmpty(),
                    "模拟首料改变工序");
            require(helper, top.insertItem(first, materials[first], false).isEmpty() && machine.state().operation().equals("sealing"),
                    "首料没有选择封存工序：" + first);
            require(helper, machine.insert(new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get())).getCount() == 1,
                    "混入新燃料原料");
            for (int i = 0; i < 3; i++) if (i != first) require(helper, top.insertItem(i, materials[i], false).isEmpty(), "封存剩余原料拒收");
            require(helper, machine.recipeReady() && machine.state().ready() && machine.state().work() == 12800, "三料合同或工时不符");
            for (int i = 0; i < 3; i++) machine.state().extractInput(i, 1, false);
            require(helper, machine.state().operation().isEmpty(), "退空没有解锁工序");
        }
        require(helper, recipe(helper, "fresh_fuel_assembly").work() == 25600
                && java.util.Arrays.equals(recipe(helper, "fresh_fuel_assembly").costs(), new int[]{8, 4, 2, 1}), "原制造合同被修改");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry_store01")
    public static void changedRecipeCountsWorkAndInvalidationUseActualData(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        List<net.minecraft.world.item.crafting.RecipeHolder<?>> originals = List.copyOf(manager.getRecipes());
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "shielded_assembly/sealed_spent_fuel_cask");
        ShieldedAssemblyRecipe original = recipe(helper, "sealed_spent_fuel_cask");
        var changedInputs = List.of(original.inputs().get(0),
                new ShieldedAssemblyRecipe.Input(original.inputs().get(1).ingredient(), 2),
                new ShieldedAssemblyRecipe.Input(original.inputs().get(2).ingredient(), 3));
        ShieldedAssemblyRecipe changed = new ShieldedAssemblyRecipe("sealing", changedInputs, original.result(), 64);
        try {
            var replacements = new java.util.ArrayList<>(originals);
            replacements.removeIf(holder -> holder.id().equals(id));
            replacements.add(new net.minecraft.world.item.crafting.RecipeHolder<>(id, changed));
            manager.replaceRecipes(replacements);
            ShieldedAssemblyBlockEntity machine = machine(helper);
            machine.insert(spent());
            machine.insert(new ItemStack(SpentFuelStorageContent.VITRIFICATION_MEDIUM.get(), 2));
            machine.insert(new ItemStack(SpentFuelStorageContent.LEAD_SHIELDING_CASK.get(), 3));
            require(helper, machine.recipeReady() && machine.state().ready() && machine.state().work() == 64
                    && machine.state().cost(1) == 2 && machine.state().cost(2) == 3, "合法修改数量或工时被硬编码拒绝");
            machine.state().advance(32, changed.actualResult(machine.state().input(0)));
            require(helper, machine.state().progress() == 32, "动态配方未计实际工时");
            replacements.removeIf(holder -> holder.id().equals(id));
            manager.replaceRecipes(replacements);
            machine.tick();
            require(helper, machine.state().progress() == 0 && machine.state().input(1).getCount() == 2,
                    "失效配方未清工时或吞掉原料");
            manager.replaceRecipes(originals);
            require(helper, machine.recipeReady() && machine.state().work() == 12800, "配方恢复后仍缓存旧参数");
        } finally { manager.replaceRecipes(originals); }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry_store01", timeoutTicks = 310)
    public static void poweredSealingThenOriginalManufacturing(GameTestHelper helper) {
        ShieldedAssemblyBlockEntity machine = machine(helper);
        machine.insert(spent());
        machine.insert(new ItemStack(SpentFuelStorageContent.VITRIFICATION_MEDIUM.get()));
        machine.insert(new ItemStack(SpentFuelStorageContent.LEAD_SHIELDING_CASK.get()));
        helper.setBlock(POS.below(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.UP));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(POS.below())).generatedSpeed.setValue(256);
        helper.runAfterDelay(30, () -> {
            require(helper, machine.state().progress() > 0 && machine.state().progress() < 12800, "真实底部动力没有推进封存");
            CompoundTag saved = machine.savePortableData();
            int progress = machine.state().progress();
            machine.loadPortableData(saved);
            require(helper, machine.state().progress() == progress && machine.state().input(0).has(DataComponents.CUSTOM_NAME), "当前版本保存丢工时或输入数据");
            machine.updateFromNetwork(0, 2048, 2);
            machine.tick();
            require(helper, machine.state().progress() == progress, "过载没有暂停工时");
            machine.updateFromNetwork(100_000, 1024, 2);
        });
        helper.runAfterDelay(95, () -> {
            ItemStack result = machine.state().output();
            require(helper, SpentFuelPayload.isValid(result) && Component.literal("原始组件记录").equals(result.get(DataComponents.CONTAINER)
                    .stream().findFirst().orElseThrow().get(DataComponents.CUSTOM_NAME)), "动力封存未转移唯一原始组件");
            for (int slot = 0; slot < 4; slot++) require(helper, machine.state().input(slot).isEmpty(), "封装未原子扣料");
            require(helper, machine.insert(new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get(), 8)).getCount() == 8,
                    "输出未取时切换工序");
            machine.insert(spent());
            machine.insert(new ItemStack(SpentFuelStorageContent.VITRIFICATION_MEDIUM.get()));
            machine.insert(new ItemStack(SpentFuelStorageContent.LEAD_SHIELDING_CASK.get()));
        });
        helper.runAfterDelay(130, () -> {
            require(helper, machine.state().progress() == 0 && machine.state().input(0).getCount() == 1, "满输出扣料或计工");
            IItemHandler west = port(helper, Direction.WEST);
            require(helper, SpentFuelPayload.isValid(west.extractItem(4, 1, true)) && !machine.state().outputEmpty(), "模拟出料改账");
            require(helper, SpentFuelPayload.isValid(west.extractItem(4, 1, false)), "封装桶出料无效");
            for (int slot = 0; slot < 3; slot++) machine.state().extractInput(slot, 1, false);
            require(helper, machine.state().operation().isEmpty(), "完整清空后未解锁");
            machine.insert(new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get(), 8));
            machine.insert(new ItemStack(BasicMaterialContent.FUEL_CLADDING_TUBE.get(), 4));
            machine.insert(new ItemStack(BasicMaterialContent.SOLDER_INGOT.get(), 2));
            machine.insert(new ItemStack(BasicMaterialContent.STEEL_GRATE.get()));
            require(helper, machine.state().operation().equals("manufacture"), "制造工序未重新选择");
        });
        helper.runAfterDelay(270, () -> {
            require(helper, machine.state().output().is(ModItems.FRESH_FUEL_ASSEMBLY.get()) && machine.state().output().getCount() == 1
                    && machine.state().output().getDamageValue() == 0, "原制造未输出唯一满耐久组件");
            for (int slot = 0; slot < 4; slot++) require(helper, machine.state().input(slot).isEmpty(), "原制造扣料错误");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry_store01")
    public static void sharedRackPortsFullInventoryAndReducedCapacity(GameTestHelper helper) {
        DryStorageBlockEntity rack = rack(helper);
        int originalCapacity = SpentFuelStorageConfig.rackSlots();
        try {
            SpentFuelStorageConfig.RACK_SLOTS.set(16);
            for (Direction side : Direction.values()) {
                IItemHandler port = port(helper, side);
                require(helper, port != null && port.getSlots() == 16, "架子漏掉物理面");
                require(helper, port.insertItem(0, sealed(), true).isEmpty() == (side != Direction.DOWN), "底面插入或其他面模拟失败");
            }
            require(helper, rack.state().used() == 0, "模拟投料改写库存");
            require(helper, !ItemHandlerHelper.insertItem(port(helper, Direction.UP), new ItemStack(SpentFuelStorageContent.SEALED_SPENT_FUEL_CASK.get()), false).isEmpty(), "接收无载荷桶");
            for (int i = 0; i < 16; i++) require(helper, ItemHandlerHelper.insertItem(port(helper, Direction.NORTH), sealed(), false).isEmpty(), "未装满16桶");
            require(helper, rack.state().used() == 16 && helper.getBlockState(POS).getValue(DryStorageBlock.STORAGE_LEVEL) == 4
                    && !ItemHandlerHelper.insertItem(port(helper, Direction.UP), sealed(), false).isEmpty(), "满架吞桶或外观不符");
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            for (int i = 0; i < player.getInventory().items.size(); i++) player.getInventory().items.set(i, new ItemStack(Items.STONE, 64));
            require(helper, !rack.takeToPlayer(player) && rack.state().used() == 16, "背包满仍扣桶");
            SpentFuelStorageConfig.RACK_SLOTS.set(4);
            IItemHandler cached = port(helper, Direction.EAST);
            require(helper, SpentFuelPayload.isValid(cached.extractItem(0, 1, true)) && rack.state().used() == 16, "模拟提取改账");
            cached.extractItem(0, 1, false);
            require(helper, !cached.insertItem(0, sealed(), false).isEmpty() && rack.state().used() == 15, "减容后总桶数超额仍插入");
            CompoundTag saved = rack.saveWithFullMetadata(helper.getLevel().registryAccess());
            rack.loadWithComponents(saved, helper.getLevel().registryAccess());
            require(helper, rack.state().used() == 15 && rack.getUpdatePacket() != null, "减容保存截断槽或同步缺失");
            for (int slot = 15; slot >= 1; slot--) require(helper, SpentFuelPayload.isValid(port(helper, Direction.DOWN).extractItem(slot, 1, false)), "超额高槽不可提取");
            require(helper, cached.insertItem(4, sealed(), false).getCount() == 1 && cached.insertItem(0, sealed(), false).isEmpty(), "减容槽范围没有落实");
            player.getInventory().items.set(0, ItemStack.EMPTY);
            require(helper, rack.takeToPlayer(player) && rack.state().used() == 0, "空手取最后一个占用槽失败");
            require(helper, cached.getStackInSlot(0).isEmpty(), "侧面未共享取出账本");
        } finally { SpentFuelStorageConfig.RACK_SLOTS.set(originalCapacity); }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry_store01")
    public static void normalCreativeAndWrenchRemovalDropContentsOnce(GameTestHelper helper) {
        for (int mode = 0; mode < 3; mode++) {
            DryStorageBlockEntity rack = rack(helper);
            require(helper, rack.insertOne(sealed()) && rack.insertOne(sealed()), "拆除场景投桶失败");
            IItemHandler stale = port(helper, Direction.EAST);
            if (mode == 0) helper.getLevel().destroyBlock(helper.absolutePos(POS), true);
            else if (mode == 1) {
                var player = helper.makeMockPlayer(GameType.CREATIVE);
                helper.getBlockState(POS).getBlock().playerWillDestroy(helper.getLevel(), helper.absolutePos(POS), helper.getBlockState(POS), player);
                helper.setBlock(POS, Blocks.AIR);
            } else {
                var player = helper.makeMockPlayer(GameType.SURVIVAL);
                player.setItemInHand(InteractionHand.MAIN_HAND, AllItems.WRENCH.asStack());
                UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(POS)), Direction.NORTH, helper.absolutePos(POS), false));
                SpentFuelStorageContent.DRY_STORAGE_RACK.get().onSneakWrenched(helper.getBlockState(POS), context);
                require(helper, player.getInventory().countItem(SpentFuelStorageContent.DRY_STORAGE_RACK_ITEM.get()) == 1, "扳手未回收一个空架");
            }
            rack.dropContents();
            List<ItemEntity> dropped = drops(helper);
            require(helper, dropped.stream().filter(e -> SpentFuelPayload.isValid(e.getItem())).count() == 2, "拆除库存重复或丢失：" + mode);
            require(helper, stale.getSlots() == 0 && stale.insertItem(0, sealed(), false).getCount() == 1, "拆除后缓存能力仍可写");
            for (ItemEntity entity : dropped) if (entity.getItem().is(SpentFuelStorageContent.DRY_STORAGE_RACK_ITEM.get()))
                require(helper, !entity.getItem().has(DataComponents.CONTAINER), "空架夹带第二份桶");
            if (mode == 0) require(helper, dropped.stream().filter(e -> e.getItem().is(SpentFuelStorageContent.DRY_STORAGE_RACK_ITEM.get())).count() == 1, "普通拆除不是一个空架");
            dropped.forEach(ItemEntity::discard);
        }
        helper.succeed();
    }
}
