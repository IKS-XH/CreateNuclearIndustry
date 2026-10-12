package com.iksxh.create_nuclear_industry.industrial;

import com.iksxh.create_nuclear_industry.content.IndustrialFluidContent;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 独立专用动力泵；不注册原生FluidTransportBehaviour，轴向方向与Create泵一致。 */
public final class IndustrialPumpBlock extends DirectionalKineticBlock implements ICogWheel, IBE<IndustrialPumpBlockEntity> {
    public IndustrialPumpBlock(Properties properties) { super(properties); }
    @Override public Direction.Axis getRotationAxis(BlockState state) { return state.getValue(FACING).getAxis(); }
    @Override public boolean hasShaftTowards(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state, Direction face) { return face.getAxis() == getRotationAxis(state); }
    @Override public BlockState getRotatedBlockState(BlockState state, Direction face) { return state.setValue(FACING, state.getValue(FACING).getOpposite()); }
    public Class<IndustrialPumpBlockEntity> getBlockEntityClass() { return IndustrialPumpBlockEntity.class; }
    public BlockEntityType<? extends IndustrialPumpBlockEntity> getBlockEntityType() { return IndustrialFluidContent.PUMP_ENTITY.get(); }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state, level, pos, previous, moving);
        if (level instanceof net.minecraft.server.level.ServerLevel server) IndustrialNetworkScheduler.invalidate(server, pos);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return com.simibubi.create.AllShapes.PUMP.get(state.getValue(FACING)); }
    /** 唯一掉落携同份缓存；不再同时调用空泵loot，创造选取沿默认空BlockItem。 */
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack item = new ItemStack(IndustrialFluidContent.PUMP_ITEM.get());
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof IndustrialPumpBlockEntity pump && !pump.cachedFluid().isEmpty())
            item.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(pump.portableData()));
        return List.of(item);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack item) {
        super.setPlacedBy(level, pos, state, placer, item);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof IndustrialPumpBlockEntity pump) {
            CustomData data = item.get(DataComponents.BLOCK_ENTITY_DATA);
            if (data != null) {
                pump.restorePortable(data.copyTag());
                // 创造模式不消耗BlockItem数量；成功放置后清理该携液凭据，第二次只可放空泵。
                item.remove(DataComponents.BLOCK_ENTITY_DATA);
            }
        }
    }
    /** 创造拆除或扳手收起持液泵也只能转移一份缓存；创造选取不走此路径。 */
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof IndustrialPumpBlockEntity pump && !pump.cachedFluid().isEmpty()) {
            ItemStack item = new ItemStack(IndustrialFluidContent.PUMP_ITEM.get()); item.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(pump.portableData())); Block.popResource(level, pos, item);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
    @Override public net.minecraft.world.InteractionResult onSneakWrenched(BlockState state, net.minecraft.world.item.context.UseOnContext context) {
        if (!(context.getLevel() instanceof net.minecraft.server.level.ServerLevel server)) return net.minecraft.world.InteractionResult.SUCCESS;
        var player = context.getPlayer(); BlockPos pos = context.getClickedPos();
        var event = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(server, pos, state, player);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return net.minecraft.world.InteractionResult.SUCCESS;
        if (player != null && (!player.isCreative() || server.getBlockEntity(pos) instanceof IndustrialPumpBlockEntity pump && !pump.cachedFluid().isEmpty()))
            Block.getDrops(state, server, pos, server.getBlockEntity(pos), player, context.getItemInHand()).forEach(player.getInventory()::placeItemBackInInventory);
        state.spawnAfterBreak(server, pos, ItemStack.EMPTY, true); server.destroyBlock(pos, false);
        com.simibubi.create.content.equipment.wrench.IWrenchable.playRemoveSound(server, pos); return net.minecraft.world.InteractionResult.SUCCESS;
    }
}
