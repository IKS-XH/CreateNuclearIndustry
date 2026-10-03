package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * 八格归属与拆卸的服务端判定。所有查询只读已加载区块；缺件时主控账本继续保留，
 * 任何从属格都不能凭坐标推造一份库存或机器物品。
 */
public final class ShieldedAssemblyStructure {
    private static final ThreadLocal<Boolean> REMOVING = ThreadLocal.withInitial(() -> false);
    private ShieldedAssemblyStructure() {}

    public static boolean removing() { return REMOVING.get(); }

    public static ShieldedAssemblyBlockEntity master(Level level, BlockPos pos, BlockState state) {
        if (!level.hasChunkAt(pos)) return null;
        if (state.is(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get())) {
            if (state.getValue(ShieldedAssemblyBlock.EXPANDED)
                    && level.getBlockEntity(pos) instanceof ShieldedAssemblyBlockEntity master
                    && master.current()) return master;
            return null;
        }
        if (!state.is(FuelProcessingContent.SHIELDED_ASSEMBLY_PART.get())
                || !(level.getBlockEntity(pos) instanceof ShieldedAssemblyPartBlockEntity proxy)) return null;
        BlockPos masterPos = proxy.masterPos();
        if (masterPos == null || !level.hasChunkAt(masterPos)) return null;
        if (!(level.getBlockEntity(masterPos) instanceof ShieldedAssemblyBlockEntity master)
                || !master.current() || !master.matchesOwner(proxy.ownerId())) return null;
        Direction facing = master.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        int part = state.getValue(ShieldedAssemblyPartBlock.PART);
        return ShieldedAssemblyLayout.position(masterPos, facing, part).equals(pos)
                && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing ? master : null;
    }

    public static boolean complete(ShieldedAssemblyBlockEntity master) {
        Level level = master.getLevel();
        if (level == null || !master.current() || !master.expanded() || master.ownerId() == null) return false;
        Direction facing = master.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        for (int part = 1; part < 8; part++) {
            BlockPos pos = ShieldedAssemblyLayout.position(master.getBlockPos(), facing, part);
            if (!level.hasChunkAt(pos)) return false;
            BlockState state = level.getBlockState(pos);
            if (!state.is(FuelProcessingContent.SHIELDED_ASSEMBLY_PART.get())
                    || state.getValue(ShieldedAssemblyPartBlock.PART) != part
                    || state.getValue(BlockStateProperties.HORIZONTAL_FACING) != facing
                    || !(level.getBlockEntity(pos) instanceof ShieldedAssemblyPartBlockEntity proxy)
                    || !master.getBlockPos().equals(proxy.masterPos())
                    || !master.matchesOwner(proxy.ownerId())) return false;
        }
        return true;
    }

    public static boolean allLoaded(ShieldedAssemblyBlockEntity master) {
        Level level = master.getLevel();
        if (level == null) return false;
        Direction facing = master.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        for (int part = 0; part < 8; part++)
            if (!level.hasChunkAt(ShieldedAssemblyLayout.position(master.getBlockPos(), facing, part))) return false;
        return true;
    }

    /** 服务端把工作灯状态投影到已加载的合法外壳，代理不自行计算配方或工时。 */
    public static void syncWorking(ShieldedAssemblyBlockEntity master, boolean working) {
        Level level = master.getLevel();
        if (level == null || !master.expanded()) return;
        Direction facing = master.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        for (int part = 1; part < 8; part++) {
            BlockPos pos = ShieldedAssemblyLayout.position(master.getBlockPos(), facing, part);
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.is(FuelProcessingContent.SHIELDED_ASSEMBLY_PART.get())
                    && state.getValue(ShieldedAssemblyPartBlock.PART) == part
                    && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing
                    && level.getBlockEntity(pos) instanceof ShieldedAssemblyPartBlockEntity proxy
                    && master.getBlockPos().equals(proxy.masterPos()) && master.matchesOwner(proxy.ownerId())
                    && state.getValue(ShieldedAssemblyPartBlock.WORKING) != working)
                level.setBlock(pos, state.setValue(ShieldedAssemblyPartBlock.WORKING, working), 3);
        }
    }

    /** 先由调用者封存唯一机器物品，再只清理归属匹配且已经加载的其余七格。 */
    public static void removeParts(ShieldedAssemblyBlockEntity master) { removeParts(master, null); }

    public static void removeParts(ShieldedAssemblyBlockEntity master, BlockPos skip) {
        if (REMOVING.get() || !master.expanded()) return;
        Level level = master.getLevel();
        if (level == null) return;
        REMOVING.set(true);
        try {
            Direction facing = master.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            UUID owner = master.ownerId();
            for (int part = 1; part < 8; part++) {
                BlockPos pos = ShieldedAssemblyLayout.position(master.getBlockPos(), facing, part);
                if (pos.equals(skip)) continue;
                if (!level.hasChunkAt(pos)) continue;
                BlockState state = level.getBlockState(pos);
                if (state.is(FuelProcessingContent.SHIELDED_ASSEMBLY_PART.get())
                        && state.getValue(ShieldedAssemblyPartBlock.PART) == part
                        && level.getBlockEntity(pos) instanceof ShieldedAssemblyPartBlockEntity proxy
                        && master.getBlockPos().equals(proxy.masterPos()) && owner != null && owner.equals(proxy.ownerId())) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 35);
                    level.invalidateCapabilities(pos);
                }
            }
        } finally {
            REMOVING.remove();
        }
    }
}
