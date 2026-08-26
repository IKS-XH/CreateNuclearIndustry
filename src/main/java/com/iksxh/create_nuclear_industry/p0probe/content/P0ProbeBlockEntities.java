package com.iksxh.create_nuclear_industry.p0probe.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.p0probe.blockentity.P0ProbeArmTargetBlockEntity;
import com.iksxh.create_nuclear_industry.p0probe.blockentity.P0ProbeBlockEntity;
import com.iksxh.create_nuclear_industry.p0probe.blockentity.P0ProbeFluidPortBlockEntity;
import com.iksxh.create_nuclear_industry.p0probe.blockentity.P0ProbeGoggleBlockEntity;
import com.iksxh.create_nuclear_industry.p0probe.blockentity.P0ProbeSliderBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** P0 回归探针方块实体类型的隔离注册层，不向正式 P1 快照提供状态。 */
public final class P0ProbeBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CreateNuclearIndustry.MOD_ID
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<P0ProbeBlockEntity>> P0_PROBE = BLOCK_ENTITY_TYPES.register(
            "p0_probe_block",
            () -> BlockEntityType.Builder.of(P0ProbeBlockEntity::new, P0ProbeContent.P0_PROBE_BLOCK.get()).build(null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<P0ProbeSliderBlockEntity>> P0_PROBE_SLIDER = BLOCK_ENTITY_TYPES.register(
            "p0_probe_slider_block",
            () -> BlockEntityType.Builder.of(P0ProbeSliderBlockEntity::new, P0ProbeContent.P0_PROBE_SLIDER_BLOCK.get()).build(null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<P0ProbeGoggleBlockEntity>> P0_PROBE_GOGGLE = BLOCK_ENTITY_TYPES.register(
            "p0_probe_goggle_block",
            () -> BlockEntityType.Builder.of(P0ProbeGoggleBlockEntity::new, P0ProbeContent.P0_PROBE_GOGGLE_BLOCK.get()).build(null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<P0ProbeArmTargetBlockEntity>> P0_PROBE_ARM_TARGET = BLOCK_ENTITY_TYPES.register(
            "p0_probe_arm_target",
            () -> BlockEntityType.Builder.of(P0ProbeArmTargetBlockEntity::new, P0ProbeContent.P0_PROBE_ARM_TARGET.get()).build(null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<P0ProbeFluidPortBlockEntity>> P0_PROBE_COLD_PORT = BLOCK_ENTITY_TYPES.register(
            "p0_probe_cold_port",
            () -> BlockEntityType.Builder.of((pos, state) -> new P0ProbeFluidPortBlockEntity(pos, state, true), P0ProbeContent.P0_PROBE_COLD_PORT.get()).build(null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<P0ProbeFluidPortBlockEntity>> P0_PROBE_HOT_PORT = BLOCK_ENTITY_TYPES.register(
            "p0_probe_hot_port",
            () -> BlockEntityType.Builder.of((pos, state) -> new P0ProbeFluidPortBlockEntity(pos, state, false), P0ProbeContent.P0_PROBE_HOT_PORT.get()).build(null)
    );

    private P0ProbeBlockEntities() {
    }

    /** 在模组注册阶段提交 P0 探针方块实体类型。 */
    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
