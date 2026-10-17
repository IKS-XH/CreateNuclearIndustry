package com.iksxh.create_nuclear_industry.equipment;

import com.iksxh.create_nuclear_industry.content.DepletedUraniumContent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;

/** 保留下界合金护甲材质与属性，仅绑定专属外观层；贫化铀锭是唯一材料修复输入。 */
public final class DepletedUraniumArmorItem extends ArmorItem {
    /** 由公共注册阶段构造；不引用任何客户端渲染类型。 */
    public DepletedUraniumArmorItem(Type type, Properties properties) {
        super(ArmorMaterials.NETHERITE, type, properties);
    }

    /**
     * 仅覆盖原生护甲贴图路径；innerModel对应护腿内层，其余槽位使用外层。
     * 参数均为公共类型，服务端加载不依赖client类；材质、饰纹和属性仍走原生机制。
     */
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
            ArmorMaterial.Layer layer, boolean innerModel) {
        return ResourceLocation.fromNamespaceAndPath("create_nuclear_industry",
                "textures/models/armor/depleted_uranium_layer_" + (innerModel ? "2" : "1") + ".png");
    }

    @Override
    public boolean isValidRepairItem(ItemStack equipment, ItemStack material) {
        return material.is(DepletedUraniumContent.INGOT.get());
    }
}
