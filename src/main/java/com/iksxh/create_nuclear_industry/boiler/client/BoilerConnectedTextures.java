package com.iksxh.create_nuclear_industry.boiler.client;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.CreateClient;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 客户端八个RECTANGLE图集、一个八向窗口图集与八个锅炉block模型的独立接入。
 * 只登记模型包装和sprite entry；结构归属与隔层由锅炉L1提供，物品/Ponder无可靠快照时保留原quad。
 */
public final class BoilerConnectedTextures {
    private static final String NAMESPACE = "create_nuclear_industry";
    private static final List<String> BLOCKS = List.of("high_pressure_boiler_casing", "high_pressure_boiler_window",
            "high_pressure_boiler_controller", "high_pressure_boiler_water_port", "high_pressure_boiler_steam_port",
            "high_pressure_boiler_hot_coolant_port", "high_pressure_boiler_cold_coolant_port", "boiler_safety_valve");
    private static final Set<String> MATERIAL_IDS = java.util.stream.Stream.concat(BLOCKS.stream(),
            java.util.stream.Stream.of("nuclear_heat_exchanger")).map(name -> id(name).toString()).collect(Collectors.toUnmodifiableSet());
    /** 固定原创资源映射；普通side与隔层side共用原图，但目标键不同，禁止原图Map覆盖。 */
    private static final List<ShiftSpec> SHIFTS = List.of(
            new ShiftSpec("high_pressure_boiler/casing_side", "side"),
            new ShiftSpec("high_pressure_boiler/casing_side", "partition_side"),
            new ShiftSpec("high_pressure_boiler/casing_top", "top"),
            new ShiftSpec("high_pressure_boiler/window_frame", "window_frame"),
            new ShiftSpec("high_pressure_boiler/water_port_face", "water_port_face"),
            new ShiftSpec("high_pressure_boiler/steam_port_face", "steam_port_face"),
            new ShiftSpec("high_pressure_boiler/controller_face", "controller_face"),
            new ShiftSpec("high_pressure_boiler_hot_coolant_port", "hot_coolant_port"),
            new ShiftSpec("high_pressure_boiler_cold_coolant_port", "cold_coolant_port"));
    private record ShiftSpec(String original, String target) { }
    private static final Registration REGISTRATION = new Registration();
    private static final ThreadLocal<BuildFrame> BUILD_SCOPE = new ThreadLocal<>();
    private static final ModelProperty<Integer> SHARED_WINDOW_FACES = new ModelProperty<>();
    private static final ModelProperty<Integer> PARTITION_FACES = new ModelProperty<>();
    private static final ThreadLocal<Integer> QUAD_PARTITION_SCOPE = new ThreadLocal<>();

    private BoilerConnectedTextures() { }

    /**
     * 在客户端同步RegisterGeometryLoaders事件内调用，早于首次模型/atlas准备；重载时幂等。
     * 不排队、不重复Create监听器；类初始化本身不触碰Create注册表或当前世界。
     */
    public static void initializeOnce() {
        REGISTRATION.initialize(BoilerConnectedTextures::createShift,
                (block, factory) -> CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(block, factory));
    }

    /** 实际Create工厂边界，保留entry供资源重载更新；类型由生产sprite域决定。 */
    static CTSpriteShiftEntry createShift(ResourceLocation original, ResourceLocation target) {
        return CTSpriteShifter.getCT(original.equals(id("block/high_pressure_boiler/window_frame"))
                ? AllCTTypes.OMNIDIRECTIONAL : AllCTTypes.RECTANGLE, original, target);
    }

    static boolean supportsBlock(String localId) {
        return MATERIAL_IDS.contains(localId);
    }

    /** 只能读取同一模型上下文的当前作用域；缺少作用域时禁止借用全局诊断快照。 */
    static BoilerSurfaceSnapshot currentSnapshot(BlockAndTintGetter context) {
        BuildFrame frame = BUILD_SCOPE.get();
        return frame != null && frame.context() == context ? frame.snapshot() : BoilerSurfaceSnapshot.empty();
    }

    /** 一次模型重建只capture一次；嵌套构建与异常退出均恢复调用者作用域，顶层退出remove。 */
    static <T> T withCapturedSnapshot(BlockAndTintGetter context, Supplier<T> operation) {
        BuildFrame previous = BUILD_SCOPE.get();
        BUILD_SCOPE.set(new BuildFrame(context, BoilerSurfaceSnapshots.capture(context)));
        try {
            return operation.get();
        } finally {
            if (previous == null) BUILD_SCOPE.remove();
            else BUILD_SCOPE.set(previous);
        }
    }

    static BakedModel wrapModel(BakedModel original, BoilerConnectedTextureBehaviour behaviour) {
        return new ScopedCTModel(original, behaviour);
    }

    /** 临时线程上下文覆盖同次super的CT数据与共享面mask计算，结束后finally恢复或清理，不传递给getQuads或其他线程。 */
    private record BuildFrame(BlockAndTintGetter context, BoilerSurfaceSnapshot snapshot) { }

    /** 保留Create原生buildContext/UV；getModelData是final，只在其gather入口建立快照作用域。 */
    private static final class ScopedCTModel extends CTModel {
        private final BoilerConnectedTextureBehaviour boilerBehaviour;

        private ScopedCTModel(BakedModel original, BoilerConnectedTextureBehaviour behaviour) {
            super(original, behaviour);
            boilerBehaviour = behaviour;
        }

        @Override
        protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world,
                                                    BlockPos pos, BlockState state, ModelData blockEntityData) {
            return withCapturedSnapshot(world, () -> super.gatherModelData(builder, world, pos, state, blockEntityData)
                    .with(SHARED_WINDOW_FACES, boilerBehaviour.sharedWindowFaces(world, pos, state))
                    .with(PARTITION_FACES, boilerBehaviour.partitionFaces(world, pos, state)));
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random,
                                        ModelData data, RenderType renderType) {
            // 不读取世界或构建快照，隔层选择来自本次ModelData，短作用域保存/恢复供Create出quad使用；缺失掩码为0。
            Integer stored = data.has(SHARED_WINDOW_FACES) ? data.get(SHARED_WINDOW_FACES) : null;
            int mask = stored == null ? 0 : stored;
            if (side != null && (mask & (1 << side.get3DDataValue())) != 0) return List.of();
            Integer previous = QUAD_PARTITION_SCOPE.get();
            Integer partition = data.has(PARTITION_FACES) ? data.get(PARTITION_FACES) : null;
            List<BakedQuad> quads;
            QUAD_PARTITION_SCOPE.set(partition == null ? 0 : partition);
            try {
                // Create在出quad时选真实sprite；这里仅借用本次ModelData的面mask，不重新捕获快照。
                quads = super.getQuads(state, side, random, data, renderType);
            } finally {
                if (previous == null) QUAD_PARTITION_SCOPE.remove();
                else QUAD_PARTITION_SCOPE.set(previous);
            }
            if (mask == 0 || side != null) return quads;
            // side=null的混合列表按实际quad方向复制过滤，不能修改super可能共享的原列表。
            return quads.stream().filter(quad -> (mask & (1 << quad.getDirection().get3DDataValue())) == 0).toList();
        }
    }

    /**
     * 实际登记器的两个外部边界是Create的shift工厂与block模型登记；不可变表供模型工作线程只读。
     * 初始化发生于客户端MOD同步事件，锁保证后续资源重载不会重复添加包装器。
     */
    static final class Registration {
        private volatile Map<ResourceLocation, CTSpriteShiftEntry> entries = Map.of();
        private volatile CTSpriteShiftEntry partitionSide;
        private boolean initialized;

        synchronized void initialize(BiFunction<ResourceLocation, ResourceLocation, CTSpriteShiftEntry> shifts,
                                     BiConsumer<ResourceLocation, NonNullFunction<BakedModel, ? extends BakedModel>> models) {
            if (initialized) return;
            Map<ResourceLocation, CTSpriteShiftEntry> created = new LinkedHashMap<>();
            CTSpriteShiftEntry partition = null;
            for (ShiftSpec spec : SHIFTS) {
                ResourceLocation original = id("block/" + spec.original());
                CTSpriteShiftEntry entry = shifts.apply(original, id("block/boiler_ct/" + spec.target()));
                if (spec.target().equals("partition_side")) partition = entry;
                else created.put(original, entry);
            }
            entries = Map.copyOf(created);
            partitionSide = partition;
            var behaviour = new BoilerConnectedTextureBehaviour(this::shiftForSprite);
            // 只用八个正式block注册ID；独立换热器可作邻接证据，但不包装其模型或物品。
            for (String block : BLOCKS) models.accept(id(block), original -> wrapModel(original, behaviour));
            initialized = true;
        }

        CTSpriteShiftEntry shiftForSprite(ResourceLocation sprite, boolean partition) {
            return partition && sprite.equals(id("block/high_pressure_boiler/casing_side"))
                    ? partitionSide : entries.get(sprite);
        }
    }

    /** 只在出quad作用域消费ModelData给定面，作用域外回普通材质。 */
    static boolean isPartitionFace(Direction face) {
        Integer mask = QUAD_PARTITION_SCOPE.get();
        return mask != null && face != null && (mask & (1 << face.get3DDataValue())) != 0;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
    }
}
