package com.iksxh.create_nuclear_industry.production;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/** 1000 mB 密闭料浆桶保留标准 BucketItem 流体能力，但不允许向世界倾倒。 */
public final class SlurryBucketItem extends BucketItem {
    public SlurryBucketItem(Fluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
