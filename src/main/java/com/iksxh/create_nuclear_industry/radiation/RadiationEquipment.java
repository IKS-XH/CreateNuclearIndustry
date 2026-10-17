package com.iksxh.create_nuclear_industry.radiation;
import com.iksxh.create_nuclear_industry.content.RadiationContent;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
/** 只按对应穿戴槽加防护，携带源的向外强度不受穿戴影响。 */
public final class RadiationEquipment {
    private RadiationEquipment() {}
    public static double reduction(LivingEntity player,RadiationParameters p) {
        double reduction=0;
        if(player.getItemBySlot(EquipmentSlot.HEAD).is(RadiationContent.HELMET)) reduction+=p.head();
        if(player.getItemBySlot(EquipmentSlot.CHEST).is(RadiationContent.CHESTPLATE)) reduction+=p.chest();
        if(player.getItemBySlot(EquipmentSlot.LEGS).is(RadiationContent.LEGGINGS)) reduction+=p.legs();
        if(player.getItemBySlot(EquipmentSlot.FEET).is(RadiationContent.BOOTS)) reduction+=p.feet();
        return Math.min(p.maxReduction(),reduction);
    }
    public static boolean holdingMeter(LivingEntity player) {
        return player.getMainHandItem().is(RadiationContent.METER)||player.getOffhandItem().is(RadiationContent.METER);
    }
}
