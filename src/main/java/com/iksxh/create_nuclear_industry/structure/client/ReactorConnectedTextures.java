package com.iksxh.create_nuclear_industry.structure.client;

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
 * 客户端13个RECTANGLE图集、一个八向窗口图集与七个block模型的专属接入。
 * 只登记模型包装和sprite entry；结构归属由L1提供，物品/Ponder无可靠快照时保留原quad。
 */
public final class ReactorConnectedTextures {
    private static final String NAMESPACE = "create_nuclear_industry";
    private static final List<String> BLOCKS = List.of("reactor_casing", "reactor_window", "reactor_instrument_port",
            "reactor_cold_port", "reactor_hot_port", "reactor_refueling_port", "control_rod_drive");
    private static final List<String> SPRITES = List.of("reactor_casing_side", "reactor_casing_top", "reactor_casing_bottom",
            "reactor_hot_port_side", "reactor_hot_port_top", "reactor_cold_port_side", "reactor_cold_port_top",
            "reactor_window", "reactor_instrument_port_side", "reactor_instrument_port_top",
            "reactor_refueling_port_side", "reactor_refueling_port_top", "control_rod_drive_side", "control_rod_drive_top");
    private static final Set<String> MATERIAL_IDS = BLOCKS.stream().map(name -> id(name).toString()).collect(Collectors.toUnmodifiableSet());
    private static final Registration REGISTRATION = new Registration();
    private static final ThreadLocal<BuildFrame> BUILD_SCOPE = new ThreadLocal<>();
    private static final ModelProperty<Integer> SHARED_WINDOW_FACES = new ModelProperty<>();

    private ReactorConnectedTextures() { }

    /**
     * 在客户端同步RegisterGeometryLoaders事件内调用，早于首次模型/atlas准备；重载时幂等。
     * 不排队、不重复Create监听器；类初始化本身不触碰Create注册表或当前世界。
     */
    public static void initializeOnce() {
        REGISTRATION.initialize(ReactorConnectedTextures::createShift,
                (block, factory) -> CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(block, factory));
    }

    /** 实际Create工厂边界，保留entry供资源重载更新；类型由生产sprite域决定。 */
    static CTSpriteShiftEntry createShift(ResourceLocation original, ResourceLocation target) {
        return CTSpriteShifter.getCT(original.equals(id("block/reactor_window"))
                ? AllCTTypes.OMNIDIRECTIONAL : AllCTTypes.RECTANGLE, original, target);
    }

    static boolean supportsBlock(String localId) {
        return MATERIAL_IDS.contains(localId);
    }

    /** 只能读取同一模型上下文的当前作用域；缺少作用域时禁止借用全局诊断快照。 */
    static ReactorSurfaceSnapshot currentSnapshot(BlockAndTintGetter context) {
        BuildFrame frame = BUILD_SCOPE.get();
        return frame != null && frame.context() == context ? frame.snapshot() : ReactorSurfaceSnapshot.empty();
    }

    /** 一次模型重建只capture一次；嵌套构建与异常退出均恢复调用者作用域，顶层退出remove。 */
    static <T> T withCapturedSnapshot(BlockAndTintGetter context, Supplier<T> operation) {
        BuildFrame previous = BUILD_SCOPE.get();
        BUILD_SCOPE.set(new BuildFrame(context, ReactorSurfaceSnapshots.capture(context)));
        try {
            return operation.get();
        } finally {
            if (previous == null) BUILD_SCOPE.remove();
            else BUILD_SCOPE.set(previous);
        }
    }

    static BakedModel wrapModel(BakedModel original, ReactorConnectedTextureBehaviour behaviour) {
        return new ScopedCTModel(original, behaviour);
    }

    /** 临时线程上下文覆盖同次super的CT数据与共享面mask计算，结束后finally恢复或清理，不传递给getQuads或其他线程。 */
    private record BuildFrame(BlockAndTintGetter context, ReactorSurfaceSnapshot snapshot) { }

    /** 保留Create原生buildContext/UV；getModelData是final，只在其gather入口建立快照作用域。 */
    private static final class ScopedCTModel extends CTModel {
        private final ReactorConnectedTextureBehaviour reactorBehaviour;

        private ScopedCTModel(BakedModel original, ReactorConnectedTextureBehaviour behaviour) {
            super(original, behaviour);
            reactorBehaviour = behaviour;
        }

        @Override
        protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world,
                                                    BlockPos pos, BlockState state, ModelData blockEntityData) {
            return withCapturedSnapshot(world, () -> super.gatherModelData(builder, world, pos, state, blockEntityData)
                    .with(SHARED_WINDOW_FACES, reactorBehaviour.sharedWindowFaces(world, pos, state)));
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random,
                                        ModelData data, RenderType renderType) {
            // 只读本次不可变ModelData，不读世界/ThreadLocal；缺失为0，不继承旧构建的有效掩码。
            Integer stored = data.has(SHARED_WINDOW_FACES) ? data.get(SHARED_WINDOW_FACES) : null;
            int mask = stored == null ? 0 : stored;
            if (side != null && (mask & (1 << side.get3DDataValue())) != 0) return List.of();
            List<BakedQuad> quads = super.getQuads(state, side, random, data, renderType);
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
        private boolean initialized;

        synchronized void initialize(BiFunction<ResourceLocation, ResourceLocation, CTSpriteShiftEntry> shifts,
                                     BiConsumer<ResourceLocation, NonNullFunction<BakedModel, ? extends BakedModel>> models) {
            if (initialized) return;
            Map<ResourceLocation, CTSpriteShiftEntry> created = new LinkedHashMap<>();
            for (String sprite : SPRITES) created.put(id("block/" + sprite), shifts.apply(id("block/" + sprite), id("block/reactor_ct/" + sprite)));
            entries = Map.copyOf(created);
            var behaviour = new ReactorConnectedTextureBehaviour(this::shiftForSprite);
            // 此处必须用block注册ID；Create负责把blockstate展开为所有模型路径，item表不参与。
            for (String block : BLOCKS) models.accept(id(block), original -> wrapModel(original, behaviour));
            initialized = true;
        }

        CTSpriteShiftEntry shiftForSprite(ResourceLocation id) {
            return entries.get(id);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
    }
}
