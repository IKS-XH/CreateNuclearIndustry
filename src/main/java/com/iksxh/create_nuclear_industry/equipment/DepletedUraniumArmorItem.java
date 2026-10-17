package com.iksxh.create_nuclear_industry.equipment;

import com.iksxh.create_nuclear_industry.content.DepletedUraniumContent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;

/** 复用原版下界合金护甲材质、属性和穿戴层；贫化铀锭是唯一材料修复输入。 */
public final class DepletedUraniumArmorItem extends ArmorItem {
    /** 由公共注册阶段构造；不引用任何客户端渲染类型。 */
    public DepletedUraniumArmorItem(Type type, Properties properties) {
        super(ArmorMaterials.NETHERITE, type, properties);
    }

    @Override
    public boolean isValidRepairItem(ItemStack equipment, ItemStack material) {
        return material.is(DepletedUraniumContent.INGOT.get());
    }
}
