package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.DepletedUraniumContent;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 独立真实服务端合同：原生配方、装备、菜单和加工均使用隔离测试世界。 */
@GameTestHolder("cni_depleted_uranium")
@PrefixGameTestTemplate(false)
public final class DepletedUraniumGameTests {
    private static final String NS = "create_nuclear_industry";
    private static final List<String> PARTS = List.of("helmet", "chestplate", "leggings", "boots", "sword", "pickaxe", "axe", "shovel", "hoe");
    private DepletedUraniumGameTests() {}

    @GameTest(template = "depleted_uranium_empty", timeoutTicks = 80)
    public static void equipmentMatchesNetheriteExceptRepairIngredient(GameTestHelper h) {
        for (String part : PARTS) {
            ItemStack actual = stack(NS, "depleted_uranium_" + part);
            ItemStack reference = stack("minecraft", "netherite_" + part);
            h.assertTrue(actual.getItem().getClass().getSuperclass() == reference.getItem().getClass()
                    || actual.getItem().getClass() == reference.getItem().getClass(), part + " 原生类型不符");
            h.assertTrue(actual.getMaxDamage() == reference.getMaxDamage(), part + " 耐久不符");
            h.assertTrue(actual.getAttributeModifiers().equals(reference.getAttributeModifiers()), part + " 攻击/护甲属性不符");
            h.assertTrue(actual.getItem().getEnchantmentValue() == reference.getItem().getEnchantmentValue(), part + " 附魔能力不符");
            h.assertTrue(actual.getMaxStackSize() == 1 && actual.isEnchantable(), part + " 不可正常附魔/堆叠数错误");
            h.assertTrue(!actual.canBeHurtBy(h.getLevel().damageSources().lava())
                    && actual.canBeHurtBy(h.getLevel().damageSources().generic()), part + " 耐火边界不符");
            h.assertTrue(actual.getItem().isValidRepairItem(actual, new ItemStack(DepletedUraniumContent.INGOT.get())), part + " 不接受贫化铀锭");
            for (Item invalid : List.of(Items.NETHERITE_INGOT, Items.IRON_INGOT, FuelProcessingContent.DEPLETED_URANIUM_DUST.get())) {
                h.assertTrue(!actual.getItem().isValidRepairItem(actual, new ItemStack(invalid)), part + " 接受错误修复材料");
            }
            // 原版工具/护甲基础标签会传递到附魔标签；遍历原版实际归属避免遗漏武器或槽位分支。
            for (var tag : reference.getTags().filter(t -> t.location().getNamespace().equals("minecraft")).toList()) {
                h.assertTrue(actual.is(tag), part + " 缺失原版标签 " + tag.location());
            }
            if (actual.getItem() instanceof ArmorItem armor) {
                h.assertTrue(armor.getMaterial() == ArmorMaterials.NETHERITE, part + " 未复用原版穿戴材质/饰纹材质身份");
                h.assertTrue(actual.is(ItemTags.TRIMMABLE_ARMOR), part + " 不可饰纹");
            } else {
                for (var block : List.of(Blocks.STONE, Blocks.OBSIDIAN, Blocks.OAK_LOG, Blocks.DIRT, Blocks.COBWEB, Blocks.HAY_BLOCK)) {
                    h.assertTrue(actual.getDestroySpeed(block.defaultBlockState()) == reference.getDestroySpeed(block.defaultBlockState()), part + " 采掘速度不符 " + block);
                    h.assertTrue(actual.isCorrectToolForDrops(block.defaultBlockState()) == reference.isCorrectToolForDrops(block.defaultBlockState()), part + " 采掘等级不符 " + block);
                }
            }
            // 菜单只需服务端玩家上下文；不模拟登录，避免触发Create对未握手连接的登录同步。
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
            var anvil = new AnvilMenu(1, player.getInventory());
            actual.setDamageValue(100);
            anvil.getSlot(0).set(actual.copy());
            anvil.getSlot(1).set(new ItemStack(DepletedUraniumContent.INGOT.get()));
            anvil.createResult();
            h.assertTrue(anvil.getSlot(2).getItem().is(actual.getItem()) && anvil.getSlot(2).getItem().getDamageValue() == 0,
                    part + " 原生铁砧修复失败");
        }
        var state = DepletedUraniumContent.BLOCK.get().defaultBlockState();
        h.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE) && state.is(BlockTags.NEEDS_STONE_TOOL), "储存块采掘标签错误");
        h.assertTrue(state.getDestroySpeed(h.getLevel(), BlockPos.ZERO) == Blocks.IRON_BLOCK.defaultBlockState().getDestroySpeed(h.getLevel(), BlockPos.ZERO), "储存块硬度错误");
        h.assertTrue(DepletedUraniumContent.BLOCK_ITEM.get().getDefaultInstance().is(tag("c", "storage_blocks/depleted_uranium")), "块材料标签缺失");
        h.assertTrue(new ItemStack(DepletedUraniumContent.INGOT.get()).is(tag("c", "ingots/depleted_uranium")), "锭材料标签缺失");
        System.out.println("[du-equipment] 9 equipment: attributes/durability/enchantability/fire/repair/anvil/tags/mining PASS");
        h.succeed();
    }

    @GameTest(template = "depleted_uranium_empty", timeoutTicks = 80)
    public static void nativeSmithingPreservesComponentsAndConsumesAllThreeInputs(GameTestHelper h) {
        for (String part : PARTS) {
            var holder = h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(NS, "smithing/depleted_uranium_" + part)).orElseThrow();
            h.assertTrue(holder.value() instanceof SmithingTransformRecipe, part + " 未加载原生锻造转换配方");
            var recipe = (SmithingTransformRecipe) holder.value();
            var base = stack("minecraft", "diamond_" + part);
            base.set(DataComponents.CUSTOM_NAME, Component.literal("贫化铀合同 " + part));
            base.setDamageValue(123);
            base.set(DataComponents.REPAIR_COST, 7);
            base.enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.UNBREAKING), 3);
            if (base.getItem() instanceof ArmorItem) {
                base.set(DataComponents.TRIM, new ArmorTrim(
                        h.getLevel().registryAccess().registryOrThrow(Registries.TRIM_MATERIAL).getHolderOrThrow(TrimMaterials.REDSTONE),
                        h.getLevel().registryAccess().registryOrThrow(Registries.TRIM_PATTERN).getHolderOrThrow(TrimPatterns.SENTRY)));
            }
            var template = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 2);
            var ingots = new ItemStack(DepletedUraniumContent.INGOT.get(), 2);
            var input = new SmithingRecipeInput(template, base, ingots);
            h.assertTrue(recipe.matches(input, h.getLevel()), part + " 正确投入不匹配");
            for (var invalid : List.of(
                    new SmithingRecipeInput(ItemStack.EMPTY, base, ingots),
                    new SmithingRecipeInput(new ItemStack(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE), base, ingots),
                    new SmithingRecipeInput(template, stack("minecraft", "iron_" + part), ingots),
                    new SmithingRecipeInput(template, stack(NS, "depleted_uranium_" + part), ingots),
                    new SmithingRecipeInput(template, base, new ItemStack(FuelProcessingContent.DEPLETED_URANIUM_DUST.get())),
                    new SmithingRecipeInput(template, base, new ItemStack(Items.NETHERITE_INGOT)),
                    new SmithingRecipeInput(template, base, ItemStack.EMPTY))) {
                h.assertTrue(!recipe.matches(invalid, h.getLevel()), part + " 错误投入被接受");
            }
            // 菜单只需服务端玩家上下文；不模拟登录，避免触发Create对未握手连接的登录同步。
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
            var menu = new SmithingMenu(2, player.getInventory());
            menu.getSlot(0).set(template.copy());
            menu.getSlot(1).set(base.copy());
            menu.getSlot(2).set(ingots.copy());
            menu.createResult();
            var result = menu.getSlot(3).getItem().copy();
            h.assertTrue(result.is(stack(NS, "depleted_uranium_" + part).getItem()) && result.getCount() == 1, part + " 菜单结果错误");
            h.assertTrue(result.getDamageValue() == 123 && result.getOrDefault(DataComponents.REPAIR_COST, 0) == 7, part + " 免费修复或丢失铁砧惩罚");
            for (var component : List.of(DataComponents.CUSTOM_NAME, DataComponents.ENCHANTMENTS, DataComponents.TRIM)) {
                h.assertTrue(java.util.Objects.equals(base.get(component), result.get(component)), part + " 底材组件丢失 " + component);
            }
            // 调用原版结果槽取出流程，实际经过SmithingMenu.onTake而非模拟扣料。
            h.assertTrue(menu.getSlot(3).mayPickup(player), part + " 无法取出结果");
            menu.getSlot(3).onTake(player, result);
            h.assertTrue(menu.getSlot(0).getItem().getCount() == 1 && menu.getSlot(1).getItem().isEmpty()
                    && menu.getSlot(2).getItem().getCount() == 1, part + " 未恰好消耗模板/底材/锭各一件");
        }
        System.out.println("[du-equipment] 9 native smithing menus: name/enchantment/damage/repair-cost/armor-trim preserved; invalid inputs rejected; exact consumption PASS");
        h.succeed();
    }

    @GameTest(template = "depleted_uranium_empty", timeoutTicks = 450)
    public static void furnaceBlastingAndStorageRecipesActuallyProcess(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager();
        var dust = new ItemStack(FuelProcessingContent.DEPLETED_URANIUM_DUST.get());
        h.assertTrue(recipes.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(dust), h.getLevel()).isPresent(), "烧炼配方未加载");
        h.assertTrue(recipes.getRecipeFor(RecipeType.BLASTING, new SingleRecipeInput(dust), h.getLevel()).isPresent(), "高炉配方未加载");
        var grid = new ArrayList<ItemStack>();
        for (int i = 0; i < 9; i++) grid.add(new ItemStack(DepletedUraniumContent.INGOT.get()));
        var input = CraftingInput.of(3, 3, grid);
        var pack = recipes.getRecipeFor(RecipeType.CRAFTING, input, h.getLevel()).orElseThrow().value();
        var block = pack.assemble(input, h.getLevel().registryAccess());
        h.assertTrue(block.is(DepletedUraniumContent.BLOCK_ITEM.get()) && block.getCount() == 1, "九锭合块数量错误");
        var single = CraftingInput.of(1, 1, List.of(block));
        var unpack = recipes.getRecipeFor(RecipeType.CRAFTING, single, h.getLevel()).orElseThrow().value().assemble(single, h.getLevel().registryAccess());
        h.assertTrue(unpack.is(DepletedUraniumContent.INGOT.get()) && unpack.getCount() == 9, "拆块数量不守恒");
        var furnacePos = new BlockPos(1, 1, 1);
        var blastPos = new BlockPos(3, 1, 1);
        h.setBlock(furnacePos, Blocks.FURNACE);
        h.setBlock(blastPos, Blocks.BLAST_FURNACE);
        for (var pos : List.of(furnacePos, blastPos)) {
            var furnace = (AbstractFurnaceBlockEntity) h.getBlockEntity(pos);
            furnace.setItem(0, new ItemStack(FuelProcessingContent.DEPLETED_URANIUM_DUST.get(), 2));
            furnace.setItem(1, new ItemStack(Items.COAL));
        }
        h.runAfterDelay(410, () -> {
            for (var pos : List.of(furnacePos, blastPos)) {
                var furnace = (AbstractFurnaceBlockEntity) h.getBlockEntity(pos);
                h.assertTrue(furnace.getItem(0).isEmpty() && furnace.getItem(2).is(DepletedUraniumContent.INGOT.get())
                        && furnace.getItem(2).getCount() == 2, "实际熔炼未满足2粉→2锭 " + pos);
            }
            System.out.println("[du-equipment] 13 recipes loaded; actual furnace+blast 2 dust -> 2 ingots each; shaped 9:1 / shapeless 1:9 storage PASS");
            h.succeed();
        });
    }

    private static ItemStack stack(String namespace, String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path)));
    }
    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
}
