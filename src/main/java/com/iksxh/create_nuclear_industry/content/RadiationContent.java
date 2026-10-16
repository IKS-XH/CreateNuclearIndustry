package com.iksxh.create_nuclear_industry.content;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
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
    public static void register(IEventBus bus) {ITEMS.register(bus);}
}
