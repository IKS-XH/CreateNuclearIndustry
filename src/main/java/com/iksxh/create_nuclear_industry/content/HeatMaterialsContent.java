package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 核换热器制造所需材料的注册入口；配方与设备行为由各自的数据和内容层负责。 */
public final class HeatMaterialsContent {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateNuclearIndustry.MOD_ID);

    public static final DeferredItem<Item> STEEL_PIPE_BLANK = ITEMS.registerSimpleItem("steel_pipe_blank");
    public static final DeferredItem<Item> REINFORCED_STEEL_PLATE = ITEMS.registerSimpleItem("reinforced_steel_plate");
    public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_REINFORCED_STEEL_PLATE =
            ITEMS.registerItem("incomplete_reinforced_steel_plate", SequencedAssemblyItem::new);
    public static final DeferredItem<Item> NUCLEAR_HEAT_EXCHANGE_BUNDLE = ITEMS.registerSimpleItem(
            "nuclear_heat_exchange_bundle");

    private HeatMaterialsContent() {
    }

    /** 将本批换热器材料和 Create 序列装配半成品接入模组物品注册总线。 */
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
