package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.content.BasicMaterialContent;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.component.CustomData;

/** 前后朝向仅控制观察窗；机器转轴固定竖直向下，五个非底面用于过滤与物料交互。 */
public final class CentrifugeBlock extends HorizontalKineticBlock implements IBE<CentrifugeBlockEntity>, IWrenchable {
    public CentrifugeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    public Class<CentrifugeBlockEntity> getBlockEntityClass() {
        return CentrifugeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CentrifugeBlockEntity> getBlockEntityType() {
        return FuelProcessingContent.CENTRIFUGE_BE.get();
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    /** 方块物品只携带一份机器快照；挖掘和 Create 扳手均从同一掉落入口取得它。 */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CentrifugeBlockEntity machine) {
            CompoundTag stateTag = machine.savePortableData();
            for (ItemStack drop : drops) {
                if (drop.is(FuelProcessingContent.ENRICHMENT_CENTRIFUGE_ITEM.get())) {
                    CompoundTag wrapper = new CompoundTag();
                    wrapper.put("CniCentrifuge", stateTag.copy());
                    drop.set(DataComponents.CUSTOM_DATA, CustomData.of(wrapper));
                }
            }
        }
        return drops;
    }

    /** 放置物品的便携快照只恢复库存、批次与磨损，不导入旧轴网状态。 */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CentrifugeBlockEntity machine) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && data.copyTag().contains("CniCentrifuge")) {
                machine.loadPortableData(data.copyTag().getCompound("CniCentrifuge"));
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof WrenchItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof CentrifugeBlockEntity machine)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Direction hitSide = hit.getDirection();
        if (machine.isFilterSlotHit(hitSide, hit)) {
            if (stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM) != null)
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.isEmpty()) {
            if (hitSide == Direction.DOWN) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (!level.isClientSide) machine.extractOutputsToPlayer(player, hitSide);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        Direction face = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (hitSide == face && stack.is(BasicMaterialContent.HEAVY_BEARING.get())) {
            if (!level.isClientSide && machine.repairBearing()) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (hitSide != Direction.DOWN && stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM) != null) {
            if (!level.isClientSide) machine.transferHeldBucket(player, hand, hitSide);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof BlockItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                BlockHitResult hit) {
        Direction side = hit.getDirection();
        if (!player.getMainHandItem().isEmpty() || side == Direction.DOWN
                || !(level.getBlockEntity(pos) instanceof CentrifugeBlockEntity machine))
            return InteractionResult.PASS;
        if (machine.isFilterSlotHit(side, hit)) return InteractionResult.PASS;
        if (!level.isClientSide) machine.extractOutputsToPlayer(player, side);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
