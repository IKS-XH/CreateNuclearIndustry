package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.blockentity.ControlRodDriveBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 正式 P1 方块实体类型的注册层。
 *
 * <p>仪表端口、冷/热/补料端口和控制棒驱动器分别声明可承载它们的方块；
 * 这些类型只负责注册和实例化，具体状态由各自的方块实体维护。</p>
 */
public final class P1BlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CreateNuclearIndustry.MOD_ID
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorInstrumentPortBlockEntity>>
            REACTOR_INSTRUMENT_PORT = BLOCK_ENTITY_TYPES.register(
            P1ContentIds.REACTOR_INSTRUMENT_PORT_ID,
            () -> BlockEntityType.Builder.of(
                    ReactorInstrumentPortBlockEntity::new,
                    P1Blocks.REACTOR_INSTRUMENT_PORT.get()
            ).build(null)
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorPortBlockEntity>> REACTOR_PORT =
            BLOCK_ENTITY_TYPES.register(
                    "reactor_port",
                    () -> BlockEntityType.Builder.of(
                            ReactorPortBlockEntity::new,
                            P1Blocks.REACTOR_COLD_PORT.get(),
                            P1Blocks.REACTOR_HOT_PORT.get(),
                            P1Blocks.REACTOR_REFUELING_PORT.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ControlRodDriveBlockEntity>>
            CONTROL_ROD_DRIVE = BLOCK_ENTITY_TYPES.register(
            P1ContentIds.CONTROL_ROD_DRIVE_ID,
            () -> BlockEntityType.Builder.of(
                    ControlRodDriveBlockEntity::new,
                    P1Blocks.CONTROL_ROD_DRIVE.get()
            ).build(null)
    );

    private P1BlockEntities() {
    }

    /** 在方块实体注册阶段提交所有 P1 方块实体类型。 */
    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
