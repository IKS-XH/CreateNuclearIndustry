package com.iksxh.create_nuclear_industry.radiation.client;
import com.iksxh.create_nuclear_industry.content.RadiationContent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.*;
/** 复用原版皮甲默认色及饰纹模型；只补自有物品的原生颜色绑定，不添加专属贴图。 */
@EventBusSubscriber(modid="create_nuclear_industry",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class RadiationClientRegistration {
    private RadiationClientRegistration() {}
    private static Item[] armor(){return new Item[]{RadiationContent.HELMET.get(),RadiationContent.CHESTPLATE.get(),RadiationContent.LEGGINGS.get(),RadiationContent.BOOTS.get()};}
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event) {
        event.register((stack,index)->index==0?DyedItemColor.getOrDefault(stack,DyedItemColor.LEATHER_COLOR):-1,armor());
    }
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override public int getDefaultDyeColor(ItemStack stack){return DyedItemColor.getOrDefault(stack,DyedItemColor.LEATHER_COLOR);}
        },armor());
    }
}
