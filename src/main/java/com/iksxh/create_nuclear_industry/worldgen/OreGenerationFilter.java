package com.iksxh.create_nuclear_industry.worldgen;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 服务端矿物放置门：实际维度与当前方块标签共同判定是否生成。
 * 不缓存标签集合，不写世界；每个矿脉尝试链开始前读取已绑定标签。
 */
public final class OreGenerationFilter extends PlacementFilter {
    public static final MapCodec<OreGenerationFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TagKey.codec(Registries.BLOCK).fieldOf("ore_tag").forGetter(filter -> filter.oreTag),
            StringRepresentable.fromEnum(Mode::values).optionalFieldOf("mode", Mode.AUTO).forGetter(filter -> filter.mode)
    ).apply(instance, OreGenerationFilter::new));
    private static final DeferredRegister<PlacementModifierType<?>> TYPES =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, CreateNuclearIndustry.MOD_ID);
    private static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<OreGenerationFilter>> TYPE =
            TYPES.register("ore_generation", () -> () -> CODEC);
    private final TagKey<Block> oreTag;
    private final Mode mode;

    /** 数据包模式；强制开启仍遵循主世界限制，不会覆盖其他提供者的矿石。 */
    public enum Mode implements StringRepresentable {
        AUTO("auto"), ENABLED("enabled"), DISABLED("disabled");
        private final String name;
        Mode(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    /** codec入口；oreTag必须是方块标签，物品锭/粉标签不能触发去重。 */
    public OreGenerationFilter(TagKey<Block> oreTag, Mode mode) {
        this.oreTag = oreTag;
        this.mode = mode;
    }

    /** 注册本批唯一放置类型；数据包管理高度/次数/规模与模式，无独立TOML。 */
    public static void register(IEventBus bus) { TYPES.register(bus); }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        // WorldGenRegion.getLevel()返回实际ServerLevel；同群系的异维度仍被拒绝。
        if (!context.getLevel().getLevel().dimension().equals(Level.OVERWORLD) || mode == Mode.DISABLED) {
            return false;
        }
        if (mode == Mode.ENABLED) {
            return true;
        }
        // 不提前解析或缓存标签。缺少本批矿石标签时关闭生成，避免把未绑定数据当作“没有外部矿”。
        return BuiltInRegistries.BLOCK.getTag(oreTag).map(members -> members.stream().noneMatch(holder ->
                !BuiltInRegistries.BLOCK.getKey(holder.value()).getNamespace().equals(CreateNuclearIndustry.MOD_ID)
        )).orElse(false);
    }

    @Override public PlacementModifierType<?> type() { return TYPE.get(); }
}
