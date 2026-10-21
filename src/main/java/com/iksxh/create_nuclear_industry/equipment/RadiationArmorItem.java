package com.iksxh.create_nuclear_industry.equipment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 沿用原皮革属性，仅为四件防护服提供公共侧的两层外观路径。
 * 默认RGB只供客户端显示回退；不新增染色玩法、材质、状态或网络数据。
 */
public final class RadiationArmorItem extends ArmorItem {
    public static final int DEFAULT_DYE_COLOR = 0xCFB56B;

    public RadiationArmorItem(ArmorItem.Type type, Item.Properties properties) {
        super(ArmorMaterials.LEATHER, type, properties);
    }

    /** 内层护腿选2、其它选1；原LEATHER可染层用布料base，固定层用overlay，不依赖客户端类型。 */
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return ResourceLocation.fromNamespaceAndPath("create_nuclear_industry", "textures/models/armor/radiation_layer_"
                + (innerModel ? 2 : 1) + (layer.dyeable() ? "" : "_overlay") + ".png");
    }
}
