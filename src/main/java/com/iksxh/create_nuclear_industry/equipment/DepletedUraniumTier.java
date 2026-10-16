package com.iksxh.create_nuclear_industry.equipment;

import com.iksxh.create_nuclear_industry.content.DepletedUraniumContent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** 贫化铀工具材料：服务端采掘和耐久沿用下界合金，仅修复输入改为本模组贫化铀锭。 */
public enum DepletedUraniumTier implements Tier {
    INSTANCE;

    @Override public int getUses() { return Tiers.NETHERITE.getUses(); }
    @Override public float getSpeed() { return Tiers.NETHERITE.getSpeed(); }
    @Override public float getAttackDamageBonus() { return Tiers.NETHERITE.getAttackDamageBonus(); }
    @Override public TagKey<Block> getIncorrectBlocksForDrops() { return Tiers.NETHERITE.getIncorrectBlocksForDrops(); }
    @Override public int getEnchantmentValue() { return Tiers.NETHERITE.getEnchantmentValue(); }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(DepletedUraniumContent.INGOT.get()); }
}
