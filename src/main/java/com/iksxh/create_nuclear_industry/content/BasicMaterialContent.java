package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 基础材料、锡条、传感器及轴承的物品注册入口；服务端加工由数据配方和原生机器执行。
 * 序列装配半成品使用 Create 的单件物品及组件进度，不保存反应堆状态。
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
    public static final DeferredItem<Item> TIN_WIRE = ITEMS.registerSimpleItem("tin_wire");
    public static final DeferredItem<Item> INDUSTRIAL_SENSOR = ITEMS.registerSimpleItem("industrial_sensor");
    public static final DeferredItem<Item> RADIATION_SENSOR = ITEMS.registerSimpleItem("radiation_sensor");
    public static final DeferredItem<Item> QUARTZ_DUST = ITEMS.registerSimpleItem("quartz_dust");
    public static final DeferredItem<Item> REFRACTORY_BRICK = ITEMS.registerSimpleItem("refractory_brick");
    public static final DeferredItem<Item> HEAVY_BEARING = ITEMS.registerSimpleItem("heavy_bearing");
    public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_INDUSTRIAL_SENSOR =
            ITEMS.registerItem("incomplete_industrial_sensor", SequencedAssemblyItem::new);
    public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_RADIATION_SENSOR =
            ITEMS.registerItem("incomplete_radiation_sensor", SequencedAssemblyItem::new);
    public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_HEAVY_BEARING =
            ITEMS.registerItem("incomplete_heavy_bearing", SequencedAssemblyItem::new);

    private BasicMaterialContent() {}

    /** 在模组注册事件总线提交普通材料和三个序列装配半成品。 */
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
