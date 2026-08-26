package com.iksxh.create_nuclear_industry.p0probe.arm;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeContent;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

/** 仅供 P0 探针使用的隔离 Create 机械臂目标类型，不拥有正式 P1 物品事务。 */
public final class P0ProbeArmInteractionPoint extends ArmInteractionPoint {
    public static final ArmInteractionPointType TYPE = new ArmInteractionPointType() {
        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return state.is(P0ProbeContent.P0_PROBE_ARM_TARGET.get());
        }

        @Override
        public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new P0ProbeArmInteractionPoint(this, level, pos, state);
        }

        @Override
        public int getPriority() {
            return 1000;
        }
    };
    private P0ProbeArmInteractionPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
        super(type, level, pos, state);
    }

    /** 在 Create 注册生命周期中挂入历史探针目标类型。 */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(P0ProbeArmInteractionPoint::onRegister);
    }

    private static void onRegister(RegisterEvent event) {
        event.register(
                CreateRegistries.ARM_INTERACTION_POINT_TYPE,
                ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "p0_probe_arm_target"),
                () -> TYPE
        );
    }
}
