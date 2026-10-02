package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.production.CentrifugeBlock;
import com.iksxh.create_nuclear_industry.production.CentrifugeBlockEntity;
import com.iksxh.create_nuclear_industry.production.CentrifugeMenu;
import com.iksxh.create_nuclear_industry.production.CentrifugeRecipe;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** 铀原料和首台离心机的独立注册入口；只装配身份，不缓存机器运行状态。 */
public final class FuelProcessingContent {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateNuclearIndustry.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(
            NeoForgeRegistries.Keys.FLUID_TYPES, CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(
            BuiltInRegistries.FLUID, CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(
            BuiltInRegistries.MENU, CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(
            BuiltInRegistries.RECIPE_TYPE, CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(
            BuiltInRegistries.RECIPE_SERIALIZER, CreateNuclearIndustry.MOD_ID);

    public static final DeferredItem<Item> URANIUM_CONCENTRATE = ITEMS.registerSimpleItem("uranium_concentrate");
    public static final DeferredItem<Item> URANIUM_TAILINGS = ITEMS.registerSimpleItem("uranium_tailings");
    public static final DeferredItem<Item> LOW_ENRICHED_URANIUM_DUST = ITEMS.registerSimpleItem("low_enriched_uranium_dust");
    public static final DeferredItem<Item> DEPLETED_URANIUM_DUST = ITEMS.registerSimpleItem("depleted_uranium_dust");
    public static final DeferredBlock<Block> URANIUM_TAILINGS_BRICK = BLOCKS.register("uranium_tailings_brick",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> URANIUM_TAILINGS_BRICK_ITEM = ITEMS.register("uranium_tailings_brick",
            () -> new BlockItem(URANIUM_TAILINGS_BRICK.get(), new Item.Properties()));
    public static final DeferredBlock<CentrifugeBlock> ENRICHMENT_CENTRIFUGE = BLOCKS.register("enrichment_centrifuge",
            () -> new CentrifugeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(4.0f).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> ENRICHMENT_CENTRIFUGE_ITEM = ITEMS.register("enrichment_centrifuge",
            () -> new BlockItem(ENRICHMENT_CENTRIFUGE.get(), new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<FluidType, FluidType> URANIUM_SLURRY_TYPE = FLUID_TYPES.register("uranium_slurry",
            () -> new FluidType(FluidType.Properties.create().descriptionId("fluid_type.create_nuclear_industry.uranium_slurry")
                    .density(1100).viscosity(1300).temperature(300)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override
                        public ResourceLocation getStillTexture() {
                            return ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "fluid/uranium_slurry_still");
                        }

                        @Override
                        public ResourceLocation getFlowingTexture() {
                            return ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "fluid/uranium_slurry_flow");
                        }
                    });
                }
            });
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> URANIUM_SLURRY = FLUIDS.register("uranium_slurry",
            () -> new BaseFlowingFluid.Source(slurryProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> URANIUM_SLURRY_FLOWING = FLUIDS.register("uranium_slurry_flowing",
            () -> new BaseFlowingFluid.Flowing(slurryProperties()));
    /** 与冷却剂相同的原生液体方块；桶、工作盆和管道共用同一个源流体身份。 */
    public static final DeferredBlock<LiquidBlock> URANIUM_SLURRY_BLOCK = BLOCKS.register("uranium_slurry",
            () -> new LiquidBlock(URANIUM_SLURRY.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
    public static final DeferredItem<BucketItem> URANIUM_SLURRY_BUCKET = ITEMS.register("uranium_slurry_bucket",
            () -> new BucketItem(URANIUM_SLURRY.get(), new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE_BE =
            BLOCK_ENTITIES.register("enrichment_centrifuge",
                    () -> BlockEntityType.Builder.of(CentrifugeBlockEntity::new, ENRICHMENT_CENTRIFUGE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<CentrifugeMenu>> CENTRIFUGE_MENU = MENUS.register(
            "enrichment_centrifuge", () -> IMenuTypeExtension.create(CentrifugeMenu::new));
    public static final DeferredHolder<RecipeType<?>, RecipeType<CentrifugeRecipe>> CENTRIFUGING_TYPE =
            RECIPE_TYPES.register("centrifuging", () -> new RecipeType<>() {});
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CentrifugeRecipe>> CENTRIFUGING_SERIALIZER =
            RECIPE_SERIALIZERS.register("centrifuging", CentrifugeRecipe.Serializer::new);

    private FuelProcessingContent() {}

    /** 在模组总线上按注册对象的依赖次序安装身份；客户端视图由独立客户端事件安装。 */
    public static void register(IEventBus bus) {
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }

    private static BaseFlowingFluid.Properties slurryProperties() {
        return new BaseFlowingFluid.Properties(URANIUM_SLURRY_TYPE, URANIUM_SLURRY, URANIUM_SLURRY_FLOWING)
                .bucket(URANIUM_SLURRY_BUCKET).block(URANIUM_SLURRY_BLOCK)
                .slopeFindDistance(4).levelDecreasePerBlock(1).tickRate(5);
    }
}
