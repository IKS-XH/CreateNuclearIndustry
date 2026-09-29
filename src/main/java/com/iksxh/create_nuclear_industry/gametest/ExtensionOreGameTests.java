package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.OreContent;
import com.iksxh.create_nuclear_industry.worldgen.OreGenerationFilter;
import com.mojang.logging.LogUtils;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 三矿真实服务端测试；自然采样仅在专用普通世界命令调用，平坦测试世界明确不计采样。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class ExtensionOreGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NS = "create_nuclear_industry";
    private static final String TEMPLATE = "p0_probe_empty";
    private ExtensionOreGameTests() {}

    /** 通过服务端玩家破坏入口验证等级门与六种矿石实际掉落，附魔使用加载后的注册表。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void miningDropsRespectToolsAndEnchantments(GameTestHelper helper) {
        ServerPlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        for (OreContent.Mineral mineral : OreContent.MINERALS) {
            Item minimum = mineral.name().equals("tin") ? Items.STONE_PICKAXE : Items.IRON_PICKAXE;
            Item insufficient = mineral.name().equals("tin") ? Items.WOODEN_PICKAXE : Items.STONE_PICKAXE;
            for (Block block : List.of(mineral.ore().get(), mineral.deepslateOre().get())) {
                verifyMine(helper, player, block, new ItemStack(minimum), mineral.raw().get(), 1, 1);
                verifyMine(helper, player, block, new ItemStack(insufficient), mineral.raw().get(), 0, 0);
                verifyMine(helper, player, block, new ItemStack(Items.IRON_AXE), mineral.raw().get(), 0, 0);
                ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
                silk.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
                verifyMine(helper, player, block, silk, block.asItem(), 1, 1);
                ItemStack fortune = new ItemStack(Items.DIAMOND_PICKAXE);
                fortune.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), 3);
                for (int sample = 0; sample < 8; sample++) {
                    verifyMine(helper, player, block, fortune.copy(), mineral.raw().get(), 1, 4);
                }
            }
            ItemStack fortune = new ItemStack(Items.DIAMOND_PICKAXE);
            fortune.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), 3);
            verifyMine(helper, player, mineral.rawBlock().get(), fortune, mineral.rawBlock().get().asItem(), 1, 1);
            verifyMine(helper, player, mineral.rawBlock().get(), new ItemStack(insufficient), mineral.rawBlock().get().asItem(), 0, 0);
        }
        helper.succeed();
    }

    private static void verifyMine(GameTestHelper helper, ServerPlayer player, Block block, ItemStack tool, Item expected, int min, int max) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        AABB bounds = new AABB(pos).inflate(2);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).forEach(ItemEntity::discard);
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        require(helper, player.gameMode.destroyBlock(pos), "实际玩家破坏失败: " + block);
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds);
        int count = drops.stream().mapToInt(entity -> {
            require(helper, entity.getItem().is(expected), "掉落了错误物品: " + entity.getItem());
            return entity.getItem().getCount();
        }).sum();
        require(helper, count >= min && count <= max, "挖掘掉落越界: " + block + " count=" + count);
        require(helper, helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, bounds).isEmpty(), "挖矿意外产生经验");
        drops.forEach(ItemEntity::discard);
    }

    /** 从真实配方管理器匹配六条压缩配方；检查欠料拒绝与双向净材料数。 */
    @GameTest(template = TEMPLATE)
    public static void craftingUsesLoadedRecipesAndConservesRawMaterial(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        for (OreContent.Mineral mineral : OreContent.MINERALS) {
            var inputs = new ArrayList<ItemStack>();
            for (int i = 0; i < 9; i++) inputs.add(new ItemStack(mineral.raw().get()));
            var full = CraftingInput.of(3, 3, inputs);
            var recipe = manager.getRecipeFor(RecipeType.CRAFTING, full, helper.getLevel()).orElseThrow();
            ItemStack packed = recipe.value().assemble(full, helper.getLevel().registryAccess());
            require(helper, packed.is(mineral.rawBlock().get().asItem()) && packed.getCount() == 1, "压缩产物错误");
            inputs.set(8, ItemStack.EMPTY);
            require(helper, !recipe.value().matches(CraftingInput.of(3, 3, inputs), helper.getLevel()), "欠一份原料仍能压缩");
            var one = CraftingInput.of(1, 1, List.of(packed));
            ItemStack unpacked = manager.getRecipeFor(RecipeType.CRAFTING, one, helper.getLevel()).orElseThrow().value()
                    .assemble(one, helper.getLevel().registryAccess());
            require(helper, unpacked.is(mineral.raw().get()) && unpacked.getCount() == 9, "解压不守恒");
        }
        helper.succeed();
    }

    /** 读取已加载的放置过滤器；模式切换仍不得绕过真实维度。外部成员由隔离测试包追加。 */
    @GameTest(template = TEMPLATE)
    public static void generationGateUsesBoundBlockTagsAndActualDimension(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (OreContent.Mineral mineral : OreContent.MINERALS) {
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores/" + mineral.name()));
            var members = BuiltInRegistries.BLOCK.getTag(tag).orElseThrow();
            require(helper, members.contains(mineral.ore().get().builtInRegistryHolder()), "矿石方块标签未绑定");
            boolean foreign = members.stream().anyMatch(holder -> !BuiltInRegistries.BLOCK.getKey(holder.value()).getNamespace().equals(NS));
            for (OreGenerationFilter.Mode mode : OreGenerationFilter.Mode.values()) {
                var filter = new OreGenerationFilter(tag, mode);
                boolean expected = mode == OreGenerationFilter.Mode.ENABLED || (mode == OreGenerationFilter.Mode.AUTO && !foreign);
                require(helper, permits(filter, level) == expected, "主世界模式/外部方块标签判断错误: " + mode);
                ServerLevel nether = level.getServer().getLevel(Level.NETHER);
                require(helper, nether != null && !permits(filter, nether), "模式绕过实际下界限制");
            }
            var feature = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                    .get(ResourceLocation.fromNamespaceAndPath(NS, "ore_" + mineral.name()));
            require(helper, feature != null && feature.placement().getFirst() instanceof OreGenerationFilter, "数据包过滤器未加载");
            LOGGER.info("ORE_GATE mineral={} external={} loadedAllows={}", mineral.name(), foreign,
                    permits((OreGenerationFilter) feature.placement().getFirst(), level));
        }
        helper.succeed();
    }

    private static boolean permits(OreGenerationFilter filter, ServerLevel level) {
        return filter.getPositions(new PlacementContext(level, level.getChunkSource().getGenerator(), Optional.empty()),
                RandomSource.create(1), BlockPos.ZERO).findAny().isPresent();
    }

    /** 真正的双粉碎轮由两个Create电机驱动，物品经capability投入并从世界实体收集；不调用applyRecipe。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 2400)
    public static void allNineInputsRunThroughPoweredCrushingWheels(GameTestHelper helper) {
        BlockPos left = new BlockPos(1, 4, 2);
        BlockPos right = new BlockPos(3, 4, 2);
        BlockPos controller = new BlockPos(2, 4, 2);
        BlockPos motorLeft = new BlockPos(1, 4, 3);
        BlockPos motorRight = new BlockPos(3, 4, 1);
        helper.setBlock(left, AllBlocks.CRUSHING_WHEEL.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(right, AllBlocks.CRUSHING_WHEEL.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(motorLeft, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.NORTH));
        helper.setBlock(motorRight, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH));
        helper.runAfterDelay(5, () -> {
            ((CreativeMotorBlockEntity) helper.getBlockEntity(motorLeft)).generatedSpeed.setValue(128);
            ((CreativeMotorBlockEntity) helper.getBlockEntity(motorRight)).generatedSpeed.setValue(128);
        });
        for (int i = 0; i < 9; i++) {
            int index = i;
            OreContent.Mineral mineral = OreContent.MINERALS.get(i / 3);
            int form = i % 3;
            Item input = form == 0 ? mineral.ore().get().asItem() : form == 1 ? mineral.raw().get() : mineral.rawBlock().get().asItem();
            helper.runAfterDelay(30 + i * 180, () -> {
                var state = helper.getBlockState(controller);
                require(helper, state.is(AllBlocks.CRUSHING_WHEEL_CONTROLLER.get()) && state.getValue(CrushingWheelControllerBlock.VALID), "真实粉碎轮未成对成型");
                var be = (CrushingWheelControllerBlockEntity) helper.getBlockEntity(controller);
                require(helper, be.crushingspeed > 0 && state.getValue(CrushingWheelControllerBlock.FACING) == Direction.DOWN, "粉碎轮没有向下转动");
                var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(controller), Direction.UP);
                require(helper, handler != null && handler.insertItem(0, new ItemStack(input), false).isEmpty(), "真实粉碎轮拒绝输入 " + input);
                require(helper, be.findRecipe().orElseThrow().id().equals(ResourceLocation.fromNamespaceAndPath("create", "crushing/" +
                        (form == 0 ? mineral.name() + "_ore" : "raw_" + mineral.name() + (form == 2 ? "_block" : "")))), "错误配方竞争");
                var loadedRecipe = be.findRecipe().orElseThrow().value();
                require(helper, loadedRecipe.getProcessingDuration() == 400, "未读取锁定Create时长");
                var results = loadedRecipe.getRollableResults();
                require(helper, results.size() == (form == 0 ? 3 : 2), "粉碎配方概率输出项数量不符");
                Item expectedCrushed = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_" + mineral.name()));
                require(helper, results.getFirst().getStack().is(expectedCrushed)
                        && results.getFirst().getStack().getCount() == (form == 2 ? 9 : 1)
                        && results.getFirst().getChance() == 1, "保证产物定义错误");
                if (form == 0) require(helper, results.get(1).getStack().is(expectedCrushed)
                        && results.get(1).getStack().getCount() == 1 && results.get(1).getChance() == .75f, "额外粗矿概率错误");
                var xp = results.getLast();
                require(helper, xp.getStack().is(AllItems.EXP_NUGGET.get()) && xp.getStack().getCount() == (form == 2 ? 9 : 1)
                        && xp.getChance() == .75f, "经验颗粒数量或逐颗概率错误");
            });
            helper.runAfterDelay(190 + i * 180, () -> {
                BlockPos center = helper.absolutePos(controller);
                List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(2, 5, 2));
                Item crushed = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_" + mineral.name()));
                int output = 0;
                int experience = 0;
                for (ItemEntity drop : drops) {
                    if (drop.getItem().is(crushed)) output += drop.getItem().getCount();
                    else if (drop.getItem().is(AllItems.EXP_NUGGET.get())) experience += drop.getItem().getCount();
                    else helper.fail("粉碎轮产出错误物品: " + drop.getItem());
                    drop.discard();
                }
                int minimum = form == 2 ? 9 : 1;
                require(helper, output >= minimum && output <= (form == 0 ? 2 : minimum), "粉碎轮保证产物丢失/越界 index=" + index + " count=" + output);
                require(helper, experience <= (form == 2 ? 9 : 1), "经验副产物越界");
                LOGGER.info("ORE_CRUSH input={} output={} experience={}", input, output, experience);
                if (index == 8) helper.succeed();
            });
        }
    }

    /** 普通世界自然生成采样；由独立服务器的固定seed与/test命令触发，不把平坦测试世界当采样。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 12000)
    public static void naturalWorldSample(GameTestHelper helper) {
        if (!(helper.getLevel().getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator)) {
            LOGGER.info("ORE_SAMPLE_NOT_RUN flat test world; use isolated normal world");
            helper.succeed();
            return;
        }
        Map<Block, long[]> counts = new LinkedHashMap<>();
        for (OreContent.Mineral mineral : OreContent.MINERALS) {
            counts.put(mineral.ore().get(), new long[]{0, 1000, -1000, 0, 0});
            counts.put(mineral.deepslateOre().get(), new long[]{0, 1000, -1000, 0, 0});
        }
        ServerLevel level = helper.getLevel();
        int origin = Integer.getInteger("cni.ore.sampleOrigin", 120);
        // 每轮仅8×8新区块，完整扫描世界高度；先生成外围以完成相邻特征写入。
        for (int x = origin - 1; x <= origin + 8; x++) {
            for (int z = origin - 1; z <= origin + 8; z++) level.getChunk(x, z);
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int cx = origin; cx < origin + 8; cx++) {
            for (int cz = origin; cz < origin + 8; cz++) {
                var chunk = level.getChunk(cx, cz);
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                    for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                        pos.set(cx * 16 + x, y, cz * 16 + z);
                        Block block = chunk.getBlockState(pos).getBlock();
                        long[] stats = counts.get(block);
                        if (stats == null) continue;
                        if (stats[0] == 0) LOGGER.info("ORE_FIRST seed={} block={} pos={},{},{}", level.getSeed(), BuiltInRegistries.BLOCK.getKey(block), pos.getX(), y, pos.getZ());
                        stats[0]++;
                        stats[1] = Math.min(stats[1], y);
                        if (y > stats[2]) { stats[2] = y; stats[3] = pos.getX(); stats[4] = pos.getZ(); }
                    }
                }
            }
        }
        counts.forEach((block, stats) -> LOGGER.info("ORE_SAMPLE seed={} chunks=64 origin={},{} block={} count={} minY={} maxY={}",
                level.getSeed(), origin, origin, BuiltInRegistries.BLOCK.getKey(block), stats[0], stats[1], stats[2]));
        counts.forEach((block, stats) -> {
            if (stats[0] == 0) return;
            BlockPos highest = new BlockPos((int) stats[3], (int) stats[2], (int) stats[4]);
            LOGGER.info("ORE_HIGHEST block={} pos={}", BuiltInRegistries.BLOCK.getKey(block), highest);
            for (Direction direction : Direction.values()) LOGGER.info("ORE_NEIGHBOR block={} direction={} state={}",
                    BuiltInRegistries.BLOCK.getKey(block), direction, level.getBlockState(highest.relative(direction)));
        });
        for (OreContent.Mineral mineral : OreContent.MINERALS) {
            long total = counts.get(mineral.ore().get())[0] + counts.get(mineral.deepslateOre().get())[0];
            require(helper, Boolean.getBoolean("cni.ore.expectNone") ? total == 0 : total > 0,
                    "自然生成与本次测试预期不符: " + mineral.name() + " count=" + total);
        }
        LOGGER.info("ORE_SAMPLE_DONE seed={}", level.getSeed());
        helper.succeed();
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
