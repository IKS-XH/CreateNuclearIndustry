package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BucketItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 样例与 P1 共用物品的注册层。
 *
 * <p>新燃料组件的耐久上限以 20 tick/s 换算为 3 小时，并作为燃料寿命的物品层
 * 锚点；反应堆模拟的服务端状态仍由反应堆快照维护。</p>
 */
public final class ModItems {
    /** 按 20 tick/s 换算的三小时燃料寿命锚点，与 P1 燃料组件耐久一致。 */
    public static final int FRESH_FUEL_ASSEMBLY_MAX_DURABILITY = 3 * 60 * 60 * 20;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(
            CreateNuclearIndustry.MOD_ID
    );

    public static final DeferredItem<Item> FRESH_FUEL_ASSEMBLY = ITEMS.register(
            P1ContentIds.FRESH_FUEL_ASSEMBLY_ID,
            () -> new Item(new Item.Properties()
                    .stacksTo(1)
                    .durability(FRESH_FUEL_ASSEMBLY_MAX_DURABILITY))
    );

    public static final DeferredItem<Item> COOLED_SPENT_FUEL_ASSEMBLY = ITEMS.register(
            P1ContentIds.COOLED_SPENT_FUEL_ASSEMBLY_ID,
            () -> new Item(new Item.Properties().stacksTo(1))
    );

    public static final DeferredItem<Item> CONTROL_ROD = ITEMS.register(
            P1ContentIds.CONTROL_ROD_ID,
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<Item> STEEL_PLATE = ITEMS.register(
            P1ContentIds.STEEL_PLATE_ID,
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<BucketItem> COMPOUND_COOLANT_BUCKET = ITEMS.register(
            P1ContentIds.COMPOUND_COOLANT_BUCKET_ID,
            () -> new BucketItem(ModFluids.COMPOUND_COOLANT_SOURCE.get(), new Item.Properties().stacksTo(1))
    );

    private ModItems() {
    }

    /** 在模组事件总线的注册阶段提交所有物品。 */
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
