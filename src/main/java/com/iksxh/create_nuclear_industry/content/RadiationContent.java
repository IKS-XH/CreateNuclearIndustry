package com.iksxh.create_nuclear_industry.content;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.*;
/** 首批四件皮甲基础属性防护服和无GUI仪表；材质直接复用原版皮革。 */
public final class RadiationContent {
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("create_nuclear_industry");
    public static final DeferredItem<ArmorItem> HELMET=armor("radiation_helmet",ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> CHESTPLATE=armor("radiation_chestplate",ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> LEGGINGS=armor("radiation_leggings",ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> BOOTS=armor("radiation_boots",ArmorItem.Type.BOOTS);
    public static final DeferredItem<Item> METER=ITEMS.register("radiation_meter",()->new Item(new Item.Properties().stacksTo(1)));
    private RadiationContent() {}
    private static DeferredItem<ArmorItem> armor(String id,ArmorItem.Type type) {
        return ITEMS.register(id,()->new ArmorItem(ArmorMaterials.LEATHER,type,new Item.Properties().durability(type.getDurability(5))));
    }
    /** 向模组事件总线注册物品及一次性护目镜接线，服务端和客户端共用同一物品身份。 */
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(RadiationContent::setup);
    }
    private static void setup(FMLCommonSetupEvent event) {
        // 物品注册完成后排队接入Create原生判定；仅头部穿戴生效，持有或背包不提供护目镜功能。
        event.enqueueWork(() -> GogglesItem.addIsWearingPredicate(
                player -> player.getItemBySlot(EquipmentSlot.HEAD).is(HELMET.get())));
    }
}
