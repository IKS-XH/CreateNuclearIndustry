package com.iksxh.create_nuclear_industry.gametest.turbine;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/** 仅在独立 GameTest 命名空间启动时注册的两种探针方块及实体，不进入正式物品表。 */
@EventBusSubscriber(modid = CreateNuclearIndustry.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class TurbineProbeContent {
    public static final String NAMESPACE = "create_nuclear_industry_turbine_probe";
    private static TurbineProbeShaftBlock shaft;
    private static TurbineProbeOwnerBlock owner;
    private static BlockEntityType<TurbineProbeShaftBlockEntity> shaftEntity;
    private static BlockEntityType<TurbineProbeOwnerBlockEntity> ownerEntity;

    private TurbineProbeContent() {}

    public static TurbineProbeShaftBlock shaft() { return shaft; }
    public static TurbineProbeOwnerBlock owner() { return owner; }
    public static BlockEntityType<TurbineProbeShaftBlockEntity> shaftEntity() { return shaftEntity; }
    public static BlockEntityType<TurbineProbeOwnerBlockEntity> ownerEntity() { return ownerEntity; }

    /** 注册阶段只识别测试专用系统属性，常规客户端与服务端不会加载探针 ID。 */
    @SubscribeEvent
    public static void register(RegisterEvent event) {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        boolean enabled = java.util.Arrays.asList(namespaces.split(",")).contains(NAMESPACE);
        if (!enabled) return;
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            // 方块只能在注册表开放的事件内构造；静态初始化会触发冻结表写入异常。
            event.register(Registries.BLOCK, id("shaft"), () -> shaft = new TurbineProbeShaftBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
            event.register(Registries.BLOCK, id("owner"), () -> owner = new TurbineProbeOwnerBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
        }
        if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
            shaftEntity = BlockEntityType.Builder.of(TurbineProbeShaftBlockEntity::new, shaft).build(null);
            ownerEntity = BlockEntityType.Builder.of(TurbineProbeOwnerBlockEntity::new, owner).build(null);
            event.register(Registries.BLOCK_ENTITY_TYPE, id("shaft"), () -> shaftEntity);
            event.register(Registries.BLOCK_ENTITY_TYPE, id("owner"), () -> ownerEntity);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
    }
}
