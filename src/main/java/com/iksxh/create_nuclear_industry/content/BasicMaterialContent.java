package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 基础金属与钢材材料的物品注册入口；这里只定义可堆叠身份，服务端加工由数据配方和原生机器执行。
 * 这些身份不保存反应堆状态，也不在客户端另建产物映射。
 */
public final class BasicMaterialContent {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateNuclearIndustry.MOD_ID);

    public static final DeferredItem<Item> LEAD_INGOT = ITEMS.registerSimpleItem("lead_ingot");
    public static final DeferredItem<Item> TIN_INGOT = ITEMS.registerSimpleItem("tin_ingot");
    public static final DeferredItem<Item> LEAD_PLATE = ITEMS.registerSimpleItem("lead_plate");
    public static final DeferredItem<Item> TIN_PLATE = ITEMS.registerSimpleItem("tin_plate");
    public static final DeferredItem<Item> LEAD_NUGGET = ITEMS.registerSimpleItem("lead_nugget");
    public static final DeferredItem<Item> TIN_NUGGET = ITEMS.registerSimpleItem("tin_nugget");
    public static final DeferredItem<Item> IRON_DUST = ITEMS.registerSimpleItem("iron_dust");
    public static final DeferredItem<Item> COAL_DUST = ITEMS.registerSimpleItem("coal_dust");
    public static final DeferredItem<Item> CHARCOAL_DUST = ITEMS.registerSimpleItem("charcoal_dust");
    public static final DeferredItem<Item> STEEL_DUST = ITEMS.registerSimpleItem("steel_dust");
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");

    private BasicMaterialContent() {}

    /** 在模组注册事件总线提交十一种普通材料物品。 */
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
