package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.ModCreativeTabs;
import com.iksxh.create_nuclear_industry.content.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * 在真实服务端注册及数据包加载后验证青金石粉身份、普通堆叠和标签追加。
 * 创造页测试调用实际内容构建入口，不代表客户端模型烘焙或外观已经人工验收。
 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class LapisDustGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final ResourceLocation ITEM_ID = ResourceLocation.fromNamespaceAndPath(
            "create_nuclear_industry", "lapis_dust");
    // lapis 子标签是本模组按通用约定新增的合同，不是 NeoForge 内置常量。
    private static final TagKey<Item> LAPIS_DUSTS = dustTag("dusts/lapis");
    private static final TagKey<Item> DUSTS = dustTag("dusts");

    private LapisDustGameTests() {
    }

    /** 正式 ID 必须解析为普通 Item；堆叠单位为个，不携带燃料耐久或自定义状态。 */
    @GameTest(template = TEMPLATE)
    public static void formalIdResolvesToOrdinaryStackableItem(GameTestHelper helper) {
        Item item = registeredDust(helper);
        ItemStack stack = new ItemStack(item);
        helper.assertTrue(item.getClass() == Item.class, "青金石粉必须沿用普通 Item");
        helper.assertTrue(stack.isStackable() && stack.getMaxStackSize() == 64, "必须保留普通物品默认堆叠");
        helper.assertTrue(!stack.isDamageableItem() && stack.getMaxDamage() == 0, "青金石粉不能带燃料耐久");
        helper.assertTrue(stack.isComponentsPatchEmpty(), "新建青金石粉不能携带实例状态");
        helper.assertTrue(item.components().equals(DataComponents.COMMON_ITEM_COMPONENTS),
                "注册属性必须与普通物品一致，不能附带热量或核安全组件");
        helper.assertTrue(stack.getDescriptionId().equals("item.create_nuclear_industry.lapis_dust"),
                "注册物品必须使用双语资源对应的翻译键");
        helper.succeed();
    }

    /** 使用真实数据包合并后的标签，验证子标签汇入父标签且其他提供者的既有成员保留。 */
    @GameTest(template = TEMPLATE)
    public static void loadedTagsAppendDustAndPreserveExistingParentMembers(GameTestHelper helper) {
        ItemStack dust = new ItemStack(registeredDust(helper));
        helper.assertTrue(dust.is(LAPIS_DUSTS), "正式粉末必须属于 c:dusts/lapis");
        helper.assertTrue(dust.is(DUSTS), "具体子标签必须汇入 c:dusts");
        helper.assertTrue(new ItemStack(Items.REDSTONE).is(DUSTS), "父标签必须保留红石粉");
        helper.assertTrue(new ItemStack(Items.GLOWSTONE_DUST).is(DUSTS), "父标签必须保留荧石粉");
        for (Item wrong : List.of(Items.LAPIS_LAZULI, Items.BLUE_DYE, Items.REDSTONE,
                Items.GLOWSTONE_DUST, ModItems.FRESH_FUEL_ASSEMBLY.get(),
                ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get())) {
            helper.assertTrue(!new ItemStack(wrong).is(LAPIS_DUSTS), "错误材料不能混入青金石粉子标签：" + wrong);
        }
        helper.succeed();
    }

    /** 在服务端用实际注册的创造页构建展示与搜索集合，检查入口可达且只出现一次。 */
    @GameTest(template = TEMPLATE)
    public static void creativeTabExposesFormalDustOnce(GameTestHelper helper) {
        Item item = registeredDust(helper);
        CreativeModeTab tab = ModCreativeTabs.MAIN.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                helper.getLevel().enabledFeatures(), false, helper.getLevel().registryAccess()));
        helper.assertTrue(tab.getDisplayItems().stream().filter(stack -> stack.is(item)).count() == 1,
                "本模组创造页必须恰好显示一份青金石粉");
        helper.assertTrue(tab.getSearchTabDisplayItems().stream().anyMatch(stack -> stack.is(item)),
                "青金石粉必须进入创造模式搜索集合");
        helper.succeed();
    }

    private static Item registeredDust(GameTestHelper helper) {
        // 默认物品注册表对不存在的 ID 返回 AIR，因此必须先检查键存在，避免假阳性。
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ITEM_ID), "正式青金石粉 ID 尚未注册");
        return BuiltInRegistries.ITEM.get(ITEM_ID);
    }

    private static TagKey<Item> dustTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
