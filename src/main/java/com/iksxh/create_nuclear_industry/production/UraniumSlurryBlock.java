package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** 铀料浆的世界方块。源态和流动态共用服务端接触判定，施加原版中毒 I。 */
@EventBusSubscriber(modid = CreateNuclearIndustry.MOD_ID)
public final class UraniumSlurryBlock extends LiquidBlock {
    private static final int POISON_DURATION = 100;
    private static final int POISON_INTERVAL = 25;

    public UraniumSlurryBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living) || !living.isAlive()) return;
        // 原版回调按方块格判定相交，浅流还需按实际液面排除悬在液面上方的活体。
        if (living.getBoundingBox().minY >= pos.getY() + state.getFluidState().getHeight(level, pos)) return;
        applyPoison(living);
    }

    /** NoAI 生物仍计算液面，但可能不移动；在服务端复用本 tick 的浸液高度。 */
    @SubscribeEvent
    public static void checkStationaryContact(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Mob mob && mob.isNoAi() && !mob.level().isClientSide && mob.isAlive()
                && mob.isInFluidType(FuelProcessingContent.URANIUM_SLURRY_TYPE.get())) {
            applyPoison(mob);
        }
    }

    private static void applyPoison(LivingEntity living) {
        MobEffectInstance poison = living.getEffect(MobEffects.POISON);
        if (poison == null) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DURATION));
            return;
        }
        if (poison.isInfiniteDuration() || poison.getAmplifier() > 0 || poison.getDuration() > POISON_INTERVAL) return;

        // 中毒 I 每逢剩余时间可被 25 整除时扣血。增加完整周期保留伤害相位，下一格接触也不会重复续期。
        living.addEffect(new MobEffectInstance(MobEffects.POISON, poison.getDuration() + POISON_DURATION - POISON_INTERVAL,
                0, poison.isAmbient(), poison.isVisible(), poison.showIcon()));
    }
}
