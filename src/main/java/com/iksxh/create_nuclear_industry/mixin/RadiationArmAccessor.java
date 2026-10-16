package com.iksxh.create_nuclear_industry.mixin;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** 只读机械臂正在转运的唯一真实栈，不改变机械臂运输。 */
@Mixin(ArmBlockEntity.class)
public interface RadiationArmAccessor {
    @Accessor("heldItem") ItemStack radiation$getHeldItem();
}
