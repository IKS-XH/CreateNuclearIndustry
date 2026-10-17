package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.equipment.DepletedUraniumArmorItem;
import com.iksxh.create_nuclear_industry.equipment.DepletedUraniumTier;
import java.util.List;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 贫化铀锭、普通储存块与九件原生装备的注册入口；加工和升级均由数据配方执行。 */
public final class DepletedUraniumContent {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateNuclearIndustry.MOD_ID);
    private static final DepletedUraniumTier TIER = DepletedUraniumTier.INSTANCE;

    public static final DeferredItem<Item> INGOT = ITEMS.registerSimpleItem("depleted_uranium_ingot");
    public static final DeferredBlock<Block> BLOCK = BLOCKS.register("depleted_uranium_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
    public static final DeferredItem<BlockItem> BLOCK_ITEM = ITEMS.registerSimpleBlockItem(BLOCK);
    public static final DeferredItem<DepletedUraniumArmorItem> HELMET = armor("helmet", ArmorItem.Type.HELMET, Items.NETHERITE_HELMET);
    public static final DeferredItem<DepletedUraniumArmorItem> CHESTPLATE = armor("chestplate", ArmorItem.Type.CHESTPLATE, Items.NETHERITE_CHESTPLATE);
    public static final DeferredItem<DepletedUraniumArmorItem> LEGGINGS = armor("leggings", ArmorItem.Type.LEGGINGS, Items.NETHERITE_LEGGINGS);
    public static final DeferredItem<DepletedUraniumArmorItem> BOOTS = armor("boots", ArmorItem.Type.BOOTS, Items.NETHERITE_BOOTS);
    public static final DeferredItem<SwordItem> SWORD = ITEMS.registerItem("depleted_uranium_sword", p -> new SwordItem(TIER,
            p.fireResistant().attributes(SwordItem.createAttributes(TIER, 3, -2.4F))));
    public static final DeferredItem<PickaxeItem> PICKAXE = ITEMS.registerItem("depleted_uranium_pickaxe", p -> new PickaxeItem(TIER,
            p.fireResistant().attributes(PickaxeItem.createAttributes(TIER, 1.0F, -2.8F))));
    public static final DeferredItem<AxeItem> AXE = ITEMS.registerItem("depleted_uranium_axe", p -> new AxeItem(TIER,
            p.fireResistant().attributes(AxeItem.createAttributes(TIER, 5.0F, -3.0F))));
    public static final DeferredItem<ShovelItem> SHOVEL = ITEMS.registerItem("depleted_uranium_shovel", p -> new ShovelItem(TIER,
            p.fireResistant().attributes(ShovelItem.createAttributes(TIER, 1.5F, -3.0F))));
    public static final DeferredItem<HoeItem> HOE = ITEMS.registerItem("depleted_uranium_hoe", p -> new HoeItem(TIER,
            p.fireResistant().attributes(HoeItem.createAttributes(TIER, -4.0F, 0.0F))));
    public static final List<DeferredItem<? extends Item>> EQUIPMENT = List.of(HELMET, CHESTPLATE, LEGGINGS, BOOTS, SWORD, PICKAXE, AXE, SHOVEL, HOE);

    private DepletedUraniumContent() {}

    private static DeferredItem<DepletedUraniumArmorItem> armor(String id, ArmorItem.Type type, Item vanilla) {
        return ITEMS.registerItem("depleted_uranium_" + id, p -> new DepletedUraniumArmorItem(type,
                p.fireResistant().durability(vanilla.components().get(net.minecraft.core.component.DataComponents.MAX_DAMAGE))));
    }

    /** 在公共模组总线提交身份；原版锻造保留底材组件，不在此建立升级事务。 */
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
